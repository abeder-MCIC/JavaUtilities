package com.mcic.analytics.wavemetadata.model;

/**
 * Represents a field in a dataset, as defined in XMD metadata.
 */
public class DatasetField {
    private String datasetName;
    private String fieldName;
    private String label;
    private String type;

    public DatasetField() {
    }

    public DatasetField(String datasetName, String fieldName, String label, String type) {
        this.datasetName = datasetName;
        this.fieldName = fieldName;
        this.label = label;
        this.type = type;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
