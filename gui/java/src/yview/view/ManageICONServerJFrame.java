package yview.view;

import yview.controller.ICONSConnectionManager;
import yview.controller.SSHWrapper;
import yview.controller.SSHLogger;
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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import pja.gui.UI;

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
	JButton pubSSHKeyButton = new JButton("Get Public SSH Key"); 
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
	boolean connectToLocalDevices;
	SSHLogger sshLogger = new SSHLogger();
	boolean startup = true;

	/**
	 * @brief Constructor
	 * @param parent The parent frame (MainFrame)
	 * @param iconsConnectionManager An ICONSConnectionManager instance
	 */
	public ManageICONServerJFrame(MainFrame mainFrame, ICONSConnectionManager iconsConnectionManager) {
		super("Manage ICON Servers");
		this.mainFrame = mainFrame;
		this.iconsConnectionManager=iconsConnectionManager;
		
		// Enable ssh logging to stdout
		jsch.setLogger(sshLogger);
		
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
		
		pubSSHKeyButton.addActionListener(this);
		addButton.addActionListener(this);
		deleteButton.addActionListener(this);
		editButton.addActionListener(this);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

        buttonPane.add(pubSSHKeyButton);
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

		

	}

	/**
	 * @brief If we reused the serverPropertiesDialog the setLocation call 
	 * on it would fail and leave the dialog at 0,0.
	 * Calling this method works around this by re creating the server 
	 * properties dialog each time.
	 */
	private void newServerPropertiesDialog() {
		if( serverPropertiesDialog != null ) {
			serverPropertiesDialog.dispose();
			serverPropertiesDialog= null;
		}
		serverPropertiesDialog = new ServerPropertiesDialog(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		newServerPropertiesDialog();
		UI.CenterInParent(this.mainFrame, serverPropertiesDialog);
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
			connectToServer(true, connectToLocalDevices);
			mainFrame.updateActiveICONS();
		} else if (e.getSource() == cancelButton) {
			setVisible(false);
			statusBar.close();
		}
		else if( e.getSource() == pubSSHKeyButton ) {
			getPublicSSHKey();
		}
	}
	

	/**
	 * @brief Get the public SSH key.
	 */
	public void getPublicSSHKey() {
		try {
			String publicSSHKey = SSHWrapper.GetPublicKey();		
			StringSelection stringSelection = new StringSelection(publicSSHKey);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
			JOptionPane.showMessageDialog(null, "Copied ssh public key to clipboard.");
		}
		catch( IOException ex) {}
	}
	
	/**
	 * @brief Connect or Reconnect to the ICON server.
	 * @param pauseReconnectDelay If we are reconnecting to the ICONS
	 *        and pauseReconnectDelay is true then we reconnect as quickly as 
	 *        possible for a about 3 seconds after this method is called.
	 *        After this we return to the delaying a reconnect to the ICONS
	 *        should the connection drop.
	 *  @param connectToLocalDevices If true attempt to connect to local devices directly. I.E don't go through ICONS.
	 */
	public void connectToServer(boolean pauseReconnectDelay, boolean connectToLocalDevices) {
		this.connectToLocalDevices = connectToLocalDevices;
		statusBar.close();
		try {
			mainFrame.initTabs();
		}
		catch(SocketException ex) {
			ex.printStackTrace();
		}
		if( iconsConnectionManager != null ) {
			iconsConnectionManager.shutdown(pauseReconnectDelay);
			iconsConnectionManager.connectICONS(getICONServerList(), connectToLocalDevices);
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
			else {
				listModel.addElement(""+iconServer);
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
		newServerPropertiesDialog();
		UI.CenterInParent(this.mainFrame, serverPropertiesDialog);
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

	/**
	 * @brief Set direct connection to local devices.
	 * @param connectToLocalDevices If true attempt to connect to local devices directly. I.E don't go through ICONS.
	 */
	public void setDirectLocalDevConnect(boolean connectToLocalDevices) {
		this.connectToLocalDevices = connectToLocalDevices;
		iconsConnectionManager.setDirectLocalDevConnect(connectToLocalDevices);
	}

}
