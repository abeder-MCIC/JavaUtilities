package com.mcic.util;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.filechooser.FileNameExtensionFilter;



public class CSVParserOld {
	private InputStream in;
	private String path;
	
	public CSVParserOld(File f) throws FileNotFoundException{
		in = new FileInputStream(f);
		path = f.getPath();
	}
	
	public void close(){
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String[] readNext(){
		String[] out = null;
		try {
			Vector<String> fields = new Vector<String>();
			char c = '0';
			boolean cont = true;
			boolean inQuote = false;
			boolean doReadNext = true;
			char[] fieldBuffer = new char[32768];
			int i = 0;
			int n = 0;
			while (cont){
				if (doReadNext){
					n = in.read();
				}
				c = (char)n;
				doReadNext = true;
				
				if (n == -1){
					if (i > 0){
						fields.add(new String(fieldBuffer, 0, i));
					}
					cont = false;
				} else {
					if (inQuote){
						if (n == '"'){
							int next = in.read();
							if (next == '"'){
								fieldBuffer[i++] = '"';
							} else {
								inQuote = false;
								n = next;
								doReadNext = false;
							}
						} else {
							fieldBuffer[i++] = (char)n;
						}
					} else {
						if (n == ','){
							fields.add(new String(fieldBuffer, 0, i));
							i = 0;
						} else if (n == '\n'){
							fields.add(new String(fieldBuffer, 0, i));
							i = 0;
							cont = false;
						} else if (n == '"'){
							inQuote = true;
						} else if (n == '\r' || n == '\t'){
							//  Do nothing
						} else {
							fieldBuffer[i++] = (char)n;
						}
					}
				}
			}
			
			//  Line has been parsed
			if (fields.size() > 0){
				out = fields.toArray(new String[fields.size()]);
			}
		} catch (IOException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out;
	}

	
	
	public String[] readNextOld(){
		String[] out = null;
		try {
			Vector<String> fields = new Vector<String>();
			char c = '0';
			boolean cont = true;
			boolean inQuote = false;
			boolean doReadNext = true;
			CharArrayWriter thisField = new CharArrayWriter();
			int n = 0;
			while (cont){
				if (doReadNext){
					n = in.read();
				}
				c = (char)n;
				doReadNext = true;
				
				if (n == -1){
					if (thisField.size() > 0){
						fields.add(thisField.toString());
					}
					cont = false;
				} else {
					if (inQuote){
						if (n == '"'){
							int next = in.read();
							if (next == '"'){
								thisField.append('"');
							} else {
								inQuote = false;
								n = next;
								doReadNext = false;
							}
						} else {
							thisField.append((char)n);
						}
					} else {
						if (n == ','){
							fields.add(thisField.toString());
							thisField.reset();
						} else if (n == '\r'){
							fields.add(thisField.toString());
							thisField.reset();
							cont = false;
						} else if (n == '"'){
							inQuote = true;
						} else if (n == '\n' || n == '\t'){
							//  Do nothing
						} else {
							thisField.append((char)n);
						}
					}
				}
			}
			
			//  Line has been parsed
			if (fields.size() > 0){
				out = fields.toArray(new String[fields.size()]);
			}
		} catch (IOException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out;
	}
	
	public String getPath() {
		return path;
	}

}
