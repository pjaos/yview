package yview.view;

import java.awt.BorderLayout;

import java.io.IOException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;
import javax.swing.JOptionPane;

import yview.controller.SSHWrapper;
import yview.model.Constants;
import yview.model.ICONServer;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

public class ServerPropertiesDialog extends JDialog implements ActionListener, WindowListener {
	public static final int TEXTFIELD_COLUMNS 	= 20;
	
	JPanel 		buttonPane 						= new JPanel();
	JButton 	okButton 						= new JButton("OK"); 
	JButton 	cancelButton 					= new JButton("Cancel");
	RowPanel  	propertiesPanel 				= new RowPanel();
	JLabel      usernameLabel                   = new JLabel("Username");
	JTextField	usernameField					= new JTextField("", ServerPropertiesDialog.TEXTFIELD_COLUMNS);
	JLabel      serverAddressLabel              = new JLabel("Server address");
	JTextField	serverAddressField				= new JTextField("", ServerPropertiesDialog.TEXTFIELD_COLUMNS);
	JLabel      portLabel                       = new JLabel("Port");
	JCheckBox   activeCheckBox					= new JCheckBox();
	JLabel      activeLabel                     = new JLabel("Active");
	
	JFormattedTextField portField;
	boolean     okSelected;
	
	public ServerPropertiesDialog(JFrame parent) {
		super(parent, "Server Properties", true);

		addWindowListener(this);

	    DecimalFormat format = new DecimalFormat("#####");
	    NumberFormatter formatter = new NumberFormatter(format);
	    formatter.setValueClass(Integer.class);
	    formatter.setMinimum(Constants.MIN_TCP_PORT_NUMBER);
	    formatter.setMaximum(Constants.MAX_TCP_PORT_NUMBER);
	    formatter.setAllowsInvalid(true);
	    formatter.setCommitsOnValidEdit(true);
	    portField = new JFormattedTextField(formatter);
	    
		propertiesPanel.setRowPanel(4);
		propertiesPanel.add(usernameLabel, usernameField);
		propertiesPanel.add(serverAddressLabel, serverAddressField);
		propertiesPanel.add(portLabel, portField);
		propertiesPanel.add(activeLabel, activeCheckBox);
		
        getContentPane().add(propertiesPanel, BorderLayout.CENTER);
       
		buttonPane.add(okButton);
		buttonPane.add(cancelButton);
		
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

        usernameField.requestFocusInWindow();
        
		pack();
		
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if( e.getSource() == okButton ) {

			//If OK selected without the required fields quit.
			if( usernameField.getText().length() == 0 ||
				serverAddressField.getText().length() == 0 ||
				portField.getText().length() == 0 ) {
				return;
			}

			okSelected=true;
			setVisible(false);	
		}
		else if( e.getSource() == cancelButton ) {
			setVisible(false);
		}
		
	}
	
	/**
	 * @brief Set this dialog to display the state in the ICONServer object
	 * @param iconServer The ICONServer object.
	 */
	public void setICONServer(ICONServer iconServer) {
		usernameField.setText(iconServer.getUsername());
		serverAddressField.setText(iconServer.getServerName());
		portField.setText(""+iconServer.getPort());
		activeCheckBox.setSelected(iconServer.getActive());
	}
	
	/**
	 * @brief Get the Icon server object state represented by the dialog configuration.
	 * @return
	 */
	public ICONServer getICONServer() {
		ICONServer iconServer = new ICONServer();
		
		iconServer.setUsername(usernameField.getText());
		iconServer.setServerName(serverAddressField.getText());
		iconServer.setPort(Integer.parseInt( portField.getText()));
		iconServer.setActive(activeCheckBox.isSelected());
		
		return iconServer;
	}
	
	public boolean okWasSelected() { return okSelected; }

	//When window becomes visible ensure we reset the flag to indicate the OK button was selected
	public void windowActivated(WindowEvent e) {
		okSelected = false;		
	}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

}
