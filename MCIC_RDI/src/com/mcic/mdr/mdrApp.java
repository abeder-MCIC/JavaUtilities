package com.mcic.mdr;

import java.awt.Component;
import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.mcic.ConfiguredApp;
import com.mcic.sfrest.SalesforceAgent;
import com.mcic.sfrest.SalesforceModel;
import com.mcic.util.json.JSONNode;
import com.mcic.util.json.JSONObject;
import com.mcic.wavemetadata.tool.WaveMetadata;
import com.mcic.wavemetadata.tool.WaveMetadata.Dataset;
import com.mcic.wavemetadata.tool.WaveMetadata.Field;

public class mdrApp extends ConfiguredApp {
	static String IGNORE_NAME = "Ignore";
	static String IGNORE_ID;

	mdrGUI gui;
	WaveMetadata meta;
	SalesforceAgent agent;
	//Map<String, Element> elementIdsByLabel;
	Map<String, Field> fields;
	Collection<String> datasets;
	Map<String, Instance> sfFields;
	Map<String, Element> sfElements;
	LinkedList<Field> missingFields;
	Field currentField;
	
	class Element {
		public String name;
		public String description;
		public String id;
		public Element(String name, String description) {
			this.name = name;
			this.description = description;
		}
		public Element(JSONNode node) {
			name = node.get("Name") != null ? node.get("Name").asString() : null;
			description = node.get("Description__c") != null ? node.get("Description__c").asString() : null;
			id = node.get("id") != null ? node.get("id").asString() : null;
			id = node.get("Id") != null ? node.get("Id").asString() : id;
		}

	}
	
	class Instance {
		public String system;
		public String name;
		public String table;
		public String label;
		public String elementId;
		public String id;
		public Instance(String system, String name, String table) {
			this.system = system;
			this.name = name;
			this.table = table;
		}
		public Instance(Field field) {
			system = "CRMA";
			name = field.name;
			table = field.parent.name;
			label = field.label;
		}
		public Instance(JSONNode node) {
			name = node.get("Field__c") != null ? node.get("Field__c").asString() : null;
			table = node.get("Table__c") != null ? node.get("Table__c").asString() : null;
			label = node.get("Label__c") != null ? node.get("Label__c").asString() : null;
			system = node.get("System__c") != null ? node.get("System__c").asString() : null;
			id = node.get("id") != null ? node.get("id").asString() : null;
		}
		public String getKey() {
			return system + "-" + table + "-" + name; 
		}
	}
	
