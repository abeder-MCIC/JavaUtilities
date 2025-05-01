package com.mcic.analytics.wavemetadata;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.analytics.wavemetadata.DashboardReader.WaveDashboard;
import com.mcic.analytics.wavemetadata.DatasetReader.WaveDataset;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Salesforce CRM Analytics dashboard JSON and collects
 * all dataset + field combinations used in its steps (queries).
 */
public class DashboardFieldReader {
	
	public static void main(String[] args) {
		try {
			BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\abeder\\Downloads\\query.json"));
			String json = "";
			String line;
			while ((line = in.readLine()) != null) {
				json += line + "\n";
			}
			in.close();
			Set<DashboardField> fields = parseDashboard("test", new JSONObject(json), new HashSet<DashboardField>());
			System.out.println("Done");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

    /** Immutable pair of dataset name and field name. */
    public static class DashboardField {
    	public final String dashboardName;
        public final String datasetName;
        public final String fieldName;

        public DashboardField(String dashboardName, String datasetName, String fieldName) {
        	this.dashboardName = dashboardName;
            this.datasetName = datasetName;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DashboardField)) return false;
            DashboardField other = (DashboardField) o;
            return Objects.equals(datasetName, other.datasetName)
                && Objects.equals(fieldName, other.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dashboardName, datasetName, fieldName);
        }

        @Override
        public String toString() {
            return dashboardName + " → " + datasetName + " → " + fieldName;
        }
    }

    // Regex to find SAQL loads: load "0Fb..."
    private static final Pattern LOAD_PATTERN = Pattern.compile("load\\s+\"(.*?)\"", Pattern.CASE_INSENSITIVE);

    // Regex to find anything in single-quotes: 'FieldName'
    private static final Pattern FIELD_PATTERN = Pattern.compile("'([^']+)'");

    /**
     * Entry point: scan the given dashboard JSON object for ALL dataset/field usage.
     *
     * @param dashboardJson the root JSONObject of the dashboard definition
     */
    public static Set<DashboardField> parseDashboard(String dashboardName, JSONObject dashboardJson, Set<DashboardField> fields) {
        recurseExtract(dashboardName, dashboardJson, fields);
        return fields;
    }

    /**
     * @return an unmodifiable view of all dataset+field pairs extracted so far.
     */
    public static Set<DashboardField> getFields(WaveDashboard dashboard, SalesforceREST agent) {
        Set<DashboardField> fields = new LinkedHashSet<>();
        String nextURL = "/services/data/v62.0/wave/dashboards/" + dashboard.id; // List resource for datasets :contentReference[oaicite:0]{index=0}

        int res = agent.get(nextURL, null);
        if (res != SalesforceREST.SUCCESS) {
        	System.out.println("Query error!");
        	System.exit(1);
        }
        JSONObject steps = agent.getResponse().getJSONObject("state").getJSONObject("steps");

        
        Iterator<String> keys = steps.keys(); // Retrieves all keys in the JSONObject
        
        while (keys.hasNext()) {
            String key = keys.next(); // Get the next key
            JSONObject item = steps.getJSONObject(key); // Retrieve the corresponding JSONObject
            parseDashboard(key, item, fields);
        }
        
        return Collections.unmodifiableSet(fields);
    }

    // Recursively walk any JSON structure looking for "query" objects or SAQL strings
    private static void recurseExtract(String dashboardName, JSONObject obj, Set<DashboardField> fields) {
        for (String key : obj.keySet()) {
            Object value = obj.get(key);

            // Simple/compact query block
            if ("query".equalsIgnoreCase(key) && value instanceof JSONObject) {
                extractFromSimpleQuery(dashboardName, (JSONObject) value, fields);
            }
            // SAQL is sometimes embedded as a string under "query" or "saql"
            else if (( "query".equalsIgnoreCase(key) || "saql".equalsIgnoreCase(key) )
                      && value instanceof String) {
                extractFromSAQL(dashboardName, (String) value, fields);
            }
            // Dive into nested objects
            else if (value instanceof JSONObject) {
                recurseExtract(dashboardName, (JSONObject) value, fields);
            }
            // Arrays may contain more objects or SAQL strings
            else if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray) value;
                for (int i = 0; i < arr.length(); i++) {
                    Object el = arr.get(i);
                    if (el instanceof JSONObject) {
                        recurseExtract(dashboardName, (JSONObject) el, fields);
                    } else if (el instanceof String
                               && ("query".equalsIgnoreCase(key) || "saql".equalsIgnoreCase(key))) {
                        extractFromSAQL(dashboardName, (String) el, fields);
                    }
                }
            }
        }
    }

    // Handle compact/simple JSON query blocks
    private static void extractFromSimpleQuery(String dashboardName, JSONObject query, Set<DashboardField> fields) {
        // Collect declared datasets
        List<String> datasets = new ArrayList<>();
        JSONArray dsArr = query.optJSONArray("datasets");
        if (dsArr != null) {
            for (int i = 0; i < dsArr.length(); i++) {
                datasets.add(dsArr.getString(i));
            }
        }

        // Helper: scan any array of field names (fields, group, measures)
        scanFieldArray(dashboardName, query.optJSONArray("fields"), datasets, fields);
        scanFieldArray(dashboardName, query.optJSONArray("group"), datasets, fields);
        scanFieldArray(dashboardName, query.optJSONArray("measures"), datasets, fields);
    }

    // Add every field in this array for each dataset
    private static void scanFieldArray(String dn, JSONArray fieldArray, List<String> datasets, Set<DashboardField> fields) {
        if (fieldArray == null) return;
        for (int i = 0; i < fieldArray.length(); i++) {
            String fld = fieldArray.getString(i);
            for (String ds : datasets) {
                addField(dn, ds, fld, fields);
            }
        }
    }

    // Parse a SAQL script: pull out loads & quoted field names
    private static void extractFromSAQL(String dn, String saql, Set<DashboardField> fields) {
        // find all dataset loads
        List<String> datasets = new ArrayList<>();
        Matcher m = LOAD_PATTERN.matcher(saql);
        while (m.find()) {
            datasets.add(m.group(1));
        }
        if (datasets.isEmpty()) {
            // nothing to map fields onto
            return;
        }

        // find all 'fields' in single quotes
        Matcher fm = FIELD_PATTERN.matcher(saql);
        while (fm.find()) {
            String fld = fm.group(1);
            for (String ds : datasets) {
                addField(dn, ds, fld, fields);
            }
        }
    }

    private static void addField(String dashboardName, String datasetName, String fieldName, Set<DashboardField> fields) {
        fields.add(new DashboardField(dashboardName, datasetName, fieldName));
    }

}
