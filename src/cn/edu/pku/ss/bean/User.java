package cn.edu.pku.ss.bean;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import javafx.concurrent.Task;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.pku.ss.crypto.abe.CPABE;
import cn.edu.pku.ss.crypto.abe.CPABE.KeyPair;
import cn.edu.pku.ss.crypto.abe.MasterKey;
import cn.edu.pku.ss.crypto.abe.PublicKey;
import cn.edu.pku.ss.service.FriendManager;
import cn.edu.pku.ss.service.ShareManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;


public class User {
	private Logger logger = LoggerFactory.getLogger(User.class);
	private static User self;
	private String email;
	private String nickName;
	private String ip;
	private PublicKey pubKey;
	private MasterKey masterKey;
	private Peer peer;
	private FriendManager friendManager;
	private ShareManager shareManager;
	private static double workDone;

	private User() {

	}

	public static synchronized User self() {
		if (self == null) {
			self = new User();
			self.friendManager = new FriendManager();
			self.shareManager = new ShareManager(self);
		}

		return self;
	}
	
	public ShareManager getShareManager() {
		return shareManager;
	}

	public static class Init extends Task<Void>{
		@Override
		protected Void call() throws Exception {
			updateProgress(workDone, 100);
			return null;
		}
	}

	public synchronized void init() {
		abeSetup();
		p2pSetup();
	}

	private void p2pSetup(){
		Random r = new Random();
		try {
			peer = new PeerMaker(new Number160(r)).setPorts(4000).makeAndListen();
		} catch (IOException e) {
			e.printStackTrace();
		}
        FutureBootstrap fb = peer.bootstrap().setBroadcast().setPorts(4001).start();
        fb.awaitUninterruptibly();
        if (fb.getBootstrapTo() != null) {
            peer.discover().setPeerAddress(fb.getBootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }
        
        ip = getLocalIPAddress();
        JSONObject json = new JSONObject();
        json.put("ip", ip);
        json.put("nickName", nickName);
        dhtPut(Number160.createHash(email), json.toJSONString());
	}

	// TODO 换一种方式实现
	private String getLocalIPAddress() {
		// 通过这种方法可以获取本机IP,而且不是虚拟机的IP地址
		String res = null;
		Socket s;
		try {
			s = new Socket("www.baidu.com", 80);
			res = s.getLocalAddress().getHostAddress();
			s.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	private void abeSetup() {
		KeyPair res = CPABE.setup();
		this.pubKey = res.getPK();
		this.masterKey = res.getMK();
	}

	public synchronized String getEmail() {
		return email;
	}

	public synchronized void setEmail(String email) {
		this.email = email;
	}

	public synchronized String getNickName() {
		return nickName;
	}

	public synchronized void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public synchronized PublicKey getPubKey() {
		return pubKey;
	}

	public synchronized MasterKey getMasterKey() {
		return masterKey;
	}

	public FriendManager getFriendManager() {
		return friendManager;
	}
	
	private JSONObject getFriendInfo(String email){
		Object o = dhtGet(Number160.createHash(email));
		if(o == null){
			logger.warn("User{email=" + email + "} seems not in this p2p network!");
			return null;
		}
		JSONObject json = JSON.parseObject((String)o);
		logger.info("find user{email=" + email + "} --> ip:" + json.getString("ip"));
		return json;
	}
	
	public void shareFile(File f, String policy){
		shareManager.share(f, policy);
	}
	
	public boolean addFriend(String email){
		JSONObject json = getFriendInfo(email);
		String ip = json.getString("ip");
		InetAddress address = null;
		try {
			address = Inet4Address.getByName(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		FutureDiscover futureDiscover = peer.discover().setInetAddress(address).setPorts(4000).start();
		futureDiscover.awaitUninterruptibly();
		
		return true;
	}

	//DHT method get/put
	private Object dhtGet(Number160 key) {
		FutureDHT futureDHT = peer.get(key).start();
		futureDHT.awaitUninterruptibly();
		if (futureDHT.isSuccess()) {
			try {
				return futureDHT.getData().getObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private void dhtPut(Number160 key, Object value) {
		try {
			peer.put(key).setData(new Data(value)).start()
					.awaitUninterruptibly();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