	public static void main(String[] args) {
		mdrApp app = new mdrApp();
		Vector<String> additionalArgs = main(args, app);
		try {
			app.gui = new mdrGUI(app);
			app.gui.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			app.gui.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int i = 0;
		while (i < additionalArgs.size()) {
			String cmd = additionalArgs.elementAt(i);
			switch (cmd.toLowerCase()) {
			case "-l":
				app.loadSalesforce();
				break;
			default:
				break;
			}
			i++;
		}
		app.init();
	}

	public mdrApp() {
		//elementIdsByLabel = new TreeMap<String, Element>();
		sfFields = new TreeMap<String, Instance>();
		sfElements = new TreeMap<String, Element>();
		missingFields = new LinkedList<Field>();
		fields = new TreeMap<String, Field>();
	}
	
	public void init() {
		if (meta == null) {
			File propFile = (File)properties.get("sfConfig"); 
			datasets = (Collection<String>)properties.get("datasets");
			SalesforceModel model = new SalesforceModel(propFile);
			agent = new SalesforceAgent(model);
			meta = new WaveMetadata(agent);
			meta.loadDatasets(datasets);
			if (datasets == null) {
				datasets = meta.getBaseDatasets().keySet();
			}
		}		
	}
	
	public void loadSalesforce() {
		gui.btnCreateNewInstance.setEnabled(false);
		gui.scanFieldsButton.setEnabled(false);

		if (meta == null) {
			init();
		}
		
		//  Load data elements
		String url = "/services/data/v60.0/query/?q=" + "SELECT Description__c,Id,Name FROM MDR_Data_Element__c".replace(' ', '+');
		JSONNode l = agent.get(url, null);
		sfElements.clear();
		for (JSONNode n : l.get("records").values()) {
			String name = n.get("Name").asString();
			sfElements.put(name, new Element(n));
		}
		IGNORE_ID = sfElements.get(IGNORE_NAME).id;
		
		//  Load data element instances
		url = "/services/data/v60.0/query/?q=" + "SELECT Id, Data_Element__c,Field__c,Table__c,System__c FROM MDR_Data_Element_Instance__c".replace(' ', '+');;
		l = agent.get(url, null);
		sfFields.clear();
		for (JSONNode n : l.get("records").values()) {
			String name = n.get("System__c").asString() + "-" + n.get("Table__c").asString() + "-" + n.get("Field__c").asString();
			sfFields.put(name, new Instance(n));
		}
		
		
		for (String datasetName : datasets) {
			Collection<Field> waveFields = meta.getDataset(datasetName).fields.values();
			for (Field field : waveFields) {
				String instanceKey = "CRMA-" + datasetName + "-" + field.name;
				boolean exists = sfFields.containsKey(instanceKey);
				if (!exists) {
					missingFields.add(field);
				}
			}
		}
		String val = missingFields.size() + "";
		gui.newElelementsLabel.setText(val);
		gui.newFieldsLabel.setText(val);
		gui.btnCreateNewInstance.setEnabled(true);
		gui.scanFieldsButton.setEnabled(true);
		gui.fieldLabel.setText(sfFields.size() + "");
		gui.sfFields.setText((sfFields.size() + missingFields.size()) + "");
		
	}
	

	public void loadNextInstance() {
		currentField = null;
		while (currentField == null) {
			currentField = missingFields.pop();
			Instance inst = null;
			for (Instance i : sfFields.values()) {
				if (i.system.equals("CRMA") && i.name.equalsIgnoreCase(currentField.name)) {
					inst = i;
				}
			}
			if (inst != null) {
				createInstance(inst.elementId);
				currentField = null;
			}
		}
		if (currentField != null) {
			String elementName = currentField.name;
			gui.elementChoicePanel.lblElementName.setText(currentField.name);
			gui.elementChoicePanel.lblElementLabel.setText(currentField.label);
			gui.elementChoicePanel.update();
		}
	}
	
	public String createElement(String name, String description) {
		JSONObject o = new JSONObject();
		o.addString("Name", name);
		o.addString("Description__c", description);
		String url = "/services/data/v60.0/sobjects/MDR_Data_Element__c/";
		JSONNode ret = agent.postJSON(url, o);
		String id = ret.get("id").asString();
		o.addString("id", id);
		sfElements.put(name, new Element(o));
		return id;
	}
	
	public String createInstance(String elementId) {
		JSONObject o = new JSONObject();
		o.addString("Field__c", currentField.name);
		o.addString("Table__c", currentField.parent.name);
		o.addString("Data_Element__c", elementId);
		o.addString("System__c", "CRMA");
		String url = "/services/data/v60.0/sobjects/MDR_Data_Element_Instance__c/";
		JSONNode ret = agent.postJSON(url, o);
		String id = ret.get("id").asString();
		String name = o.get("System__c").asString() + "-" + o.get("Table__c").asString() + "-" + o.get("Field__c").asString();
		o.addString("id", id);
		sfFields.put(name, new Instance(o));
		loadNextInstance();
		return id;
	}
		
	/*
	public void updateMDR() {	
		List<Field> fields = new LinkedList<Field>();
		for (String datasetName : datasets) {
			Dataset dataset = meta.getDataset(datasetName);
			for (Field field : dataset.fields.values()) {
				if (!sfFields.contains(field)) {
					fields.add(field);
				}
			}
		}
		
		for (Field field : fields) {
			JSONObject o = new JSONObject();
			String elementId = getElement(field.label);
			o.addString("Element__c", elementId);
			o.addString("Table__c", field.parent.name);
			o.addString("Field__c", field.name);
			o.addString("System__c", "CRMA");
			JSONNode n = agent.postJSON("/services/data/v58.0/sobjects/MDR_Data_Element_Instance__c/", o);
		}

	}
	*/
	/*
	public String getElement(String label) {
		String id = elementIdsByLabel.get(label).id;
		if (id == null) {
			JSONObject e = new JSONObject();
			e.addString("Name", label);
			JSONNode n = agent.postJSON("/services/data/v58.0/sobjects/MDR_Data_Element__c/", e);
			id = n.get("id").asString();
			//elementIdsByLabel.put(label, id);
		}
		return id;
	}
`*/
}
