package com.mcic.analytics.wavemetadata;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mcic.analytics.wavemetadata.DashboardFieldReader.DashboardField;
import com.mcic.analytics.wavemetadata.DashboardReader.WaveDashboard;
import com.mcic.analytics.wavemetadata.DatasetReader.WaveDataset;
import com.mcic.analytics.wavemetadata.DatasetReader.WaveDatasetField;

public class WaveMetadata extends SalesforceApp{
	private Map<String, WaveDataset> datasets = null;
	private Map<String, WaveDashboard> dashboards = null;
	private Map<String, Set<WaveDatasetField>> datasetFields = new TreeMap<String, Set<WaveDatasetField>>();
	private Map<String, Set<DashboardField>> dashboardFields = new ConcurrentHashMap<String, Set<DashboardField>>();
	private SalesforceREST agent;
	
	public static void main(String[] args) {
		WaveMetadata app = new WaveMetadata();
		app.setArgs(args);
		app.setAgent(app.getAgent());
		app.execute();
	}
	
	public void execute() {
		try {
			DatasetBuilder db;
			/*
			//  Load Dashboard records
			db = new DatasetBuilder("DashbaordName", "Application", "MasterLabel", "Id", "CreatedBy", "LastModifiedBy");
			for (WaveDashboard d : getDashboards().values()) {
				db.addRecord(d.name, d.appName, d.label, d.id, d.createdBy, d.lastModifiedBy);
			}
			getAgent().writeDataset("Dashboards", "Dashboards", "RDI_Inventory", db);

		
			//  Load Dataset records
			db = new DatasetBuilder("DatasetName", "MasterLabel", "Id", "Application");
			for (WaveDataset d : getDatasets().values()) {
				db.addRecord(d.name, d.label, d.id, d.appName);
			}
			getAgent().writeDataset("Datasets", "Datasets", "RDI_Inventory", db);
			
			//  Load Dashboard Field records
			db = new DatasetBuilder("DashboardName", "DatasetName", "FieldName");
			for (DashboardField f : getAllDashboardFields()) {
				db.addRecord(f.dashboardName, f.datasetName, f.fieldName);
			}			
			getAgent().writeDataset("DashboardFields", "DashboardFields", "RDI_Inventory", db);

			//  Load Dataset Field records
//			db = new DatasetBuilder("DashboardName", "DatasetName", "FieldName");
//			for (DashboardField f : getAllDashboardFields()) {
//				db.addRecord(f.dashboardName, f.datasetName, f.fieldName);
//			}			
//			getAgent().writeDataset("DashboardFields", "DashboardFields", "RDI_Inventory", db);
			*/
			
			db = new DatasetBuilder("DashboardName", "DatasetName");
			Set<String> dashDatasets = new TreeSet<String>();
			for (DashboardField f : getAllDashboardFields()) {
				String val = f.dashboardName + "`" + f.datasetName;
				dashDatasets.add(val);
			}
			for (String val : dashDatasets) {
				String[] parts = val.split("`");
				db.addRecord(parts[0], parts[1]);
			}
			getAgent().writeDataset("DashboardDatasetJunction", "DashboardDatasetJunction", "RDI_Inventory", db);

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setAgent(SalesforceREST agent) {
		this.agent = agent;
	}

	public Map<String, WaveDataset> getDatasets() {
		if (datasets == null){
            datasets = new TreeMap<String, WaveDataset>();
            datasets.putAll(DatasetReader.getDatasets(agent));
		}
		return datasets;
	}
	
	public Map<String, WaveDashboard> getDashboards() {
		if (dashboards == null){
			dashboards = new TreeMap<String, WaveDashboard>();
			dashboards.putAll(DashboardReader.getDashboards(agent));
		}
		return dashboards;
	}
	
	public Set<WaveDatasetField> getDatasetFields(WaveDataset dataset) {
		String n = dataset.name;
		if (!datasetFields.containsKey(n)){
			datasetFields.put(n, DatasetReader.getFields(dataset, agent));
		}
		return datasetFields.get(n);
	}
	
	public Set<DashboardField> getDashboardFields(WaveDashboard dashboard) {
		String n = dashboard.toString();
		if (!dashboardFields.containsKey(n)){
			dashboardFields.put(n, DashboardFieldReader.getFields(dashboard, agent));
		}
		return dashboardFields.get(n);
	}
	
	public Set<WaveDatasetField> getAllDatasetFields(){
		Set<WaveDatasetField> allFields = new HashSet<WaveDatasetField>();
		for (WaveDataset ds : getDatasets().values()) {
			allFields.addAll(getDatasetFields(ds));
		}
		return allFields;
	}

	public Set<DashboardField> getAllDashboardFields(){
		Set<DashboardField> allFields = new HashSet<DashboardField>();
		ExecutorService cluster = Executors.newFixedThreadPool(5);
		for (WaveDashboard ds : getDashboards().values()) {
			cluster.execute(new Runnable() {
				public void run() {
					System.out.println("Loading dashboard fields for: " + ds.name);
					Set<DashboardField> fields = getDashboardFields(ds);
					allFields.addAll(fields);
				}
			});
		}
		cluster.shutdown();
		try {
			cluster.awaitTermination(60, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return allFields;
	}
}
