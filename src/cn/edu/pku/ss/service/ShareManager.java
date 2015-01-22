package cn.edu.pku.ss.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.edu.pku.ss.bean.User;
import cn.edu.pku.ss.crypto.abe.CPABE;

import com.turn.ttorrent.common.Torrent;

public class ShareManager {
	private Map<String, Torrent> sharedFiles;
	private static final URI ANNOUNCE = URI.create("http://192.168.1.100:8080/announce");
	private User user;

	public ShareManager(User user){
		sharedFiles = new HashMap<String, Torrent>();
		this.user = user;
	}
	
	public void share(File f, String p){
		try {
			File enc = CPABE.enc(f, p, user.getPubKey());
			Torrent torrent = Torrent.create(enc, ANNOUNCE, user.getEmail());
			sharedFiles.put(enc.getCanonicalPath(), torrent);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<String> getSharedFile(){
		return sharedFiles.keySet();
	}
}
