/*****************************************************************************************
 *                        Copyright 2015 Paul Austen                                  *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
package yview.controller;

import yview.model.*;
import yview.view.*;

import javax.swing.UIManager;
import java.net.SocketException;
import javax.swing.JOptionPane;
import javax.swing.UIManager.LookAndFeelInfo;

import java.net.*;
import java.io.*;

/**
 * 
 * @author Paul Austen
 * @brief Responsible for holding the program entry point method.
 **/
public class Main
{
  public static ServerSocket instanceLockSocket=null;

  public static boolean ForceDefaultRemoteServices=false;
  static MainFrame MainFrame;

  public static void usage() {
	  System.out.println("Usage\n");
	  System.out.println("-f : Force remote device (from ICONS server) service list from local file.\n");
	  System.out.println("   : Normallly the services that remote devices advertise are used.\n");
	  System.out.println("-h : Show this help/usage message.\n");
  }
  /**
   * @brief Program entry point.
   * @param args Command line arguments.
   */
  public static void main(String[] args)
  { 
    Logger.SetLevel(Logger.LEVEL_INFO);

    //Process cmd line arguments
    for( String arg : args ) {
    	arg=arg.toLowerCase();
    	if( arg.equals("-h") ) {
    		usage();
    		System.exit(0);
    	}
    	else if( arg.equals("-f") ) {
    		ForceDefaultRemoteServices=true;
    	}
    }
    
    try {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	ReleaseInstanceLock();
            }
        });
        
    	GetInstanceLock();

    	Main.MainFrame = new MainFrame(Constants.APP_NAME);
    }
    catch(IOException e) {
    	JOptionPane.showMessageDialog(null, "YView is already running.\nYou may only have on instance YView running at a time.", "Warning", JOptionPane.ERROR_MESSAGE);
    }

  }
  
  /**
   * @brief Get an application instance lock to ensure only one instance runs at a time.
   */
  public static void GetInstanceLock() throws IOException {
	  instanceLockSocket = new ServerSocket(Constants.INSTANCE_LOCK_TCP_PORT_NUMBER);
  }
  
  /**
   * @brief Release an application instance lock if we have one.
   */
  public static void ReleaseInstanceLock() {
	  try { 
		  if( instanceLockSocket != null ) {
			  instanceLockSocket.close();
		  }
	  }
	  catch(IOException ex) {}
  }

}
