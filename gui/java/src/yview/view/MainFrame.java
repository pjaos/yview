package yview.view;

import javax.swing.*;

import mtightvnc.OptionsPanel;
import mtightvnc.VNCFrame;
import mtightvnc.visitpc.VNCOptionsConfig;
import pja.gui.Dialogs;
import pja.gui.GenericOKCancelDialog;
import pja.gui.LookAndFeelDialog;
import pja.gui.UI;
import pja.io.FileIO;
import pja.io.ShellCmd;
import pja.io.SimpleConfig;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

import javax.swing.UIManager.LookAndFeelInfo;

import java.awt.Desktop;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.URL;
import java.io.*;
import java.util.*;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.awt.*;
import java.awt.event.*;

import yview.controller.*;
import yview.model.*;
import yview.view.ManageICONServerJFrame;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;
import javax.swing.Timer;

import org.json.JSONObject;

/**
 * Change Log
 * 
 * This changelog is no longer in use. Use git log in future.
 * 
 * 2.9 Now when devices timeout they don't disappear from the list but turn red.
 *     Added ability to configure, save and load the device timeout period.
 * 
 * 2.8 When connecting to some web servers the web page failed to load. This was
 *     because index.html was requested. This may not exist on all web servers.
 *     Therefore the URL ends / rather than /index.html as previously.
 *     
 * 2.7 Remove BEACON from names on Constants
 * 
 * 2.6 Fix bug which stopped connecting to units on LAN.
 *     Ensure that service names can be connected to regardless of the name case.
 * 
 * 2.5 Double clicking in LAN pane did not start web browser to connect to device. Fixed.
 * 
 * 2.4 Add message to status bar to indicate a problem with the ICON server should
 *     the connection fail to the server.
 *     Remove dialog if the connection fails. The user can still see the messages
 *     in the status bar window.
 *     
 * 2.3 Allow Launch of the same local and remote services.
 *     When servers reconfigured close all tabs so that new connections are displayed correctly.
 * 
 * 2.2 Add detection for UDP port usage and don't show LAN tab if in use.
 * 
 * 2.1 Reuse forwarded TCPIP ports .
 * 
 * 2.0 Added ability to talk to remote devices via ICON server.
 * 
 * 1.9 Fix bug that threw exception when row selected in table.
 * 
 * 1.8 Add New Window option in Unity launcher.
 *
 * 1.7 Show text splash window when opening browser to connect to device as
 *     it may take a little while depending upon network connectivity and 
 *     machine speed.
 *     
 * 1.6 Add Ubuntu Unity Launcher and add WyView Icon.
 *
 * 1.5 Only the WySw unit has the ability to measure current. Therefore ensure power
 *     is only displayed for units with the WySw product ID.
 *
 * 1.4 Added support for displaying power in kW
 * 
 * 1.3 ?
 *
 * 1.2 Changed the button names to reflect their function
 * 
 * 1.1 Initial release 
 **/
  
/**
 * Responsible for displaying received Beacons so that users can configured each unit and connect to each unit
 */
public class MainFrame extends JFrame implements ActionListener, WindowListener, DeviceTableSelectionListener, Runnable, JSONListener
{
	static final long serialVersionUID=2;
	public static final String 	LAN_LOCATION  			= "LAN";
	public static final String 	XPOS	    			= "XPOS";
	public static final String 	YPOS	    			= "YPOS";
	public static final String 	WIDTH	    			= "WIDTH";
	public static final String 	HEIGHT                  = "HEIGHT";
	public static final String 	LOOK_AND_FEEL			= "LOOK_AND_FEEL";
	public static final String 	GROUP_NAME 				= "GROUP_NAME";
	public static final String 	WARNING_DEVICE_TIMEOUT  = "WARNING_DEVICE_TIMEOUT";
	public static final String 	LOST_DEVICE_TIMEOUT  	= "LOST_DEVICE_TIMEOUT";
	public static final boolean 	DEBUG     			= true;            //Enable this to print stack traces
	private static float DeviceTimeoutWarningSeconds 	= Constants.DEFAULT_WARNING_DEVICE_TIMEOUT_SECS;
	private static float DeviceTimeoutLostSeconds 		= Constants.DEFAULT_LOST_DEVICE_TIMEOUT_SECS;
	private Vector<DeviceMsgDebug> deviceMsgDebugList 	= new Vector<DeviceMsgDebug>();
	private String deviceSearchText						=	"";
	private JButton copyDebugButton;
	private JButton clearDebugButton;
	private JTextPane devDebugTextPane;

	JMenuItem         		configJMenuItem;
	JMenuItem         		configDeviceWarningTimeoutJMenuItem;
	JMenuItem         		configDeviceLostTimeoutJMenuItem;
	JMenuItem         		configICONSJMenuItem;
	JMenuItem         		showDevMessagesMenuItem;
	JMenuItem         		lookAndFeelMenuItem;
	JMenuItem         		exitJMenuItem;
	LookAndFeelDialog		lookAndFeelDialog;
	static File 			ConfigFile;
	static File 			DevicePropertiesConfigFile;
	static String           GroupName="";
	ManageICONServerJFrame	manageICONServerJFrame;
	JTabbedPane 			tabbedPane;
	ICONSConnectionManager  iconsConnectionManager;
	JPanel                  bottomPanel;
	static StatusBar      	statusBar;
	JScrollPane				scrollPane;
	DeviceTablePanel		localDeviceTablePanel;
	LanDeviceReceiver 		lanDeviceReceiver;
	AreYouThereTXThread 	areYouThereTXThread;
	DatagramSocket			lanDatagramSocket;
	int						selectedLookAndFeel;
	Timer 					removeLocationTimer;

