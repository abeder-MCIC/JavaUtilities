package com.mcic.mdr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ElementForm extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	JTextField name;
	JTextArea description;
	mdrApp app;
	String id;

	/**
	 * Create the dialog.
	 */
	public ElementForm(mdrApp app) {
		super(app.gui, true);
		this.app = app;
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("", "[][grow]", "[][grow]"));
		{
			JLabel lblNewLabel = new JLabel("Element Name");
			contentPanel.add(lblNewLabel, "cell 0 0,alignx trailing,aligny top");
		}
		{
			name = new JTextField();
			contentPanel.add(name, "cell 1 0,growx");
			name.setColumns(10);
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Description");
			contentPanel.add(lblNewLabel_1, "cell 0 1");
		}
		{
			description = new JTextArea();
			description.setLineWrap(true);
			contentPanel.add(description, "cell 1 1,grow");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						id = app.createElement(name.getText(), description.getText());
						setVisible(false);
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						id = null;
						setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

}
