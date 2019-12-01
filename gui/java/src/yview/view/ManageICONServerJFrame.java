package yview.view;

import yview.controller.ICONSConnectionManager;
import yview.controller.SSHWrapper;
import yview.model.Constants;
import yview.model.ICONServer;

import pja.io.SimpleConfig;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.net.SocketException;
import java.lang.management.ManagementFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionListener;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;

import pja.gui.Dialogs;

import javax.swing.event.ListSelectionEvent;
import javax.swing.JOptionPane;

/**
 * @brief Responsible for allowing the user to create and delete references to ICON (Internet 
 * connection) servers. 
 * WYG servers connect to ICON servers and provide details of all the IOT devices that 
 * they can see on there local networks. Clients can then connect to the ICON server 
 * to get access to all the IOT devices.
 */
public class ManageICONServerJFrame extends JFrame implements ActionListener, ListSelectionListener, MouseListener {
	static int SelectedIndex;
	JPanel buttonPane = new JPanel();
	JButton addButton = new JButton("Add");
	JButton deleteButton = new JButton("Delete");
	JButton editButton = new JButton("Edit");
	JButton okButton = new JButton("OK");
	JButton cancelButton = new JButton("Cancel");
	DefaultListModel listModel;
	JList list;
	static File ConfigFile;
	Properties serverProperties;
	ServerPropertiesDialog serverPropertiesDialog;
	JSch jsch = new JSch();
	MainFrame mainFrame;
	JPanel mainPanel = new JPanel(new BorderLayout());
	StatusBar statusBar;
	UserInfo userInfo;
	ICONSConnectionManager iconsConnectionManager;

