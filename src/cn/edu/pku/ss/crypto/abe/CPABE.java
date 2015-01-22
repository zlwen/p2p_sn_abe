package cn.edu.pku.ss.crypto.abe;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.pku.ss.crypto.abe.SecretKey.SKComponent;
import cn.edu.pku.ss.crypto.abe.serialize.SerializeUtils;
import cn.edu.pku.ss.crypto.aes.AES;
import cn.edu.pku.ss.pairing.PairingManager;

public class CPABE{
	private static Logger logger = LoggerFactory.getLogger(CPABE.class);
	private static Pairing pairing = PairingManager.defaultPairing;
	private static Parser parser = new Parser();
	public static boolean debug = false;
	
	public static class KeyPair{
		PublicKey PK;
		MasterKey MK;

		public PublicKey getPK() {
			return PK;
		}
		public MasterKey getMK() {
			return MK;
		}
	}
	
	public static KeyPair setup(){
		PublicKey PK = new PublicKey();
		MasterKey MK = new MasterKey();
		
		Element alpha  = pairing.getZr().newElement().setToRandom();
		PK.g           = pairing.getG1().newElement().setToRandom();
		PK.gp          = pairing.getG2().newElement().setToRandom();
		MK.beta        = pairing.getZr().newElement().setToRandom();
		MK.g_alpha     = PK.gp.duplicate().powZn(alpha);
		PK.h           = PK.g.duplicate().powZn(MK.beta);
		PK.g_hat_alpha = pairing.pairing(PK.g, MK.g_alpha);
		
		KeyPair res = new KeyPair();
		res.PK = PK;
		logger.info("generate Public Key: " + PK);
		res.MK = MK;
		logger.info("generate Master Key: " + MK);
		return res;
	}
	
	public static SecretKey keygen(String[] attrs, PublicKey PK, MasterKey MK, String SKFileName){
		SecretKey SK = new SecretKey();
		Element r = pairing.getZr().newElement().setToRandom();
		Element g_r = PK.gp.duplicate().powZn(r);
		SK.D = MK.g_alpha.duplicate().mul(g_r);
		Element beta_inv = MK.beta.duplicate().invert();
		SK.D.powZn(beta_inv);
		SK.comps = new SecretKey.SKComponent[attrs.length];
		
		for(int i=0; i<attrs.length; i++){
			Element rj = pairing.getZr().newElement().setToRandom();
			Element hash = pairing.getG2().newElementFromBytes(attrs[i].getBytes()).powZn(rj);
			SK.comps[i] = new SecretKey.SKComponent();
			SK.comps[i].attr = attrs[i];
			SK.comps[i].Dj = g_r.mul(hash);
			SK.comps[i]._Dj = PK.gp.duplicate().powZn(rj);
		}
		
		logger.info("For attributes: " + Arrays.toString(attrs) + " generate Secret Key: " + SK);
		return SK;
	}
	
