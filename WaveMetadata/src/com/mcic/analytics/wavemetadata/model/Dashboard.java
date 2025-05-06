package com.mcic.analytics.wavemetadata.model;

/**
 * Represents a Salesforce CRM Analytics dashboard.
 */
public class Dashboard {
    private String name;
    private String folder;
    private String label;
    private String id;
    private String createdBy;
    private String lastModifiedBy;

    public Dashboard() {
    }

    public Dashboard(String name, String folder, String label, String id, String createdBy, String lastModifiedBy) {
        this.name = name;
        this.folder = folder;
        this.label = label;
        this.id = id;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}




