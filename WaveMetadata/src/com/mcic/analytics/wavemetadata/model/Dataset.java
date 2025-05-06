package com.mcic.analytics.wavemetadata.model;

/**
 * Represents a Salesforce CRM Analytics dataset.
 */
public class Dataset {
    private String name;
    private String label;
    private String id;
    private String container;
    private String versionUrl;

    public Dataset() {
    }

    public Dataset(String name, String label, String id, String container, String versionUrl) {
        this.name = name;
        this.label = label;
        this.id = id;
        this.container = container;
        this.versionUrl = versionUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getVersionUrl() {
        return versionUrl;
    }

    public void setVersionUrl(String versionUrl) {
        this.versionUrl = versionUrl;
    }
}
