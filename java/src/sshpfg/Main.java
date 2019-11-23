/*****************************************************************************************
 *                        Copyright 2013 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package sshpfg;

import java.awt.*;

import javax.swing.UIManager;

import pja.sshpfg.view.*;
import pja.sshpfg.model.*;

public class Main
{
  public static final String CONFIG_FILENAME=GUI.PROGRAM_NAME.toLowerCase()+".cfg";
  public static final String OS_NAME = System.getProperty("os.name");
  
  public static final String DEFAULT_WINDOWS_TERMINAL_CMD="putty";
  public static final String DEFAULT_WINDOWS_EXECUTE_CMD_ARG="-x";
  public static final String DEFAULT_WINDOWS_VNC_CMD="vncviewer";

  public static final String DEFAULT_TERMINAL_CMD="gnome-terminal";
  public static final String DEFAULT_EXECUTE_CMD_ARG="-x";
  public static final String DEFAULT_VNC_CMD="vinagre";
  
  public static boolean Debug=false;
  public static boolean EnableMemoryUsageReport=false;
    
  public Config config;
  
  GUI       gui;

  /**
   * @param args
   */
  public static void main(String[] args) 
  {
    //Check for debug argument
    for( String s : args ) {
      if( s.toLowerCase().equals("-d") ) {
        Debug=true;
      }
      if( s.toLowerCase().equals("-m") ) {
        EnableMemoryUsageReport=true;
      }
      if( s.toLowerCase().equals("-h") ) {
        cmdLineUsage();
      }
    }
    new Main();
  }
  
  /**
   * @brief Display command line usage text
   */
  public static void cmdLineUsage() {
    System.out.println("java -jar sshpfg.jar <options>");
    System.out.println("Command line options");
    System.out.println("-h : Show this help text.");
    System.out.println("-d : Enable debugging output.");
    System.out.println("-m : Enable memory usage reports.");  
    System.exit(0);
  }
  
  public Main() {
    config = new Config(GUI.PROGRAM_NAME);

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
    gui = new GUI(config, Main.CONFIG_FILENAME);
    gui.enableDebug(Debug);
    gui.enableShowMemoryUsage(EnableMemoryUsageReport);
    gui.setSize( new Dimension(config.guiWidth, config.guiHeight) );
    gui.setLocation( config.guiX, config.guiY);
    gui.setVisible(true);
    gui.startLoadConnectionListThread();
  }
  
}
