package yview.view;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.JComponent;

import org.json.JSONObject;

import mtightvnc.OptionsPanel;
import mtightvnc.visitpc.VNCOptionsConfig;
import pja.gui.Dialogs;
import pja.gui.GenericOKCancelDialog;
import pja.gui.RowPane;
import pja.gui.UI;
import pja.io.FileIO;
import pja.io.SimpleConfig;
import yview.controller.JSONProcessor;
import yview.controller.Main;
import yview.model.Constants;
import yview.model.Service;
import yview.model.ServiceCmd;
import yview.model.ServiceCmdException;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.File;
import java.io.IOException;

import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 * @brief Responsible for holding a table each row of which is a remote device.
 */
public class DeviceTablePanel extends JPanel implements MouseListener, ActionListener {
	static final long serialVersionUID=1;
	public static int UNIT_TYPE_COL_INDEX=0;
	public static int NAME_COL_INDEX=1;
	public static int IP_ADDRESS_COL_INDEX=2;
	public static int INFORMATION_COL_INDEX=3;

	JTable 		table;
	Vector 		<Vector>rowData;
	Vector 		<String>columnNames;
	Vector 		<JSONObject>jsonDeviceVector;
	Vector      <DeviceTableSelectionListener>deviceTableSelectionListenerList;
	JPopupMenu  launcherPopupMenu;
	String 		location;
	Timer		deviceCheckTimer;
	HighLightedRowRenderer highLightedRowRenderer;
	boolean 	lan=false;
	GenericOKCancelDialog optionsConfigDialog;
	Vector<ServiceCmd> serviceCmdList;
	JButton textEditButton = new JButton("Text Editor");
	JSONObject configureJsonDevice;
	GenericOKCancelDialog textEditDialog;
	JEditorPane editorPane;
	JList serviceJList;
	JButton editButton = new JButton("Edit");
	JButton copyButton = new JButton("Copy");
	JButton deleteButton = new JButton("Delete");
	JButton defaultsButton = new JButton("Defaults");
	JButton upButton = new JButton("Up");
	JButton downButton = new JButton("Down");
	GenericOKCancelDialog editVNCDialog;
	GenericOKCancelDialog editExternalCmdDialog;
	JTextField menuOptionNameField;
	JTextField externalCmdField;
	JScrollPane serviceListscrollPane;
	
