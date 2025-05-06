// src/com/mcic/anbalytics/wavemetadata/AppLauncher.java
package com.mcic.anbalytics.wavemetadata;

import com.mcic.anbalytics.wavemetadata.config.ConfigManager;
import com.mcic.anbalytics.wavemetadata.auth.OAuthService;
import com.mcic.anbalytics.wavemetadata.client.SalesforceApiClient;
import com.mcic.anbalytics.wavemetadata.extractor.FieldExtractor;
import com.mcic.anbalytics.wavemetadata.extractor.XmdExtractor;
import com.mcic.anbalytics.wavemetadata.model.*;
import com.mcic.anbalytics.wavemetadata.output.CsvWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class AppLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java AppLauncher <propertiesFile> <outputDir>");
            System.exit(1);
        }
        ConfigManager cfg = new ConfigManager();
        cfg.load(args[0]);
        OAuthService auth = new OAuthService();
        auth.authenticate(cfg);
        SalesforceApiClient client = new SalesforceApiClient(cfg, auth);

        int poolSize = Optional.ofNullable(cfg.get("threadPoolSize"))
                               .filter(s -> !s.isEmpty())
                               .map(Integer::parseInt)
                               .orElse(10);
        ExecutorService dashExec = Executors.newFixedThreadPool(poolSize);
        ExecutorService dsExec   = Executors.newFixedThreadPool(poolSize);

        List<Dashboard> dashboards = client.fetchAllDashboards();
        List<Dataset>   datasets   = client.fetchAllDatasets();

        List<DashboardField> allFields   = Collections.synchronizedList(new ArrayList<>());
        List<JunctionEntry>   allJuncs    = Collections.synchronizedList(new ArrayList<>());

        for (Dashboard dash : dashboards) {
            dashExec.submit(() -> {
                try {
                    JSONObject root  = new JSONObject(client.fetchDashboardJson(dash.getId()));
                    JSONObject state = root.optJSONObject("state");
                    if (state != null) {
                        FieldExtractor ext = new FieldExtractor();
                        // globals once
                        ext.extractGlobalFilters(state).forEach(df -> {
                            df.setDashboardName(dash.getName());
                            allFields.add(df);
                        });
                        // data-source links once
                        ext.extractDataSourceLinks(state).forEach(df -> {
                            df.setDashboardName(dash.getName());
                            allFields.add(df);
                        });
                        // step-level
                        JSONObject steps = state.optJSONObject("steps");
                        if (steps != null) {
                            for (String step : steps.keySet()) {
                                ext.extractStepLevel(state, step).forEach(df -> {
                                    df.setDashboardName(dash.getName());
                                    allFields.add(df);
                                    allJuncs.add(new JunctionEntry(dash.getName(), df.getDatasetName()));
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing " + dash.getName() + ": " + e.getMessage());
                }
            });
        }
        dashExec.shutdown(); dashExec.awaitTermination(1, TimeUnit.HOURS);
        
        List<DatasetField> allDsFields = Collections.synchronizedList(new ArrayList<>());
        XmdExtractor xmdExt = new XmdExtractor();
        for (Dataset ds : datasets) {
            dsExec.submit(() -> {
                try {
                    String xmd = client.fetchXmd(ds.getVersionUrl());
                    allDsFields.addAll(xmdExt.extractFromXmd(xmd, ds.getName()));
                } catch (Exception e) {
                    System.err.println("Error processing XMD for " + ds.getName());
                }
            });
        }
        dsExec.shutdown(); dsExec.awaitTermination(1, TimeUnit.HOURS);

        // dedupe fields
        Set<String> seen = new LinkedHashSet<>();
        List<DashboardField> uniqueFields = new ArrayList<>();
        for (DashboardField f : allFields) {
            String key = f.getDashboardName() + '|' + f.getStepName() + '|' + f.getDatasetName() + '|' + f.getFieldName();
            if (seen.add(key)) uniqueFields.add(f);
        }
        
        // dedupe junctions
        Set<String> seenJ = new LinkedHashSet<>();
        List<JunctionEntry> uniqueJunc = new ArrayList<>();
        for (JunctionEntry j : allJuncs) {
            String key = j.getDashboardName() + '|' + j.getDatasetName();
            if (seenJ.add(key)) uniqueJunc.add(j);
        }

        CsvWriter writer = new CsvWriter();
        Path outDir = Paths.get(args[1]);
        writer.writeDashboards(dashboards, outDir.resolve("dashboards.csv"));
        writer.writeDatasets(datasets,     outDir.resolve("datasets.csv"));
        writer.writeDashboardFields(uniqueFields, outDir.resolve("dashboard_fields.csv"));
        writer.writeDatasetFields(allDsFields,    outDir.resolve("dataset_fields.csv"));
        writer.writeJunctions(uniqueJunc,         outDir.resolve("dashboard_dataset_junction.csv"));

        System.out.println("Completed. Outputs in " + args[1]);
    }
}
