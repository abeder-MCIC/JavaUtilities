package com.mcic.analytics.wavemetadata;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.analytics.wavemetadata.DashboardReader.WaveDashboard;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Salesforce CRM Analytics dashboard JSON and collects
 * all dataset + field combinations used in its steps (queries),
 * including filters at both step and dashboard levels.
 */
public class DashboardFieldReader {

    public static void main(String[] args) {
    	try {
			BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\abeder\\Documents\\Dashboard.json"));
			String line;
			String json = "";
			while ((line = in.readLine()) != null) {
				json += line + "\n";
			}
			in.close();
			JSONObject node = new JSONObject(json);
			Set<DashboardField> fields = parseDashboard("Test", node);
			System.out.println("Stop!");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /** Immutable triple of stepName, datasetName, and fieldName. */
    public static class DashboardField {
        public final String stepName;
        public final String datasetName;
        public final String fieldName;
        public final String dashboardName;

        public DashboardField(String stepName, String datasetName, String fieldName, String dashboardName) {
            this.stepName    = stepName;
            this.datasetName = datasetName;
            this.fieldName   = fieldName;
            this.dashboardName = dashboardName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DashboardField)) return false;
            DashboardField that = (DashboardField) o;
            return Objects.equals(stepName,    that.stepName)    &&
                   Objects.equals(datasetName, that.datasetName) &&
                   Objects.equals(fieldName,   that.fieldName)   &&
                   Objects.equals(dashboardName,   that.dashboardName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dashboardName, stepName, datasetName, fieldName);
        }

        @Override
        public String toString() {
            return dashboardName + " → " + stepName + " → " + datasetName + " → " + fieldName;
        }
    }

    // Regex to find SAQL loads: load "0Fb…"
    private static final Pattern LOAD_PATTERN  =
        Pattern.compile("load\\s+\"(.*?)\"", Pattern.CASE_INSENSITIVE);
    // Regex to find anything in single-quotes: 'FieldName'
    private static final Pattern FIELD_PATTERN =
        Pattern.compile("'([^']+)'");

    /**
     * Fetch the dashboard via REST, then scan global filters,
     * dataSourceLinksInfo.links, and each step for fields.
     */
    
    public static Set<DashboardField> getFields(WaveDashboard dashboard, SalesforceREST agent){
        String nextURL = "/services/data/v58.0/wave/dashboards/" + dashboard.id;
        int res = agent.get(nextURL, null);
        if (res == SalesforceREST.FAILURE) {
            System.err.println("Error retrieving dashboard data");
            System.exit(1);
        }
        return parseDashboard(dashboard.name, agent.getResponse());
    }
    
    public static Set<DashboardField> parseDashboard(String dashName, JSONObject resp) {
        Set<DashboardField> fields = new LinkedHashSet<>();
        JSONObject state = resp.getJSONObject("state");

        // 1) Global dashboard filters
        JSONArray globalFilters = state.optJSONArray("filters");
        if (globalFilters != null) {
            for (int i = 0; i < globalFilters.length(); i++) {
                JSONObject gf = globalFilters.getJSONObject(i);
                JSONObject dsObj = gf.optJSONObject("dataset");
                if (dsObj != null) {
                    String dsName = dsObj.optString("name", dsObj.optString("id"));
                    scanFieldArray(dashName, "Global",
                                   gf.optJSONArray("fields"),
                                   Collections.singletonList(dsName),
                                   fields);
                }
            }
        }

        // 2) dataSourceLinksInfo → links
        JSONObject linksInfo = state.optJSONObject("dataSourceLinksInfo");
        if (linksInfo != null) {
            JSONArray links = linksInfo.optJSONArray("links");
            if (links != null) {
                for (int i = 0; i < links.length(); i++) {
                    JSONObject link = links.getJSONObject(i);
                    JSONObject dsObj = link.optJSONObject("dataset");
                    if (dsObj != null) {
                        String dsName = dsObj.optString("name", dsObj.optString("id"));
                        scanFieldArray(dashName, "Global",
                                       link.optJSONArray("fields"),
                                       Collections.singletonList(dsName),
                                       fields);
                    }
                }
            }
        }

        // 3) Steps
        JSONObject steps = state.getJSONObject("steps");
        for (String stepKey : steps.keySet()) {
            JSONObject step = steps.getJSONObject(stepKey);
            recurseExtract(dashName, stepKey, step, fields);
        }

        return Collections.unmodifiableSet(fields);
    }

