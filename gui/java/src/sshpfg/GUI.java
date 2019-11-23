/*****************************************************************************************
 *                        Copyright 2013 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package sshpfg;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.Cursor;
import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.Timer;

import java.util.concurrent.TimeUnit;

import mtightvnc.OptionsPanel;
import mtightvnc.VNCFrame;
import mtightvnc.VncViewerListener;
import mtightvnc.visitpc.VNCOptionsConfig;
import pja.gui.*;
import pja.io.*;
import sshpfg.*;
import pja.sshpfg.model.Config;
import pja.sshpfg.view.ConfigDialog;
import pja.sshpfg.SSHPFGException;
import pja.sshpfg.view.CmdLinePanel;
import yview.model.Constants;
import yview.view.MainFrame;

/**
 * Changelog
 * 3.2
 * - Use the same version as the yview application.
 * 
 * 2.1
 * - When over a desktop and the mouse wheel is used send up down arrow commands as the 
 *   mouse wheel is moved. This sends keys to the remote desktop.
 *   If the mouse is over the vertical scroll bar when the mouse wheel event occurs 
 *   then the local view of the remote desktop moves over the remote desktop
 * 
 * 2.0
 * - Fix a bug which used up CPU while running.
 *   viewer.buttonPanel.getButtonPanelConfig().autoHideButtonPanelPeriodMS=0;
 *   in VncCanvas changed to 
 *   viewer.buttonPanel.getButtonPanelConfig().autoHideButtonPanelPeriodMS=100;
 * - Fix bug that broke the ability to set the VNCV look and feel (dialog failed to display).
 * - Use the same program version for the VNCV and SSHPFG SW.
 *  
 * 1.90
 * - Be a little cleverer when providing default command lines to launch ssh terminals and web browsers
 *   from the sshpfg connection GUI. On windows a default putty command line is provided for ssh 
 *   sessions. The user must enter the path to the putty.exe file. For other OS's we check for 
 *   the /usr/bin/terminator and /usr/bin/gnome-terminal programs. If neither of these are found then
 *   the user must enter the ssh terminal program that they wish to use.
 *   For http connections the user must enter the abs path of the web browser program to launch. 
 * - Bug when launching a terminal or web browser and no saved command existed we would not use
 *   the default command. Now we do, although if the terminal or web browser programs (terminator, 
 *   gnome-terminal, putty, firefox or google chrome) do not exist then the program will fail to 
 *   start. In this case the user must right click the connection and enter the command line manually.
 * 
 * 1.89
 * - When stretch across all screens option was selected the desktop window would not 
 *   always open on the left most screen so that the maximum remote desktop area was 
 *   visible. This has been fixed.
 *   
 * 1.88
 * - When refreshing the displayed destination list, change to a waiting cursor until
 *   data is available to update the table.
 * - Disable the automatic memory usage reports in vncv and sshpfg.
 * - Add command line option to enable memory usage report in the status bar for vncv
 *   and sshpfg. 
 * - Add command line option usage test to vncv and sshpfg.
 *   
 * 1.87
 * - For sshpfg, If user right clicks on an unselected row the connection config
 *   now selects the row and then opens the config. Previously a user would right
 *   click on a row and the config would be brought up for a previously selected 
 *   row which mean the user had to lect click to select a row and then right click
 *   to open the config dialog for that row. 
 * 
 * 1.86
 * - Show the java memory usage every 5 seconds in the status bar
 * 
 * 1.85
 * - Add ability to execute the sshpfg on windows platform as well as Linux. 
 * 
 * 1.84
 * - Add -d (debug) command line option. If this is used when external commands are launched
 *   debugging is printed on stdout.
 * - Added a better command line argument parser to the ShellCmd class.
 * 
 * 1.83
 * - For non vnc services the cmd line to execute when a table row is double clicked can be entered 
 *   directly if the user selects a row and right clicks on it. On this cmd line $h=hostname and $p 
 *   is the TCP port. If no cmd line text is found then for ssh and http services default cmd lines 
 *   are provided when the cmd line dialog is opened.
 * 
 * 1.82
 * sshpfg would not start ssh sessions as the wrong host was used. Should have been localhost. Fixed.
 * 
 * 1.81
 * VNCV load config was not working. Fixed.
 * 
 * 1.80
 * Change default encoding in dialog to ZRLE as its the fastest in testing.
 * 
 * 1.79
 * VNC config file will now be saved encrypted although the key is obvious in the code, just obfuscate slightly.
 * 
 * 1.78
 * - Store VNC config option in a VNC config option class file. This fixes problems where previously config would get lost 
 * if the sshpf src client (local) TCP IP ports changed. We now save the config files with the hostname and the service 
 * name in the filename.
 * - The desktop name is updated and if no resize strategy is selected the previous VNC desktop size and location is used.
 * 
 * 1.77
 * Add restart local clients menu option.
 * 
 * 1.76
 * When user selects refresh, remove the table as an indication to the user that a new connection list is being read.
 * 
 * 1.75
 * Added connected for field to the destination list.
 * 
 * 1.74
 * Use a refresh icon for the refresh menu option.
 * 
 * 1.73
 * Add a refresh menu option to refresh the list of available dest clients.
 * 
 * 1.72
 * Show the contents of the sshpf connections file in the status window so the the user can access all the connections 
 * details if they wish.
 * 
 * 1.71
 * Fix vncv bug that caused GUI not to start if no config present.
 * 
 * 1.7
 * VNCV added (vncv.Main to run application). This is a VNC GUI that allows the user to connect to VNC servers directly.
 * 
 * 1.6
 * - Allow user to configure the time period that the vnc control panel (along top edge of VNC window is hidden when the 
 *   mouse moves back over the desktop window. The use can now configure this time period to be 0 so that when the mouse 
 *   moves over the desktop the control panel is immediately hidden.
 * 
 * 1.5
 * As the button panel stays in view if the mouse is outside the desktop we no longer use the auto hide period for the button panel.
 *
 * 1.4
 * - Automatically copy the remote copy buffer to the local one when the remote copy 
 *   buffer is changed.
 * 
 * 1.3
 * - Added Fit To Desktop a frame resize strategy.
 * - If the vnc button panel is set to auto hide and the user moves the mouse 
 *   away from the vnc window, keep the panel visible while the mouse stays 
 *   away from the vnc viewer window. 
 * 
 * v1.2
 * - Implement a VNC viewer in Java
 * - Allow each connection to be configured by right clicking selected connection.
 * 
 * v1.1
 * Add -X when launching ssh sessions.
 * 
 * v1.0
 * Initial release.
 *
 */
