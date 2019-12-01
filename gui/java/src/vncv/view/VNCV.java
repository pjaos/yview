package vncv.view;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.Timer;

import pja.gui.*;
import pja.io.*;
import mtightvnc.OptionsPanel;
import mtightvnc.VNCFrame;
import mtightvnc.VncViewer;
import mtightvnc.VncViewerListener;
import vncv.model.*;
import yview.view.MainFrame;
import pja.cmdline.CmdLineHelper;

/**
 * A GUI to allow VNC connections to be saved and double clicked to connect.
 */
public class VNCV extends GenericFrame implements WindowListener, MouseListener, ActionListener, VncViewerListener
{
  public static final String PROGRAM_NAME="VNCV";
  public static final String HOSTNAME="HOSTNAME";
  public static final String PORT="PORT";  
  public static final String DESKTOP_NAME="DESKTOP_NAME";  
  public static final String[] TABLE_COLUMN_NAMES = { HOSTNAME, PORT, DESKTOP_NAME };
  public static final int TABLE_COL_0=0;
  public static final int TABLE_COL_1=1;
  public static final int TABLE_COL_2=2;
  public static final int HOSTNAME_COL=0;
  public static final int SERVICE_NAME_COL=1;
  public static int PERIODIC_TIMER_UPDATE_MS = 10000;
  
  DataSource tableDataSource;
  ReadOnlyTable connectionTable;  
  JScrollPane scrollpane;  
  StatusBar statusBar;
  ConfigDialog configDialog;
  JMenuItem configMenuItem;
  JPanel buttonPanel = new JPanel();
  JButton addButton = new JButton("Add");
  JButton delButton = new JButton("Delete");
  JButton editButton = new JButton("Edit");
  GenericOKCancelDialog genericOKCancelDialog = new GenericOKCancelDialog(this, "Connection Configuration", true);
  OptionsPanel vncOptionsPanel = new OptionsPanel();
  VNCOptionsConfig vncOptionsConfig;
  Vector<VNCOptionsConfig> vncOptionsConfigList;
  Timer memUsageTimer;
  boolean   showMemoryUsage;
  Config config;
  
  /**
   * @brief VNCV Constructor
   * @param config The GUI configuration instance.
   * @param configFilename The filename to store the configuration.
   */
  public VNCV(Config config, String configFilename) {
	super(VNCV.PROGRAM_NAME);
    this.setTitle(PROGRAM_NAME+" (v"+yview.model.Constants.VERSION+")");
    this.config=config;

    vncOptionsConfig = new VNCOptionsConfig( getConfigPath().getAbsolutePath() );
    this.addWindowListener(this);
    statusBar = new StatusBar();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.add(statusBar, BorderLayout.SOUTH);
    
    configDialog = new ConfigDialog((JFrame)this, "Look And Feel", true, config, configFilename);
    
    JMenuBar menubar = new JMenuBar();
    
    JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);
     
