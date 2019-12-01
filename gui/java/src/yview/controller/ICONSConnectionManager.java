package yview.controller;

import java.util.Vector;

import yview.model.DeviceMsgDebug;
import yview.model.ICONServer;
import yview.view.MainFrame;
import yview.view.StatusBar;

public class ICONSConnectionManager extends Thread {
	private Vector<ICONSConnection> iconsConnectionList = new Vector<ICONSConnection>();
	private boolean   running;
	private StatusBar statusBar;
	private Vector<JSONListener> deviceListeners = new Vector<JSONListener>();
	
	/**
	 * @brief Add a listener for device messages as they are received from the ICONS
	 * @param deviceListener The device listener
	 */
	public void addDeviceListener(JSONListener deviceListener) { deviceListeners.add(deviceListener); }
	
	/**
	 * @brief Remove the device that is listening for device messages 
	 * @param deviceListener The device listener
	 */
	public void removeDeviceListener(JSONListener deviceListener) { deviceListeners.remove(deviceListener); }
	
	/**
	 * @brief Remove all the listeners for device messages
	 */
	public void removeAllDeviceListeners() { deviceListeners.removeAllElements(); }
	
	
	/**
	 * @brief Responsible for managing connections to the ICON servers
	 */
	public ICONSConnectionManager() {

	}
	
	/**
	 * @brief Set a reference to a status bar to be used to provide user info when connecting to ICONS.
	 * @param statusBar A StatusBar instance
	 */
	public void setStatusBar(StatusBar statusBar) {
		this.statusBar=statusBar;
	}
	
	public void run() {
		running = true;

		while(running) {
			
			try {
				Thread.sleep(250);
			}
			catch( InterruptedException e ) {}
			
		}

		shutDownConnections();
		
	}
	
	/**
	 * @brief Force a shutdown of this ICONSConnectionManager instance.
	 */
	public void shutdown() {
		running = false;
	}

	/**
	 * @brief Shutdown connections to current servers.
	 */
	private void shutDownConnections() {
		if( iconsConnectionList != null && iconsConnectionList.size() > 0 ) {
			for( ICONSConnection iconsConnection : iconsConnectionList ) {
				iconsConnection.shutdown();
			}
		}
	}
	
	/**
	 * @brief Connect to all the configured ICON servers
	 * @param iconServerList The list of ICON servers to connect to
	 */
	public void connectICONS(ICONServer[] iconServerList) {
		MainFrame.SetStatus("Shutting down ICONS connections");
 		shutDownConnections();

		if( iconServerList != null || iconServerList.length > 0 ) {
			
			for( ICONServer iconServer : iconServerList ) {
				if( iconServer.getActive() ) {
					ICONSConnection iconSConnection = new ICONSConnection();
					iconSConnection.setICONServer(iconServer);
					iconSConnection.setDeviceListeners(deviceListeners);
					iconSConnection.setStatusBar(statusBar);
					iconSConnection.resetReconnectTimer();
					iconSConnection.start();
					iconsConnectionList.add(iconSConnection);
					
					
				}
			}
		
		}
		
	}

}
