package com.mcic.sfrest;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

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

import com.mcic.util.CSVAuthor;
import com.mcic.util.Progressive;
import com.mcic.util.RecordSet;
import com.mcic.util.RecordsetOld;
import com.mcic.util.json.JSONNode;
import com.mcic.util.json.JSONObject;
import com.mcic.util.json.JSONString;
import com.mcic.util.json.ThreadCluster;
import com.mcic.wavemetadata.ui.ProgressPanel;
import com.mcic.wavemetadata.ui.ProgressPanel.ProgressPanelStep;


public class SalesforceAgent extends Progressive {
    String accessToken;
    SalesforceModel model;
    Map<String, Set<String>> sObjectFields;
    //public String restEndpoint;
    private CloseableHttpClient httpClient;
    ProgressPanel panel;
    
    /*
    public static void main(String[] args) {
    	SalesforceModel m = new SalesforceModel(false);
    	SalesforceAgent a = new SalesforceAgent(m);
    	a.getAccessToken();
    }
    */
    public SalesforceAgent(SalesforceModel m) {
    	accessToken = null;
    	this.model = m;
    	sObjectFields = new TreeMap<String, Set<String>>();
    	panel = null;
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
    	root.put("dataflowId", new JSONString(id));
    	root.put("command", new JSONString("start"));
    	JSONNode o = postJSON(url, root);
    	System.out.println(o);
    }
    
    public JSONNode writeDataset(String APIName, String label, String app, RecordSet data, String operation, String JSONMetadata) {
    	int PART_SIZE = 8000000;
    	
    	String url = "/services/data/v58.0/sobjects/InsightsExternalData";
    	String metadata = Base64.getEncoder().encodeToString(JSONMetadata.getBytes());
    	JSONObject root = new JSONObject();
    	root.addString("Format", "Csv");
    	root.addString("EdgemartAlias", APIName);
    	root.addString("EdgemartLabel", label);
    	root.addString("EdgemartContainer", app);
    	root.addString("Operation", operation);
    	root.addString("Action", "None");
    	root.addString("MetadataJson", metadata);
    	JSONNode o = postJSON(url, root);
    	String id = o.get("id").asString();
    	//System.out.println(id);

    	if (panel == null) {
    		panel = new ProgressPanel(10);
    	}
    	
    	
    	url = "/services/data/v58.0/sobjects/InsightsExternalDataPart";
        //FileInputStream fis = new FileInputStream(file);
        //FileOutputStream fos = new FileOutputStream(gzipFile);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int start = 0;
		int len = data.size();
		int remaining = len;
		int part = 1;
		List<Integer> remainingParts = new LinkedList<Integer>();
		Map<Integer, String> blocks = new LinkedHashMap<Integer, String>();
		
		while (remaining > 0) {
			String dataStr = data.toBase64(start, PART_SIZE);
			remainingParts.add(part);
			blocks.put(part, dataStr);
			part ++;
			remaining -= PART_SIZE;
		}

		ThreadCluster c = new ThreadCluster(4);
		while (remainingParts.size() > 0) {
			int thisPart = remainingParts.remove(0);
			c.dispatch(new Runnable() {
				
				public void run() {
					boolean succeeded = false;
					String nextBlock = blocks.get(thisPart);
			    	String url = "/services/data/v58.0/sobjects/InsightsExternalDataPart";
					JSONObject root = new JSONObject();
			     	root.clear();
			    	root.addString("InsightsExternalDataId", id);
			    	root.addNumber("PartNumber", thisPart);
			    	root.addString("DataFile", nextBlock);
			    	while (!succeeded) {
						ProgressPanelStep step = nextStep("Writing block number " + thisPart + "...");
			    		JSONNode o = postJSON(url, root);
			    		System.out.println(o);
			    		if (o != null && o.getType() == JSONNode.Type.OBJECT) {
			    			succeeded = o.get("success").asBoolean();
			    		}
			    		if (!succeeded) {
			    			System.out.println("Failure uploading, retrying");
			    			remainingParts.add(thisPart);
			    		} else {
			    		}
		    			step.complete();
			    	}
				}
			});
			
		}
		
		c.join();
    	
		ProgressPanelStep step = panel.nextStep("Processing Upload");
    	url = "/services/data/v58.0/sobjects/InsightsExternalData/" + id;
    	root.clear();
    	root.addString("Action", "Process");
    	o = patchJSON(url, root);
    	step.complete();
    	return o;
    }
    