	public static File createNewFile(String fileName){
		File file = new File(fileName);
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			try {
				String path = file.getCanonicalPath();
				file.delete();
				file = new File(path);
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	
	public static File enc(File file, String p, PublicKey PK){
		String name = null;
		Policy policy = parser.parse(p);
		try {
			name = file.getCanonicalPath();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		File ciphertextFile = createNewFile(name + ".cpabe");
		Element m = PairingManager.defaultPairing.getGT().newRandomElement();
		Element s = pairing.getZr().newElement().setToRandom();
		fill_policy(policy, s, PK);
		Ciphertext ciphertext = new Ciphertext();
		ciphertext.p = policy;
		ciphertext.Cs = m.duplicate().mul(PK.g_hat_alpha.duplicate().powZn(s));
		ciphertext.C = PK.h.duplicate().powZn(s); 
		
		SerializeUtils.serialize(ciphertext, ciphertextFile);
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(file);
			fos = new FileOutputStream(ciphertextFile, true);
			AES.crypto(Cipher.ENCRYPT_MODE, fis, fos, m);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally{
			try {
				fis.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ciphertextFile;
	}
	
	public static boolean dec(File ciphertextFile, SecretKey SK, PublicKey PK){
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(ciphertextFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Ciphertext ciphertext = SerializeUtils._unserialize(Ciphertext.class, dis);
		if(ciphertext == null){
			logger.error("ciphertext is null, check the encrypted file!");
			return false;
		}
		check_sat(SK, ciphertext.p);
		if(ciphertext.p.satisfiable != 1){
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			for(SKComponent comp : SK.comps){
				sb.append(comp.attr);
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]");
			logger.info("SK with attrs" + sb.toString() + " does not satisfy the policy in ciphertext!");
			return false;
		}
		pick_sat_min_leaves(ciphertext.p, SK);
		Element r = dec_flatten(ciphertext.p, SK);
		Element m = ciphertext.Cs.mul(r);
		r = pairing.pairing(ciphertext.C, SK.D);
		r.invert();
		m.mul(r);

		String output = null;
		String name = null;
		try {
			name = ciphertextFile.getCanonicalPath();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(name.endsWith(".cpabe")){
			int end = name.indexOf(".cpabe");
			output = name.substring(0, end);
		}
		else{
			output = name + ".out";
		}
	
		File outputFile = createNewFile(output);
		OutputStream os = null;
		try {
			os =  new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		AES.crypto(Cipher.DECRYPT_MODE, dis, os, m);
		return true;
	}
	
	public static Element dec(Ciphertext ciphertext, SecretKey SK, PublicKey PK){
		check_sat(SK, ciphertext.p);
		if(ciphertext.p.satisfiable != 1){
			return null;
		}
		pick_sat_min_leaves(ciphertext.p, SK);
		Element r = dec_flatten(ciphertext.p, SK);
		Element m = ciphertext.Cs.mul(r);
		r = pairing.pairing(ciphertext.C, SK.D);
		r.invert();
		m.mul(r);
		
		return m;
	}
	
	private static Element dec_flatten(Policy p, SecretKey SK){
		Element r = pairing.getGT().newElement().setToOne();
		Element one = pairing.getZr().newElement().setToOne();
		dec_node_flatten(r, one, p, SK);
		return r;
	}
	
	private static void dec_node_flatten(Element r, Element exp, 
			Policy p, SecretKey SK){
		assert(p.satisfiable == 1);
		if(p.children == null || p.children.length == 0){
			dec_leaf_flatten(r, exp, p, SK);
		}
		else{
			dec_internal_flatten(r, exp, p, SK);
		}
	}
	
	private static void dec_leaf_flatten(Element r, Element exp, 
			Policy p, SecretKey SK){
		SecretKey.SKComponent comp = SK.comps[p.attri];
		Element s = pairing.pairing(p.Cy, comp.Dj);
		Element t = pairing.pairing(p._Cy, comp._Dj);
		t.invert();
		s.mul(t);
		s.powZn(exp);
		r.mul(s);
	}
	
	private static void dec_internal_flatten(Element r, Element exp,
			 Policy p, SecretKey SK){
		int i;
		Element t;
		Element expnew;
		Element zero = pairing.getZr().newElement().setToZero();
		
		for(i=0; i<p.satl.size(); i++){
			t = lagrange_coef(p.satl, p.satl.get(i), zero);
			expnew = exp.duplicate().mul(t);    //ע�������duplicate
			dec_node_flatten(r, expnew, p.children[p.satl.get(i)-1], SK);
		}
	}

	
	private static void pick_sat_min_leaves(Policy p, SecretKey SK){
		int i,k,l;
		int size = p.children == null ? 0 : p.children.length;
		Integer[] c;
		assert(p.satisfiable == 1);
		if(p.children == null || p.children.length == 0){
			p.min_leaves = 1;
		}
		else{
			for(i=0; i<p.children.length; i++){
				if(p.children[i].satisfiable == 1){
					pick_sat_min_leaves(p.children[i], SK);
				}
			}
			
			c = new Integer[p.children.length];
			for(i=0; i<size; i++){
				c[i] = i;
			}
			
			Arrays.sort(c, new PolicyInnerComparator(p));
			p.satl = new ArrayList<Integer>();
			p.min_leaves = 0;
			l = 0;
			for(i=0; i<size && l<p.k; i++){
				if(p.children[i].satisfiable == 1){
					l++;
					p.min_leaves += p.children[i].min_leaves;
					k = c[i] + 1;
					p.satl.add(k);
				}
			}
			assert(l == p.k);
		}
	}
	
	private static class PolicyInnerComparator implements Comparator<Integer>{
		Policy p;
		public PolicyInnerComparator(Policy p){
			this.p = p;
		}
		
		@Override
		public int compare(Integer o1, Integer o2) {
			int k, l;
			k = p.children[o1].min_leaves;
			l = p.children[o2].min_leaves;
			return k < l ? -1 : k == l ? 0 : 1;
		}
		
	}
	
	private static void check_sat(SecretKey SK, Policy p){
		int i,l;
		int size = p.children == null ? 0 : p.children.length;
		p.satisfiable = 0;
		if(p.children == null || size == 0){
			for(i=0; i<SK.comps.length; i++){
				if(SK.comps[i].attr.equals(p.attr)){
					p.satisfiable = 1;
					p.attri = i;
					break;
				}
			}
		}
		else{
			for(i=0; i<size; i++){
				check_sat(SK, p.children[i]);
			}
			
			l = 0;
			for(i=0; i<size; i++){
				if(p.children[i].satisfiable == 1){
					l++;
				}
			}
			if(l >= p.k){
				p.satisfiable = 1;
			}
		}
	}
	
	
	public static void fill_policy(Policy p, Element e, PublicKey PK){
		int i;
		int size = p.children == null ? 0 : p.children.length;
		Element r, t;
		p.q = rand_poly(p.k - 1, e);
		if(p.children == null || size == 0){
			p.Cy = PK.g.duplicate().powZn(p.q.coef.get(0));
			p._Cy = pairing.getG1().newElementFromBytes(p.attr.getBytes()).powZn(p.q.coef.get(0));
		}
		else{
			for(i=0; i<size; i++){
				r = pairing.getZr().newElement().set(i+1);
				t = Polynomial.eval_poly(p.q, r);
				fill_policy(p.children[i], t, PK);
			}
		}
	}
	
	public static Polynomial rand_poly(int deg, Element zero_val){
		int i;
		Polynomial q = new Polynomial();
		q.deg = deg;
		q.coef = new ArrayList<Element>();

		q.coef.add(zero_val.duplicate());
		for(i=1; i<q.deg+1; i++){
			q.coef.add(pairing.getZr().newElement().setToRandom());
		}
		
		return q;
	}
	
	public static Element lagrange_coef(List<Integer> S, int i, Element x){
		int j,k;
		Element r = pairing.getZr().newElement().setToOne();
		Element t;
		for(k=0; k<S.size(); k++){
			j = S.get(k);
			if(j == i){
				continue;
			}
			t = x.duplicate().sub(pairing.getZr().newElement().set(j));   //ע�������duplicate
			r.mul(t);
			t.set(i-j).invert();
			r.mul(t);
		}
		
		return r;
	}
}
