package com.mcic.sfrest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class SalesforceModel {
	
	private String userName, password, securityKey, key, secret, endpoint;
	
	public String getConsumerKey() {
		return key;
	}

	public String getConsumerSecret() {
		return secret;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getSecurityKey() {
		return securityKey;
	}

	public SalesforceModel(File propFile) {
		Properties props = new Properties();
		try {
			props.load(new FileReader(propFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		userName = props.getProperty("username");
		password = props.getProperty("password");
		securityKey = props.getProperty("securityKey");
		endpoint = props.getProperty("endpoint");
		key = props.getProperty("key");
		secret = props.getProperty("secret");
		boolean crdi = endpoint.equals("https://mcic--crdi.sandbox.my.salesforce.com");
		/*
		if (crdi) {
			key = "3MVG934iBBsZ.kUGJW_XmNtzKUlfiCffRwZOl4ngBHPdxBVK4uiHMC_RFqGcVI0KeTVPt0wLfIH9ilYTxqZiq";
			secret = "FDDFF7C65191C7948864D5D126D850E4CAAB5AFEE182C9D9AE83B7D8731E870E";
		} else {
			key = "3MVG9A2kN3Bn17hvQm86vPpHKvrnfedU0NZ_Pr1DeZo.CJ.KN5ydDzr2L6fcIW5Qg7jxgOTlOnvUritQUiqD5";
			secret = "2C875351231225AC73A4ECD46DCF654DB5A5EA25CD8C6D222156495E87622EEA";
		}
		*/
	}

	public String getEndpoint() {
		return endpoint;
	}
}