public class GUI extends GenericFrame implements WindowListener, MouseListener, ActionListener, VncViewerListener
{
  public static int       PERIODIC_TIMER_UPDATE_MS = 60000;
  public static final int HOSTNAME_COL=0;
  public static final int SERVICE_NAME_COL=1;
  public static final int SERVICE_PORT_COL=2;
  public static final int LOCAL_PORT_COL=3;
  public static final int SSH_SERVER_PORT_COL=4;
  public static final int CONNECTED_FOR_COL=5;
  
  public static final String HOSTNAME="HOSTNAME";
  public static final String SERVICE_NAME="SERVICE NAME";
  public static final String SERVICE_PORT="SERVICE PORT";
  
  public static final String LOCAL_PORT="LOCAL PORT";
  public static final String SSH_SERVER_PORT="SSH SERVER PORT";
  public static final String CONNECTED_FOR="CONNECTED FOR";
  
  public static final String LOCALHOST="localhost";
  
  public static final String[] CONNECTION_ATTRIBUTES = {HOSTNAME,
                                                        SERVICE_NAME,
                                                        SERVICE_PORT,
                                                        LOCAL_PORT,
                                                        SSH_SERVER_PORT,
                                                        CONNECTED_FOR};
  public static final String STATUS_ERROR_TEXT = "ERROR: ";
  public static final String SSHPF_CLIENT_LIST_FILE="sshpf_client_list.txt";
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");
  public static final String[] TABLE_COLUMN_NAMES = { HOSTNAME, SERVICE_NAME, CONNECTED_FOR };
  public static final int TABLE_COL_0=0;
  public static final int TABLE_COL_1=1;
  public static final int TABLE_COL_2=2;
  
  public static final double VERSION=Constants.VERSION;
  public static final String PROGRAM_NAME="SSHPFG";

  public static final String SERVER_PORT_CONNECTION_ATTRIBUTE="SERVER PORT";
  public static final String HOSTNAME_CONNECTION_ATTRIBUTE="HOSTNAME";
  public static final String COMPUTER_CONNECTION_ATTRIBUTE="COMPUTER";
  public static final String LOCAL_PORT_CONNECTION_ATTRIBUTE="LOCAL PORT";
  public static final String SERVICE_PORT_CONNECTION_ATTRIBUTE="SERVICE PORT";
  public static final String SERVICE_NAME_CONNECTION_ATTRIBUTE="SERVICE NAME";
  public static final String CONNECTED_FOR_CONNECTION_ATTRIBUTE="CONNECTED FOR";
  public static final String LINUX_SSHPF_CMD="/usr/local/bin/sshpf";
  public static final String WINDOWS_SSHPF_CMD_A="C:\\Program Files\\sshpf\\sshpf.exe";
  public static final String WINDOWS_SSHPF_CMD_B="C:\\Program Files (x86)\\sshpf\\sshpf.exe";

