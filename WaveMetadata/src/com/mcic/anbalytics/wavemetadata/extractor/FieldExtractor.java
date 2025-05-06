// src/com/mcic/anbalytics/wavemetadata/extractor/FieldExtractor.java
package com.mcic.anbalytics.wavemetadata.extractor;

import com.mcic.anbalytics.wavemetadata.model.DashboardField;
import com.mcic.anbalytics.wavemetadata.parser.JsonQueryParser;
import com.mcic.anbalytics.wavemetadata.parser.SaqlQueryParser;
import com.mcic.anbalytics.wavemetadata.util.ProgressLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates extraction of global filters, data-source links, and step-level queries.
 */
public class FieldExtractor {
    private final JsonQueryParser jsonParser = new JsonQueryParser();
    private final SaqlQueryParser saqlParser = new SaqlQueryParser();
    private final ProgressLogger logger = new ProgressLogger(FieldExtractor.class);
    private static final String GLOBAL_STEP = "GlobalFilter";
    private static final String LINK_STEP   = "DataSourceLink";

    public List<DashboardField> extractGlobalFilters(JSONObject state) {
        List<DashboardField> results = new ArrayList<>();
        try {
            JSONArray filters = state.optJSONArray("filters");
            if (filters != null) {
                for (int i = 0; i < filters.length(); i++) {
                    JSONObject filter = filters.optJSONObject(i);
                    if (filter != null) {
                        JSONObject ds = filter.optJSONObject("dataset");
                        String datasetName = ds != null ? ds.optString("name") : null;
                        JSONArray fields = filter.optJSONArray("fields");
                        if (fields != null) {
                            for (int j = 0; j < fields.length(); j++) {
                                String fieldName = fields.optString(j);
                                results.add(new DashboardField(null, GLOBAL_STEP, datasetName, fieldName));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting global filters", e);
        }
        return results;
    }

    public List<DashboardField> extractDataSourceLinks(JSONObject state) {
        List<DashboardField> results = new ArrayList<>();
        try {
            JSONObject dsInfo = state.optJSONObject("dataSourceLinksInfo");
            if (dsInfo != null) {
                JSONArray links = dsInfo.optJSONArray("links");
                if (links != null) {
                    for (int i = 0; i < links.length(); i++) {
                        JSONObject link = links.optJSONObject(i);
                        if (link != null) {
                            JSONArray fields = link.optJSONArray("fields");
                            if (fields != null) {
                                for (int j = 0; j < fields.length(); j++) {
                                    JSONObject f = fields.optJSONObject(j);
                                    String dsName  = f.optString("dataSourceName");
                                    String fldName = f.optString("fieldName");
                                    results.add(new DashboardField(null, LINK_STEP, dsName, fldName));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting data-source links", e);
        }
        return results;
    }

    public List<DashboardField> extractStepLevel(JSONObject state, String stepName) {
        List<DashboardField> results = new ArrayList<>();
        try {
            JSONObject stepsNode = state.optJSONObject("steps");
            if (stepsNode != null && stepsNode.has(stepName)) {
                JSONObject stepObj = stepsNode.optJSONObject(stepName);
                // determine datasets for this step
                List<String> dsNames = new ArrayList<>();
                JSONArray dsArr = stepObj.optJSONArray("datasets");
                if (dsArr != null) {
                    for (int i = 0; i < dsArr.length(); i++) {
                        dsNames.add(dsArr.optJSONObject(i).optString("name"));
                    }
                }
                // compact JSON
                JSONObject queryObj = stepObj.optJSONObject("query");
                if (queryObj != null) {
                    List<DashboardField> parsed = jsonParser.parseCompact(queryObj, stepName);
                    for (String ds : dsNames) {
                        parsed.forEach(df -> df.setDatasetName(ds));
                        results.addAll(parsed);
                    }
                }
                // escaped JSON string
                else if (stepObj.has("query")) {
                    String queryStr = stepObj.optString("query");
                    String unescaped = queryStr.replace("&quot;", '"'+"")
                                                .replace("&#92;", "\\");
                    JSONObject inner = new JSONObject(unescaped);
                    List<DashboardField> parsed = jsonParser.parseCompact(inner, stepName);
                    for (String ds : dsNames) {
                        parsed.forEach(df -> df.setDatasetName(ds));
                        results.addAll(parsed);
                    }
                }
                // SAQL
                String saql = stepObj.optString("pigql", null);
                if (saql == null) saql = stepObj.optString("saql", null);
                if (saql != null) {
                    List<DashboardField> parsed = saqlParser.parseSaql(saql, stepName);
                    results.addAll(parsed);
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting step-level fields for " + stepName, e);
        }
        return results;
    }
}
