package com.mcic.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class BlockRecordSet implements RecordSet {
	List<String> lines;
	String header;
	String line;
	boolean isHeader;
	
	public BlockRecordSet() {
		lines = new LinkedList<String>();
		isHeader = true;
		line = "";
		header = "";
	}

	@Override
	public void add(String key, String val) {
		if (isHeader) {
			header += key + ",";
		}
		if (val.matches("[\\s\\S]*[\"\\n][\\s\\w]*") || val.contains(",")) {
			val = val.replaceAll("\"", "\"\"");
			line += "\"" + val + "\",";
		} else {
			line += val + ",";
		}
	}

	@Override
	public void next() {
		if (isHeader) {
			isHeader = false;
			header = header.substring(0, header.length() - 1) + "\n";
		}
		line = line.substring(0, line.length() - 1) + "\n";
		lines.add(line);
		line = "";
	}

	@Override
	public String toBase64() {
		ByteArrayOutputStream byteSteam = new ByteArrayOutputStream();
		GZIPOutputStream gzipOS = null;
		try {
			gzipOS = new GZIPOutputStream(byteSteam);
			gzipOS.write(header.getBytes());
			for (String line : lines) {
				gzipOS.write(line.getBytes());
			}
			byte[] zipped = byteSteam.toByteArray();
			return Base64.getEncoder().encodeToString(zipped);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