  StatusBar statusBar;
  JScrollPane scrollpane;
  List connectionAttributeList;
  List <Hashtable>connectionList; 
  File outputFile;
  DataSource tableDataSource;
  ReadOnlyTable connectionTable;
  ConfigDialog configDialog;
  JMenuItem configMenuItem, refreshMenuItem, restartMenuItem;
  GenericOKCancelDialog genericOKCancelDialog = new GenericOKCancelDialog(this, "Connection Configuration", true);
  OptionsPanel vncOptionsPanel = new OptionsPanel();
  CmdLinePanel cmdLinePanel = new CmdLinePanel();
  Timer periodicTimer;
  boolean   debug;
  boolean   showMemoryUsage;
  Config config;
  
  public GUI(Config config, String configFilename) {
	super(PROGRAM_NAME);
    this.setTitle(PROGRAM_NAME+" (v"+VERSION+")");
    this.config=config;
    this.addWindowListener(this);
    statusBar = new StatusBar();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.add(statusBar, BorderLayout.SOUTH);
    
    configDialog = new ConfigDialog((JFrame)this, "Look And Feel", true, config, configFilename);

    JMenuBar menubar = new JMenuBar();
    
    JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);
     
    ImageIcon cogIcon = new ImageIcon(getClass().getResource("/sshpfg/images/cog.png"));
    configMenuItem = new JMenuItem("Look And Feel", cogIcon);
    configMenuItem.setMnemonic(KeyEvent.VK_C);
    configMenuItem.setToolTipText("Configure application");
    configMenuItem.addActionListener(this);
    
    ImageIcon refreshIcon = new ImageIcon(getClass().getResource("/sshpfg/images/arrow_refresh.png"));
    refreshMenuItem = new JMenuItem("Refresh", refreshIcon);
    refreshMenuItem.setMnemonic(KeyEvent.VK_R);
    refreshMenuItem.setToolTipText("Refresh the list of dest clients");
    refreshMenuItem.addActionListener(this);
    
    ImageIcon linkIcon = new ImageIcon(getClass().getResource("/sshpfg/images/link.png"));
    restartMenuItem = new JMenuItem("Restart Local Clients", linkIcon);
    restartMenuItem.setMnemonic(KeyEvent.VK_P);
    restartMenuItem.setToolTipText("Restart the src and dest clients");
    restartMenuItem.addActionListener(this);
    
