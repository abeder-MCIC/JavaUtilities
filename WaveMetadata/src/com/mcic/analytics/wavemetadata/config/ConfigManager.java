package com.mcic.analytics.wavemetadata.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manages loading of credentials and endpoints from a .properties file.
 * Properties loaded:
 *   username, password, securityKey,
 *   endpoint, client_id, client_secret
 */
public class ConfigManager {
    private final Properties properties = new Properties();

    private String username;
    private String password;
    private String securityKey;
    private String endpoint;
    private String clientId;
    private String clientSecret;

    /**
     * Reads the given properties file and populates fields.
     *
     * @param propsFilePath path to wave-metadata.properties
     * @throws IOException if file can’t be read
     */
    public void load(String propsFilePath) throws IOException {
        try (FileInputStream in = new FileInputStream(propsFilePath)) {
            properties.load(in);
        }
        username    = properties.getProperty("username");
        password    = properties.getProperty("password");
        securityKey = properties.getProperty("securityKey");
        endpoint    = properties.getProperty("endpoint");
        clientId    = properties.getProperty("client_id");
        clientSecret= properties.getProperty("client_secret");
    }

    /** Generic getter for any raw property. */
    public String get(String key) {
        return properties.getProperty(key);
    }

    // Explicit getters if desired:
    public String getUsername()    { return username; }
    public String getPassword()    { return password; }
    public String getSecurityKey() { return securityKey; }
    public String getEndpoint()    { return endpoint; }
    public String getClientId()    { return clientId; }
    public String getClientSecret(){ return clientSecret; }
}
