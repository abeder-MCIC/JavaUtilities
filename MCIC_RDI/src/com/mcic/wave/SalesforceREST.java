package com.mcic.wave;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.util.Progressive;
import com.mcic.util.json.JSONString;
import com.mcic.wavemetadata.ui.ProgressPanel;
import com.mcic.wavemetadata.ui.ProgressPanel.ProgressPanelStep;


public class SalesforceREST extends Progressive {
	public static final int FAILURE = 1;
	public static final int SUCCESS = 2;
	public static final int INDETERMINATE = 0;
	public static final int BUFFER_SIZE = 1_000_000;
	
    String accessToken;
    SalesforceModel model;
    Map<String, Set<String>> sObjectFields;
    //public String restEndpoint;
    private CloseableHttpClient httpClient;
    JSONObject response;
    
    /*
    public static void main(String[] args) {
    	SalesforceModel m = new SalesforceModel(false);
    	SalesforceAgent a = new SalesforceAgent(m);
    	a.getAccessToken();
    }
    */
    public SalesforceREST(SalesforceModel m) {
    	accessToken = null;
    	this.model = m;
    	sObjectFields = new TreeMap<String, Set<String>>();
    }
    
    public String getAccessToken() {
    	if (accessToken == null) {
    		authenticate(model);
    	}
    	
		return accessToken;
	}

    public String getRestEndpoint() {
		return model.getEndpoint();
	}
    
