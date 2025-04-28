package com.mcic.wave;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.Base64;
import java.util.Base64.Encoder;

public class DatasetBuilder {
    private final List<String[]> records = new ArrayList<>();
    private final String[] headers;
    private File tempFile;
    private BufferedWriter writer;
    private int encodedSize;
    

    // Constructor for headers
    public DatasetBuilder(String... headers) throws IOException {
        this.headers = headers;
        tempFile = File.createTempFile("dataset", ".csv");
        writer = new BufferedWriter(new FileWriter(tempFile));
        writeRow(headers); // Write the headers initially
        encodedSize = 0;
    }

    // Method to add a row
    public void addRecord(String... row) throws IOException {
        if (row.length != headers.length) {
            throw new IllegalArgumentException("Row length does not match the number of headers.");
        }
        writeRow(row);
    }

    // Method to finish and close the file
    public void finish() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    // Method to build and return a BufferedReader with Base64-encrypted, GZipped data
    public BufferedReader build() {
        try {
			finish(); // Ensure file is closed
			File compressedFile = File.createTempFile("compressed_dataset", ".gz");
			try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(compressedFile));
			     FileInputStream fileIn = new FileInputStream(tempFile)) {
			    byte[] buffer = new byte[1024];
			    int bytesRead;
			    while ((bytesRead = fileIn.read(buffer)) != -1) {
			        gzipOut.write(buffer, 0, bytesRead);
			    }
			}

			Encoder encoder = Base64.getEncoder();
			File encodedFile = File.createTempFile("encoded_dataset", ".txt");
			try (FileInputStream fileIn = new FileInputStream(compressedFile);
				BufferedWriter fileOut = new BufferedWriter(new FileWriter(encodedFile))) {
			    byte[] buffer = new byte[1024];
			    int bytesRead;
			    while ((bytesRead = fileIn.read(buffer)) != -1) {
			    	if (bytesRead != buffer.length) {
			    		byte[] newBuffer = new byte[bytesRead];
			    		System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
			    		buffer = newBuffer;
			    	}
			    	String base64Data = encoder.encodeToString(buffer);
			    	fileOut.write(base64Data);
			    	encodedSize += base64Data.length() * 2;  //  UNICODE is 16 bits (2 bytes) long
			    }
			    fileOut.close();
			}

			try (FileReader fileIn = new FileReader(encodedFile)){
				return new BufferedReader(fileIn);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
        return null;
    }
    
    

    public int getEncodedSize() {
		return encodedSize;
	}

	// Helper method to write a row to the file
    private void writeRow(String... row) throws IOException {
        writer.write(String.join(",", row));
        writer.newLine();
    }

    // Cleanup temporary files on object destruction
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}