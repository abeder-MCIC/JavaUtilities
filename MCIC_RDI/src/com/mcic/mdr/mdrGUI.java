package com.mcic.mdr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import java.awt.Color;
import javax.swing.border.BevelBorder;

public class mdrGUI extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private mdrApp app;
	JLabel fieldLabel;
	JLabel newElelementsLabel;
	JLabel newFieldsLabel;
	public ElementChoicePanel elementChoicePanel;
	private JPanel newElements;
	private JPanel newFields;
	JButton btnCreateNewInstance;
	JButton scanFieldsButton;
	JLabel sfFields;

	/**
	 * Create the dialog.
	 */
	public mdrGUI(mdrApp app) {
		this.app = app == null ? new mdrApp() : app;
		setBounds(100, 100, 695, 753);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			scanFieldsButton = new JButton("Load Salesforce Data");
			scanFieldsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					scanFieldsButton.setEnabled(false);
					SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
						@Override
						protected Void doInBackground() throws Exception {
							app.loadSalesforce();
							return null;
						}

						@Override
						protected void done() {
							scanFieldsButton.setEnabled(true);
							fieldLabel.setText(app.sfFields.size() + "");
						}
						
						
					};
					worker.execute();
				}
			});
			contentPanel.setLayout(new MigLayout("", "[183px,grow][grow][grow][grow][grow]", "[23px,grow][grow][grow]"));
			contentPanel.add(scanFieldsButton, "cell 0 0,alignx left,aligny top");
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(new Color(255, 255, 255));
			contentPanel.add(panel, "cell 1 0,grow");
			{
				JLabel lblNewLabel = new JLabel("Salesforce Elements");
				lblNewLabel.setBackground(new Color(255, 255, 255));
				panel.add(lblNewLabel);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(new Color(255, 255, 255));
			contentPanel.add(panel, "cell 2 0,grow");
			{
				fieldLabel = new JLabel("0");
				panel.add(fieldLabel);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(Color.WHITE);
			contentPanel.add(panel, "cell 3 0,grow");
			{
				JLabel lblSalesforceFields = new JLabel("Salesforce Fields");
				lblSalesforceFields.setBackground(Color.WHITE);
				panel.add(lblSalesforceFields);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(Color.WHITE);
			contentPanel.add(panel, "cell 4 0,grow");
			{
				sfFields = new JLabel("0");
				panel.add(sfFields);
			}
		}
		{
			btnCreateNewInstance = new JButton("Create Next Element Instance");
			btnCreateNewInstance.setEnabled(false);
			btnCreateNewInstance.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					app.loadNextInstance();
				}
			});
			contentPanel.add(btnCreateNewInstance, "cell 0 1");
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(Color.WHITE);
			contentPanel.add(panel, "cell 1 1,grow");
			{
				JLabel lblNewElements = new JLabel("New Elements");
				lblNewElements.setBackground(Color.WHITE);
				panel.add(lblNewElements);
			}
		}
		{
			newElements = new JPanel();
			newElements.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			newElements.setBackground(Color.WHITE);
			contentPanel.add(newElements, "cell 2 1,grow");
			{
				newElelementsLabel = new JLabel("0");
				newElements.add(newElelementsLabel);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			panel.setBackground(Color.WHITE);
			contentPanel.add(panel, "cell 3 1,grow");
			{
				JLabel lblNewFields = new JLabel("New Fields");
				lblNewFields.setBackground(Color.WHITE);
				panel.add(lblNewFields);
			}
		}
		{
			newFields = new JPanel();
			newFields.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			newFields.setBackground(Color.WHITE);
			contentPanel.add(newFields, "cell 4 1,grow");
			{
				newFieldsLabel = new JLabel("0");
				newFields.add(newFieldsLabel);
			}
		}
		{
			elementChoicePanel = new ElementChoicePanel(app);
			elementChoicePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
			contentPanel.add(elementChoicePanel, "cell 0 2 5 1,grow");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cancelButton = new JButton("Close");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						 dispose();
					}
				});
				cancelButton.setActionCommand("Close\r\n");
				buttonPane.add(cancelButton);
			}
		}
	}

}