    /**
     * Walk any JSON structure looking for "query" blocks or SAQL strings.
     */
    private static void recurseExtract(String dashName, String stepName,
                                       JSONObject obj,
                                       Set<DashboardField> fields) {
        for (String key : obj.keySet()) {
            Object value = obj.get(key);

            // Simple/compact query block
            if ("query".equalsIgnoreCase(key) && value instanceof JSONObject) {
                JSONObject query = (JSONObject) value;
                extractFromSimpleQuery(dashName, stepName, obj, query, fields);
            }
            // SAQL embedded directly as a string
            else if (("query".equalsIgnoreCase(key) || "saql".equalsIgnoreCase(key))
                     && value instanceof String) {
                extractFromSAQL(dashName, stepName, (String) value, fields);
            }
            // Recurse into child objects
            else if (value instanceof JSONObject) {
                recurseExtract(dashName, stepName, (JSONObject) value, fields);
            }
            // Arrays may contain more queries or SAQL/scripts
            else if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray) value;
                for (int i = 0; i < arr.length(); i++) {
                    Object el = arr.get(i);
                    if (el instanceof JSONObject) {
                        recurseExtract(dashName, stepName, (JSONObject) el, fields);
                    } else if (el instanceof String
                               && ("query".equalsIgnoreCase(key)
                                   || "saql".equalsIgnoreCase(key))) {
                        extractFromSAQL(dashName, stepName, (String) el, fields);
                    }
                }
            }
        }
    }

    /**
     * Handle the simple/compact JSON query format:
     *   – Pull dataset names (use "name", fallback to "id")
     *   – Descend into any "pigql" script
     *   – Scan measures, groups, values, AND filters
     */
    private static void extractFromSimpleQuery(String dashName, String stepName,
                                               JSONObject step,
                                               JSONObject query,
                                               Set<DashboardField> fields) {
        // Gather datasets
        List<String> datasets = new ArrayList<>();
        JSONArray dsArr = step.optJSONArray("datasets");
        if (dsArr != null) {
            for (int i = 0; i < dsArr.length(); i++) {
                JSONObject dsObj = dsArr.getJSONObject(i);
                String dsName = dsObj.optString("name", dsObj.optString("id"));
                datasets.add(dsName);
            }
        }

        // If there's embedded PigQL, extract from it too
        if (query.has("pigql")) {
            extractFromSAQL(dashName, stepName, query.getString("pigql"), fields);
        }

        // Scan the standard field arrays
        scanFieldArray(dashName, stepName, query.optJSONArray("measures"), datasets, fields);
        scanFieldArray(dashName, stepName, query.optJSONArray("groups"),   datasets, fields);
        scanFieldArray(dashName, stepName, query.optJSONArray("values"),   datasets, fields);

        // ALSO scan any filters on this query or step
        scanFieldArray(dashName, stepName, query.optJSONArray("filters"), datasets, fields);
        scanFieldArray(dashName, stepName, step.optJSONArray("filters"),  datasets, fields);
    }

    /**
     * Recursively scan ANY JSONArray of fields (or nested arrays),
     * adding each String element against every dataset in the list.
     */
    private static void scanFieldArray(String dashName, String stepName,
                                       JSONArray fieldArray,
                                       List<String> datasets,
                                       Set<DashboardField> fields) {
        if (fieldArray == null) return;
        for (int i = 0; i < fieldArray.length(); i++) {
            Object elem = fieldArray.get(i);
            if (elem instanceof JSONArray) {
                // Nested array → dive in
                scanFieldArray(dashName, stepName, (JSONArray) elem, datasets, fields);
            }
            else if (elem instanceof String) {
                String fld = (String) elem;
                for (String ds : datasets) {
                    addField(dashName, stepName, ds, fld, fields);
                }
            }
            // other types (e.g. JSONObject) are ignored here
        }
    }

    /** Parse a SAQL script: pull out loaded datasets and quoted field names. */
    private static void extractFromSAQL(String dashName, String stepName,
                                        String saql,
                                        Set<DashboardField> fields) {
        List<String> datasets = new ArrayList<>();
        Matcher m = LOAD_PATTERN.matcher(saql);
        while (m.find()) {
            datasets.add(m.group(1));
        }
        if (datasets.isEmpty()) {
            return;
        }
        Matcher fm = FIELD_PATTERN.matcher(saql);
        while (fm.find()) {
            String fld = fm.group(1);
            for (String ds : datasets) {
                addField(dashName, stepName, ds, fld, fields);
            }
        }
    }

    private static void addField(String dashName, String stepName,
                                 String datasetName,
                                 String fieldName,
                                 Set<DashboardField> fields) {
    	DashboardField f = new DashboardField(stepName, datasetName, fieldName, dashName);
        fields.add(f);
        System.out.println("Adding field: " + f.toString());
    }
}
