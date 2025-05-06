package com.mcic.anbalytics.wavemetadata.model;

/**
 * Represents a field used in a dashboard step.
 */
public class DashboardField {
    private String dashboardName;
    private String stepName;
    private String datasetName;
    private String fieldName;

    public DashboardField() {
    }

    public DashboardField(String dashboardName, String stepName, String datasetName, String fieldName) {
        this.dashboardName = dashboardName;
        this.stepName = stepName;
        this.datasetName = datasetName;
        this.fieldName = fieldName;
    }

    public String getDashboardName() {
        return dashboardName;
    }

    public void setDashboardName(String dashboardName) {
        this.dashboardName = dashboardName;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