	Hashtable<String, Integer>    remoteLocalPortHashtable; //A cache for local TCP ports to be used to connect to remote devices
  
	JButton	reconnectButton = new JButton("Reconnect");
	
  /**
   * Constructor.
   * @param title The terminal frames window title.
   */
  public MainFrame(String title) throws SocketException {
      super(title+" (V"+Constants.VERSION+")");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      statusBar = new StatusBar();
      
      ensureLanDefaultCfgExists();

	  remoteLocalPortHashtable = new Hashtable<String, Integer>();

      iconsConnectionManager = new ICONSConnectionManager();
      
      ConfigFile = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), MainFrame.class.getName());
      DevicePropertiesConfigFile = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), MainFrame.class.getName()+"_device_properties.cfg");

      //Load window pos and size, if no saved details then set default size and position
      if ( !loadUIConfig() ) {
    	  setDefaultLookAndFeel(); 	  
    	  setMinimumSize(new Dimension(640, 480));
    	  setPreferredSize(new Dimension(640, 480));
    	  pack();
      }
      
      manageICONServerJFrame = new ManageICONServerJFrame(this, iconsConnectionManager);
      
      tabbedPane = new JTabbedPane();
            
      scrollPane = new JScrollPane( tabbedPane );
      getContentPane().add("Center", scrollPane);
      
      bottomPanel = new JPanel( new BorderLayout() );
      
      bottomPanel.add(statusBar, BorderLayout.SOUTH);
      
      add(bottomPanel, BorderLayout.SOUTH);
            
      loadDeviceTimeouts();
      
      addWindowListener(this);
     
      JMenuBar mb = new JMenuBar();
      JMenu m = new JMenu("File");
      
      configJMenuItem = new JMenuItem("Group Name", KeyEvent.VK_C);
      configDeviceWarningTimeoutJMenuItem = new JMenuItem("Warning Device Timeout", KeyEvent.VK_W);
      configDeviceLostTimeoutJMenuItem = new JMenuItem("Lost Device Timeout", KeyEvent.VK_L);
      configICONSJMenuItem = new JMenuItem("ICON server configuration", KeyEvent.VK_I);
      showDevMessagesMenuItem = new JMenuItem("Device Message Viewer", KeyEvent.VK_S);
      lookAndFeelMenuItem = new JMenuItem("Look And Feel", KeyEvent.VK_F);
      exitJMenuItem = new JMenuItem("Exit", KeyEvent.VK_E);
      
      m.add(configJMenuItem);
      m.add(configDeviceWarningTimeoutJMenuItem);
      m.add(configDeviceLostTimeoutJMenuItem);
      m.add(configICONSJMenuItem);
      m.add(showDevMessagesMenuItem);
      m.add(lookAndFeelMenuItem);
      m.add(exitJMenuItem);

      configICONSJMenuItem.addActionListener(this);
      configDeviceWarningTimeoutJMenuItem.addActionListener(this);
      configDeviceLostTimeoutJMenuItem.addActionListener(this);
      configJMenuItem.addActionListener(this);
      showDevMessagesMenuItem.addActionListener(this);
      lookAndFeelMenuItem.addActionListener(this);
      exitJMenuItem.addActionListener(this);
      
      reconnectButton.addActionListener(this);
      
      mb.add(m);
      mb.add(reconnectButton);
      setJMenuBar( mb );
      
      setVisible(true);

      //Allow the user to double click the tab title to reorder them.
      tabbedPane.addMouseListener(new MouseAdapter() {
    	    @Override
    	    public void mouseClicked(MouseEvent me) {
    	    	if( me.getClickCount() == 2 ) {
    	    		int selectedTab = tabbedPane.getSelectedIndex();
    	    		
    	    		if( selectedTab >  0 ) {
    	    			String tabTitle = tabbedPane.getTitleAt(selectedTab-1);
    	    			JPanel panel = (JPanel)tabbedPane.getComponent(selectedTab-1);
    	    			String toolTipText = panel.getToolTipText();
    	    			tabbedPane.removeTabAt(selectedTab-1);
    	    			tabbedPane.addTab(tabTitle, null, panel, toolTipText);
    	    			
    	    		}
    	    	}
    	    }
    	}); 
      initTabs();
      
      removeLocationTimer = new Timer(5000, this);
      removeLocationTimer.start();
 
      iconsConnectionManager.setStatusBar(statusBar);
      iconsConnectionManager.addDeviceListener(this);
      new Thread(this).start();
  }
  
  /**
   * @brief Called periodically to check for location tabs with no devices.
   *        If an empty location tab is found it is removed.
   * @return void.
   */
  void purgeEmptyLocations() {  
	  //Check through all display location tabs for empty locations
	  for( int tabIndex=0; tabIndex<tabbedPane.getTabCount() ; tabIndex++ ) {
		  String locationName = tabbedPane.getTitleAt(tabIndex);
		  
		  Component component = tabbedPane.getComponentAt(tabIndex);
		  if( component != null ) {
			  DeviceTablePanel devTablePanel = (DeviceTablePanel)component;
			  int locationDevCount = devTablePanel.getDeviceCount();
			  if( locationDevCount == 0 ) {
				  tabbedPane.remove(tabIndex);
			  }
		  }
	  }
  }
  
  /**
   * @brief Set the default look and feel for the GUI.
   */
  private void setDefaultLookAndFeel() {
	  try {
		  int index=0;
		  for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {    	
			  if ("Nimbus".equals(info.getName())) {
				  UIManager.setLookAndFeel(info.getClassName());
				  selectedLookAndFeel=index;
				  break;
			  }
			  index++;
		  }

	  }
	  catch(Exception e) {}
  }
  
  /**
   * @brief Init tabs...
   * @throws SocketException
   */
  public void initTabs() throws SocketException {
	  tabbedPane.removeAll();
	  
	  if( getActiveICONSCount() == 0 ) {
	      try {
	    	  lanDatagramSocket = new DatagramSocket(Constants.UDP_MULTICAST_PORT);
	      }
	      catch(BindException e ) {
	    	  statusBar.println("UDP port "+Constants.UDP_MULTICAST_PORT+" is in use on this computer.\nLAN device detection disabled as the above TCPIP port is in use.");
	      }
	      if( lanDatagramSocket != null ) {
	    	  initLocalDeviceInterface();
	      }
	  }

  }
  
  /**
   * @return The number of ICONSServers that are configured and active.
   */
  private int getActiveICONSCount() {
	  int activeICONSServerCount = 0;
	  
	  for( ICONServer iconsServer : manageICONServerJFrame.getICONServerList() ) {
		  if( iconsServer.getActive() ) {
			  activeICONSServerCount++;
		  }
	  }
	  return activeICONSServerCount;
  }
  
  /**
   * Init (display GUI and start servers) for detection of devices on the local LAN.
   */
  private void initLocalDeviceInterface() throws SocketException {
	  
	  localDeviceTablePanel = new DeviceTablePanel(LAN_LOCATION);
	  tabbedPane.addTab(localDeviceTablePanel.getLocationName(), null, localDeviceTablePanel, "All the devices found on the local area network.");
	  localDeviceTablePanel.addDeviceTableSelectionListener(this);
	  tabbedPane.setMnemonicAt(0, KeyEvent.VK_L);

	  if( lanDatagramSocket != null ) {
		  
	      areYouThereTXThread = new AreYouThereTXThread(lanDatagramSocket);
		  areYouThereTXThread.start();    
	
	      lanDeviceReceiver = new LanDeviceReceiver(lanDatagramSocket);
	      lanDeviceReceiver.setDaemon(true);
	      lanDeviceReceiver.addJSONListener(this);
	      lanDeviceReceiver.start();
	      
	  }
	  
  }
  
  
  /**
   * @brief Thread to build connections to the ICON (ssh) server/s
   */
  public void run() {
	  iconsConnectionManager.connectICONS(manageICONServerJFrame.getICONServerList());
  }
  
  /**
   * Called when either the configure or connect button is called
   */
  public void actionPerformed(ActionEvent e) {
	  
	  if( e.getSource() == configJMenuItem ) {
    	  
        String name = (String)JOptionPane.showInputDialog(this, "Group name", "Enter configured WyTerm group name", JOptionPane.INFORMATION_MESSAGE, null, null, GroupName);
        if( name != null ) {
          GroupName=name;
          
        }
      } else if ( e.getSource() == configDeviceWarningTimeoutJMenuItem ) {

    	  DeviceTimeoutWarningSeconds = getFloatDialog(this, "Set Warning Device Timeout", "Input the device timeout in seconds", DeviceTimeoutWarningSeconds);
		  saveDeviceTimeouts();
		  
	  }  else if ( e.getSource() == configDeviceLostTimeoutJMenuItem ) {

		  DeviceTimeoutLostSeconds = getFloatDialog(this, "Set Lost Device Timeout", "Input the device timeout in seconds", DeviceTimeoutLostSeconds);
		  saveDeviceTimeouts();
		  
	  }  else if( e.getSource() == configICONSJMenuItem ) {
    	  manageICONServerJFrame.setLocationRelativeTo( this );
    	  manageICONServerJFrame.setVisible(true);
    	  
      }  else if ( e.getSource() == showDevMessagesMenuItem ) {
    	  
    	  deviceSearchText = (String)JOptionPane.showInputDialog(this, "Show Device Messages", "Enter search text", JOptionPane.INFORMATION_MESSAGE, null, null, deviceSearchText);
          if( deviceSearchText != null ) {
        	  addDevMsgDebug(deviceSearchText);
          }
    	  
      } else if( e.getSource() == lookAndFeelMenuItem ) {
    	  
          setLookAndFeel();        
          
      } else if( e.getSource() == exitJMenuItem ) {
    	  
    	  saveUIConfig();
    	  System.exit(0);
      
      } else if( e.getSource() == copyDebugButton ) {
    	  String text = devDebugTextPane.getText();
    	  StringSelection stringSelection = new StringSelection(text);
    	  Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    	  clipboard.setContents(stringSelection, null);
      }
      else if( e.getSource() == clearDebugButton ) {
    	  devDebugTextPane.setText("");
      }
      else if( e.getSource() == reconnectButton ) {
    	  if( iconsConnectionManager != null ) {
    		  //Execute the reconnection from another thread so as not to block
    		  new Thread() {
    			  public void run() {
    				  manageICONServerJFrame.connectToServer(true);
    			  }
    		  }.start();
    	  }
      }
      else if( e.getSource() == removeLocationTimer ) {
    	  purgeEmptyLocations();
      }
	  
	  
  }
  
  /**
   * @brief Allow the user to select the look and feel of the UI.
   */
  private void setLookAndFeel() {
	  if( lookAndFeelDialog == null ) {
		  lookAndFeelDialog = new LookAndFeelDialog(this, "Look And Feel", Constants.APP_NAME);
		  lookAndFeelDialog.pack();
		  UI.CenterInParent(this, lookAndFeelDialog);
	  }
	  
	  lookAndFeelDialog.setSelectedLookAndFeel(selectedLookAndFeel);
	  lookAndFeelDialog.setVisible(true);
	  if( lookAndFeelDialog.isOkSelected() ) {
		  selectedLookAndFeel = lookAndFeelDialog.lookAndFeelComboBox.getSelectedIndex();
		  saveUIConfig() ;
		  setLookAndFeel(selectedLookAndFeel);
	  }
	  lookAndFeelDialog.setVisible(false);
  }
  
  /**
   * @brief Set the selected look and feel in the GUI.
   * @param selectedIndex The index in UIManager.getInstalledLookAndFeels(). Must be a valid look and feel index.
   */
  private void setLookAndFeel(int selectedIndex)
  {
    if (selectedIndex >= 0)
    {
      try
      {
        UIManager.LookAndFeelInfo[] lookAndFeels = null;
        lookAndFeels = UIManager.getInstalledLookAndFeels();
        UIManager.setLookAndFeel(lookAndFeels[selectedIndex].getClassName());
        SwingUtilities.updateComponentTreeUI(this);
      } catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }
  
  public static float getFloatDialog(Frame frame,String title,String message, float currentValue)
  {
	String value="";
        float floatValue=currentValue;
	while(true) {
		try {
			value = (String) JOptionPane.showInputDialog(frame, message,title,JOptionPane.INFORMATION_MESSAGE, null, null, ""+currentValue);
			if( value == null ) {
				break;
			}
			floatValue = Float.parseFloat(value);
			break;
		}
		catch(NumberFormatException e) {
			Dialogs.showErrorDialog(frame, title, value+" is not a valid number.");
		}
	}
	return floatValue;
  }

  /**
   * @brief Add debugging for a given device.
   * @param devName The name of partial name of the device/devices to debug.
   */
  private void addDevMsgDebug(String devName) {
	  JFrame devMsgWindow = new JFrame(devName+" messages.");
	  devDebugTextPane = new JTextPane(); 
	  devDebugTextPane.setContentType("text/plain");
	  devDebugTextPane.setToolTipText("Use CTRL A, CTRL C to copy text.");
	  JScrollPane jScrollPane = new JScrollPane(devDebugTextPane);
	  jScrollPane.setPreferredSize( new Dimension(500, 640) );
	  devMsgWindow.add(jScrollPane);
	  
	  SimpleAttributeSet attributeSet = new SimpleAttributeSet();
	  
	  DeviceMsgDebug deviceMsgDebug = new DeviceMsgDebug();
	  deviceMsgDebug.doc = devDebugTextPane.getStyledDocument();
	  deviceMsgDebug.devName = devName;
	  deviceMsgDebug.attributeSet = attributeSet;
	  deviceMsgDebug.jTextPane=devDebugTextPane;
	  devMsgWindow.pack();
	  devMsgWindow.setVisible(true);

	  deviceMsgDebugList.add(deviceMsgDebug);
  
	  JPanel buttonPanel = new JPanel();
	  clearDebugButton = new JButton("Clear");
	  clearDebugButton.addActionListener(this);
	  buttonPanel.add(clearDebugButton);
	  copyDebugButton = new JButton("Copy");
	  copyDebugButton.addActionListener(this);
	  buttonPanel.add(copyDebugButton);
	  devMsgWindow.add(buttonPanel, BorderLayout.SOUTH);
  }
  
  /**
   * @brief set the message to display in the status bar.
   * @param line The line of text.
   */
  public static void SetStatus(String line) {
	  statusBar.println(line);
  }
  
  /**
   * Load the window details
   */
  public boolean loadUIConfig() {
	boolean loadedWindowDetails=false;
	  try {
		  Properties	windowProperties = new Properties();
		  FileInputStream fis = new FileInputStream( MainFrame.ConfigFile );
		  windowProperties.load(fis);
		  int xpos = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.XPOS) );
		  int ypos = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.YPOS) );
		  int width = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.WIDTH) );
		  int height = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.HEIGHT) );
		  GroupName = windowProperties.getProperty(MainFrame.GROUP_NAME);
		  selectedLookAndFeel = (int)Double.parseDouble( windowProperties.getProperty(MainFrame.LOOK_AND_FEEL) );
	      UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
	      UIManager.setLookAndFeel( lookAndFeels[selectedLookAndFeel].getClassName() );
		  this.setSize( width, height );
		  this.setLocation( xpos, ypos );
		  fis.close();
		  loadedWindowDetails=true;
	  }
	  catch(Exception e) {
        if( DEBUG) {
          e.printStackTrace();
        }
	  }
	  return loadedWindowDetails;
  }
  
  /**
   * Save the window size and position so that it can be loaded on restart.
   */
  public void saveUIConfig() {
	  try {
		  Properties	windowProperties = new Properties();
		  FileWriter fw = new FileWriter( MainFrame.ConfigFile );
		  windowProperties.put(MainFrame.XPOS, ""+this.getLocation().getX());
		  windowProperties.put(MainFrame.YPOS, ""+this.getLocation().getY());
		  windowProperties.put(MainFrame.WIDTH, ""+this.getSize().getWidth());
		  windowProperties.put(MainFrame.HEIGHT, ""+this.getSize().getHeight());
		  windowProperties.put(MainFrame.GROUP_NAME, ""+GroupName);
		  windowProperties.put(MainFrame.LOOK_AND_FEEL, ""+selectedLookAndFeel);
		  windowProperties.store(fw, "");
		  fw.close();
	  }
	  catch(Exception e) {
        if( DEBUG) {
          e.printStackTrace();
        }
	  }
  }

  /**
   * Save the device timeout period.
   */
  public void saveDeviceTimeouts() {
	  try {
		  Properties	deviceProperties = new Properties();
		  FileWriter fw = new FileWriter( MainFrame.DevicePropertiesConfigFile );
		  deviceProperties.put(MainFrame.WARNING_DEVICE_TIMEOUT, ""+DeviceTimeoutWarningSeconds);
		  deviceProperties.put(MainFrame.LOST_DEVICE_TIMEOUT, ""+DeviceTimeoutLostSeconds);
		  deviceProperties.store(fw, "");
		  fw.close();
	  }
	  catch(Exception e) {
        if( DEBUG) {
          e.printStackTrace();
        }
	  }
  }  
  
  /**
   * Load the window details
   */
  public boolean loadDeviceTimeouts() {
	boolean loadedConfig=false;
	  try {
		  Properties	deviceProperties = new Properties();
		  FileInputStream fis = new FileInputStream( MainFrame.DevicePropertiesConfigFile );
		  deviceProperties.load(fis);
		  DeviceTimeoutWarningSeconds = (float)Float.parseFloat( deviceProperties.getProperty(MainFrame.WARNING_DEVICE_TIMEOUT) );
		  DeviceTimeoutLostSeconds = (float)Float.parseFloat( deviceProperties.getProperty(MainFrame.LOST_DEVICE_TIMEOUT) );
		  fis.close();
		  loadedConfig=true;
	  }
	  catch(Exception e) {
        if( DEBUG) {
          e.printStackTrace();
        }
	  }
	  return loadedConfig;
  }

  /**
   * Window selection event API's
   */
  public void windowActivated(WindowEvent arg0) {}
  public void windowClosed(WindowEvent arg0) {}
  public void windowClosing(WindowEvent arg0) {
	  saveUIConfig();
  }
  public void windowDeactivated(WindowEvent arg0) {}
  public void windowDeiconified(WindowEvent arg0) {}
  public void windowIconified(WindowEvent arg0) {}
  public void windowOpened(WindowEvent arg0) {}
   
  /**
   * @brief Get the key format string for the location and serverPort.
   * @param location
   * @param hostAddress host address of the device or server.
   * @param serverPort
   * @return The string concatenating location and serverPort.
   */
  private String getRemoteLocalPortHashtableKey(String location, String hostAddress, int serverPort) {	
	  String key = location+hostAddress+serverPort;
	  return key;
  }
  
  /**
   * @brief Get the local host forwarding port associated with the serverPort.
   *        If setup previously then we read this from storage. IF not setup previously
   *        then a port is allocated.
   * @param location The location of the device.
   * @param hostAddress host address of the device or server.
   * @param serverPort
   * @return The local host forwarding port.
   */
  private int getLocalhostForwardingPort(String location, String hostAddress, int serverPort) {
      int localhostPort = -1;
      String key =  getRemoteLocalPortHashtableKey(location, hostAddress, serverPort);

      if( remoteLocalPortHashtable.containsKey(key) ) {
          localhostPort = remoteLocalPortHashtable.get(key);
      }
      else {
          try {
              ServerSocket s = new ServerSocket(0);
              localhostPort = s.getLocalPort();
              s.close();             
          } catch (IOException e) {}
      }
      return localhostPort;
  }

  /**
   * @brief Check if local host forwarding port associated with the serverPort has already been setup.
   * @param location The location of the device.
   * @param hostAddress host address of the device or server.
   * @param serverPort
   * @param localhostPort
   * @return true if already setup, false if not.
   */
  private boolean isPortForwardingAlreadySetup(String location, String hostAddress, int serverPort, int localhostPort) {
      boolean portForwardingAlreadySetup = false;
      String key =  getRemoteLocalPortHashtableKey(location, hostAddress, serverPort);
      
      if ( remoteLocalPortHashtable.containsKey(key)) {
          int storedLocalhostPort = remoteLocalPortHashtable.get(key);
          if (storedLocalhostPort == localhostPort) {
              portForwardingAlreadySetup=true;
          }
      }
      return portForwardingAlreadySetup;
  }

  /**
   * @brief Store the local host forwarding port associated with the serverPort. This should be
   *        called once port forwarding has been setup.
   * @param location The location of the device.
   * @param hostAddress host address of the device or server.
   * @param serverPort
   * @param localhostPort
   */
  private void storeLocalhostForwardingPort(String location, String hostAddress, int serverPort, int localhostPort) {
	  String key =  getRemoteLocalPortHashtableKey(location, hostAddress, serverPort);
	  
      if( remoteLocalPortHashtable.containsKey(key) ) {
          int storedLocalhostPort = remoteLocalPortHashtable.get(key);
          if( storedLocalhostPort != localhostPort ) {
        	  System.out.println("ERROR serverPort="+serverPort+" should be associated with localhostPort="+localhostPort+" but is associated with storedLocalhostPort="+storedLocalhostPort);
          }
      }
      else {
          remoteLocalPortHashtable.put(key, localhostPort);
      }
  }
  
  /**
   * @brief Determine if a device is on the LAN or remote
   * @return true if on the LAN, else False
   */
  private boolean isLocalDevice(JSONObject jsonDevice) {
	  
	  if( JSONProcessor.GetLocation(jsonDevice).equals(Constants.LOCAL_LOCATION) ) {
		  return true;
	  }
	  return false;

  }
  
  /**
   * @brief Ensure that the file containing the LAN tab default service config exists.
   * @detail The LAN tab shows devices on the local LAN. Devices on the local LAN may 
   * not advertise the services that they support (E.G web server). Therefore this file 
   * defines the services based upon the product type wit ha fallback to a default web 
   * server service.
   */
  public void ensureLanDefaultCfgExists() {
	  File configFile = Service.GetDefaultSrvConfigFile();
	  if( !configFile.exists() ) {
		  JSONObject jsonDefaults = new JSONObject(Service.LAN_SERVICE_DEFAULTS);
		  try {
			  FileIO.Set(configFile.getAbsolutePath(), jsonDefaults.toString(4), false);
		  }
		  catch(IOException e) {
			  statusBar.println("Failed to create the "+configFile.getAbsolutePath()+" file: "+e.getLocalizedMessage() );
		  }
		  statusBar.println("Created "+configFile.getAbsolutePath());
	  }
	  statusBar.println("Using LAN defaults from "+configFile.getAbsolutePath());
  }
  
  /**
   * @brief If a device has been selected by the user is from the LAN table then handle 
   *        connecting to the device here.
   * @param jsonDevice The device to connect to.
   * @param serviceCmd The ServiceCmd instance.
   */
  private void handleLocalDevice(JSONObject jsonDevice, ServiceCmd serviceCmd) {
	  String cmdLineString = serviceCmd.getCmd();
	  String deviceAddress = JSONProcessor.GetIPAddress(jsonDevice);
	  int servicePort = serviceCmd.getServicePort();

	  //As we're local all connections will be to a local address and local port.
	  cmdLineString=cmdLineString.replace(Constants.HOST_STRING, deviceAddress );
	  cmdLineString=cmdLineString.replace(Constants.PORT_STRING, ""+servicePort );	
	  
	  if( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_HTTP_CMD_TYPE ) {
		  OpenWebpage(this, statusBar, cmdLineString);		  
      }
	  else if ( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_VNC_CMD_TYPE ) {
		  OpenVNC(jsonDevice, deviceAddress, servicePort);
	  }
	  else if ( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_SERIAL_PORT_CMD_TYPE ) {
		  OpenTerminal(jsonDevice, deviceAddress, servicePort);
	  }
	  else if ( serviceCmd.getCmdType() == ServiceCmd.EXTERNAL_CMD_TYPE ) {
		  runExternalCmd(cmdLineString);
	  }
  }

  /**
   * @brief If a device has been selected by the user is not from the LAN table then handle
   *        connecting to the device here. This involves connecting through the local and reverse
   *        ssh port forwarding connections via the ICON (Internet connection) server.
   * @param jsonDevice The device to connect to.
   * @param serviceCmd The ServiceCmd instance.
   */
  private void handleRemoteDevice(JSONObject jsonDevice, ServiceCmd serviceCmd) {
	  String cmdLineString;
	  boolean lan=false;
	  boolean serviced=false;
      int localPort=-1;

      try {

	      Service serviceArray[] = Service.GetServiceList( JSONProcessor.GetServicesList(jsonDevice) );
		  
		  for( Service service : serviceArray ) {
	
	          localPort = getLocalhostForwardingPort(JSONProcessor.GetLocation(jsonDevice), JSONProcessor.GetIPAddress(jsonDevice), service.port );
	          if (localPort != -1) {
	        	  
	        	  if( !isPortForwardingAlreadySetup(JSONProcessor.GetLocation(jsonDevice), JSONProcessor.GetIPAddress(jsonDevice), service.port, localPort) ) {
	                  //Setup forwarding from a local TCP server port to the remote ssh server port = that connected to the remote device.
	        		  Session session = JSONProcessor.GetICONSSSHSession(jsonDevice);
	        		  if( session != null ) {
	        			  session.setPortForwardingL(Constants.LOCAL_HOST, localPort, Constants.LOCAL_HOST, service.port);
	        		  }
	        		  else {
	        			  statusBar.println("No session object found for: "+jsonDevice.toString(4));
	        		  }
	                  storeLocalhostForwardingPort(JSONProcessor.GetLocation(jsonDevice), JSONProcessor.GetIPAddress(jsonDevice), service.port, localPort);
	                  statusBar.println("Forwarding local port "+localPort+" to connect to this service (server port="+service.port+").");
	        	  }
	
	        	  //As we're remote all connections will be to a local port forwarded via ssh tunnel to a remote device
	        	  String deviceAddress=Constants.LOCAL_HOST;
	        	  cmdLineString = serviceCmd.getCmd();
	        	  cmdLineString=cmdLineString.replace(Constants.HOST_STRING, deviceAddress);
	        	  cmdLineString=cmdLineString.replace(Constants.PORT_STRING, ""+localPort);

	        	  if( service.serviceName.equals(serviceCmd.getServiceName()) ) {
	        		  
	        		  if( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_HTTP_CMD_TYPE ) {
	        			  
	        			  OpenWebpage(this, statusBar, cmdLineString);		  
	        			  serviced=true;
	        	    	  
	        	      }
	        		  else if ( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_VNC_CMD_TYPE ) {
	        			  
	        			  OpenVNC(jsonDevice, deviceAddress, localPort);
	        			  serviced=true;
	        			  
	        		  }
	        		  else if ( serviceCmd.getCmdType() == ServiceCmd.INTERNAL_SERIAL_PORT_CMD_TYPE ) {
	  
	        			  OpenTerminal(jsonDevice, deviceAddress, localPort);
	        			  serviced=true;
	        			  
	        		  }
	        		  else if ( serviceCmd.getCmdType() == ServiceCmd.EXTERNAL_CMD_TYPE ) {
	        			  runExternalCmd(cmdLineString);
	        			  serviced=true;
	        		  }
	        	  }
	          }
	          if( serviced ) {
	        	  break;
	          }
		  }
	  
	  }
	  catch(JSchException e) {
		  e.printStackTrace();
	  }
  
  }


  /**@brief Open VNC session
   * @param jsonDevice
   * @param deviceAddress
   * @param servicePort
   */
  private void OpenTerminal(JSONObject jsonDevice, String deviceAddress, int servicePort ) {
	  new  SplashWindow(this, "Opening: "+deviceAddress+":"+servicePort+ " (Serial Port). Please wait...", 1000);
	  MainFrame.SetStatus("Opening: "+deviceAddress+":"+servicePort+" (Serial Port)");
	  new WyTermTerminal(deviceAddress, servicePort, JSONProcessor.GetUnitName(jsonDevice), JSONProcessor.GetSerialConfigString(jsonDevice) );
  }

  /**@brief Open VNC session
   * @param jsonDevice
   * @param deviceAddress
   * @param servicePort
   */
  private void OpenVNC(JSONObject jsonDevice, String deviceAddress, int servicePort ) {
	  new  SplashWindow(this, "Opening: "+deviceAddress+":"+servicePort+ " (VNC). Please wait...", 1000);
	  MainFrame.SetStatus("Opening: "+deviceAddress+":"+servicePort+" (VNC)");
		
      VNCOptionsConfig vncOptionsConfig = GetSelectedVNCOptionsConfig(jsonDevice);
      if( vncOptionsConfig != null ) {
    	  vncOptionsConfig.serverAddress=deviceAddress;
    	  vncOptionsConfig.serverPort=servicePort;                        		  
          statusBar.println("CONNECT TO "+vncOptionsConfig.serverAddress+":"+vncOptionsConfig.serverPort);
          VNCFrame.Launch(vncOptionsConfig.getOptionsArray(), null, null, Constants.APP_NAME);
    	  statusBar.println("Opening VNC @ "+deviceAddress+":"+servicePort);	
      }		  	  
  }

  /**
   * Called when the configure button is selected in order to start a web browser that will 
   * allow the user to configure the hardware.
   * @param uri The URI object to open
   */
  private static void OpenWebpage(Frame frame,  StatusBar statusBar, String url) {
	new  SplashWindow(frame, "Opening "+url+ ". Please wait...", 1000);
	MainFrame.SetStatus("Opening system web browser: "+url);	
	
    //Open web page in a daemon thread so as not to block closing of this application.
    class Worker extends Thread {
      String url;
      public Worker(String url) {
        this.url=url;
        this.setDaemon(true);
      }
      
      public void run() {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URL(url).toURI());
            } catch (Exception e) {
                if( DEBUG) {
                  e.printStackTrace();
                }
            }
        }
      }
    }
    new Worker(url).start();
    statusBar.println("Opening web page: "+url);
  }

  
  /**
   * Run an external program.
   * 
   * @param command The command line to run the external program.
   */
  private void runExternalCmd(String command) {
		new  SplashWindow(this, "Running external command: "+command+". Please wait...", 1000);
		MainFrame.SetStatus("Running external command: "+command);	  
	  
		class Worker extends Thread {
			String command;
			  
			public Worker(String command) {
				this.command=command;
			}
			  
			public void run() { 
		  		try {
		  			statusBar.println("Run external command: "+command);
					Runtime.getRuntime().exec(command);
		  		}
		  		catch(Exception e) {
		  			statusBar.println(e.getLocalizedMessage());
		  			e.printStackTrace();
		  		} 
			}
		}
		new Worker(command).start();
  }
  
  /**
   * @brief Get the selected VNC Options config or null if not selected
   * @param jsonDevice JSONObject instance.
   * @return
   */
  public static VNCOptionsConfig GetSelectedVNCOptionsConfig(JSONObject jsonDevice) {
	  VNCOptionsConfig vncOptionsConfig=null;
	  String topLevelConfigPath = SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME);
	  
	  if( new File(topLevelConfigPath).isDirectory() ) {
		  vncOptionsConfig = new VNCOptionsConfig( topLevelConfigPath, Constants.APP_NAME );
		  try {
			  vncOptionsConfig.loadHS(JSONProcessor.GetLocation(jsonDevice), JSONProcessor.GetUnitName(jsonDevice) ); 
		  }
		  catch(Exception e) {}		  
	  }
	  return vncOptionsConfig;
  }
  
  /**
   * @brief Get the name of the filename to hold the command line config for this device and service.
   * @param jsonDevice An instance of JSONObject.
   * @param serviceName The name of the service.
   * @return The filename string.
   */
  public static String GetServiceConfigFilename(JSONObject jsonDevice, String serviceName) {
	  return Constants.APP_NAME.toLowerCase()+"_"+serviceName+"_"+JSONProcessor.GetLocation(jsonDevice)+"_"+JSONProcessor.GetIPAddress(jsonDevice);
  }
  
  /**
   * @brief Called when the user select a row in the DeviceTablePanel
   * @param jsonDevice The device object selected in the table by the user
   * @param serviceName The name of the service.
   */
  public void setSelectedDevice(JSONObject jsonDevice, ServiceCmd serviceCmd) {
    if( isLocalDevice(jsonDevice) ) {
    	handleLocalDevice(jsonDevice, serviceCmd);
    }
    else {
    	handleRemoteDevice(jsonDevice, serviceCmd);
    }
  }

  /**
   * @brief Get the location of the tab with the title = location
   * @param location The location String
   * @return The index of the tab or -1 of not found.
   */
  public int getLocationTabIndex(String location) {
	  int indexFound = -1;
	  
	  for( int tabIndex=0; tabIndex<tabbedPane.getTabCount() ; tabIndex++ ) {
		  
		  if( tabbedPane.getTitleAt(tabIndex).equals(location) ) {
			  indexFound = tabIndex;
			  break;
		  }
		  
	  }
	  
	  return indexFound;
  }
  
  /**
   * @brief Determine if the location tab is present in the tab pane.
   * @param location The Location text.
   */
  private boolean isLocationTabPresent(String location) {
	  boolean tabFound = true;
	  int tabIndex = getLocationTabIndex(location);
	  
	  if( tabIndex == -1 ) {
		  tabFound = false;
	  }
	  
	  return tabFound;
  }
  
  /**
   * @brief Called when device messages are received from the ICONS. A Device object is constructed 
   *        from these messages and set here.
   * @param jsonDevice The JSONObject instance.
   */
  public void setJSONDevice(JSONObject jsonDevice) {
	  DeviceTablePanel deviceTablePanel = (DeviceTablePanel)null;
	  String location = JSONProcessor.GetLocation(jsonDevice);
	
	  //If the device is on the LAN
	  if( JSONProcessor.LocationMatch(Constants.LOCAL_LOCATION, jsonDevice) ) {
		  localDeviceTablePanel.setJSONDevice(jsonDevice, GroupName);
	  }
	  //If the device is remote (from an ICON server)
	  else {
		  	if( location != null && !isLocationTabPresent( location ) ) {
		      //If the group name in the JSON message matches that in the received JSON message
		  	  if( JSONProcessor.GroupNameMatch(GroupName, jsonDevice) ) {
				  deviceTablePanel = new DeviceTablePanel( location );
				  tabbedPane.addTab(location, null, deviceTablePanel, "All the devices found at "+location+". Double click to move to the left.");
				  deviceTablePanel.addDeviceTableSelectionListener(this);
		  	  }
			}
			else {
				int tabCount = tabbedPane.getTabCount();
				int locationIndex = getLocationTabIndex(location);
				if( locationIndex >= 0 && locationIndex < tabCount ) {
					deviceTablePanel = (DeviceTablePanel)tabbedPane.getComponent( locationIndex );
				}
			}
		  	if( deviceTablePanel != null ) {
		  		deviceTablePanel.setJSONDevice(jsonDevice, GroupName);
		  	}
	  }
	  
	  //Display the debug messages if required.
	  for( DeviceMsgDebug deviceMsgDebug : deviceMsgDebugList ) {
		String jsonStr = jsonDevice.toString(4);
		String jsonStrLower = jsonStr.toLowerCase();
		if( jsonStrLower.indexOf(deviceMsgDebug.devName) > -1 ) {
			try {
				deviceMsgDebug.doc.insertString(deviceMsgDebug.doc.getLength(), jsonStr+"\n", deviceMsgDebug.attributeSet );
				deviceMsgDebug.jTextPane.setCaretPosition(deviceMsgDebug.doc.getLength());
			}
			catch(BadLocationException e) {
				e.printStackTrace();
			}						
		}
	  }
		
  }

  /**
   * @brief Get the configured warning timeout period in milliseconds.
   * @return The device timeout period in millisseconds.
   */
  public static float GetDeviceTimeoutWarningMilliSeconds() {
	  return DeviceTimeoutWarningSeconds*1000.0F;
  }

  /**
   * @brief Get the configured lost timeout period in milliseconds.
   * @return The device timeout period in milliseconds.
   */
  public static float GetDeviceTimeoutLostMilliSeconds() {
	  return DeviceTimeoutLostSeconds*1000.0F;
  }

  /**
   * @brief called when the connection to the ICONS server drops.
   */
  public void iconsShutdown() {
	  remoteLocalPortHashtable.clear();
  }
  
  /**
   * @brief Get the configured Group name
   * @return The configured group name.
   * @return
   */
  public static String GetGroupName() {
	  return GroupName;
  }
}