    public void startJob(String id) {
    	String url = "/services/data/v58.0/wave/dataflowjobs";
    	JSONObject root = new JSONObject();
    	root.put("dataflowId", id);
    	root.put("command", "start");
    	int res = postJSON(url, root);
    	System.out.println(response);
    }
    
    
    public void writeDataset(String APIName, String label, String app, DatasetBuilder builder) {
    	
    	String url = "/services/data/v58.0/sobjects/InsightsExternalData";
    	JSONObject root = new JSONObject();
    	root.put("Format", "Csv");
    	root.put("EdgemartAlias", APIName);
    	root.put("EdgemartLabel", label);
    	root.put("EdgemartContainer", app);
    	root.put("Action", "None");
       	root.put("Operation", "Overwrite");
    	int res = postJSON(url, root);
    	if (res == SalesforceREST.SUCCESS) {
	    	String id = response.getString("id");
	    	//System.out.println(id);
	
	    	BufferedReader encoded = builder.build();
	    	int size = builder.getEncodedSize();
	    	char[] charBuff = new char[BUFFER_SIZE];
			Vector<JSONObject> packets = new Vector<JSONObject>();
	    	final String partUrl = "/services/data/v58.0/sobjects/InsightsExternalDataPart";
	    	int part = 1;
	    	
			while (size > 0) {
				int readCount = size;
				if (readCount > BUFFER_SIZE) {
					readCount = BUFFER_SIZE;
				} else {
					charBuff = new char[readCount];
				}
				try {
					encoded.read(charBuff, 0, readCount);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JSONObject packet = new JSONObject();
	         	packet.put("InsightsExternalDataId", id);
	         	packet.put("PartNumber", part ++);
	         	packet.put("DataFile", charBuff);
	         	packets.add(packet);
	         	
	         	size -= readCount;
			}
				/***************************************************************************************************
				 *   Kick off a set of threads uploading individual packet data
				 */
				
			ExecutorService cluster = Executors.newFixedThreadPool(5);
			
			
	        
			for (int i = 0;i < packets.size();i++) {
				final int thisPart = i + 1;
				final JSONObject thisPacket = packets.elementAt(i);
				cluster.execute(new Runnable() {
					
					
					public void run() {
						ProgressPanelStep step = nextStep("Writing block number " + thisPart);
			        	int res = FAILURE;
			        	while (res == FAILURE) {
				        	res = postJSON(partUrl, thisPacket);
				        	if (res != SUCCESS) {
				        		step.addNote("Failure sending, re-posting");
				        	}
			        	}
			        	step.complete();
					}
					
					
				});
				// End of Runnable
	        	
			}
			cluster.shutdown();
			try {
				cluster.awaitTermination(30, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	url = "/services/data/v58.0/sobjects/InsightsExternalData/" + id;
        	root.clear();
        	root.put("Action", "Process");
        	res = patchJSON(url, root);

	    
    	} else {
    		System.out.println("Error creating InsightesExternalData object");
    	}
    	
    }
    

    public Set<String> getFields(String objectName){
    	Set<String> fields = sObjectFields.get(objectName);
    	if (fields == null) {
    		fields = new TreeSet<String>();
    		sObjectFields.put(objectName, fields);
	    	String url = "/services/data/v58.0/sobjects/" + objectName + "/describe/";
	    	int res = get(url, null);
	    	JSONArray fList = response.getJSONArray("fields");
	    	for (int i = 0;i < fList.length();i++) {
	    		JSONObject field = fList.getJSONObject(i);
	    		String fieldName = field.getString("name");
	    		fields.add(fieldName);
	    	}
    	}
    	return fields;
    }

	public int get(String resource, String urlParameters) {
    	String url = getRestEndpoint() + resource;
    	HttpGet get = new HttpGet(url);
		if (urlParameters != null && !urlParameters.equals("")){
			url = url + "?" + urlParameters;
		}
		get.setHeader("accept", "application/json");
		get.setHeader("Authorization", "OAuth " + getAccessToken());
		
    	try {
			CloseableHttpResponse resp = httpClient.execute(get);
			String rstr = EntityUtils.toString(resp.getEntity());
			if (rstr.matches("^[{\\[][\\s\\S]+")) {  //  Is this JSON?
				response = new JSONObject(rstr);
				return SUCCESS;
    		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return FAILURE;
    }
	
	public int postJSON(String resource, JSONObject json) {
		return postJSON(resource, json, false);
	}
	
	public int patchJSON(String resource, JSONObject json) {
		return postJSON(resource, json, true);
	}
	
	public JSONObject delete(String resource) {
		String url = getRestEndpoint() + resource;
		HttpUriRequestBase post = new HttpDelete(url);
    	post.setHeader("Content-Type", "application/json");
    	post.setHeader("accept", "application/json");
    	post.setHeader("Authorization", "OAuth " + getAccessToken());
        try {
			CloseableHttpResponse authResponse = httpClient.execute(post);
			int code = authResponse.getCode();
			if (code != 204) {
				String jstr = EntityUtils.toString(authResponse.getEntity());
				JSONObject node = new JSONObject(jstr);
				return node;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
     			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
	}

	public int postJSON(String resource, JSONObject json, boolean isPatch) {
		
		String url = getRestEndpoint() + resource;
		HttpUriRequestBase post = null;
    	if (isPatch) {
    		post = new HttpPatch(url);
    	} else {
    		post = new HttpPost(url);
    	}
    	
    	String jsonString = json.toString();
//    	if (json.getClass().getSimpleName().equals("JSONArray")) {
//    		jsonString = "{" + jsonString + "}";
//    	}
    	
    	post.setHeader("Content-Type", "application/json");
    	post.setHeader("accept", "application/json");
    	post.setHeader("Authorization", "OAuth " + getAccessToken());
    	HttpEntity str = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    	post.setEntity(str);
    	
        try {
        	boolean succeeded = false;
        	while (!succeeded) {
            	CloseableHttpResponse authResponse;
				try {
					authResponse = httpClient.execute(post);
	    			int code = authResponse.getCode();
	    			if (code != 204) {
	    				String jstr = EntityUtils.toString(authResponse.getEntity());
	    				response = new JSONObject(jstr);
	    				return SUCCESS;
	    			}
				} catch (IOException e) {
					return FAILURE;
				}
        	}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return FAILURE;
	}
	
	public JSONObject getResponse() {
		return response;
	}

	private void authenticate(SalesforceModel m) {
        // Replace these variables with your Salesforce credentials and token
        String username = m.getUserName();
        String password = m.getPassword();
        String securityToken = m.getSecurityKey();
        String authEndpoint = m.getEndpoint() + "/services/oauth2/token"; // Salesforce authentication URL

        // Salesforce REST API endpoint

        // Salesforce REST API resource you want to access
        String resource = "query?q=SELECT+Id,Name+FROM+Account+LIMIT+5"; // Example query

        // Create HttpClient instance
        try  {
            // Construct authentication request
        	httpClient = HttpClients.createDefault();
        	
        	MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("grant_type", "password");
            builder.addTextBody("client_id", m.getConsumerKey());
            builder.addTextBody("client_secret", m.getConsumerSecret());
            builder.addTextBody("username", username);
            builder.addTextBody("password", password);
            HttpEntity multipart = builder.build();
            
            String authBody = "grant_type=password&client_id=" + m.getConsumerKey() + "&client_secret=" + m.getConsumerSecret() + "&" +
                    "username=" + username + "&password=" + password + securityToken;

            // Create HTTP POST request to authenticate
            HttpPost authRequest = new HttpPost(authEndpoint);
            authRequest.setEntity(multipart);
            //authRequest.setEntity(new StringEntity(authBody));

            // Execute authentication request
            CloseableHttpResponse authResponse = httpClient.execute(authRequest);
            int authStatusCode = authResponse.getCode();

            if (authStatusCode == HttpStatus.SC_OK) {

                try {
                    // Parse access token from authentication response
                	JSONObject authResult = new JSONObject(EntityUtils.toString(authResponse.getEntity()));
                    accessToken = authResult.getString("access_token");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    authResponse.close();
                }

            } else {
                System.err.println("Authentication failed: " + authStatusCode + " - " + authResponse.getReasonPhrase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}