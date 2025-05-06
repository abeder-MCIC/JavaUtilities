package com.mcic.anbalytics.wavemetadata.auth;

import com.mcic.anbalytics.wavemetadata.config.ConfigManager;
import com.mcic.anbalytics.wavemetadata.util.RetryHandler;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs OAuth2 “password”-grant against Salesforce and caches the access token.
 */
public class OAuthService {
    private String accessToken;
    private Instant expiry;
    private final RetryHandler retryHandler = new RetryHandler();

    /**
     * Authenticates using the password grant flow:
     * POST {endpoint}/services/oauth2/token
     *   grant_type=password
     *   username, password+securityKey
     *   client_id, client_secret
     */
    public void authenticate(ConfigManager cfg) {
        retryHandler.executeWithRetry(() -> {
            String url = cfg.getEndpoint() + "/services/oauth2/token";
            try (CloseableHttpClient http = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(url);

                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("grant_type",    "password"));
                params.add(new BasicNameValuePair("username",      cfg.getUsername()));
                params.add(new BasicNameValuePair("password",      cfg.getPassword() + cfg.getSecurityKey()));
                params.add(new BasicNameValuePair("client_id",     cfg.getClientId()));
                params.add(new BasicNameValuePair("client_secret", cfg.getClientSecret()));
                post.setEntity(new UrlEncodedFormEntity(params));

                CloseableHttpResponse response = http.execute(post);
                String body = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(body);

                accessToken = json.getString("access_token");
                long expiresIn = json.getLong("expires_in");
                expiry = Instant.now().plusSeconds(expiresIn);

                response.close();
            }
            return null;
        }, 3);
    }

    /**
     * Returns a valid access token, refreshing if it’s null or near expiry.
     */
    public String getToken(ConfigManager cfg) {
        if (accessToken == null || Instant.now().isAfter(expiry.minusSeconds(60))) {
            authenticate(cfg);
        }
        return accessToken;
    }
}