    public JSONNode writeDataset(String APIName, String label, String app, String data, String operation, String JSONMetadata) {
    	
    	String metadata = Base64.getEncoder().encodeToString(JSONMetadata.getBytes());
    	
    	String url = "/services/data/v58.0/sobjects/InsightsExternalData";
    	JSONObject root = new JSONObject();
    	root.addString("Format", "Csv");
    	root.addString("EdgemartAlias", APIName);
    	root.addString("EdgemartLabel", label);
    	root.addString("EdgemartContainer", app);
    	root.addString("Operation", operation);
    	root.addString("Action", "None");
    	root.addString("MetadataJson", metadata);
    	JSONNode o = postJSON(url, root);
    	String id = o.get("id").asString();
    	//System.out.println(id);

    	if (panel == null) {
    		panel = new ProgressPanel(10);
    	}
    	
    	
    	url = "/services/data/v58.0/sobjects/InsightsExternalDataPart";
    	byte[] str = data.getBytes();
    	if (str.length >= 5000000) {
            //FileInputStream fis = new FileInputStream(file);
            //FileOutputStream fos = new FileOutputStream(gzipFile);
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		
            try {
				GZIPOutputStream gzipOS = new GZIPOutputStream(out);
				gzipOS.write(str);
				byte[] zipped = out.toByteArray();
				Map<Integer, String> blocks = new TreeMap<Integer, String>();
				List<Integer> parts = new LinkedList<Integer>();
				String dataStr = Base64.getEncoder().encodeToString(zipped);
				int part = 1;
				while (dataStr.length() > 0) {
					//System.out.println("Writing block number " + part);
					int nextBlockSize = dataStr.length() > 5000000 ? 5000000 : dataStr.length();
					String nextBlock = dataStr.substring(0, nextBlockSize);
					dataStr = dataStr.substring(nextBlockSize);
					parts.add(part);
					blocks.put(part, nextBlock);
					part ++;
				}
				
				while (parts.size() > 0) {
					part = parts.remove(0);
					String nextBlock = blocks.get(part);
					ProgressPanelStep step = panel.nextStep("Writing block number " + part);
		         	root.clear();
		        	root.addString("InsightsExternalDataId", id);
		        	root.addNumber("PartNumber", part);
		        	root.addString("DataFile", nextBlock);
		        	o = postJSON(url, root);
		        	System.out.println(o);
		        	boolean succeeded = false;
		        	if (o.getType() == JSONNode.Type.OBJECT) {
		        		succeeded = o.get("success").asBoolean();
		        	}
		        	if (!succeeded) {
		        		step.addNote("Failure uploading, retrying");
		        		parts.add(part);
		        	} else {
		        		step.complete();
		        	}
		        	//System.out.println(o.toString());
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
    	} else {
        	root.clear();
        	String dataStr = Base64.getEncoder().encodeToString(data.toString().getBytes());
        	root.addString("InsightsExternalDataId", id);
        	root.addNumber("PartNumber", 1);
        	root.addString("DataFile", dataStr);
        	o = postJSON(url, root);
        	
    	}

    	
    	url = "/services/data/v58.0/sobjects/InsightsExternalData/" + id;
    	root.clear();
    	root.addString("Action", "Process");
    	o = patchJSON(url, root);
    	return o;
    }
    

    public Set<String> getFields(String objectName){
    	Set<String> fields = sObjectFields.get(objectName);
    	if (fields == null) {
    		fields = new TreeSet<String>();
    		sObjectFields.put(objectName, fields);
	    	String url = "/services/data/v58.0/sobjects/" + objectName + "/describe/";
	    	JSONNode root = get(url, null);
	    	for (JSONNode field : root.get("fields").values()) {
	    		String fieldName = field.get("name").asString();
	    		fields.add(fieldName);
	    	}
    	}
    	return fields;
    }

	public JSONObject get(String resource, String urlParameters) {
    	String url = getRestEndpoint() + resource;
    	HttpGet get = new HttpGet(url);
		if (urlParameters != null && !urlParameters.equals("")){
			url = url + "?" + urlParameters;
		}
		get.setHeader("accept", "application/json");
		get.setHeader("Authorization", "OAuth " + getAccessToken());
		
    	try {
			CloseableHttpResponse response = httpClient.execute(get);
			String rstr = EntityUtils.toString(response.getEntity());
			if (rstr.charAt(0) == '{') {
				JSONNode n = JSONNode.parse(rstr);
				if (n != null) {
					return (JSONObject)n;
				}
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
	
	public JSONNode postJSON(String resource, JSONNode json) {
		return postJSON(resource, json, false);
	}
	
	public JSONNode patchJSON(String resource, JSONNode json) {
		return postJSON(resource, json, true);
	}
	
	public JSONNode delete(String resource) {
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
				JSONNode node = JSONNode.parse(jstr);
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

	public JSONNode postJSON(String resource, JSONNode json, boolean isPatch) {
		
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
			CloseableHttpResponse authResponse = httpClient.execute(post);
			int code = authResponse.getCode();
			if (code != 204) {
				String jstr = EntityUtils.toString(authResponse.getEntity());
				JSONNode node = JSONNode.parse(jstr);
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
                	JSONNode authResult = JSONNode.parse(EntityUtils.toString(authResponse.getEntity()));
                    accessToken = authResult.get("access_token").asString();
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
