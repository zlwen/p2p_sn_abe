package test;

import java.io.IOException;
import java.util.Random;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.RequestP2PConfiguration;
import net.tomp2p.p2p.RoutingConfiguration;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapConfiguration;
import net.tomp2p.storage.Data;

import org.apache.log4j.PropertyConfigurator;

public class P2P {
	static int port = 12221;
	static {
		String file = "log4j/log4j.properties";
		PropertyConfigurator.configure(file);
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Peer p1 = createPeer(12221);
		Peer p2 = createPeer(21112);
		p1.peerBean().peerMap().peerFound(p2.peerAddress(), null, null, null);
		p2.peerBean().peerMap().peerFound(p1.peerAddress(), null, null, null);

		FutureBootstrap bootstrap = p1.bootstrap()
				.peerAddress(p2.peerAddress()).start();
		bootstrap.awaitUninterruptibly();
		System.out.println("boot success:" + bootstrap.isSuccess());
		if (bootstrap.isSuccess()) {
			RoutingConfiguration rc = new RoutingConfiguration(2, 10, 2);
			RequestP2PConfiguration pc = new RequestP2PConfiguration(3, 5, 0);
			final Data data1 = new Data(new byte[1]);
			data1.ttlSeconds(3);
			FuturePut futurePut = new PeerBuilderDHT(p1).start().put(p1.peerID())
					.domainKey(Number160.createHash("test"))
					.routingConfiguration(rc)
					.requestP2PConfiguration(pc)
					.data(new Number160(5),new Data("abc")).start();
//			FuturePut futurePut = new PeerBuilderDHT(p1).start().put(Number160.createHash("test")).domainKey(Number160.createHash("test")).data(new Data("hello")).start();
			futurePut.awaitUninterruptibly();
			futurePut.futureRequests().awaitUninterruptibly();
			System.out.println(futurePut.isSuccess());
//			FutureGet futureGet = new PeerBuilderDHT(p2).start().get(Number160.createHash("test")).domainKey(Number160.createHash("test")).start();
			FutureGet futureGet = new PeerBuilderDHT(p2).start().get(p1.peerID())
					.domainKey(Number160.createHash("test"))
					.contentKey(new Number160(5))
					.routingConfiguration(rc)
					.requestP2PConfiguration(pc)
					.start();
			futureGet.awaitUninterruptibly();
			System.out.println(futureGet.data().object());
		}
		// }
	}

	private static Peer createPeer(int port) throws IOException {
		Random r = new Random();
		Number160 peerId = new Number160(r);
		PeerMap peerMap = new PeerMap(new PeerMapConfiguration(peerId));
		return new PeerBuilder(peerId).ports(port).peerMap(peerMap).start();
	}
}
