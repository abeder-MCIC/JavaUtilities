package com.mcic.analytics.wavemetadata.parser;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.analytics.wavemetadata.model.DashboardField;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts (dataset, field) pairs from compact JSON queries (including unescaped inner JSON).
 */
public class JsonQueryParser {
    public List<DashboardField> parseCompact(JSONObject queryObj, String stepName) {
        List<DashboardField> results = new ArrayList<>();
        // Extract measures
        if (queryObj.has("measures")) {
            JSONArray measures = queryObj.optJSONArray("measures");
            if (measures != null) {
                for (int i = 0; i < measures.length(); i++) {
                    Object measureItem = measures.get(i);
                    if (measureItem instanceof JSONArray) {
                        JSONArray measure = (JSONArray) measureItem;
                        if (measure.length() >= 2) {
                            String fieldName = measure.optString(1);
                            results.add(new DashboardField(null, stepName, null, fieldName));
                        }
                    }
                }
            }
        }
        // Extract groups
        if (queryObj.has("groups")) {
            JSONArray groups = queryObj.optJSONArray("groups");
            if (groups != null) {
                for (int i = 0; i < groups.length(); i++) {
                    Object groupItem = groups.get(i);
                    if (groupItem instanceof JSONArray) {
                        JSONArray groupArray = (JSONArray) groupItem;
                        for (int j = 0; j < groupArray.length(); j++) {
                            String fieldName = groupArray.optString(j);
                            results.add(new DashboardField(null, stepName, null, fieldName));
                        }
                    }
                }
            }
        }
        // Extract values
        if (queryObj.has("values")) {
            JSONArray values = queryObj.optJSONArray("values");
            if (values != null) {
                for (int i = 0; i < values.length(); i++) {
                    Object valItem = values.get(i);
                    if (valItem instanceof JSONArray) {
                        JSONArray valArr = (JSONArray) valItem;
                        for (int j = 0; j < valArr.length(); j++) {
                            String fieldName = valArr.optString(j);
                            results.add(new DashboardField(null, stepName, null, fieldName));
                        }
                    } else if (valItem instanceof String) {
                        results.add(new DashboardField(null, stepName, null, (String) valItem));
                    }
                }
            }
        }
        // Extract filters
        if (queryObj.has("filters")) {
            JSONArray filters = queryObj.optJSONArray("filters");
            if (filters != null) {
                for (int i = 0; i < filters.length(); i++) {
                    Object filterItem = filters.get(i);
                    if (filterItem instanceof JSONArray) {
                        JSONArray filterArr = (JSONArray) filterItem;
                        for (int j = 0; j < filterArr.length(); j++) {
                            Object f = filterArr.get(j);
                            if (f instanceof String) {
                                results.add(new DashboardField(null, stepName, null, (String) f));
                            } else if (f instanceof JSONArray) {
                                JSONArray nested = (JSONArray) f;
                                for (int k = 0; k < nested.length(); k++) {
                                    String fname = nested.optString(k);
                                    results.add(new DashboardField(null, stepName, null, fname));
                                }
                            }
                        }
                    }
                }
            }
        }
        return results;
    }
}