    ImageIcon exitIcon = new ImageIcon(getClass().getResource("/vncv/images/door_out.png"));
    JMenuItem eMenuItem = new JMenuItem("Exit", exitIcon);
    eMenuItem.setMnemonic(KeyEvent.VK_E);
    eMenuItem.setToolTipText("Exit application");
    eMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
          System.exit(0);
      }
    });
    
    ImageIcon cogIcon = new ImageIcon(getClass().getResource("/vncv/images/cog.png"));
    configMenuItem = new JMenuItem("Look And Feel", cogIcon);
    configMenuItem.setMnemonic(KeyEvent.VK_C);
    configMenuItem.setToolTipText("Configure application");
    configMenuItem.addActionListener(this);
    
    file.add(configMenuItem);
    file.add(eMenuItem);

    menubar.add(file);

    setJMenuBar(menubar);

    buttonPanel.add(addButton);
    addButton.addActionListener(this);
    buttonPanel.add(delButton);
    delButton.addActionListener(this);
    buttonPanel.add(editButton);
    editButton.addActionListener(this);
    
    getContentPane().add(buttonPanel, BorderLayout.CENTER);
    
    vncOptionsPanel.showHostPort(true);
    genericOKCancelDialog.add(vncOptionsPanel);
    updateConnectionView();
    
    memUsageTimer = new Timer(VNCV.PERIODIC_TIMER_UPDATE_MS, this);
    memUsageTimer.start();
  }
  
  /**
   * @return a Vector of VNCOptionsConfig objects from disk files.
   */
  private Vector<VNCOptionsConfig> getVNCOptionsConfigList() {
    Vector<VNCOptionsConfig> vncOptionsConfigList = new Vector<VNCOptionsConfig>();
    
    File configPath = getConfigPath();
    File fileList[] = configPath.listFiles();
    for( File f : fileList) {
      try {
        if( f.getName().startsWith( VNCOptionsConfig.class.getName() )) {
          vncOptionsConfig.loadEncrypted(f.getAbsolutePath());
          vncOptionsConfigList.add((VNCOptionsConfig)vncOptionsConfig.clone());
        }
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    
    return vncOptionsConfigList;
  }

  /**
   * Load the persistent connection data into the table for the user to select.
   */
  public void loadConnectionList() {
   
    if( scrollpane != null ) {
      getContentPane().remove(scrollpane);
    }
    
    vncOptionsConfigList = getVNCOptionsConfigList();
    tableDataSource = new DataSource(vncOptionsConfigList.size(), TABLE_COLUMN_NAMES.length);
    connectionTable  = new ReadOnlyTable(TABLE_COLUMN_NAMES, tableDataSource);
    int row=0;
    for( VNCOptionsConfig vncOptionsConfig : vncOptionsConfigList ) {
      tableDataSource.setValueAt(row, TABLE_COL_0, vncOptionsConfig.serverAddress);
      tableDataSource.setValueAt(row, TABLE_COL_1, ""+vncOptionsConfig.serverPort);
      tableDataSource.setValueAt(row, TABLE_COL_2, vncOptionsConfig.desktopName);      
      row++;
    }
    
    connectionTable.addMouseListener(this);
    
    scrollpane = new JScrollPane(connectionTable);
    
    //Adjust the column size now its in a scrollpane
    connectionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnAdjuster tca = new TableColumnAdjuster(connectionTable);
    tca.adjustColumns();
    
    getContentPane().add(scrollpane, BorderLayout.NORTH);
    pack();
  }
  
  /**
   * Update the list of connections
   */
  private void updateConnectionView() { 
    loadConnectionList();
    this.validate();
  }
  
  /**
   * Handle all action events for this frame.
   */
  public void actionPerformed(ActionEvent e) {
    if( e.getSource() == addButton ) {
      //Show add connection dialog box
      showVNCConnectionConfigDialog(false);
      updateConnectionView();
    }
    else if( e.getSource() == editButton ) {
      //Show add connection dialog box
      showVNCConnectionConfigDialog(true);
      updateConnectionView();
    }
    else if( e.getSource() == delButton ) {
      delSelectedConnection();
    }
    else if( e.getSource() == configMenuItem ) {
      UI.CenterInParent(this, configDialog);
      configDialog.setVisible(true);
    }
    else if( e.getSource() == memUsageTimer ) {
      if( showMemoryUsage ) {
        statusBar.println(VNCV.GetMemoryUsageReport());
      }
    }
  }
  
  /**
   * Delete the currently selected connection
   */
  private void delSelectedConnection() {
    //If the user has selected a table line
    if( loadSelectedVncOptionsConfig() ) {
      if( Dialogs.showYesNoDialog(this, "Delete connection", "Are you sure you wish to delete the selected connection.") == JOptionPane.YES_OPTION ) {
        vncOptionsConfig.deleteSPConfigFile();
        updateConnectionView();
      }      
    }
  }
  
  public void windowActivated(WindowEvent arg0) {}

  public void windowClosed(WindowEvent arg0) {}

  /**
   * Save the window size and position when closing window.
   */
  public void windowClosing(WindowEvent arg0) {
    try {
      config.guiWidth  = this.getWidth();
      config.guiHeight = this.getHeight();
      config.guiX      = this.getX();
      config.guiY      = this.getY();
      configDialog.saveConfig();
    }
    catch(Exception e) {
      //e.printStackTrace();
    }
  }
  

  public void windowDeactivated(WindowEvent arg0) {}

  public void windowDeiconified(WindowEvent arg0) {}

  public void windowIconified(WindowEvent arg0) {}

  public void windowOpened(WindowEvent arg0) {}


  public void mouseClicked(MouseEvent arg0) {}

  public void mouseEntered(MouseEvent arg0) {}

  public void mouseExited(MouseEvent arg0) {}

  /**
   * @brief When a VNC host is doublke clicked create a VNC window and connect to the host.
   */
  public void mousePressed(MouseEvent arg0) {
    if( SwingUtilities.isLeftMouseButton(arg0) && arg0.getClickCount() == 2 ) {    
      //Attempt to load the configuration for the selected connection.
      loadSelectedVncOptionsConfig();
      spawnVNCSession(vncOptionsConfig.getOptionsArray());
    }
  }
  
  /**
   * @brief Provide an easy way of accessing the configuration of a host
   *        by right clicking on the VNC host.
   */
  public void mouseReleased(MouseEvent arg0) {
    if( SwingUtilities.isRightMouseButton(arg0) ) {
      showVNCConnectionConfigDialog(true);
    }
  }
  
  /**
   * Load the vncOptionsConfig attribute with the selected configuration.
   * Return true if vncOptionsConfig was successfully loaded.
   * @return
   */
  private boolean loadSelectedVncOptionsConfig() {
    boolean loaded=false;
    int selectedIndex = connectionTable.getSelectedRow();
    if( selectedIndex >= 0 ) {
      String host = connectionTable.getValueAt(selectedIndex, 0).toString();
      String port = connectionTable.getValueAt(selectedIndex, 1).toString();
      try {
        vncOptionsConfig.loadSP(host, port);
        loaded=true;
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
    return loaded;
  }
  
  /**
   * @brief Display the VNC config dialog.
   * @param edit If true all editing of config parameters.
   */
  public void showVNCConnectionConfigDialog(boolean edit) {
    //Attempt to load the vncOptionsConfig for the selected connection
    loadSelectedVncOptionsConfig();
    vncOptionsPanel.setArgs( vncOptionsConfig.getOptionsArray() );

    UI.CenterInParent(this, genericOKCancelDialog);
    genericOKCancelDialog.setVisible(true);
    if( genericOKCancelDialog.isOkSelected() ) {
      try {
        //If editing delete the old config file.
        if( edit ) {
          vncOptionsConfig.deleteSPConfigFile();
        }
        //Load the config object from the attributes the user selected
        vncOptionsConfig.setOptionsArray( vncOptionsPanel.getArgs() );

        if( !edit && alreadyInList(vncOptionsConfig) ) {
          Dialogs.showErrorDialog(this, "Duplicate connection", "The server address and server port are already used.");
          return;
        }
     
        vncOptionsConfig.setOptionsArray( vncOptionsPanel.getArgs() );
        vncOptionsConfig.saveSP();
        statusBar.println("Saved connection configuration to "+vncOptionsConfig.getSPConfigFile());
        updateConnectionView();
      }
      catch(Exception e) {
        Dialogs.showErrorDialog(this, "Save Configuration Error", e.getLocalizedMessage() );
      }
    }
  }
  
  /**
   * @brief Determine if a configuration is already present.
   * @param vncOptionsConfig
   * @return true if already in the list of connections.
   */
  public boolean alreadyInList(VNCOptionsConfig vncOptionsConfig) {
    boolean alreadyInList=false;
    for(VNCOptionsConfig currentVNCOptionsConfig : vncOptionsConfigList) {
      if( currentVNCOptionsConfig.serverAddress.equals(vncOptionsConfig.serverAddress) && 
          currentVNCOptionsConfig.serverPort == vncOptionsConfig.serverPort ) {
        alreadyInList=true;
      }
    }
    return alreadyInList;
  }
  
  /***
   * @brief  Get the configuration path.
   * @return The path where config files are saved.
   */
  public File getConfigPath() {
    File configPath = new File(SimpleConfig.GetTopLevelConfigPath(VNCV.PROGRAM_NAME), "config"+System.getProperty("file.separator")+VNCV.PROGRAM_NAME.toLowerCase());
    if( !configPath.isDirectory() ) {
      if( !configPath.mkdirs() ) {
        Dialogs.showErrorDialog(this, "Error", "Failed to create the "+configPath+" folder.");
      }
    }
    return configPath;
  }
  
  /**
   * @brief Start a VNC terminal session.
   * @param args The command line arguments used to spawn a VNC session.
   */
  private void spawnVNCSession(String args[]) {
    String host = CmdLineHelper.ReadParameter(args, OptionsPanel.HOST, false);
    String port = CmdLineHelper.ReadParameter(args, OptionsPanel.PORT, false);
    statusBar.println("CONNECT TO "+host+":"+port);

    /**
     * Responsible for starting an vnc session
     */
    class Worker extends Thread {
      String args[];
      VncViewerListener vncViewerListener;
      Worker(String args[], VncViewerListener vncViewerListener) {
        this.args=args;
        this.vncViewerListener=vncViewerListener;
      }
      public void run() {      
        VNCFrame.Launch(args, statusBar, vncViewerListener, VNCV.PROGRAM_NAME);
      }
    }
    Worker worker = new Worker(args, this);
    worker.start();
  } 
  
  /**
   * @brief Set the desktop name in the table.
   * @param row The row to set the desktop name for.
   * @param desktomeName The name of the VNC desktop.
   */
  private void setTableDesktopName(int row, String desktopName) {
    tableDataSource.setValueAt(row, TABLE_COL_2, desktopName);
  }
  
  /**
   * @brief Callback when connected to VNC host, update the desktop name in the table and it's associated persistent configuration object.
   * @param host The VNC server address.
   * @param port The VNC server port number.
   * @param desktomeName The name of the VNC desktop.
   */
  public void connected(String host, int port, String desktopName) {
    try {
      vncOptionsConfig.loadSP(host, ""+port);
      vncOptionsConfig.desktopName = desktopName;
      int row=0;
      while( row < connectionTable.getRowCount() ) {
        if( connectionTable.getValueAt(row, TABLE_COL_0).toString().equals(host) &&
            connectionTable.getValueAt(row, TABLE_COL_1).toString().equals(""+port) ) {
          setTableDesktopName(row, desktopName);
        }
        row++;
      }
      vncOptionsConfig.saveSP();
    }
    catch(Exception e) {
      //e.printStackTrace();
    }
  }
  
  //Do nothing if this listener method is called
  public void disconnected() {}

  /**
   * @brief Called when desktop is resized to save it size and position.
   * @param host The VNC server address.
   * @param port The VNC server port number.
   * @param width The width of the desktop.
   * @param height The height of the desktop.
   * @param x The x postions to the top left corner of the desktop.
   * @param y The y postions to the top left corner of the desktop. 
   */
  public void deskTopResized(String host, int port, int width, int height, int x, int y) {
    try {
      vncOptionsConfig.loadSP(host, ""+port);
      vncOptionsConfig.windowWidth=width;
      vncOptionsConfig.windowHeight=height;
      vncOptionsConfig.windowPosX=x;
      vncOptionsConfig.windowPosY=y;      
      vncOptionsConfig.saveSP();
    }
    catch(Exception e) {
      //e.printStackTrace();
    }
  }

  /**
   * @brief Enable/Disable periodically displaying the memory memory usage 
   * @param showMemoryUsage If true then the memory usage will be reported in the status bar every PERIODIC_TIMER_UPDATE_MS.
   */
  public void enableShowMemoryUsage(boolean showMemoryUsage) {
    this.showMemoryUsage=showMemoryUsage;
  }
  
  /**
   * @brief Get the state of the showMemoryUsage attribute
   * @return If true then the memory usage will be reported in the status bar every PERIODIC_TIMER_UPDATE_MS.
   */
  public boolean enableShowMemoryUsage() {
    return showMemoryUsage;
  }
  
  /**
   * @brief Get a string representation of the current memory usage in the JVM
   * @return The string containing the memory usage information.
   */
  public static String GetMemoryUsageReport() {
    
    double totalMemory = Runtime.getRuntime().totalMemory()/1E6;
    double maxMemory = Runtime.getRuntime().maxMemory()/1E6;
    double freeMemory = Runtime.getRuntime().freeMemory()/1E6;
    
    return "Memory stats (MB): TOTAL: "+String.format("%2.2f", totalMemory)+", MAX: "+String.format("%2.2f", maxMemory)+" FREE: "+String.format("%2.2f", freeMemory);    
  }
  
}
