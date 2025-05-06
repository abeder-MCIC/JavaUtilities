package com.mcic.analytics.wavemetadata.model;

/**
 * Represents a junction entry between a dashboard and a dataset.
 */
public class JunctionEntry {
    private String dashboardName;
    private String datasetName;

    public JunctionEntry() {
    }

    public JunctionEntry(String dashboardName, String datasetName) {
        this.dashboardName = dashboardName;
        this.datasetName = datasetName;
    }

    public String getDashboardName() {
        return dashboardName;
    }

    public void setDashboardName(String dashboardName) {
        this.dashboardName = dashboardName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
}
