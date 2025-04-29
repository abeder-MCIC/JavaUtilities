package com.mcic.wave;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mcic.wave.DatasetBuilder;

public class DashboardReader extends SalesforceApp {
    private String apiName;
    private String label;
    private String app;

    public static void main(String[] args) {
        SalesforceApp app = new DashboardReader(args);
        app.execute();
    }

    public DashboardReader(String[] args) {
        super(args);
        String[] na = getArgument("-n", "-name");
        apiName = (na != null && na.length > 0) ? na[0] : "Dashboards";
        String[] la = getArgument("-l", "-label");
        label   = (la != null && la.length > 0) ? la[0] : "Dashboards";
        String[] aa = getArgument("-a", "-app");
        app     = (aa != null && aa.length > 0) ? aa[0] : "RDI_Inventory";
    }

    @Override
    public void execute() {
        SalesforceREST agent = getAgent();
        Collection<WaveDashboard> dashboards = getDashboards().values();
        try {
            DatasetBuilder db = new DatasetBuilder(
                "DashboardName", "Application", "MasterLabel", "Id", "CreatedBy", "LastModifiedBy"
            );
            for (WaveDashboard d : dashboards) {
                db.addRecord(d.name, d.appName, d.label, d.id, d.createdBy, d.lastModifiedBy);
            }
            agent.writeDataset(apiName, label, app, db);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, WaveDashboard> getDashboards() {
        SalesforceREST agent = getAgent();
        Map<String, WaveDashboard> dashboards = new TreeMap<>();
        String nextURL = "/services/data/v58.0/wave/dashboards";
        while (nextURL != null && !"null".equals(nextURL)) {
        	System.out.print("Reading dashboards: " + nextURL + "...");
            int res = agent.get(nextURL, null);
        	System.out.println("done");
            if (res == SalesforceREST.FAILURE) {
                System.err.println("Error retrieving dashboard data");
                System.exit(1);
            }
            JSONObject resp = agent.getResponse();
            JSONArray items = resp.getJSONArray("dashboards");
            for (int i = 0; i < items.length(); i++) {
                JSONObject node = items.getJSONObject(i);
                String[] vals = scrapeJSON(node,
                    "name", "folder.label", "label", "id", "createdBy.name", "lastModifiedBy.name"
                );
                WaveDashboard wd = new WaveDashboard(vals);
                dashboards.put(wd.name, wd);
            }
            nextURL = resp.optString("nextPageUrl", null);
        }
        return dashboards;
    }

    public String[] scrapeJSON(JSONObject node, String... fields) {
        String[] output = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String[] parts = fields[i].split("\\.");
            JSONObject cursor = node;
            for (int j = 0; j < parts.length - 1; j++) {
                cursor = cursor.getJSONObject(parts[j]);
            }
            output[i] = cursor.getString(parts[parts.length - 1]);
        }
        return output;
    }

    public static class WaveDashboard {
        public String name, appName, label, id, createdBy, lastModifiedBy;
        public WaveDashboard(String... args) {
            this.name            = args[0];
            this.appName         = args[1];
            this.label           = args[2];
            this.id              = args[3];
            this.createdBy       = args[4];
            this.lastModifiedBy  = args[5];
        }
    }
}
