package com.mcic.anbalytics.wavemetadata.parser;

import com.mcic.anbalytics.wavemetadata.model.DashboardField;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans SAQL scripts for load statements and quoted fields.
 */
public class SaqlQueryParser {
    private static final Pattern LOAD_PATTERN = Pattern.compile("load \\\"(\\w+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_PATTERN = Pattern.compile("'([A-Za-z0-9_]+)'");

    /**
     * Parse SAQL script for dataset loads and field references.
     * @param script full SAQL or pigql string
     * @param stepName the dashboard step name
     * @return list of DashboardField entries with datasetName and fieldName
     */
    public List<DashboardField> parseSaql(String script, String stepName) {
        List<DashboardField> results = new ArrayList<>();
        Matcher loadMatcher = LOAD_PATTERN.matcher(script);
        while (loadMatcher.find()) {
            String datasetName = loadMatcher.group(1);
            Matcher fieldMatcher = FIELD_PATTERN.matcher(script);
            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                if (fieldName != null && !fieldName.isEmpty()) {
                    results.add(new DashboardField(null, stepName, datasetName, fieldName));
                }
            }
        }
        return results;
    }
}