	/**
	 * @brief Constructor
	 * @param parent The parent frame (MainFrame)
	 * @param iconsConnectionManager An ICONSConnectionManager instance
	 */
	public ManageICONServerJFrame(MainFrame mainFrame, ICONSConnectionManager iconsConnectionManager) {
		super("Manage ICON Servers");
		this.mainFrame = mainFrame;
		this.iconsConnectionManager=iconsConnectionManager;

		statusBar = new StatusBar();

		setMinimumSize(new Dimension(400, 200));

		listModel = new DefaultListModel();

		// Create the list and put it in a scroll pane.
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.addListSelectionListener(this);
		list.setVisibleRowCount(5);
		list.addMouseListener(this);

		JScrollPane listScrollPane = new JScrollPane(list);
		mainPanel.add(listScrollPane, BorderLayout.CENTER);

		addButton.addActionListener(this);
		deleteButton.addActionListener(this);
		editButton.addActionListener(this);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		buttonPane.add(cancelButton);
		buttonPane.add(addButton);
		buttonPane.add(deleteButton);
		buttonPane.add(editButton);
		buttonPane.add(okButton);
		mainPanel.add(buttonPane, BorderLayout.SOUTH);

		getContentPane().add(mainPanel, BorderLayout.CENTER);

		getContentPane().add(statusBar, BorderLayout.SOUTH);

		addButton.requestFocusInWindow();
		pack();

		ConfigFile = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), ManageICONServerJFrame.class.getName());
		serverProperties = getServerProperties();
		updateConnectionList();

		serverPropertiesDialog = new ServerPropertiesDialog(this);

	}

	public void actionPerformed(ActionEvent e) {
		serverPropertiesDialog.setLocationRelativeTo(this);
		if (e.getSource() == addButton) {
			addServer();
		} else if (e.getSource() == editButton) {
			editSelectedItem();
		} else if (e.getSource() == deleteButton) {
			deleteSelectedItem();
		} else if (e.getSource() == okButton) {
			saveServerDetails(serverProperties);
			setVisible(false);
			statusBar.close();
			connectToServer();
		} else if (e.getSource() == cancelButton) {
			setVisible(false);
			statusBar.close();
		}

	}
	
	/**
	 * @brief Connect or reconnect to the ICON server.
	 */
	public void connectToServer() {
		statusBar.close();
		try {
			mainFrame.initTabs();
		}
		catch(SocketException ex) {}
		if( iconsConnectionManager != null ) {
			iconsConnectionManager.shutdown();
			iconsConnectionManager.connectICONS(getICONServerList());
		}
	}
	
	private void addServer() {
		serverPropertiesDialog.setICONServer( new ICONServer() );
		serverPropertiesDialog.setVisible(true);
		if (serverPropertiesDialog.okWasSelected()) {
			final ICONServer iconServer = serverPropertiesDialog.getICONServer();
			
			//If the user marking this connection as active
			if( iconServer.getActive() ) {
				(new Thread() {
					  public void run() {
							if (SSHWrapper.AbleToConnectToHost(jsch, iconServer.getUsername(),
									iconServer.getServerName(), iconServer.getPort(),
									statusBar)) {
								listModel.addElement(""+iconServer);
							}
					  }
				}).start();
			}
		}
	}
	
	private void deleteSelectedItem() {
		int selectedIndex = list.getSelectedIndex();
		if (selectedIndex >= 0) {
			int reply = JOptionPane.showConfirmDialog(mainFrame,
					"Are you sure you wish to delete the selected ICON server Connection", "Delete Connection",
					JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {
				listModel.remove(selectedIndex);
			}
		}		
	}
	
	private void editSelectedItem() {
		serverPropertiesDialog.setLocationRelativeTo(this);
		SelectedIndex = list.getSelectedIndex();
		if (SelectedIndex >= 0) {
			ICONServer iconServer = new ICONServer();
			iconServer.setFromString( listModel.getElementAt(SelectedIndex).toString() );
			serverPropertiesDialog.setICONServer(iconServer);
			serverPropertiesDialog.setVisible(true);
			if (serverPropertiesDialog.okWasSelected()) {
				iconServer = serverPropertiesDialog.getICONServer();
				if( iconServer.getActive() ) {	
					(new Thread() {
						  public void run() {
								ICONServer iconServer = serverPropertiesDialog.getICONServer();
								if (SSHWrapper.AbleToConnectToHost(jsch, iconServer.getUsername(), iconServer.getServerName(), iconServer.getPort(), statusBar)) {
									listModel.setElementAt(iconServer.toString(), SelectedIndex);
								}
								else {
									Dialogs.showOKDialog(mainFrame, "Warning", "Unable to connect to "+iconServer.getServerName()+". Set server inactive.");
									//If we cannot connect to the server set it inactive
									iconServer.setActive(false);
									listModel.setElementAt(iconServer.toString(), SelectedIndex);
								}
						  }
					}).start();
				}
				else {
					listModel.setElementAt(iconServer.toString(), SelectedIndex);
				}
			}
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			if (list.getSelectedIndex() != -1) {
				// Do something
			}
		}
	}

	/**
	 * @brief Load the ICON server details
	 * @return The server properties
	 */
	public Properties getServerProperties() {
		Properties serverProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(ManageICONServerJFrame.ConfigFile);
			serverProperties.load(fis);
			fis.close();
		} catch (Exception e) {
			if (MainFrame.DEBUG) {
				e.printStackTrace();
			}
		}
		return serverProperties;
	}

	/**
	 * @brief Update the displayed connection list from the serverProperties
	 */
	private void updateConnectionList() {
		Enumeration<Object> keyNames = serverProperties.keys();
		while (keyNames.hasMoreElements()) {
			String userHostPort = (String) serverProperties.get("" + keyNames.nextElement());
			listModel.addElement(userHostPort);
		}
	}

	/**
	 * @brief Save the ICON server details
	 */
	public void saveServerDetails(Properties serverProperties) {
		try {
			serverProperties = new Properties();
			FileWriter fw = new FileWriter(ManageICONServerJFrame.ConfigFile);
			int connectionCount = listModel.getSize();
			for (int index = 0; index < connectionCount; index++) {
				String rowString = (String) listModel.getElementAt(index);
				serverProperties.put("" + index, rowString);
			}
			serverProperties.store(fw, "ICON server connections");
			fw.close();
		} catch (Exception e) {
			if (MainFrame.DEBUG) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get an array of configured ICON (SSH) servers. The fact that they are in
	 * the list means that the ssh server was reachable and we have added our
	 * public key to the remote server in order to allow automatic (password
	 * less) ssh logins.
	 * 
	 * @return An array of ICON Server objects
	 */
	public ICONServer[] getICONServerList() {
		ICONServer iconServer;
		ICONServer iconServerArray[] = new ICONServer[listModel.getSize()];

		for (int serverIndex = 0; serverIndex < listModel.getSize(); serverIndex++) {
			iconServer = new ICONServer();
			iconServer.setFromString(listModel.getElementAt(serverIndex).toString());
			iconServerArray[serverIndex] = iconServer;
		}
		return iconServerArray;
	}

	public void mouseClicked(MouseEvent e) {

		if (e.getSource() == list && e.getClickCount() == 2) {
			editSelectedItem();
		}

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

}
