/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package application;


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.ChannelClientConfiguration;
import net.tomp2p.connection.StandardProtocolFamily;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.FutureRemove;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.BaseFutureListener;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.futures.FuturePeerConnection;
import net.tomp2p.nat.FutureNAT;
import net.tomp2p.nat.FutureRelayNAT;
import net.tomp2p.nat.PeerBuilderNAT;
import net.tomp2p.nat.PeerNAT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapConfiguration;
import net.tomp2p.relay.RelayConfig;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test bootstrapping, DHT operations like put/get/add/remove and sendDirect in both LAN and WAN environment
 * Test scenarios in direct connection, auto port forwarding or relay mode.
 * <p>
 * To start a bootstrap node code use the {@link net.tomp2p.examples.SeedNodeForTesting} class.
 * <p>
 * To configure your test environment edit the static fields for id, IP and port.
 * In the configure method and the connectionType you can define your test scenario.
 */
//@Ignore
public class P2PTest {
    private enum ConnectionType {
        UNKNOWN, DIRECT, NAT, RELAY
    }

    private static final Logger log = LoggerFactory.getLogger(P2PTest.class);

    final static String BOOTSTRAP_NODE_ID = "seed";
    private final static String BOOTSTRAP_NODE_IP = "127.0.0.1";
    final static int BOOTSTRAP_NODE_PORT = 8800;

    // If you want to test in one specific connection mode define it directly, otherwise use UNKNOWN
    private static final ConnectionType FORCED_CONNECTION_TYPE = ConnectionType.DIRECT;

    private static final PeerAddress BOOTSTRAP_NODE_ADDRESS;

    static {
        try {
            BOOTSTRAP_NODE_ADDRESS = new PeerAddress(
                    Number160.createHash(BOOTSTRAP_NODE_ID),
                    BOOTSTRAP_NODE_IP, BOOTSTRAP_NODE_PORT, BOOTSTRAP_NODE_PORT);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    private Peer peer;
    private PeerDHT peer1DHT;
    private PeerDHT peer2DHT;
    private int client1Port;
    private int client2Port;
    private ConnectionType resolvedConnectionType;

//    @Before
    public void setUp() {
        client1Port = 7777;
        client2Port = 7778;
    }

//    @After
    public void tearDown() {
        if (peer1DHT != null) {
            BaseFuture future = peer1DHT.shutdown();
            future.awaitUninterruptibly();
            future.awaitListenersUninterruptibly();
        }
        if (peer2DHT != null) {
            BaseFuture future = peer2DHT.shutdown();
            future.awaitUninterruptibly();
            future.awaitListenersUninterruptibly();
        }
        if (peer != null) {
            BaseFuture future = peer.shutdown();
            future.awaitUninterruptibly();
            future.awaitListenersUninterruptibly();
        }
    }
 
    //simulate peer 2
    public static void main(String[] args) throws IOException {
		PeerDHT peer = bootstrapDirectConnection(5777);
        FuturePut futurePut = peer.put(Number160.createHash("peer2")).data(new Data(peer.peer().peerAddress())).start();
        futurePut.awaitUninterruptibly();
        peer.peer().objectDataReply(new ObjectDataReply() {
            @Override
            public Object reply(PeerAddress sender, Object request) throws Exception {
                System.out.println(String.valueOf(request));
                return "pong";
            }
        });
	}


    // The sendDirect operation fails in port forwarding mode because most routers does not support NAT reflections.
    // So if both clients are behind NAT they cannot send direct message to each other.
    // That will probably be fixed in a future version of TomP2P
//    @Test
//    @Repeat(STRESS_TEST_COUNT)
    public void testSendDirectBetweenLocalPeers() throws Exception {
        if (FORCED_CONNECTION_TYPE != ConnectionType.NAT && resolvedConnectionType != ConnectionType.RELAY) {
//            peer1DHT = getDHTPeer(client1Port);
//            peer2DHT = getDHTPeer(client2Port);

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            final StringBuilder result = new StringBuilder();
            peer2DHT.peer().objectDataReply(new ObjectDataReply() {
                @Override
                public Object reply(PeerAddress sender, Object request) throws Exception {
                    countDownLatch.countDown();
                    result.append(String.valueOf(request));
                    return "pong";
                }
            });

            FuturePeerConnection futurePeerConnection = peer1DHT.peer().createPeerConnection(peer2DHT.peer()
                                                                                                     .peerAddress(), 500);
            FutureDirect futureDirect = peer1DHT.peer().sendDirect(futurePeerConnection).object("hallo").start();
            futureDirect.awaitUninterruptibly();


            countDownLatch.await(3, TimeUnit.SECONDS);
//            if (countDownLatch.getCount() > 0)
//                Assert.fail("The test method did not complete successfully!");

//            assertEquals("hallo", result.toString());
//            assertTrue(futureDirect.isSuccess());
//            log.debug(futureDirect.object().toString());
//            assertEquals("pong", futureDirect.object());
        }
    }

	private static PeerDHT bootstrapDirectConnection(int clientPort) {
		Peer peer = null;
//		Number160 peerId = new Number160(new Random(43L));
//		PeerMapConfiguration pmc = new PeerMapConfiguration(peerId).peerNoVerification();
//		PeerMap pm = new PeerMap(pmc);
//		ChannelClientConfiguration cc = PeerBuilder.createDefaultChannelClientConfiguration();
//		cc.maxPermitsTCP(100);
//		cc.maxPermitsUDP(100);
		
		try {
//			peer = new PeerBuilder(peerId).bindings(getBindings()).channelClientConfiguration(cc).peerMap(pm)
//					.ports(clientPort).start();
			peer = new PeerBuilder(new Number160(new Random(42L))).ports(clientPort).start();
			
		} catch (IOException e) {
			log.warn("Discover with direct connection failed. Exception = " + e.getMessage());
			e.printStackTrace();
//			return null;
		}
		
		FutureDiscover futureDiscover = peer.discover().peerAddress(BOOTSTRAP_NODE_ADDRESS).start();
		futureDiscover.awaitUninterruptibly();
		if (futureDiscover.isSuccess()) {
			log.info("Discover with direct connection successful. Address = " + futureDiscover.peerAddress());

			FutureBootstrap futureBootstrap = peer.bootstrap().peerAddress(BOOTSTRAP_NODE_ADDRESS).start();
			futureBootstrap.awaitUninterruptibly();
			if (futureBootstrap.isSuccess()) {
				return new PeerBuilderDHT(peer).start();
			} else {
				log.warn("Bootstrap failed. Reason = " + futureBootstrap.failedReason());
				peer.shutdown().awaitUninterruptibly();
				return null;
			}
		} else {
			log.warn("Discover with direct connection failed. Reason = " + futureDiscover.failedReason());
			peer.shutdown().awaitUninterruptibly();
			return null;
		}
	}

	private static Bindings getBindings() {
        Bindings bindings = new Bindings();
        bindings.addProtocol(StandardProtocolFamily.INET);
        return bindings;
    }
}
