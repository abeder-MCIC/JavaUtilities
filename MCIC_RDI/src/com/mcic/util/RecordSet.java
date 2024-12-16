package com.mcic.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

public class RecordSet {
	Map<String, Vector<String[]>> blocks;
	int record;
	int blockPtr;
	int recordPtr;
	int currentBlockSize;
	static int BLOCK_SIZE = 100000;
	byte[] bytes;
	Base64.Encoder encoder;
	String encoded;
	
	public RecordSet() {
		blocks = new LinkedHashMap<String, Vector<String[]>>();
		clear();
		encoder = Base64.getEncoder();
		encoded = null;
	}
	
	public void clear() {
		record = blockPtr = recordPtr = 0;
		currentBlockSize = BLOCK_SIZE;
		for (Vector<String[]> v : blocks.values()) {
			v.clear();
			v.add(new String[BLOCK_SIZE]);
		}
		bytes = null;
	}
	
	public void add(String key, String value) {
		Vector<String[]> blockList = blocks.get(key);
		if (blockList == null) {
			blockList = new Vector<String[]>();
			blocks.put(key, blockList);
		}
		String[] block = blockList.elementAt(blockPtr);
		block[blockPtr] = value;
	}
	
	public void next() {
		record ++;
		recordPtr ++;
		if (recordPtr >= currentBlockSize) {
			recordPtr = 0;
			for (Vector<String[]> blockList : blocks.values()) {
				blockList.add(new String[BLOCK_SIZE]);
				blockPtr = blockList.size() - 1;
				currentBlockSize = BLOCK_SIZE;
			}
		}
		bytes = null;
		encoded = null;
	}
	
	public byte[] toBytes() {
		if (bytes == null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				toStream(out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bytes = out.toByteArray();
		}
		return bytes;
	}
	
	public int size() {
		return toBase64(0, 0).length() * 2;  //  Strings in Java are two bytes
	}
	
	public String toBase64(int start, int length) {
		start /= 2; //  Strings in Java are two bytes
		length /= 2; //  Strings in Java are two bytes
		
		if (encoded == null) {
			byte[] bytes = toBytes();
			encoded = encoder.encodeToString(bytes);
		}
		if (length == 0) {
			return encoded;
		} else {
			int len = encoded.length();
			int l = start + length > len ? len - start : length;
			return encoded.substring(start, start + l);
		}
	}
	
	
	private void toStream(OutputStream s) throws IOException {
		//  Scrape headers
		String[] headers = new String[blocks.size()];
		Vector<String[]>[] blockLists = (Vector<String[]>[])(new Vector[blocks.size()]);
		int i = 0;
		for (String key : blocks.keySet()) {
			headers[i] = key;
			blockLists[i++] = blocks.get(key);
		}
		int hlen = headers.length;
		
		//  Write headers
		for (i = 0;i < headers.length;i++) {
			s.write(headers[i].getBytes());
			s.write(i == headers.length - 1 ? "\r\n".getBytes() : ",".getBytes());
		}
		
		//  Write records
		int blockPtr = -1;
		int recordPtr = 0;
		int currentBlockSize = 0;
		String[][] currentBlocks = new String[hlen][];
		i = 0;
		while (i < record) {
			//  Do we need to move to the next block?
			if (recordPtr >= currentBlockSize) {
				blockPtr ++;
				for (int j = 0;j < headers.length;j++) {
					currentBlocks[j] = blockLists[j].elementAt(blockPtr);
				}
				recordPtr = 0;
				currentBlockSize = currentBlocks[0].length;
			}
			
			//  Write CSV data
			for (int j = 0;j < headers.length;j++) {
				String val = currentBlocks[j][recordPtr];
				if (val.matches("[\\s\\S]*[\"\\n][\\s\\w]*")) {
					val = val.replaceAll("\"", "\"\"");
					s.write("\"".getBytes());
					s.write(val.getBytes());
					s.write("\"".getBytes());
				} else {
					s.write(val.getBytes());
				}
				s.write(j == headers.length - 1 ? "\r\n".getBytes() : ",".getBytes());
			}
			
			//  Advance to the next record
			i ++;
			recordPtr  ++;
		}
	}

	public RecordSet partition(int start, int length) {
		RecordSet out = new RecordSet();
		for (String key : blocks.keySet()) {
			out.blocks.put(key, new Vector<String[]>());
		}
		int startBlock = start / BLOCK_SIZE;
		int startRecord = start - (startBlock * BLOCK_SIZE);
		int thisBlockPtr = startBlock;
		int thisBlockSize = BLOCK_SIZE;
		while (length > 0) {
			for (Entry<String, Vector<String[]>> es : blocks.entrySet()) {
				String[] block = es.getValue().elementAt(thisBlockPtr);
				thisBlockSize = block.length - startRecord;
				if (thisBlockSize > length) {
					//  We need to copy only up to a certain place in the block
					thisBlockSize = length;
				}
				if (startRecord == 0 && thisBlockSize == block.length - startRecord) {
					//  We can point to the entire block
					es.getValue().add(block);
				} else {
					//  We can copy the entire block from startRecord for thisBlockSize
					String[] newBlock = new String[thisBlockSize];
					es.getValue().add(newBlock);
					for (int ptr = 0;ptr + startRecord < thisBlockSize;ptr++) {
						newBlock[ptr] = block[ptr + startRecord];
					}
				}
			}
			
			//  Move on to the next block
			out.record += thisBlockSize;
			length -= thisBlockSize;
			thisBlockPtr ++;
		}
		return out;
	}
}
