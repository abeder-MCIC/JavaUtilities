package com.mcic.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;

public class CSVColumnCount {

	public static void main(String[] args) {
		File inFile = null;
		if (args.length > 1){
			inFile = new File(args[0]);
		} else {
			JFileChooser ch = new JFileChooser();
			ch.setCurrentDirectory(new File("C:\\temp\\Workingspace"));
			ch.setDialogTitle("CSV File to Read");
			ch.showSaveDialog(null);
			inFile = ch.getSelectedFile();
		}
		
		try {
			CSVAuthor out = new CSVAuthor(new File("C:\\temp\\Workingspace\\output.csv"));
			CSVParser in = new CSVParser(inFile);
			boolean good = true;
			int i = 0;
			int max = 1000;
			while (good && i < max) {
				String[] line = in.readNext();
				if (line != null) {
					if (i < 2) {
						for (String s : line) {
							System.out.println(s);
						}
					}
					if (i < 5) {
						System.out.println(line.length);
					}
					out.writeNext(line);
				} else {
					good = false;
				}
				i ++;
			}
			out.close();
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	

}