    ImageIcon exitIcon = new ImageIcon(getClass().getResource("/sshpfg/images/door_out.png"));
    JMenuItem eMenuItem = new JMenuItem("Exit", exitIcon);
    eMenuItem.setMnemonic(KeyEvent.VK_E);
    eMenuItem.setToolTipText("Exit application");
    eMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
          System.exit(0);
      }
    });

    file.add(configMenuItem);
    file.add(refreshMenuItem);
    file.add(restartMenuItem);
    file.add(eMenuItem);

    menubar.add(file);

    setJMenuBar(menubar);
    
    periodicTimer = new Timer(PERIODIC_TIMER_UPDATE_MS, this);
    periodicTimer.start();
  }
  
  /**
   * Handle all action events for this frame.
   */
  public void actionPerformed(ActionEvent e) {
    UI.CenterInParent(this, configDialog);
    
    if( e.getSource() == configMenuItem ) {
      configDialog.setVisible(true);
    }
    
    if( e.getSource() == refreshMenuItem ) {
      startLoadConnectionListThread();
    }
    
    if( e.getSource() == restartMenuItem ) {
      try {
        if( System.getProperty("os.name").startsWith("Linux") )
        {
          runCmdThread( "sudo "+getSSHPFCmd()+" --startup_clients" );
        }
        else {
          runCmdThread( getSSHPFCmd()+" --startup_clients" );
        }
      }
      catch(SSHPFGException ex) {
       this.statusBar.error(ex.getLocalizedMessage());
      }
    }
    else if( e.getSource() == periodicTimer ) {
      startLoadConnectionListThread();
      if( showMemoryUsage ) {
        statusBar.println(GUI.GetMemoryUsageReport());
      }
    }

  }
  
  /**
   * Get a string representation of the current memory usage in the JVM
   * @return The string containing the memory usage information.
   */
  public static String GetMemoryUsageReport() {
    
    double totalMemory = Runtime.getRuntime().totalMemory()/1E6;
    double maxMemory = Runtime.getRuntime().maxMemory()/1E6;
    double freeMemory = Runtime.getRuntime().freeMemory()/1E6;
    
    return "Memory stats (MB): TOTAL: "+String.format("%2.2f", totalMemory)+", MAX: "+String.format("%2.2f", maxMemory)+" FREE: "+String.format("%2.2f", freeMemory);    
  }
  
  /**
   * Return the SSHPF command that runs on this platform.
   * @return The String detailing the absolute path of the sshpf command.
   */
  private String getSSHPFCmd() throws SSHPFGException {
    File f = new File(LINUX_SSHPF_CMD);
    statusBar.info("Checking for "+f);
    if( f.exists() ) {
      return f.getAbsolutePath();
    }
    
    f = new File(WINDOWS_SSHPF_CMD_A);
    statusBar.info("Checking for "+f);
    if( f.exists() ) {
      return f.getAbsolutePath();
    }
    
    f = new File(WINDOWS_SSHPF_CMD_B);
    statusBar.info("Checking for "+f);
    if( f.exists() ) {
      return f.getAbsolutePath();
    }
    
    throw new SSHPFGException("Unable to find the sshpf command on this system.");
  }
  
  
  /**
   * Get the selected hostname
   * @return The selected host name or null if no connection is selected.
   */
  private String getSelectedHostname() {
    String hostname=null;
    int selectedIndex = connectionTable.getSelectedRow();
    if( selectedIndex >= 0 ) {
      hostname = connectionTable.getValueAt(selectedIndex, TABLE_COL_0).toString();
    }
    return hostname;
  }
  
  /**
   * Get the selected service name
   * @return The selected service name or null if no connection is selected.
   */
  private String getSelectedServiceName() {
    String serviceName=null;
    int selectedIndex = connectionTable.getSelectedRow();
    if( selectedIndex >= 0 ) {
      serviceName = connectionTable.getValueAt(selectedIndex, TABLE_COL_1).toString();
    }
    return serviceName;
  }
  
  private void loadConnectionList() {
    String cmd;
    
    //Change to the waiting cursor to indicate we are reading the status of available connections
    Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
    setCursor(hourglassCursor);
    
    statusBar.println("Reading connection information, please wait...");
    
    connectionAttributeList = Collections.synchronizedList(new ArrayList());
    connectionList = Collections.synchronizedList(new ArrayList()); //Contains a list of Hashtables, each Hashtable containing all the attributes for one connection.
  
    String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
    ShellCmd sc = new ShellCmd(debug);
    try {
      outputFile = new File(tmpDir+FILE_SEPARATOR+GUI.SSHPF_CLIENT_LIST_FILE);
      cmd = getSSHPFCmd()+" -a "+outputFile.getAbsolutePath();
      statusBar.println(cmd);
      //This takes a few seconds to complete
      sc.runSysCmd(cmd);
      
      if( scrollpane != null ) {
        //Remove the pane as an indication to the user that a new connection list is being read.
        getContentPane().remove(scrollpane);
        //this.repaint();
      }
      
     processConnectionDetails(outputFile);
    }
    catch(Exception e) {
      //e.printStackTrace();
      statusBar.println(GUI.STATUS_ERROR_TEXT+e.getMessage());
    }
    //Return to normal cursor
    Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    setCursor(normalCursor);
  }
  
  /**
   * Load the current list of available dest clients
   */
  public void runCmdThread(String cmd) {
    /**
     * Responsible for starting an ssh session
     */
    class Worker extends Thread {
      String cmd;
      Worker(String cmd) {
        this.cmd=cmd;
      }
      public void run() {
        ShellCmd sc = new ShellCmd(debug);
        try {
          statusBar.println(cmd);
          sc.runSysCmd(cmd);
          statusBar.println(""+sc.getStdOut()+"\n"+sc.getStdErr());
        }
        catch(Exception e) {
          statusBar.println(e.getLocalizedMessage());
        }
      }
    }
    Worker worker = new Worker(cmd);
    worker.start();
  }
  
  /**
   * Load the current list of available dest clients
   */
  public void startLoadConnectionListThread() {
    statusBar.println("Load available dest clients.");
    /**
     * Responsible for starting an ssh session
     */
    class Worker extends Thread {
      Worker() {
      }
      public void run() {
        loadConnectionList();
      }
    }
    Worker worker = new Worker();
    worker.start();
  }
  
  /**
   * Process the connection details output file.
   */
  void processConnectionDetails(File connectionDetailsFile) throws IOException, SSHPFGException {        
    StringTokenizer strTok;
    
    if( connectionDetailsFile.isFile() ) {
      if( scrollpane != null ) {
        getContentPane().remove(scrollpane);
      }
      List clientList = Collections.synchronizedList(new ArrayList());
      statusBar.println("Reading sshpf connection info from "+outputFile.getAbsolutePath());
      String lines[] = FileIO.GetLines(connectionDetailsFile.getAbsolutePath());
      statusBar.println("Read sshpf connection info from "+outputFile.getAbsolutePath());
      for( String l : lines ) {
        statusBar.println(l);
        strTok = new StringTokenizer(l, ",");
        //Process the title line
        if( l.startsWith("HOSTNAME") ) {
          while( strTok.hasMoreTokens() ) {
            connectionAttributeList.add( strTok.nextToken() );
          }
          continue;
        }
        else {
          String token;
          int attributeIndex=0;
          Hashtable connectionAtributes = new Hashtable();
          while( strTok.hasMoreTokens() ) {
            token=strTok.nextToken();
            connectionAtributes.put(GUI.CONNECTION_ATTRIBUTES[attributeIndex], token);
            attributeIndex++;
          }
          validateConnectionAttributes(connectionAtributes, l);
          connectionList.add(connectionAtributes);
          clientList.add(connectionAtributes.get(GUI.CONNECTION_ATTRIBUTES[HOSTNAME_COL])+" ("+connectionAtributes.get(GUI.CONNECTION_ATTRIBUTES[SERVICE_NAME_COL])+")");
        }
      }
      tableDataSource = new DataSource(connectionList.size(), TABLE_COLUMN_NAMES.length);
      connectionTable  = new ReadOnlyTable(TABLE_COLUMN_NAMES, tableDataSource);
      int row=0;
      for( Hashtable connectionAtributes : connectionList) {;
        tableDataSource.setValueAt(row, TABLE_COL_0, connectionAtributes.get(GUI.CONNECTION_ATTRIBUTES[HOSTNAME_COL]).toString());
        tableDataSource.setValueAt(row, TABLE_COL_1, connectionAtributes.get(GUI.CONNECTION_ATTRIBUTES[SERVICE_NAME_COL]).toString());
        tableDataSource.setValueAt(row, TABLE_COL_2, getConnectedTime(connectionAtributes.get(GUI.CONNECTION_ATTRIBUTES[CONNECTED_FOR_COL]).toString()) );
        row++;
      }
      connectionTable.addMouseListener(this);
      scrollpane = new JScrollPane(connectionTable);
      
      //Adjust the column size now its in a scrollpane
      connectionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      TableColumnAdjuster tca = new TableColumnAdjuster(connectionTable);
      tca.adjustColumns();
      getContentPane().add(scrollpane, BorderLayout.CENTER);
      validate();
    }
  }
  
  private static String getConnectedTime(String connectedSecondsString) {
    String result="";
    try {
      int seconds=Integer.parseInt(connectedSecondsString);
      int day = (int) TimeUnit.SECONDS.toDays(seconds);
      long hours = TimeUnit.SECONDS.toHours(seconds) -
                   TimeUnit.DAYS.toHours(day);
      long minute = TimeUnit.SECONDS.toMinutes(seconds) - 
                    TimeUnit.DAYS.toMinutes(day) -
                    TimeUnit.HOURS.toMinutes(hours);
      long second = TimeUnit.SECONDS.toSeconds(seconds) -
                    TimeUnit.DAYS.toSeconds(day) -
                    TimeUnit.HOURS.toSeconds(hours) - 
                    TimeUnit.MINUTES.toSeconds(minute);
    
      if( day == 0 ) {
        result = hours+":"+minute+":"+second;
      }
      else if( day == 1 ) {
        result = day+" day "+hours+":"+minute+":"+second;
      }
      else if( day > 1 ) {
        result = day+" days "+hours+":"+minute+":"+second;
      }
    }
    catch(NumberFormatException e) {
      //e.printStackTrace();
    }
    
    return result;
}
  
  /**
   * Responsible for checking that all the required attributes are held in the Hashtable.
   * @param connectionAtributes The Hashtable containing the connection attributes.
   *
   */
  private void validateConnectionAttributes(Hashtable connectionAtributes, String line) throws SSHPFGException {
    if( !connectionAtributes.containsKey(GUI.HOSTNAME) ) {
      throw new SSHPFGException(GUI.HOSTNAME+" field not found on line ("+line+") of "+outputFile.getAbsolutePath());
    }
    if( !connectionAtributes.containsKey(GUI.SERVICE_NAME) ) {
      throw new SSHPFGException(GUI.SERVICE_NAME+" field not found on line ("+line+") of "+outputFile.getAbsolutePath());
    }
    if( !connectionAtributes.containsKey(GUI.SERVICE_PORT) ) {
      throw new SSHPFGException(GUI.SERVICE_PORT+" field not found on line ("+line+") of "+outputFile.getAbsolutePath());
    }
    if( !connectionAtributes.containsKey(GUI.LOCAL_PORT) ) {
      throw new SSHPFGException(GUI.LOCAL_PORT+" field not found on line ("+line+")of "+outputFile.getAbsolutePath());
    }
    if( !connectionAtributes.containsKey(GUI.SSH_SERVER_PORT) ) {
      throw new SSHPFGException(GUI.SSH_SERVER_PORT+" field not found on line ("+line+") of "+outputFile.getAbsolutePath());
    }
    if( !connectionAtributes.containsKey(GUI.CONNECTED_FOR) ) {
      throw new SSHPFGException(GUI.CONNECTED_FOR+" field not found on line ("+line+") of "+outputFile.getAbsolutePath());
    }
  }

  /** 
   * Get the command string to launch an application to use the connection
   * This will be loaded from the config file if it exists, else a default 
   * will be used. The default may not launch an application, not ideal.
   * @param serviceName The name of the service to start
   * @return The command line start
   */
  private String getCommandString(String serviceName, String port) {
    String cmdString = "";
    
    try {
      String argListA[] = GetStringArray( getCommandLineConfigFile(LOCALHOST, port) );
      cmdString = argListA[1];

    }
    catch(IOException e) {
      //Use the default config if we can't load one from a file.

      String osName=System.getProperty("os.name");
      
      //If this is an ssh service
      if( serviceName.toLowerCase().equals("ssh") ) {
        //On windows we assume putty is available. The user must edit the path to 
        //enter the location of the putty.exe. Not ideal but it's OK for now.
        if( osName.startsWith("Windows") )
        {
          cmdString = "<replace with path to putty>putty.exe -ssh -P $p $h";
        }
        //Make some attempt to find a terminal program to use. Not ideal but it's OK for now.
        else {
          String terminalProgram = "/usr/bin/terminator";
          
          if( new File(terminalProgram).isFile() ) {
            cmdString = terminalProgram + " -x ssh -X -p $p $h";
          }
          else {
            
            terminalProgram = "/usr/bin/gnome-terminal";
            
            if( new File(terminalProgram).isFile() ) {
              cmdString = terminalProgram + " -x ssh -X -p $p $h";
            }
            else {
              cmdString = "<replace with local terminal program> $h:$p";
            }
            
          }
        }
        statusBar.println("No saved command found. Using default ssh command.");
      }
      //If this is an http service
      if( serviceName.toLowerCase().equals("http") ) {
        
        String browserProgram = "/usr/bin/firefox";
        if( new File(browserProgram).isFile() ) {
          cmdString = browserProgram + " $h:$p"; 
        }
        else {
          browserProgram = "/usr/bin/google-chrome";
          if( new File(browserProgram).isFile() ) {
            cmdString = browserProgram + " --no_proxy $h:$p"; 
          }
          else {
            cmdString = "<replace with local web browser program> $h:$p";
            statusBar.println("No saved command found. Using default http command.");
          }
        }
      }
    }
    
    return cmdString;
  }
  
  /**
   * Execute the required command line.
   */
  private void spawnCommandLine(String hostname, String serviceName, String localPort) {
    statusBar.println("CONNECT TO "+serviceName+" service @ "+LOCALHOST+":"+localPort);
    /**
     * Responsible for starting an ssh session
     */
    class Worker extends Thread {
      String serviceName;
      String port;
      Worker(String serviceName, String port) {
        this.serviceName=serviceName;
        this.port=port;
      }
      public void run() {
        ShellCmd sc = new ShellCmd(debug);
        
        String cmd = getCommandString(serviceName, port);

        cmd=cmd.replace("$h", "127.0.0.1");
        cmd=cmd.replace("$p", port);
        cmd=cmd.replace("$H", "127.0.0.1");
        cmd=cmd.replace("$P", port);
        statusBar.println("Running system command: "+cmd);
        try {
          sc.runSysCmd(cmd);
        }
        catch(InterruptedException e ) {}
        catch(IOException e ) {
          statusBar.println(e.getLocalizedMessage());
        }      
      }
    }
    Worker worker = new Worker(serviceName, localPort);
    worker.start();
  }
  
  /**
   * Start a VNC terminal session.
   */
  private void spawnVNCSession(String localPort) {
   /**
     * Responsible for starting an vnc session
     */
    class Worker extends Thread {
      String localPort;
      VncViewerListener vncViewerListener;
      Worker(String localPort, VncViewerListener vncViewerListener) {
        this.localPort=localPort;
        this.vncViewerListener=vncViewerListener;
      }
      public void run() {
        VNCOptionsConfig vncOptionsConfig = getSelectedVNCOptionsConfig();
        if( vncOptionsConfig != null ) {
           vncOptionsConfig.serverAddress=LOCALHOST;
          vncOptionsConfig.serverPort=Integer.parseInt(localPort);
          statusBar.println("CONNECT TO "+vncOptionsConfig.serverAddress+":"+vncOptionsConfig.serverPort);
          VNCFrame.Launch(vncOptionsConfig.getOptionsArray(), statusBar, vncViewerListener, PROGRAM_NAME);
        }
      }
    }
    Worker worker = new Worker(localPort, this);
    worker.start();
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
    catch(Exception e) {}
  }
  

  public void windowDeactivated(WindowEvent arg0) {}

  public void windowDeiconified(WindowEvent arg0) {}

  public void windowIconified(WindowEvent arg0) {}

  public void windowOpened(WindowEvent arg0) {}

  public void mouseClicked(MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
        int index = connectionTable.rowAtPoint(e.getPoint());
        connectionTable.setRowSelectionInterval(index,index);
        showConnectionConfigDialog();
     }
  }

  public void mouseEntered(MouseEvent arg0) {}

  public void mouseExited(MouseEvent arg0) {}

  public void mousePressed(MouseEvent arg0) {
    if( SwingUtilities.isLeftMouseButton(arg0) && arg0.getClickCount() == 2 ) {
      int selectedIndex = connectionTable.getSelectedRow();
      if( selectedIndex >= 0 ) {
        String hostname = connectionList.get(selectedIndex).get(HOSTNAME).toString();
        String serviceName = connectionList.get(selectedIndex).get(SERVICE_NAME).toString();
        String localPort = connectionList.get(selectedIndex).get(LOCAL_PORT).toString();
        if( serviceName.toLowerCase().contains("vnc") ) {
          spawnVNCSession(localPort);
        }
        else {
          spawnCommandLine(hostname, serviceName, localPort);
        }
      }
    } 
  }

  public void mouseReleased(MouseEvent arg0) {
    if( SwingUtilities.isRightMouseButton(arg0) && arg0.getClickCount() == 1 ) {
      //showConnectionConfigDialog();
    }
  }
  
  /**
   * Display the connection config dialog for the selected connection.
   */
  private void showConnectionConfigDialog() {
    int selectedIndex = connectionTable.getSelectedRow();
    if( selectedIndex >= 0 ) {
      String serviceName = connectionList.get(selectedIndex).get(SERVICE_NAME).toString();
      String localPort = connectionList.get(selectedIndex).get(LOCAL_PORT).toString();
      if( serviceName.toLowerCase().indexOf("vnc") != -1 ) {
        showVNCConnectionConfigDialog(localPort);
      }
      else {
        showCommandConfigDialog(serviceName, LOCALHOST, localPort);
      }
    }
  }
  
  /**
   * Get the selected VNC Options config or null if not selected
   * @return
   */
  private VNCOptionsConfig getSelectedVNCOptionsConfig() {
    VNCOptionsConfig vncOptionsConfig = new VNCOptionsConfig( getConfigPath().getAbsolutePath(), GUI.PROGRAM_NAME );
    int selectedIndex = connectionTable.getSelectedRow();
    if( selectedIndex >= 0 ) {
      try {
        vncOptionsConfig.loadHS(getSelectedHostname(), getSelectedServiceName());
      }
      catch(Exception e) {}
      return vncOptionsConfig;
    }
    return null;
  }
  
  /**
   * Display the VNC config dialog
   * 
   * @param connectionAtributes
   */
  public void showVNCConnectionConfigDialog(String localPort) {    
    VNCOptionsConfig vncOptionsConfig = getSelectedVNCOptionsConfig();
    if( vncOptionsConfig != null ) {
      vncOptionsPanel.setArgs( vncOptionsConfig.getOptionsArray() );
 
      setPanel(vncOptionsPanel);
      
      genericOKCancelDialog.setVisible(true);
      if( genericOKCancelDialog.isOkSelected() ) {
        try {
          //Load the config object from the attributes the user selected
          vncOptionsConfig.setOptionsArray( vncOptionsPanel.getArgs() );
          vncOptionsConfig.serverAddress=LOCALHOST;
          vncOptionsConfig.serverPort=Integer.parseInt(localPort);
          vncOptionsConfig.saveHS(getSelectedHostname(), getSelectedServiceName());
          statusBar.println("Saved connection configuration to "+vncOptionsConfig.getHSConfigFile(getSelectedHostname(), getSelectedServiceName()));
        }
        catch(Exception e) {
          Dialogs.showErrorDialog(this, "Save Configuration Error", e.getLocalizedMessage() );
        }
      }
    }
  }
  
  /**
   * Ensure the config dialog is displaying this JPanel
   * @param panel
   */
  public void setPanel(JPanel panel) {
    genericOKCancelDialog.remove(vncOptionsPanel);
    genericOKCancelDialog.remove(cmdLinePanel);
    genericOKCancelDialog.add(panel);
  }
  
  /**
   * Display the SSH config dialog
   * 
   * @param connectionAtributes
   */
  public void showCommandConfigDialog(String serviceName, String hostname, String localPort) {
    File configFile = getCommandLineConfigFile(hostname, localPort);
    
    setPanel(cmdLinePanel);

    try {
      cmdLinePanel.setArgs( GetStringArray( configFile ) );
    }
    catch(IOException e ) {}
    
    cmdLinePanel.setCommand( getCommandString(serviceName, localPort) );

    genericOKCancelDialog.setVisible(true);
    if( genericOKCancelDialog.isOkSelected() ) {
      try {
        SaveStringArray( cmdLinePanel.getArgs(), configFile);
        statusBar.println("Saved connection configuration to "+configFile);
      }
      catch(IOException e) {
        Dialogs.showErrorDialog(this, "Save Configuration Error", e.getLocalizedMessage() );
      }
    }
  }
  
  /***
   * Return the path where config files are saved.
   * @return
   */
  public File getConfigPath() {
    File configPath = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), "config"+System.getProperty("file.separator")+"sshpfg");
    if( !configPath.isDirectory() ) {
      if( !configPath.mkdirs() ) {
        Dialogs.showErrorDialog(this, "Error", "Failed to create the "+configPath+" folder.");
      }
    }
    return configPath;
  }
  
  
  /**
   * Get the cmd line config file for the given hostname and port
   * @param connectionAtributes
   * @return
   */
  public File getCommandLineConfigFile(String hostname, String port) {
    File configPath = getConfigPath();
    hostname=hostname.replace("@","_");
    return new File(configPath.getAbsoluteFile(), VNCOptionsConfig.class.getName()+"_CMD_LINE_"+hostname+"_"+port);
  }
  
  /**
   * Save a String array to a file. It may be read back with GetStringArray()
   * @param stringArray
   * @param file
   * @throws IOException
   */
  public static void SaveStringArray(String stringArray[], File file) throws IOException {
    File parentPath = file.getParentFile();
    if( !parentPath.isDirectory() ) {
      if( !parentPath.mkdirs() ) {
        throw new IOException("Failed to create "+parentPath);
      }
    }
    FileWriter writer = new FileWriter(file); 
    boolean value=false;
    String currentAttributeName="";
    for(String str: stringArray) {
      if( value ) {
        writer.write(currentAttributeName+"="+str+"\t");
      }
      else {
        currentAttributeName=str;
      }
      value=!value;
    }
    writer.close();
  }
  
  /**
   * Fetch a String array from a file previously saved with SaveStringArray()
   * @param file
   * @return
   * @throws IOException
   */
  public static String[] GetStringArray(File file) throws IOException {
    String line;
    Vector<String> stringList = new Vector<String>();
    FileReader fileReader = new FileReader(file);
    BufferedReader br = new BufferedReader(fileReader);
    while(true) {
      line = br.readLine();
      if( line == null ) {
        break;
      }
      StringTokenizer strTok = new StringTokenizer(line,"\t");
      while( strTok.hasMoreElements() ) {
        String keyValue = strTok.nextToken();
        StringTokenizer strTok2 = new StringTokenizer(keyValue,"=");
        if( strTok2.countTokens() == 2 ) {
          stringList.add( strTok2.nextToken() );
          stringList.add( strTok2.nextToken() );
        }        
      }
    }
    fileReader.close();    
    String args[] = new String[stringList.size()];
    stringList.toArray(args);

    return args;
  }

    
  /**
   * Callback when connected to VNC host, update the desktop name in the table and it's associated persistent configuration object
   */
  public void connected(String host, int port, String desktopName) {
    try {
      VNCOptionsConfig vncOptionsConfig = new VNCOptionsConfig( getConfigPath().getAbsolutePath(), GUI.PROGRAM_NAME );
      vncOptionsConfig.loadHS(getSelectedHostname(), getSelectedServiceName());
      vncOptionsConfig.desktopName = desktopName;
      vncOptionsConfig.saveHS(getSelectedHostname(), getSelectedServiceName());
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
      VNCOptionsConfig vncOptionsConfig = new VNCOptionsConfig( getConfigPath().getAbsolutePath(), GUI.PROGRAM_NAME );
      vncOptionsConfig.loadHS(getSelectedHostname(), getSelectedServiceName());
      vncOptionsConfig.windowWidth=width;
      vncOptionsConfig.windowHeight=height;
      vncOptionsConfig.windowPosX=x;
      vncOptionsConfig.windowPosY=y;      
      vncOptionsConfig.saveHS(getSelectedHostname(), getSelectedServiceName());
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
   * @brief Enable/Disable debugging
   * @param debug If true then debug is enabled.
   */
  public void enableDebug(boolean debug) {
    this.debug=debug;
  }
  
  /**
   * @brief Get the state of the debug attribute
   * @return If true then debug is enabled.
   */
  public boolean enableDebug() {
    return debug;
  }
  

  


}
