package cn.edu.pku.ss.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
	
	public static byte[] toByteArray(File f) throws IOException {
		if(!f.exists()){
			return null;
		}
		
		byte[] res = new byte[(int)f.length()];
		InputStream is = new FileInputStream(f);
		is.read(res);
		return res;
	}
}