	/**
	 * @brief Constuctor
	 * @param location The location of this device panel.
	 */
	public DeviceTablePanel(String location) {
		super(new GridLayout(1,0));

		this.location=location;

		if( location == Constants.LOCAL_LOCATION ) {
			lan=true;
		}

		columnNames = new Vector<String>();
		columnNames.add("Device Type");
		columnNames.add("Name");
		columnNames.add("IP Address");
		columnNames.add("Information");

		rowData = new Vector<Vector>();

		table = new JTable(rowData, columnNames) {
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int column) {                
				return false;               
			};
	
			/**
			 * @brief Show how long ago we heard from a device 
			 */
            public String getToolTipText(MouseEvent e) {
            	long now = System.currentTimeMillis();
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                try {
                    //comment row, exclude heading
                    if(rowIndex >= 0){
        	        	JSONObject jsonDev = jsonDeviceVector.get(rowIndex);
        	        	if( jsonDev != null ) {
        	        		long rxMS = JSONProcessor.GetLocalRxTimeMs(jsonDev);
        	        		if( rxMS != -1 ) {
        	        			float secondsAgo = (now - rxMS)/1000F;
        	        			tip = "Updated "+String.format("%.1f", secondsAgo)+" seconds ago.";
        	        		}      	        		
        	        	}
                    }
                } catch (RuntimeException e1) {
                    //catch null pointer exception if mouse is over an empty line
                }

                return tip;
            }
            
		};
		highLightedRowRenderer = new HighLightedRowRenderer();
		table.setDefaultRenderer(Object.class, highLightedRowRenderer);
		table.addMouseListener(this);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);

		//Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(table);

		//Add the scroll pane to this panel.
		add(scrollPane);

		launcherPopupMenu = new JPopupMenu();

		jsonDeviceVector = new Vector<JSONObject>();
		highLightedRowRenderer.setJsonDevVector(jsonDeviceVector);
		deviceTableSelectionListenerList = new Vector<DeviceTableSelectionListener>(); 

		deviceCheckTimer = new Timer(Constants.CHECK_DEVICE_POLL_MS, this);
		deviceCheckTimer.setInitialDelay(Constants.CHECK_DEVICE_POLL_MS);
		deviceCheckTimer.start(); 
	}

	/**
	 * @brief Responsible for rendering the device table.
	 */
	class HighLightedRowRenderer extends DefaultTableCellRenderer {
		Vector<JSONObject> devList = new Vector<JSONObject>();
		Color highLightColor = Color.ORANGE;
		Color nonHighLightColor = Color.WHITE;

		/**
		 * @brief Set the device list for this location.
		 * @param devList A list (Vector) of JSONObject instances.
		 */
		public void setJsonDevVector(Vector<JSONObject> devList) {
			this.devList=devList;
		}
			

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row, int col) {
			
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			long now = System.currentTimeMillis();
			
			if( !isSelected ) {
				if( row >= 0 && row < devList.size() ) {
					JSONObject jsonD = devList.get(row);
					long updateMS = JSONProcessor.GetLocalRxTimeMs(jsonD);
					if( updateMS != -1 ) {
						long msAgo = now-updateMS;
						if( msAgo > MainFrame.GetDeviceTimeoutWarningMilliSeconds() ) {
							if( getBackground() != highLightColor ) {
								setBackground(highLightColor);
							}	
						}
						else {
							if( getBackground() != nonHighLightColor ) {
								setBackground(nonHighLightColor);
							}
						}
					}
				}
				
			}

			return this;
		}   

		public void setHighLightColor(Color highLightColor) {
			this.highLightColor=highLightColor;
		}

		public Color getHighLightColor() { return highLightColor; }

		public void setNonHighLightColor(Color nonHighLightColor) {
			this.nonHighLightColor=nonHighLightColor;
		}

		public Color getNonHighLightColor() { return nonHighLightColor; }

	}


	/**
	 * @brief Called when a device is lostEnsure a device is not displayed in the device table.
	 * @param jsonDevice
	 */
	private void deviceLost(JSONObject jsonDevice) {
		int deviceTableIndex = getJsonDeviceIndex(jsonDevice);
		//Is the device currently displayed.
		if( deviceTableIndex >= 0 ) {
			remove(jsonDevice);
		}
	}
	
	/**
	 * @brief Called when device messages are received. 
	 * @param jsonDevice The json message received from the device.
	 * @param groupName The name of the configured group.
	 */
	public void setJSONDevice(JSONObject jsonDevice, String groupName) {
		int selectedRow = table.getSelectedRow();
		boolean showDevice = true;
		String deviceGroupName = JSONProcessor.GetGroupName(jsonDevice);
	
		//Note that the group name can be used to hide devices from users.
		//This is not secure as the device response packets show the device 
		//group name but should be OK within trusted networks.

		//If the user sets the WyView group name do not show devices that do not have this group name set
		if( groupName != null && groupName.length() > 0 && !JSONProcessor.GroupNameMatch(groupName, jsonDevice) ) {
			showDevice = false;
		}
		//If the received device specifies a group name and this does not match the local group name then don't display device.
		else if( deviceGroupName != null && groupName != null && !JSONProcessor.GroupNameMatch(groupName, jsonDevice) ) {
			showDevice = false;
		}
		//If this device is for a different DeviceTablePanel (location) instance ignore it here.
		else if( !JSONProcessor.LocationMatch(location, jsonDevice) ) {
			showDevice = false;
		}
		if( showDevice ) {
			
			//If we have not heard from this device before
			if( !isKnownDevice(jsonDevice) ) {
				Vector <String>row = new Vector<String>();
				row.add( JSONProcessor.GetProductID(jsonDevice) );
				row.add( JSONProcessor.GetUnitName(jsonDevice) );
				row.add( JSONProcessor.GetIPAddress(jsonDevice) );
				//We could update the information here but a subsequent update gives better L&F
				row.add("");
				rowData.add(row);
				((AbstractTableModel) table.getModel()).fireTableDataChanged();
				jsonDeviceVector.add(  jsonDevice );
			}
			//Update the device state
			else {
				updateDeviceState(jsonDevice);
			}
		}
		else {
			remove(jsonDevice);
		}

		//Maintain the selected row after update
		if( selectedRow != -1 && table.getRowCount() > selectedRow ) {
			table.setRowSelectionInterval(selectedRow, selectedRow);
		}

	}

	/**
	 * @brief Check if we have a record of the device. I.E we have heard from it before.
	 * @param jsonDevice The json message received from the device.
	 * @return True if we have heard from this device before.
	 */
	private boolean isKnownDevice(JSONObject jsonDevice) {
		boolean knownDevice = false;
		int index = getJsonDeviceIndex(jsonDevice);
		if( index != -1 ) {
			knownDevice = true;
		}
		return knownDevice;
	}


	/**
	 * @brief Get the index in the list of json devices that we have this device record.
	 * @param jsonDevice The json message relieved from the device.
	 * @return The index or -1 if the device is unknown.
	 */
	private int getJsonDeviceIndex(JSONObject jsonDevice) {
		int index=0;
		int devIndex=-1;
		String jsonDeviceLocation = JSONProcessor.GetLocation(jsonDevice);

		//If this panels location name is not the same as the location in the jsonDevice
		//Then this jsonDevice is for a different panel.
		if( !jsonDeviceLocation.equals(location) ) {
			return -1;
		}

		//If we have a reference at this location to this IP address then we can get the location index
		String jsonDeviceIPAddress = JSONProcessor.GetIPAddress(jsonDevice);

		for( JSONObject json : jsonDeviceVector ) {
			String jsonIPAddress = JSONProcessor.GetIPAddress(json);
			if( jsonDeviceIPAddress.equals(jsonIPAddress) ) {
				devIndex=index;
				break;
			}
			index++;
		}
		return devIndex;
	}


	/**
	 * @brief Update of the all the currently stored devices
	 * @param json The JSONObject instance.
	 */
	private void updateDeviceState(JSONObject jsonDevice) {
		int index = 0;
		int indexToReplace=-1;

		//Only update where the group name matches.
		if( JSONProcessor.GroupNameMatch(MainFrame.GetGroupName(), jsonDevice) ) {
			
			for( Vector rd : rowData ) {
				//If we have the matching IP address
				if( JSONProcessor.IPAddressMatch((String)rd.get(DeviceTablePanel.IP_ADDRESS_COL_INDEX), jsonDevice) ) {
	
					//If the unit name name has changed
					if( !JSONProcessor.UnitNameMatch((String)rd.get(DeviceTablePanel.NAME_COL_INDEX), jsonDevice) ) {
						rd.set(DeviceTablePanel.NAME_COL_INDEX, JSONProcessor.GetUnitName(jsonDevice));
						((AbstractTableModel) table.getModel()).fireTableDataChanged();
					}
	
					String jsonPowerWatts = JSONProcessor.GetPowerWatts(jsonDevice);
					String serConf = JSONProcessor.GetSerialConfigString(jsonDevice);
					
					if( JSONProcessor.GetProductID(jsonDevice).equals(Constants.WYTERM_PRODUCT_ID) ) {
						//If the serial port config has changed
						if( serConf != null && !rd.get(DeviceTablePanel.INFORMATION_COL_INDEX  ).equals(serConf) ) {
							rd.set(DeviceTablePanel.INFORMATION_COL_INDEX  , serConf);
							((AbstractTableModel) table.getModel()).fireTableDataChanged();
						}    		  
					}
					else if( JSONProcessor.GetProductID(jsonDevice).equals(Constants.WYSW_PRODUCT_ID) ||
							JSONProcessor.GetProductID(jsonDevice).equals(Constants.WYSWITCH2_PRODUCT_ID) ) {
						if( !rd.get(DeviceTablePanel.INFORMATION_COL_INDEX).equals( getPowerKiloWattsString(jsonPowerWatts) ) ) {
							rd.set(DeviceTablePanel.INFORMATION_COL_INDEX, getPowerKiloWattsString(jsonPowerWatts) );
							((AbstractTableModel) table.getModel()).fireTableDataChanged();    		  
						}    		  
					}
					//Each time we receive a JSON message for a device that is already in the 
					//jsonDeviceVector (I.E has been received previously) we need to replace the
					//device info held in jsonDeviceVector with the latest. It is important that
					//jsonDeviceVector holds the latest state so that we can check for device 
					//timeouts (devices that we have not heard from for a while as these 
					//change background color to red when they timeout.
					indexToReplace = getJsonDeviceIndex(jsonDevice);
					if( indexToReplace >= 0 ) {
						jsonDeviceVector.set(indexToReplace, jsonDevice);
					}
	
	
				}   
			}
		}
	}

	/**
	 * @brief Get the selected device.
	 * @return The selected device or null of no device is selected.
	 */
	private JSONObject getSelectedDevice() {
		JSONObject selectedJsonDevice = null;
		int selectedRow = table.getSelectedRow();

		if( selectedRow > -1 && selectedRow < rowData.size() ) {

			Vector rd = rowData.elementAt(selectedRow);
			for( JSONObject json : jsonDeviceVector ) {
				if( rd.get(DeviceTablePanel.IP_ADDRESS_COL_INDEX).equals( JSONProcessor.GetIPAddress(json) ) ) {
					selectedJsonDevice = json;
					break;
				}
			}

		}

		return selectedJsonDevice;
	}

	/**
	 * @brief Notify listeners that a device has been double clicked in the table.
	 * @param device
	 * @param serviceCmd
	 */
	private void notifyDeviceListeners(JSONObject jsonDevice, ServiceCmd serviceCmd) {

		//Notify all the listeners of the selected beacon
		for( DeviceTableSelectionListener deviceTableSelectionListener : deviceTableSelectionListenerList ) {
			deviceTableSelectionListener.setSelectedDevice( jsonDevice , serviceCmd );
		}

	}

	/**
	 * @brief Get the config file that holds the options for the device
	 * @param jsonDevice The Device details
	 * @return The absolute options filename 
	 */
	private File getOptionsConfigFile(JSONObject jsonDevice) {
		String serviceNames="";
		
		String location = JSONProcessor.GetLocation(jsonDevice);
		String ipAddress = JSONProcessor.GetIPAddress(jsonDevice);
		String services = JSONProcessor.GetServicesList(jsonDevice);
		
		if( services.length() > 0 ) {
			Service serviceList[] = Service.GetServiceList( services );

			for( Service service : serviceList ) {
				serviceNames=serviceNames+"_"+service.serviceName;
			}

		}
		return new File(SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), Constants.OPTIONS_CONFIG_FILE_PREFIX+location+"_"+ipAddress+serviceNames);
	}

	/**
	 * @brief Add header that documents the service command file formatting.
	 * @throws IOException
	 */
	private void adServiceCmdFileHeader(File serviceCmdConfigFile) throws IOException {
		JFrame f =  (JFrame) SwingUtilities.getWindowAncestor(this);

		Vector<String> lines = new Vector<String>();
		lines.add("# This is the configuration file for a device service list.");
		lines.add("# Comment lines start with the # character");
		lines.add("# All other lines represent one entry in the device popup menu list.");
		lines.add("# These lines are comma separated text as detailed below.");
		lines.add("# <NAME>,<PORT>,<OPTION_NAME>,<CMD_TYPE>,<CMD>");
		lines.add("# NAME:        The service name as advertised by the device.");
		lines.add("# PORT:        The TCP port on which the device provides the above service.");
		lines.add("# OPTION_NAME: The option name as shown in the popup menu list.");
		lines.add("# CMD_TYPE:    An integer representing the command type as shown below.");
		lines.add("#              Only the followinng options are valid.");
		lines.add("#              0: An external system command.");
		lines.add("#              1: The internal VNC viewer.");
		lines.add("#              2: The internal Java system web browser.");
		lines.add("#              3: The internal serial port terminal.");
		lines.add("# CMD:         The contents of this field depends upon the CMD_TYPE.");
		lines.add("#              The $h text denotes the host address.");
		lines.add("#              The $p text denotes the service port.");
		lines.add("#  CMD_TYPE=0: The command line to be executed. ");
		lines.add("#  CMD_TYPE=1: $h $p.");
		lines.add("#  CMD_TYPE=2: The URL to open (E.G http://$h:$p).");
		lines.add("#  CMD_TYPE=3: $h $p.");
		lines.add("# ");

		FileIO.SetLines(serviceCmdConfigFile.getAbsolutePath(), lines, false);

	}

	/**
	 * @brief Create default options file.
	 * @param configFile
	 * @param jsonDevice
	 * @throws ServiceCmdException, IOException
	 */
	private void createDefaultOptionsFile(File configFile, JSONObject jsonDevice) throws ServiceCmdException, IOException {
		String cmd;
		boolean local=false;
		String location = JSONProcessor.GetLocation(jsonDevice);
		if( location.equals(Constants.LOCAL_LOCATION) ) {
			local=true;
		}

		if( !configFile.isFile() ) {
			adServiceCmdFileHeader(configFile);
			Vector <String>cfgLines = new Vector<String>();
			Service services[] = Service.GetServiceList( JSONProcessor.GetServicesList(jsonDevice) );
			for( Service service : services ) {
				ServiceCmd serviceCmd;

				if( service.serviceNameMatch(ServiceCmd.VNC_SERVICE_NAME) ) {
					cmd =  ServiceCmd.GetDefaultCmd(ServiceCmd.INTERNAL_VNC_CMD_TYPE, "vnc");
					serviceCmd = new ServiceCmd(service.serviceName, service.port, "VNC Viewer", ServiceCmd.INTERNAL_VNC_CMD_TYPE, cmd);
				}
				else if( service.serviceNameMatch(ServiceCmd.HTTP_SERVICE_NAME) ) {
					cmd =  ServiceCmd.GetDefaultCmd(ServiceCmd.INTERNAL_HTTP_CMD_TYPE, "http");
					serviceCmd = new ServiceCmd(service.serviceName, service.port, "Web Browser", ServiceCmd.INTERNAL_HTTP_CMD_TYPE, cmd);
				}
				else if( service.serviceNameMatch(ServiceCmd.SERIAL_PORT_SERVICE_NAME) ) {
					cmd =  ServiceCmd.GetDefaultCmd(ServiceCmd.INTERNAL_SERIAL_PORT_CMD_TYPE, "serial");
					serviceCmd = new ServiceCmd(service.serviceName, service.port, "Serial Terminal", ServiceCmd.INTERNAL_SERIAL_PORT_CMD_TYPE, cmd);
				}
				else {
					String optionName = "External Command";
					if( service.serviceName.toLowerCase().startsWith("ssh") ) {
						optionName = "SSH Terminal";
					}
					else if( service.serviceName.toLowerCase().startsWith("web") ) {
						optionName = "Web Browser";
					}
					cmd =  ServiceCmd.GetDefaultCmd(ServiceCmd.EXTERNAL_CMD_TYPE, service.serviceName);
					serviceCmd = new ServiceCmd(service.serviceName, service.port, optionName, ServiceCmd.EXTERNAL_CMD_TYPE, cmd);

					if( service.serviceName.toLowerCase().equals("ssh") ) {
						cfgLines.add(serviceCmd.toString());
						cmd =  ServiceCmd.GetDefaultCmd(ServiceCmd.EXTERNAL_CMD_TYPE, "scp");
						serviceCmd = new ServiceCmd(service.serviceName, service.port, "File Browser", ServiceCmd.EXTERNAL_CMD_TYPE, cmd);
					}
				}   			
				cfgLines.add(serviceCmd.toString());

			}

			if( cfgLines.size() > 0 ) {
				FileIO.SetLines(configFile.getAbsoluteFile().toString(), cfgLines, true, "\n");
				MainFrame.SetStatus("Created service defaults in "+configFile);
			}

		}
	}

	/**
	 * @brief Load the service menu options from a config file.
	 * @param jsonDevice The JSON Device we're interested in.
	 * @return A Vector of ServiceMenuOption instances.
	 * @throws ServiceCmdException 
	 **/
	private Vector<ServiceCmd> loadServiceCmdList(JSONObject jsonDevice) throws IOException, ServiceCmdException {
		Vector<ServiceCmd> serviceCmdList = new Vector<ServiceCmd>();
		ServiceCmd serviceCmd;
		File optionsfile = getOptionsConfigFile(jsonDevice);

		String lines[] = FileIO.GetLines(optionsfile.getAbsolutePath());

		for( String line : lines ) {
			line=line.trim();
			//Ignore commented and empty lines
			if( !line.startsWith("#") && line.length() > 0 ) {
				serviceCmd = ServiceCmd.GetServiceCmd(line);
				serviceCmdList.add(serviceCmd);
			}
		}
		return serviceCmdList;
	}

	/**
	 * @brief Update the list of services displayed.
	 * @throws ServiceCmdException, IOException
	 */
	private void updateOptionsConfigDialog() throws ServiceCmdException, IOException {
		serviceCmdList = loadServiceCmdList(configureJsonDevice);
				
		Vector<String> serviceMenuNameList = new Vector<String>();
		for( ServiceCmd serviceCmd : serviceCmdList ) {
			serviceMenuNameList.add( serviceCmd.getMenuOptionName() );
		}
		int index = -1;
		if ( serviceJList != null ) {
			
			index = serviceJList.getSelectedIndex();
			
		}
		if( serviceJList == null) {

			serviceJList = new JList(serviceMenuNameList);
			serviceJList.setToolTipText("Double click option to edit.");
			serviceJList.addMouseListener(this);
		}
		else {

			serviceJList.setListData(serviceMenuNameList);
		}
		if( index != -1 && index < serviceJList.getModel().getSize() ) {
			
			serviceJList.setSelectedIndex(index);
			
		}
		if( serviceListscrollPane != null ) {
			
			optionsConfigDialog.remove(serviceListscrollPane);
			
		}
		serviceListscrollPane = new JScrollPane(serviceJList);

		optionsConfigDialog.add(BorderLayout.NORTH, serviceListscrollPane);
		optionsConfigDialog.revalidate();
	}
	
	/**
	 * @brief Configure the popup menu for the device.
	 * @param jsonDevice
	 * @param serviceCmdList A list (Vector) of ServiceCmd instances.
	 */
	public void configureOptionsMenu(JSONObject jsonDevice, Vector<ServiceCmd> serviceCmdList ) throws ServiceCmdException, IOException {

		configureJsonDevice = jsonDevice;

		if( optionsConfigDialog == null ) {
			optionsConfigDialog = new GenericOKCancelDialog(null, "Configure Servce Options", true);
			
			JPanel buttonPanel = new JPanel();
			
			defaultsButton.setToolTipText("Reset to the default service list for this device.");
			textEditButton.setToolTipText("Manually edit the text file holding the service list configuration.");
			deleteButton.setToolTipText("Deleted the selected option.");
			copyButton.setToolTipText("Copy the selected service option.");
			upButton.setToolTipText("Move the selected service up in the option list.");
			downButton.setToolTipText("Move the selected service down in the option list.");
			editButton.setToolTipText("Edit the selected option.");
			
			buttonPanel.add(defaultsButton);
			buttonPanel.add(textEditButton);
			buttonPanel.add(deleteButton);
			buttonPanel.add(copyButton);
			buttonPanel.add(upButton);
			buttonPanel.add(downButton);
			buttonPanel.add(editButton);
			
			textEditButton.addActionListener(this);
			defaultsButton.addActionListener(this);
			deleteButton.addActionListener(this);
			copyButton.addActionListener(this);
			upButton.addActionListener(this);
			downButton.addActionListener(this);
			editButton.addActionListener(this);
			
			optionsConfigDialog.add(BorderLayout.CENTER, buttonPanel);
		}
		
		updateOptionsConfigDialog();
		optionsConfigDialog.pack();
		UI.CenterInParent((JFrame) SwingUtilities.getWindowAncestor(this), optionsConfigDialog);
		optionsConfigDialog.setVisible(true);

	}

	/**
	 * @brief Edit the service command list for a device.
	 */
	private void textEditDev() throws IOException {

		if( textEditDialog == null ) {
			textEditDialog = new GenericOKCancelDialog(null, "", true);
	
			editorPane = new JEditorPane();
			editorPane.setPreferredSize( new Dimension(500,200) );
			
			JScrollPane editorScrollPane = new JScrollPane(editorPane);
	
			textEditDialog.add(BorderLayout.CENTER, editorScrollPane);
	
			textEditDialog.getOKPanel().getOKButton().addActionListener(this);
			
		}
		File optionsfile = getOptionsConfigFile(configureJsonDevice);
		String text = FileIO.Get(optionsfile.getAbsolutePath());
		editorPane.setText(text);
		textEditDialog.pack();
		UI.CenterInParent((JFrame) SwingUtilities.getWindowAncestor(this), textEditDialog);
		textEditDialog.setVisible(true);
	}

	/**
	 * @brief Save device text.
	 */
	public void saveDevText() throws ServiceCmdException, IOException{
		File optionsfile = getOptionsConfigFile(configureJsonDevice);
		JFrame f =  (JFrame) SwingUtilities.getWindowAncestor(this);

		String text = editorPane.getText();
		String lines[] = text.split(System.getProperty("line.separator"));
		//Check line formatting
		serviceCmdList.removeAllElements();
		for( String line : lines ) {
			line = line.trim();
			//Ignore commented and empty lines
			if( !line.startsWith("#") && line.length() > 0 ) {
				serviceCmdList.add( ServiceCmd.GetServiceCmd(line) );
			}
		}
		updateOptionsConfig();
		updateOptionsConfigDialog();
 	
	}

	/**
	 * @brief Create a popup menu for the jsonDevice to launch programs to use the connection to the device.
	 * @param jsonDevice
	 * @throws ServiceCmdException, IOException
	 */
	private void createPopupMenu(JSONObject jsonDevice ) throws ServiceCmdException, IOException {
		File optionsfile = getOptionsConfigFile(jsonDevice);   	
		JMenuItem jMenuItem;

		if( !optionsfile.isFile() ) {
			createDefaultOptionsFile(optionsfile, jsonDevice);
		}

		launcherPopupMenu.removeAll();

		serviceCmdList = loadServiceCmdList(jsonDevice);
		for( ServiceCmd serviceCmd : serviceCmdList ) {
			jMenuItem = new JMenuItem(serviceCmd.getMenuOptionName());
			jMenuItem.addActionListener(this);
			launcherPopupMenu.add(jMenuItem);        	
		}

		jMenuItem = new JMenuItem(Constants.CONFIGURE_OPTIONS);
		jMenuItem.addActionListener(this);
		launcherPopupMenu.add(jMenuItem);
	}

	/**
	 * @brief Display the VNC config dialog.
	 * @param jsonDevice
	 * @param jFrame
	 * @throws IOException
	 */
	private void editVNCConfiguration(JFrame jFrame) throws Exception {
		
		if( editVNCDialog != null ) {
			editVNCDialog.setVisible(false);
			editVNCDialog.dispose();
		}
		
		editVNCDialog = new GenericOKCancelDialog(jFrame, "Connection Configuration", true);
		OptionsPanel vncOptionsPanel = new OptionsPanel();
		editVNCDialog.add(vncOptionsPanel);
		editVNCDialog.pack();
		VNCOptionsConfig vncOptionsConfig = GetSelectedVNCOptionsConfig(configureJsonDevice);
		vncOptionsPanel.setArgs(vncOptionsConfig.getOptionsArray());

		UI.CenterInParent(jFrame, editVNCDialog);
		editVNCDialog.setVisible(true);
		if( editVNCDialog.isOkSelected() ) {

				vncOptionsConfig.deleteSPConfigFile();    
				vncOptionsConfig.setOptionsArray( vncOptionsPanel.getArgs() );
				vncOptionsConfig.saveHS(JSONProcessor.GetLocation(configureJsonDevice), JSONProcessor.GetUnitName(configureJsonDevice) );

		}
	}
	
	/**
	 * @brief Get the selected VNC Options config or null if not selected
	 * @param jsonDevice JSONObject instance.
	 * @return
	 * @throws Exception
	 */
	private static VNCOptionsConfig GetSelectedVNCOptionsConfig(JSONObject jsonDevice) throws Exception {
		VNCOptionsConfig vncOptionsConfig=null;

		String filename = Constants.APP_NAME.toLowerCase()+JSONProcessor.GetLocation(jsonDevice)+"_"+JSONProcessor.GetIPAddress(jsonDevice);  
		File topLevelConfigPath = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME) );

		if( topLevelConfigPath.isDirectory() ) {
			vncOptionsConfig = new VNCOptionsConfig( topLevelConfigPath.getAbsolutePath(), Constants.APP_NAME );
			try {
				vncOptionsConfig.loadHS(JSONProcessor.GetLocation(jsonDevice), JSONProcessor.GetUnitName(jsonDevice) ); 
			}
			catch(Exception e) {}
		}
		return vncOptionsConfig;
	}
	  
	/**
	 * @brief Edit the given service.
	 * @param serviceCmd
	 * @throws Exception
	 */
	private void editService(ServiceCmd serviceCmd) throws Exception {

		if( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_HTTP_CMD_TYPE ) {
			
			editExternalCmd(serviceCmd);

		}
		else if( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_VNC_CMD_TYPE ) {

			editVNCConfiguration( (JFrame) SwingUtilities.getWindowAncestor(this) );

		}
		else if( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_SERIAL_PORT_CMD_TYPE ) {
			MainFrame.SetStatus("Unable to edit Serial Port cmd.");			
		}
		else if( serviceCmd.getCmdType() == ServiceCmd.EXTERNAL_CMD_TYPE ) {
			
			editExternalCmd(serviceCmd);
			
		}

	}
	
	/**
	 * @brief Edit external command
	 * @param serviceCmd
	 * @throws Exception
	 */
	private void editExternalCmd(ServiceCmd serviceCmd) throws Exception {
		
		int selectedIndex = -1;
		int index=0;
		for( ServiceCmd srvCmd : serviceCmdList ) {
			if( srvCmd.toString().equals(serviceCmd.toString()) ) {
				selectedIndex=index;
				break;
			}
			index++;
		}
		if( selectedIndex == -1 ) {
			throw new ServiceCmdException("Unknown Cmd: "+serviceCmd);
		}
		
		if( editExternalCmdDialog == null ) {
			editExternalCmdDialog = new GenericOKCancelDialog(null, "", true);
	
			JFrame f =  (JFrame) SwingUtilities.getWindowAncestor(this);
	
			RowPane rowPane = new RowPane();
			menuOptionNameField = new JTextField(40);
			externalCmdField = new JTextField(40);
			externalCmdField.setToolTipText("Enter $h for the host address and $p for the TCPIP port.");
			
			rowPane.add(new JLabel("Menu Option Name"), menuOptionNameField );
			rowPane.add(new JLabel("Command"), externalCmdField );
			editExternalCmdDialog.add(BorderLayout.CENTER, rowPane);
			editExternalCmdDialog.pack();
			UI.CenterInParent(f, editExternalCmdDialog);
		}
		
		menuOptionNameField.setText(serviceCmd.getMenuOptionName());
		externalCmdField.setText(serviceCmd.getCmd());
		
		updateOptionsConfigDialog();
		
		editExternalCmdDialog.setVisible(true);

		if( editExternalCmdDialog.isOkSelected() ) {
			
			serviceCmd.setMenuOptionName(menuOptionNameField.getText());
			serviceCmd.setCmd(externalCmdField.getText());
			serviceCmdList.setElementAt(serviceCmd, selectedIndex);
			
			updateOptionsConfig();
			
		}
		
	}
	
	/**
	 * @brief Update a config file with the service details
	 * @param serviceCmdList
	 * @throws IOException
	 */
	private void updateOptionsConfig() throws IOException, ServiceCmdException {
		
		ensureNoDuplicates(serviceCmdList);
		
        File configFile = getOptionsConfigFile(configureJsonDevice);
		adServiceCmdFileHeader(configFile);
		Vector<String> cfgLines = new Vector<String>();
		for( ServiceCmd sCmd : serviceCmdList ) {
			cfgLines.add(sCmd.toString());
		}
		FileIO.SetLines(configFile.getAbsoluteFile().toString(), cfgLines, true, "\n");
		MainFrame.SetStatus("Updated config file: "+configFile);
	}
	
	/**
	 * @brief Check for duplicate service menu option names.
	 * @param serviceCmdList
	 * @throws ServiceCmdException
	 */
	private void ensureNoDuplicates(Vector<ServiceCmd> serviceCmdList) throws ServiceCmdException {
		for( ServiceCmd sCmdA : serviceCmdList ) {
			int nameCount=0;
			for( ServiceCmd sCmdB : serviceCmdList ) {
				if( sCmdA.getMenuOptionName().equals(sCmdB.getMenuOptionName())) {
					nameCount++;
				}
				if( nameCount > 1 ) {
					throw new ServiceCmdException("You may not have two options with the same name.");
				}
			}
		}
	}
	
	/**
	 * @brief Edit the selected service.
	 */
	private void editSelectedService() throws Exception {
		int index = serviceJList.getSelectedIndex();

		if( index >= 0 ) {

			Vector<ServiceCmd> serviceCmdList = loadServiceCmdList( configureJsonDevice	);
			
			if( index < serviceCmdList.size() ) {
				ServiceCmd serviceCmd = serviceCmdList.get(index);
				editService(serviceCmd);
			}

		}
		else {
			Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Please select the command to edit.");
		}
	}
	
	/**
	 * @brief Set a default set of services for the selected device.
	 * @throws IOException, ServiceCmdException
	 */
	private void setDefaultServices() throws IOException, ServiceCmdException {
		if( configureJsonDevice != null ) {
			int response = Dialogs.showYesNoDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Delete Option", "Are you sure you wish to reset to the default service list.");
			if ( response == JOptionPane.YES_OPTION ) {
				File optionsfile = getOptionsConfigFile(configureJsonDevice);  
				if( optionsfile.isFile() ) {
					optionsfile.delete();
				}
				createDefaultOptionsFile(optionsfile, configureJsonDevice);
			}
		}
	}

	/**
	 * @brief Copy the selected service.
	 * @throws ServiceCmdException
	 */
	private void copyService() throws ServiceCmdException, IOException {
		int index = serviceJList.getSelectedIndex();

		if( index >= 0 && index < serviceCmdList.size() && configureJsonDevice != null ) {

			ServiceCmd serviceCmd = serviceCmdList.get(index);
			ServiceCmd newServiceCmd = ServiceCmd.GetCopy(serviceCmd);
			newServiceCmd.setMenuOptionName(newServiceCmd.getMenuOptionName()+"_copy");
			
			serviceCmdList.addElement(newServiceCmd);
			
			updateOptionsConfig();
			
		}
		else {
			Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Please select an option to copy.");
		}
		
	}

	/**
	 * @brief Delete the selected service.
	 * @throws IOException
	 */
	private void deleteService() throws IOException, ServiceCmdException {
		
		int index = serviceJList.getSelectedIndex();

		if( index >= 0 && index < serviceCmdList.size() ) {
			
			
			int response = Dialogs.showYesNoDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Delete Option", "Are you sure you wish to delete the "+serviceCmdList.get(index).getMenuOptionName()+" option.");
			if ( response == JOptionPane.YES_OPTION ) {
			
				serviceCmdList.remove(index);
				updateOptionsConfig();
							
			}
			
		}
		else {
			Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Please select an option to delete.");
		}
		
	}
	
	/**
	 * @brief Move the selected service up in the list.
	 * @throws IOException
	 */
	private void serviceUp() throws IOException, ServiceCmdException {

		int index = serviceJList.getSelectedIndex();

		if( index != -1 ) {
			
			if( index == 0 ) {
				Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Already at the top of the list.");
			}
			else {
				ServiceCmd serviceCmd = serviceCmdList.elementAt(index);
				serviceCmdList.remove(index);
				serviceCmdList.insertElementAt(serviceCmd, index-1);
				updateOptionsConfig();
				
			}
					
		}
		else {
			Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Please select an option to move up.");
		}
		
	}
	
	/**
	 * @brief Move the selected service down in the list.
	 * @throws IOException
	 */
	private void serviceDown() throws IOException, ServiceCmdException {
		int index = serviceJList.getSelectedIndex();

		if( index != -1 ) {
			
			if( index >= serviceCmdList.size()-1 ) {
				Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Allready at the bottom of the list.");
			}
			else {
				ServiceCmd serviceCmd = serviceCmdList.elementAt(index);
				serviceCmdList.remove(index);
				serviceCmdList.insertElementAt(serviceCmd, index+1);
				updateOptionsConfig();
				
			}
					
		}
		else {
			Dialogs.showOKDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Info", "Please select an option to move down.");
		}
		
	}
	
	/**
	 * @brief Start an application to connect to the device
	 *        If user double clicks a row then web browser or terminal to that device.
	 * @param e The mouse event object.
	 */
	public void mouseClicked(MouseEvent e) {
		JSONObject selectedJsonDevice = getSelectedDevice();
		
		try {
			if( e.getClickCount() == 2 ) {
				
				if( e.getSource() == serviceJList ) {
					editSelectedService();
				}
				else if( selectedJsonDevice != null ) {
	
						createPopupMenu(selectedJsonDevice);
					launcherPopupMenu.show(this, e.getX(), e.getY() );
					launcherPopupMenu.setVisible(true);
	
				}
			}
		}
		catch(Exception ex) {
			//Ensure we have the last valid service device list loaded
			try {
				serviceCmdList = loadServiceCmdList(configureJsonDevice);
			}
			catch(Exception exx) {}
			MainFrame.statusBar.println(ex.getLocalizedMessage());
		}

	}
	
	/**
	 * @brief Check for device warnings in the list of devices that we have.
	 */
	private void checkForDeviceWarnings() {
		//Save the selected row
		int selectedRow = table.getSelectedRow();
		for( JSONObject jsonD : jsonDeviceVector ) {
			long updateMS = JSONProcessor.GetLocalRxTimeMs(jsonD);
			if( updateMS != -1 ) {
				long now = System.currentTimeMillis();
				long msAgo = now-updateMS;
				if( msAgo > MainFrame.GetDeviceTimeoutWarningMilliSeconds() ) {
					//Fire a table update so that the table is rendered with rows highlighted
					//that have breached the warning timeout.
					((AbstractTableModel) table.getModel()).fireTableDataChanged();
				}		
			}
		}
		//IF a row was selected ensure it is still selected.
		if( selectedRow >= 0 ) {
			table.setRowSelectionInterval(selectedRow,selectedRow);
		}
	}	
	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void actionPerformed(ActionEvent e) {
		boolean updateCfgDialog=true;
		JSONObject selectedJsonDevice = getSelectedDevice();

		try {
			JButton textEditorButton = null;
			if( textEditDialog != null && textEditDialog.getOKPanel() != null && textEditDialog.getOKPanel().getOKButton() != null ) {
				textEditorButton = textEditDialog.getOKPanel().getOKButton();
			}
	
			if( e.getSource() == deviceCheckTimer ) {
				checkForDeviceWarnings();
				checkForDeviceTimeouts();
				updateCfgDialog=false;
				
			}
			else if ( e.getActionCommand().equals(Constants.CONFIGURE_OPTIONS) ) {
	
				configureOptionsMenu(selectedJsonDevice, serviceCmdList);

			}
			else if ( e.getSource() == textEditButton ) {
				
				textEditDev();

			}
			else if ( e.getSource() == defaultsButton ) {
				
				setDefaultServices();

			}		
			else if ( e.getSource() == deleteButton ) {
				
				deleteService();

			}
			else if ( e.getSource() == copyButton ) {
				
				copyService();

			}
			else if ( e.getSource() == upButton ) {
				
				serviceUp();

			}
			else if ( e.getSource() == downButton ) {
				
				serviceDown();

			}
			else if ( e.getSource() == textEditorButton ) {
				
				saveDevText();

			}
			else if ( e.getSource() == editButton ) {
				
				editSelectedService();

			}
			else {
				
				for( ServiceCmd serviceCmd : serviceCmdList ) {
					if( e.getActionCommand().equals(serviceCmd.getMenuOptionName()) ) {
						notifyDeviceListeners(selectedJsonDevice, serviceCmd);
						break;
					}
				}
				updateCfgDialog=false;
			}
			
			if( updateCfgDialog && configureJsonDevice != null ) {
				updateOptionsConfigDialog();
			}	

		}
		catch(Exception ex) {
			ex.printStackTrace();
			//Ensure we have the last valid service device list loaded
			try {
				serviceCmdList = loadServiceCmdList(configureJsonDevice);
			}
			catch(Exception exx) {}
			Dialogs.showErrorDialog( (JFrame) SwingUtilities.getWindowAncestor(this) , "Service Error", ex.getLocalizedMessage() );
		} 

	}
	
	/**
	 * @brief Check for device timeouts in the list of devices that we have.
	 */
	private void checkForDeviceTimeouts() {
		int deviceIndex = 0;
		long now = System.currentTimeMillis();
		long msAgo;
		Vector<JSONObject> devicesToRemove = new Vector<JSONObject>();

		for( JSONObject jsonDevice : jsonDeviceVector ) {
			msAgo = now-JSONProcessor.GetLocalRxTimeMs(jsonDevice);

			//If we have not received an update for the timeout period
			if( msAgo > MainFrame.GetDeviceTimeoutLostMilliSeconds() ) {
				devicesToRemove.add(jsonDevice);
			}
			
			deviceIndex++;
		}
		
		//Remove all the timed out devices from our list.
		for( JSONObject jsonDevice : devicesToRemove ) {
			deviceLost(jsonDevice);
		}

	}

	/**
	 * @brief Get the index (0 based) of the given device.
	 * @param jsonDevice The JSONObject instance.
	 * @return The index of the device in the table or -1 if not found.
	 */
	private int getRowIndex(JSONObject jsonDevice) {
		boolean foundDevice = false;
		int tableRowIndex = 0;

		String location = JSONProcessor.GetLocation(jsonDevice);
		if( location != null && location.equals(location) ) {
			//Look through the table rows
			for( Vector <String>row :  rowData ) {
				if( row.get(IP_ADDRESS_COL_INDEX).equals( JSONProcessor.GetIPAddress(jsonDevice)) ) {
					foundDevice = true;
					break;
				}
				tableRowIndex++;
			}    	
		}

		if( foundDevice ) {
			return tableRowIndex;
		}
		return -1;
	}

	/**
	 * Remove a device from the list of displayed devices.
	 * @param jsonDevice
	 */
	private void remove(JSONObject jsonDevice) {
		int tableRowIndex = 0;
		String devIPAddress = JSONProcessor.GetIPAddress(jsonDevice);

		if( devIPAddress != null ) {

			//Look through the table rows
			for( Vector <String>row :  rowData ) {
				if( row.get(IP_ADDRESS_COL_INDEX).equals(devIPAddress) ) {
					rowData.remove(tableRowIndex);
					((AbstractTableModel) table.getModel()).fireTableDataChanged();
					break;
				}
				tableRowIndex++;
			}
			removeFromDeviceList(jsonDevice);
		}

	}

	/***
	 * @brief Remove a device from the list of devices
	 * @param jsonDevice
	 */
	private void removeFromDeviceList(JSONObject jsonDevice) {
		int devIndex = -1;
		int devIndexCount = 0;
		for( JSONObject jsonFromList : jsonDeviceVector ) {
			if( JSONProcessor.IPAddressMatch( JSONProcessor.GetIPAddress(jsonFromList), jsonDevice) ) {
				devIndex=devIndexCount;
				break;
			}
			devIndexCount++;
		}
		if( devIndex >= 0 ) {
			jsonDeviceVector.remove(devIndex);
		}
	}

	/**
	 * @brief Get the power in KW
	 * @param powerWatts The value in watts
	 * @return The Information String for power in KW
	 */
	private String getPowerKiloWattsString(String powerWatts) {
		String pwrKWStr = "";
		try {
			int powerWattsInt = Integer.parseInt(powerWatts);
			pwrKWStr = ((float)powerWattsInt)/1000F + " kW";
		}
		catch(NumberFormatException e) {}

		return pwrKWStr;
	}   

	/**
	 * @brief Add to the list of beacon beaconTableSelectionListener objects.
	 * @param beaconTableSelectionListener The object to be notified when the user selects a beacon in the beaconTablePanel.
	 */
	public void addDeviceTableSelectionListener(DeviceTableSelectionListener deviceTableSelectionListener) {
		deviceTableSelectionListenerList.add(deviceTableSelectionListener);
	}

	/**
	 * @brief Remove a beaconTableSelectionListener object from the list of of beaconTableSelectionListener objects.
	 * @param beaconTableSelectionListener The object to be notified when the user selects a beacon in the beaconTablePanel.
	 */
	public void removeDeviceTableSelectionListener(DeviceTableSelectionListener deviceTableSelectionListener) {
		deviceTableSelectionListenerList.remove(deviceTableSelectionListener);
	}

	/**
	 * @brief Remove all beaconTableSelectionListener objects
	 */
	public void removeAllDeviceTableSelectionLisenters() {
		deviceTableSelectionListenerList.removeAllElements();
	}

	public String getLocationName() { return location; }
	
	/**
	 * @brief Get the number of devices currently found at this location.
	 * @return The number of devices found at this location.
	 */
	public int getDeviceCount() {
		int devCount = 0;
		
		if( jsonDeviceVector != null ) {
			devCount = jsonDeviceVector.size();
		}
		return devCount;
	}
}
