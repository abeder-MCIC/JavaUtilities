package com.mcic.mdr;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import java.awt.Font;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.mcic.mdr.mdrApp.Element;
import com.mcic.util.ButtonColumn;
import com.mcic.util.ButtonTable;
import com.mcic.util.FuzzyScore;
import com.mcic.util.json.JSONNode;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;

public class ElementChoicePanel extends JPanel {
	//static String IGNORE_NAME = "Ignore";
	//private static String IGNORE_ID;
	
	private mdrApp app;
	private JTable table;
	private DefaultTableModel model;
	private LinkedHashMap<String, Integer> distances;
	private String thisName;
	JLabel lblElementName;
	String[] columnNames = {"Element Name", "Description", "Match"};
	//Integer[] currentDistances;
	Element[] elements;
	FuzzyScore fs = new FuzzyScore(Locale.ENGLISH);
	JLabel lblElementLabel;
	private JButton btnNewButton;
	private JScrollPane scrollPane;
	private JButton btnSkip;
	private JButton btnLink;

	/**
	 * Create the panel.
	 */
	
	public void update() {
		
		thisName = app.currentField.label;
		lblElementName.setText(thisName);
		List<Element> eColl = app.sfElements.values().stream().collect(Collectors.toList());
		
		
		Map<String, Integer> currentDistances = new TreeMap<String, Integer>();
		model.setRowCount(0);
		for (Element e : app.sfElements.values()) {
			Integer score = fs.fuzzyScore(e.name, thisName);
			currentDistances.put(e.name, score);
		}

		Collections.sort(eColl, new Comparator<Element>() {

			@Override
			public int compare(Element o1, Element o2) {
				int d1 = currentDistances.get(o1.name);
				int d2 = currentDistances.get(o2.name);
				return d2 - d1;
			}});
		
		for (Element e : eColl) {
			Object[] newRow = { e.name, e.description, currentDistances.get(e.name), "" };
			model.addRow(newRow);
		}

		elements = eColl.toArray(new Element[eColl.size()]);
		model.fireTableDataChanged();
	}
	
	@SuppressWarnings("serial")
	public ElementChoicePanel(mdrApp appInst) {
		app = appInst == null ? new mdrApp() : appInst;
		distances = new LinkedHashMap<String, Integer>();
		
		
		model = new DefaultTableModel(columnNames, 0);
		setLayout(new BorderLayout(0, 0));
		table = new JTable(model);
		//ButtonColumn bc = new ButtonColumn(table, connect, 3);
//		table.getColumn("Action").setCellRenderer(new ButtonTable.ButtonRenderer());
//		table.getColumn("Action").setCellEditor(new ButtonTable.ButtonEditor(new JCheckBox(), new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				int row = Integer.parseInt(e.getActionCommand());
//				String id = elements[row].id;
//				app.createInstance(id);
//			}
//		}));
		scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.SOUTH);
		
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		
		lblElementName = new JLabel("Element Name");
		panel.add(lblElementName);
		
		JButton btnNewElement = new JButton("New Element");
		btnNewElement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ElementForm form = new ElementForm(app);
				form.name.setText(app.currentField.label);
				form.setVisible(true);
				String id = form.id;
				if (id != null) {
					app.createInstance(id);
				}
			}});
		
		lblElementLabel = new JLabel("Element Label");
		panel.add(lblElementLabel);
		panel.add(btnNewElement);
		
		btnNewButton = new JButton("Ignore");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				app.createInstance(app.IGNORE_ID);
			}
		});
		panel.add(btnNewButton);
		
		btnSkip = new JButton("Skip");
		btnSkip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				app.loadNextInstance();
			}
		});
		panel.add(btnSkip);
		
		btnLink = new JButton("Link");
		btnLink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i = table.getSelectedRow();
				if (i >= 0) {
					String id = elements[i].id;
					app.createInstance(id);
				}
			}
		});
		panel.add(btnLink);
		
		
	}

/*
	class ElementTableModel extends AbstractTableModel {		
		@Override
		public void fireTableDataChanged() {
			refresh();
			super.fireTableDataChanged();
		}



		public void refresh() {
			distances.clear();
			currentDistances = new Integer[getRowCount()];
			elements = new Element[app.sfElements.size()];
			int i = 0;
			for (Element e : app.sfElements.values()) {
				elements[i++] = e;
			}
			for (Element n : elements) {
				String name = n.name;
				Integer score = fs.fuzzyScore(name, thisName);
				distances.put(n.name, score);
			}                                                                    
			Arrays.sort(elements, new Comparator<Element>() {
				public int compare(Element o1, Element o2) {
					return distances.get(o1.name) - distances.get(o2.name);
				}});
			for (i = 0;i < elements.length;i++) {
				currentDistances[i] = distances.get(elements[i].name);
			}
		}			
		
		@Override
		public int getRowCount() {
			return app.sfElements.size();
		}

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 2:
				return Integer.class;
			case 3:
				return JButton.class;
			}
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex >= elements.length) {
				return "";
			}
			switch (columnIndex) {
			case 0:
				return elements[rowIndex].name;
			case 1:
				return elements[rowIndex].description;
			case 2:
				return currentDistances[rowIndex];
			}
			return "Choose";
		}			
	};
	*/
}
