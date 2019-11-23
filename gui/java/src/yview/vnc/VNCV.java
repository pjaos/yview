/*****************************************************************************************
 *                        Copyright 2013 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package yview.vnc;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.Timer;

import pja.sshpfg.view.ConfigDialog;
import yview.view.MainFrame;
import pja.io.SimpleConfig;
import pja.gui.*;
import pja.sshpfg.model.Config;
import pja.cmdline.CmdLineHelper;
import sshpfg.GUI;

import mtightvnc.OptionsPanel;
import mtightvnc.VNCFrame;
import mtightvnc.VncViewer;
import mtightvnc.VncViewerListener;
import mtightvnc.visitpc.VNCOptionsConfig;

/**
 * A GUI to allow VNC connections to be saved and double clicked to connect.
 *
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
  
  public VNCV(Config config, String configFilename) {
	super(PROGRAM_NAME);
    this.setTitle(PROGRAM_NAME+" (v"+GUI.VERSION+")");
    this.config=config;

    vncOptionsConfig = new VNCOptionsConfig( getConfigPath().getAbsolutePath(), PROGRAM_NAME );
    this.addWindowListener(this);
    statusBar = new StatusBar();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.add(statusBar, BorderLayout.SOUTH);
    
    configDialog = new ConfigDialog((JFrame)this, "Look And Feel", true, config, configFilename);
    
    JMenuBar menubar = new JMenuBar();
    
    JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);
     
    ImageIcon exitIcon = new ImageIcon(getClass().getResource("/sshpfg/images/door_out.png"));
    JMenuItem eMenuItem = new JMenuItem("Exit", exitIcon);
    eMenuItem.setMnemonic(KeyEvent.VK_E);
    eMenuItem.setToolTipText("Exit application");
    eMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
          System.exit(0);
      }
    });
    
    ImageIcon cogIcon = new ImageIcon(getClass().getResource("/sshpfg/images/cog.png"));
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
    
    memUsageTimer = new Timer(GUI.PERIODIC_TIMER_UPDATE_MS, this);
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
        statusBar.println(GUI.GetMemoryUsageReport());
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
   * Save the window size and position
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

  public void mousePressed(MouseEvent arg0) {
    if( SwingUtilities.isLeftMouseButton(arg0) && arg0.getClickCount() == 2 ) {    
      //Attempt to load the configuration for the selected connection.
      loadSelectedVncOptionsConfig();
      spawnVNCSession(vncOptionsConfig.getOptionsArray());
    }
  }

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
   * Display the VNC config dialog
   * 
   * @param connectionAtributes
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
   * Return true if already in the list of connections.
   * @param vncOptionsConfig
   * @return
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
   * Return the path where config files are saved.
   * @return
   */
  public File getConfigPath() {
    File configPath = new File( SimpleConfig.GetTopLevelConfigPath(VNCV.PROGRAM_NAME), "config"+System.getProperty("file.separator")+VNCV.PROGRAM_NAME.toLowerCase());
    if( !configPath.isDirectory() ) {
      if( !configPath.mkdirs() ) {
        Dialogs.showErrorDialog(this, "Error", "Failed to create the "+configPath+" folder.");
      }
    }
    return configPath;
  }
  
  /**
   * Start a VNC terminal session.
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
        VNCFrame.Launch(args, statusBar, vncViewerListener, PROGRAM_NAME);
      }
    }
    Worker worker = new Worker(args, this);
    worker.start();
  } 
  
  /**
   * Set the desktop name in the table.
   * @param row The row to set the desktop name for.
   */
  private void setTableDesktopName(int row, String desktopName) {
    tableDataSource.setValueAt(row, TABLE_COL_2, desktopName);
  }
  /**
   * Callback when connected to VNC host, update the desktop name in the table and it's associated persistent configuration object
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

  //Listener method called if desktop is resized.
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
  

}
