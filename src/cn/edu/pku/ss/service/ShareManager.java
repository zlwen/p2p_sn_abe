package cn.edu.pku.ss.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.pku.ss.bean.User;
import cn.edu.pku.ss.crypto.abe.CPABE;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;

public class ShareManager {
	private Map<String, Torrent> sharedFiles;
	private static final URI ANNOUNCE = URI
			.create("http://192.168.1.100:8080/announce");
	private User user;
	private ExecutorService threadPool = Executors.newFixedThreadPool(5);

	public ShareManager(User user) {
		sharedFiles = new HashMap<String, Torrent>();
		this.user = user;
	}

	private class ShareTask implements Runnable {
		private File f;
		private String p;

		public ShareTask(File f, String p) {
			this.f = f;
			this.p = p;
		}

		@Override
		public void run() {
			FileOutputStream os = null;
			try {
				File enc = CPABE.enc(f, p, user.getPubKey());
				File dir = enc.getParentFile();
				Torrent torrent = Torrent
						.create(enc, ANNOUNCE, user.getEmail());
				File torrentFile = new File(enc.getCanonicalPath() + ".torrent");
				if (!torrentFile.exists()) {
					torrentFile.createNewFile();
				}
				os = new FileOutputStream(torrentFile);
				torrent.save(os);
				sharedFiles.put(enc.getCanonicalPath(), torrent);
				SharedTorrent sharedTorrent = new SharedTorrent(torrent, dir);
				Client client = new Client(
						Inet4Address.getByName(user.getIp()), sharedTorrent);
				client.share();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void share(File f, String p) {
		threadPool.execute(new ShareTask(f,p));
	}

	public Set<String> getSharedFile() {
		return sharedFiles.keySet();
	}
}
