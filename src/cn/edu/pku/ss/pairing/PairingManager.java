package cn.edu.pku.ss.pairing;

import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class PairingManager {
	public static final String TYPE_A = "pairing/params/curves/a.properties";
	public static final Pairing defaultPairing = PairingFactory.getPairing(TYPE_A);
}
