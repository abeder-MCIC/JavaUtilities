package com.mcic.anbalytics.wavemetadata.extractor;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mcic.anbalytics.wavemetadata.model.DatasetField;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts dataset field definitions from XMD metadata.
 */
public class XmdExtractor {
    /**
     * Parses XMD JSON to extract dataset fields with names, labels, and types.
     * @param xmdJson raw XMD metadata JSON string
     * @param datasetName name of the dataset
     * @return list of DatasetField entries
     */
    public List<DatasetField> extractFromXmd(String xmdJson, String datasetName) {
        List<DatasetField> results = new ArrayList<>();
        JSONObject root = new JSONObject(xmdJson);

        // Date fields
        if (root.has("dates")) {
            JSONArray dates = root.optJSONArray("dates");
            if (dates != null) {
                for (int i = 0; i < dates.length(); i++) {
                    JSONObject dateObj = dates.optJSONObject(i);
                    if (dateObj != null && dateObj.has("fields")) {
                        JSONArray fields = dateObj.optJSONArray("fields");
                        for (int j = 0; j < fields.length(); j++) {
                            JSONObject fieldObj = fields.optJSONObject(j);
                            if (fieldObj != null) {
                                String fullField = fieldObj.optString("fullField");
                                String label = fieldObj.optString("label", fullField);
                                results.add(new DatasetField(datasetName, fullField, label, "Date"));
                            }
                        }
                    }
                }
            }
        }

        // Dimension fields
        if (root.has("dimensions")) {
            JSONArray dimensions = root.optJSONArray("dimensions");
            if (dimensions != null) {
                for (int i = 0; i < dimensions.length(); i++) {
                    JSONObject dim = dimensions.optJSONObject(i);
                    if (dim != null) {
                        String fieldName = dim.optString("field");
                        String label = dim.optString("label", fieldName);
                        results.add(new DatasetField(datasetName, fieldName, label, "Dimension"));
                    }
                }
            }
        }

        // Measure fields
        if (root.has("measures")) {
            JSONArray measures = root.optJSONArray("measures");
            if (measures != null) {
                for (int i = 0; i < measures.length(); i++) {
                    JSONObject meas = measures.optJSONObject(i);
                    if (meas != null) {
                        String fieldName = meas.optString("field");
                        String label = meas.optString("label", fieldName);
                        results.add(new DatasetField(datasetName, fieldName, label, "Measure"));
                    }
                }
            }
        }

        return results;
    }
}
