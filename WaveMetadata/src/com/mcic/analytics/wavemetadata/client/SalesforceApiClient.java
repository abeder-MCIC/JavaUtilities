// src/com/mcic/anbalytics/wavemetadata/client/SalesforceApiClient.java
package com.mcic.analytics.wavemetadata.client;

import com.mcic.analytics.wavemetadata.auth.OAuthService;
import com.mcic.analytics.wavemetadata.config.ConfigManager;
import com.mcic.analytics.wavemetadata.model.Dashboard;
import com.mcic.analytics.wavemetadata.model.Dataset;
import com.mcic.analytics.wavemetadata.util.ProgressLogger;
import com.mcic.analytics.wavemetadata.util.RetryHandler;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Low-level HTTP wrapper around Analytics REST endpoints,
 * now with retry logic and progress logging.
 */
public class SalesforceApiClient {
    private final CloseableHttpClient http = HttpClients.createDefault();
    private final OAuthService auth;
    private final ConfigManager config;
    private final String apiVersion = "v62.0";
    private final RetryHandler retryHandler = new RetryHandler();
    private final ProgressLogger logger = new ProgressLogger(SalesforceApiClient.class);

    public SalesforceApiClient(ConfigManager config, OAuthService auth) {
        this.config = config;
        this.auth = auth;
    }

    /**
     * Paginate through /wave/dashboards to retrieve all Dashboard metadata.
     */
    public List<Dashboard> fetchAllDashboards() {
        List<Dashboard> results = new ArrayList<>();
        String url = config.getEndpoint() + "/services/data/" + apiVersion + "/wave/dashboards";

        while (url != null) {
            // snapshot the current URL so the lambda can capture it
            final String requestUrl = url;
            JSONObject root = retryHandler.executeWithRetry(() -> {
                logger.info("Fetching dashboards from URL: " + requestUrl);
                HttpGet get = new HttpGet(requestUrl);
                get.setHeader("Authorization", "Bearer " + auth.getToken(config));
                try (CloseableHttpResponse response = http.execute(get)) {
                    String body = EntityUtils.toString(response.getEntity());
                    return new JSONObject(body);
                }
            }, 3);

            JSONArray arr = root.getJSONArray("dashboards");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject dash = arr.getJSONObject(i);
                Dashboard d = new Dashboard(
                    dash.getString("name"),
                    dash.getJSONObject("folder").getString("name"),
                    dash.getString("label"),
                    dash.getString("id"),
                    dash.getJSONObject("createdBy").getString("name"),
                    dash.getJSONObject("lastModifiedBy").getString("name")
                );
                results.add(d);
            }

            if (root.has("nextRecordsUrl")) {
                url = config.getEndpoint() + root.getString("nextRecordsUrl");
            } else {
                url = null;
            }
        }
        return results;
    }

    /**
     * Paginate through /wave/datasets to retrieve all Dataset metadata.
     */
    public List<Dataset> fetchAllDatasets() {
        List<Dataset> results = new ArrayList<>();
        String url = config.getEndpoint() + "/services/data/" + apiVersion + "/wave/datasets";

        while (url != null) {
            final String requestUrl = url;
            JSONObject root = retryHandler.executeWithRetry(() -> {
                logger.info("Fetching datasets from URL: " + requestUrl);
                HttpGet get = new HttpGet(requestUrl);
                get.setHeader("Authorization", "Bearer " + auth.getToken(config));
                try (CloseableHttpResponse response = http.execute(get)) {
                    String body = EntityUtils.toString(response.getEntity());
                    return new JSONObject(body);
                }
            }, 3);

            JSONArray arr = root.getJSONArray("datasets");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject ds = arr.getJSONObject(i);
                String container  = ds.getJSONObject("folder").getString("name");
                String versionUrl = ds.getString("url");

                Dataset d = new Dataset(
                    ds.getString("name"),
                    ds.getString("label"),
                    ds.getString("id"),
                    container,
                    versionUrl
                );
                results.add(d);
            }

            if (root.has("nextRecordsUrl")) {
                url = config.getEndpoint() + root.getString("nextRecordsUrl");
            } else {
                url = null;
            }
        }
        return results;
    }

    /**
     * Fetch the full JSON definition for a given dashboard.
     */
    public String fetchDashboardJson(String dashboardId) {
        return retryHandler.executeWithRetry(() -> {
            String url = config.getEndpoint() + "/services/data/" + apiVersion + "/wave/dashboards/" + dashboardId;
            logger.info("Fetching dashboard JSON for ID: " + dashboardId);
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + auth.getToken(config));
            try (CloseableHttpResponse response = http.execute(get)) {
                return EntityUtils.toString(response.getEntity());
            }
        }, 3);
    }

    /**
     * Fetch the XMD metadata for a given dataset version URL.
     */
    public String fetchXmd(String datasetVersionUrl) {
        return retryHandler.executeWithRetry(() -> {
            String url = config.getEndpoint() + datasetVersionUrl;
            logger.info("Fetching XMD metadata from URL: " + url);
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + auth.getToken(config));
            try (CloseableHttpResponse response = http.execute(get)) {
                return EntityUtils.toString(response.getEntity());
            }
        }, 3);
    }
}
