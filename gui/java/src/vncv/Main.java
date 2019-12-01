/*****************************************************************************************
 *                        Copyright 2013 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package vncv;

import java.awt.*;

import javax.swing.UIManager;

import vncv.model.Config;
import vncv.view.*;

public class Main
{
  public static final String CONFIG_FILENAME=VNCV.PROGRAM_NAME.toLowerCase()+".cfg";
  public static final String OS_NAME = System.getProperty("os.name");
  
  public static final String DEFAULT_WINDOWS_TERMINAL_CMD="putty";
  public static final String DEFAULT_WINDOWS_EXECUTE_CMD_ARG="-x";
  public static final String DEFAULT_WINDOWS_VNC_CMD="vncviewer";

  public static final String DEFAULT_TERMINAL_CMD="gnome-terminal";
  public static final String DEFAULT_EXECUTE_CMD_ARG="-x";
  public static final String DEFAULT_VNC_CMD="vinagre";
  
  public static boolean EnableMemoryUsageReport=false;
  
  VNCV       gui;
  
  Config config;
  

  /**
   * @brief The VNCV programs entry point.
   * @param args
   */
  public static void main(String[] args) 
  {
    //Check for debug argument
    for( String s : args ) {
      if( s.toLowerCase().equals("-m") ) {
        EnableMemoryUsageReport=true;
      }
      if( s.toLowerCase().equals("-h") ) {
        CmdLineUsage();
      }
    }
    new Main();
  }
  
  /**
   * @brief Display command line usage text
   */
  public static void CmdLineUsage() {
    System.out.println("java -jar sshpfg.jar <options>");
    System.out.println("Command line options");
    System.out.println("-h : Show this help text.");
    System.out.println("-m : Enable memory usage reports (every "+(VNCV.PERIODIC_TIMER_UPDATE_MS/1000)+" seconds).");    
    System.exit(0);
  }
  
  /**
   * @brief Contructor that caused the VNCV GUI to be displayed.
   */
  public Main() {
    config = new Config();
    try {
      config.load(CONFIG_FILENAME);
    }
    //Ignore exception if we have no config or it has an error
    catch(Exception e) {}
    
    //Setup the default look and feel
    try {
      UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
      UIManager.setLookAndFeel( lookAndFeels[config.lookAndFeelIndex].getClassName() );
    }
    catch(Exception e) {}
    
    //Startup the GUI
    gui = new VNCV(config, CONFIG_FILENAME);
    gui.enableShowMemoryUsage(EnableMemoryUsageReport);
    gui.setSize( new Dimension(config.guiWidth, config.guiHeight) );
    gui.setLocation( config.guiX, config.guiY);
    gui.setVisible(true);
  }
  
}
