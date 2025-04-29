package com.mcic.wave;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.util.Progressive;

public class SalesforceREST extends Progressive {
    public static final int FAILURE = 1, SUCCESS = 2, BUFFER_SIZE = 1_000_000;

    private String accessToken;
    private final SalesforceModel model;
    private final Map<String, Set<String>> sObjectFields = new TreeMap<>();
    private CloseableHttpClient httpClient;
    private JSONObject response;

    public SalesforceREST(SalesforceModel m) {
        this.model = m;
    }

    public String getAccessToken() {
        if (accessToken == null) authenticate();
        return accessToken;
    }

    public String getRestEndpoint() {
        return model.getEndpoint();
    }

    private void authenticate() {
        try {
            httpClient = HttpClients.createDefault();
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody("grant_type", "password")
                .addTextBody("client_id", model.getConsumerKey())
                .addTextBody("client_secret", model.getConsumerSecret())
                .addTextBody("username", model.getUserName())
                .addTextBody("password", model.getPassword() + model.getSecurityKey());
            HttpEntity multipart = builder.build();
            HttpPost authReq = new HttpPost(model.getEndpoint() + "/services/oauth2/token");
            authReq.setEntity(multipart);
            String respStr = new String(httpClient.execute(authReq).getEntity().getContent().readAllBytes());
            response = new JSONObject(respStr);
            accessToken = response.getString("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }

    public int get(String resource, String urlParameters) {
        try {
            String url = getRestEndpoint() + resource +
                         ((urlParameters != null && !urlParameters.isEmpty()) ? "?" + urlParameters : "");
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + getAccessToken());
            CloseableHttpResponse resp = httpClient.execute(get);
            String body = EntityUtils.toString(resp.getEntity());
            response = new JSONObject(body);
            return SUCCESS;
        } catch (IOException e) {
			e.printStackTrace();
            return FAILURE;
        } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return FAILURE;
		}
    }

    public int postJSON(String resource, JSONObject json) {
        return sendJSON(resource, json, false);
    }

    public int patchJSON(String resource, JSONObject json) {
        return sendJSON(resource, json, true);
    }

    private int sendJSON(String resource, JSONObject json, boolean isPatch) {
        try {
            String url = getRestEndpoint() + resource;
            HttpUriRequestBase req = isPatch ? new HttpPatch(url) : new HttpPost(url);
            StringEntity entity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
            req.setEntity(entity);
            req.setHeader("Accept", "application/json");
            req.setHeader("Authorization", "Bearer " + getAccessToken());

            System.out.print("Executing, ");
            CloseableHttpResponse resp = httpClient.execute(req);
            int code = resp.getCode(); 
            if (code >= 200 && code < 300) {
                System.out.print("Reading Entity, ");
                if (code == 204) {
                	response = new JSONObject();
                } else {
                	String content = EntityUtils.toString(resp.getEntity());
                    response = new JSONObject(content);
                    if (content.length() > 0) {
                        System.out.print("Parsing, ");
                    }
                }
                return SUCCESS;
            } else {
            	String content = EntityUtils.toString(resp.getEntity());
                JSONArray error = new JSONArray(content);
                System.err.println("Error in REST call: " + error.toString());
            }
            return FAILURE;
        } catch (IOException e) {
			e.printStackTrace();
            return FAILURE;
        } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return FAILURE;
		}
    }

    public JSONObject getResponse() {
        return response;
    }

    public void writeDataset(String APIName, String label, String app, DatasetBuilder builder) {
        JSONObject hdr = new JSONObject()
            .put("Format", "Csv")
            .put("EdgemartAlias", APIName)
            .put("EdgemartLabel", label)
            .put("EdgemartContainer", app)
            .put("Action", "None")
            .put("Operation", "Overwrite");
        if (postJSON("/services/data/v58.0/sobjects/InsightsExternalData", hdr) != SUCCESS) {
            System.err.println("Failed to create external data header: " + response.toString());
            return;
        }
        String headerId = response.getString("id");

        // Read base64-encoded file and split into parts
        try (BufferedReader reader = builder.build()) {
            List<JSONObject> parts = new ArrayList<>();
            char[] buf = new char[BUFFER_SIZE];
            int read, partNum = 1;
            while ((read = reader.read(buf)) != -1) {
                String data = new String(buf, 0, read);
                JSONObject pkt = new JSONObject()
                    .put("InsightsExternalDataId", headerId)
                    .put("PartNumber", partNum++)
                    .put("DataFile", data)
                    .put("DataLength", read)
                    .put("CompressedDataLength", read);
                parts.add(pkt);
            }

            ExecutorService pool = Executors.newFixedThreadPool(5);
            for (JSONObject pkt : parts) {
                pool.execute(() -> {
                    while (postJSON(
                        "/services/data/v58.0/sobjects/InsightsExternalDataPart", pkt
                    ) == FAILURE) {
                        // retry until success
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.MINUTES);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Trigger processing
        JSONObject proc = new JSONObject().put("Action", "Process");
        patchJSON("/services/data/v58.0/sobjects/InsightsExternalData/" + headerId, proc);
    }
}
