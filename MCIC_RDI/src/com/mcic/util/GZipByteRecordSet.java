package com.mcic.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class GZipByteRecordSet extends RecordSet {
	ByteArrayOutputStream out;
	GZIPOutputStream gzipOS;
	Map<String, Integer> headers;
	boolean isFirstValue;
	
	public GZipByteRecordSet() {
		headers = new LinkedHashMap<String, Integer>();
		out = new ByteArrayOutputStream();
		gzipOS = null;
		try {
			gzipOS = new GZIPOutputStream(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isFirstValue = true;
	}

	@Override
	public void add(String key, String val) {
		
		try {
			if (isFirstValue) {
				isFirstValue = false;
			} else {
				out.write(",".getBytes());
			}
			if (val.matches("[\\s\\S]*[\"\\n][\\s\\w]*")) {
				val = val.replaceAll("\"", "\"\"");
				out.write("\"".getBytes());
				out.write(val.getBytes());
				out.write("\"".getBytes());
			} else {
				out.write(val.getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	@Override
	public byte[] toBytes() {
		// TODO Auto-generated method stub
		return out.toByteArray();
	}

	@Override
	public void next() {
		isFirstValue = true;
		try {
			out.write("\r\n".getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
