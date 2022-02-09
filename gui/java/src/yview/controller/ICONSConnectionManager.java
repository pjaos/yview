package yview.controller;

import java.util.Vector;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;
import java.net.DatagramSocket;
import java.net.SocketException;

import yview.model.DeviceMsgDebug;
import yview.model.ICONServer;
import yview.view.MainFrame;
import yview.view.StatusBar;
import yview.model.Constants;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Enumeration;

public class ICONSConnectionManager extends Thread implements JSONListener {
	public static final int AYT_THREAD_RUNTIME_SECONDS = 15;

	private Vector<ICONSConnection> iconsConnectionList = new Vector<ICONSConnection>();
	private boolean   running;
	private StatusBar statusBar;
	private Vector<JSONListener> deviceListeners = new Vector<JSONListener>();
	private AreYouThereTXThread areYouThereTXThread;
	private LanDeviceReceiver lanDeviceReceiver;
	private ReentrantLock devListLock = new ReentrantLock();
	private Hashtable<String, JSONObject> localDevHashtable = new Hashtable<String, JSONObject>(); 

	/**
	 * @brief Get the key in the localDevHashtable used to identify a unique device on the local LAN.
	 * @param jsonDevice The JSONObject instance.
	 * @return The ID string of the device on the local LAN.
	 */
	public static String GetLocalLanIDStr(JSONObject jsonDevice) {
		String idStr = JSONProcessor.GetProductID(jsonDevice)+JSONProcessor.GetUnitName(jsonDevice)+JSONProcessor.GetIPAddress(jsonDevice);
		return idStr;
	}
	
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
	 * @param pauseReconnectDelay If we are reconnecting to the ICONS
	 *        and pauseReconnectDelay is true then we reconnect as quickly as 
	 *        possible for a about 3 seconds after this method is called.
	 *        After this we return to the delaying a reconnect to the ICONS
	 *        should the connection drop.
	 */
	public void shutdown(boolean pauseReconnectDelay) {
		if( pauseReconnectDelay && iconsConnectionList != null && iconsConnectionList.size() > 0 ) {
			for( ICONSConnection iconsConnection : iconsConnectionList ) {
				iconsConnection.shutdown(pauseReconnectDelay);
			}
		}
		running = false;
	}

	/**
	 * @brief Shutdown connections to current servers.
	 */
	private void shutDownConnections() {
		if( iconsConnectionList != null && iconsConnectionList.size() > 0 ) {
			for( ICONSConnection iconsConnection : iconsConnectionList ) {
				iconsConnection.shutdown(false);
			}
		}
		if( areYouThereTXThread != null ) {
			areYouThereTXThread.shutdown();
			areYouThereTXThread = null;
		}
		if( lanDeviceReceiver != null ) {
			lanDeviceReceiver.shutdown();
			lanDeviceReceiver = null;
		}
	}
	
	/**
	 * @brief Connect to all the configured ICON servers
	 * @param iconServerList The list of ICON servers to connect to
	 * @param connectToLocalDevices If true attempt to connect to local devices directly. I.E don't go through ICONS.
	 */
	public void connectICONS(ICONServer[] iconServerList, boolean connectToLocalDevices) {
		MainFrame.SetStatus("Shutting down ICONS connections");
 		shutDownConnections();

		if( iconServerList != null || iconServerList.length > 0 ) {
			
			for( ICONServer iconServer : iconServerList ) {
				if( iconServer.getActive() ) {
					ICONSConnection iconSConnection = new ICONSConnection();
					iconSConnection.setICONServer(iconServer);
					iconSConnection.setDeviceListeners(deviceListeners);
					iconSConnection.setStatusBar(statusBar);
					iconSConnection.start();
					iconsConnectionList.add(iconSConnection);

				}
			}
		
			discoverLocalDevices(connectToLocalDevices);
		      
		}
		
	}
	
	/**
	 * @brief Attempt to discover local devices.
	 * @param enable If true attempt to discover new local devices. If false then empty list of known devices.
	 */
	public void discoverLocalDevices(boolean enable) {
		if( areYouThereTXThread != null ) {
			areYouThereTXThread.shutdown();
			areYouThereTXThread = null;
		}
		if( lanDeviceReceiver != null ) {
			lanDeviceReceiver.shutdown();
			lanDeviceReceiver = null;
		}
		localDevHashtable = new Hashtable<String, JSONObject>();
		
		if( enable ) {
			
			// We start threads to find any local devices when connecting to the ICONS
			// so that the device list can be used to connect to devices directly rather than 
			// the unnecessary effort of connecting through the ICONS server.
			try {
				DatagramSocket lanDatagramSocket = new DatagramSocket();
			    areYouThereTXThread = new AreYouThereTXThread(lanDatagramSocket, MainFrame.GetJSONAYTM(Constants.DEFAULT_AYT_MESSAGE));
			    areYouThereTXThread.setRuntime(ICONSConnectionManager.AYT_THREAD_RUNTIME_SECONDS);
			    areYouThereTXThread.setDaemon(true);
				areYouThereTXThread.start();    
	
			    lanDeviceReceiver = new LanDeviceReceiver(lanDatagramSocket);
			    lanDeviceReceiver.setDaemon(true);
			    lanDeviceReceiver.addJSONListener(this);
			    lanDeviceReceiver.start();
					
			} catch(SocketException e) {
				e.printStackTrace();
			}		
			
		}
	}
	
	//Called when we receive data from a device
	public void setJSONDevice(JSONObject jsonDevice) {
		devListLock.lock();
		// Ensure we have a record of all the devices on the local LAN
		String idStr = ICONSConnectionManager.GetLocalLanIDStr(jsonDevice);
		localDevHashtable.put(idStr, jsonDevice);
		
		MainFrame.SetStatus("---------------------Local Device List -------------------------------------");
		Enumeration<String> e = localDevHashtable.keys();
        while (e.hasMoreElements()) {
    	   String key = e.nextElement();
    	   MainFrame.SetStatus(JSONProcessor.GetUnitName(localDevHashtable.get(key))+" = "+JSONProcessor.GetIPAddress(localDevHashtable.get(key)));
        }
		devListLock.unlock();
	}
	
	//Called when the connection to the ICONS drops
	public void iconsShutdown() {
		areYouThereTXThread.shutdown();
		lanDeviceReceiver.shutdown();
	}
	
	/**
	 * @brief Determine if a device is present on a local LAN.
	 * @param jsonDevice A JSONObject instance.
	 * @return True if present.
	 */
	public boolean isOnLocalLan(JSONObject jsonDevice) {
		boolean foundOnLocalLan = false;
		devListLock.lock();
		String idStr = ICONSConnectionManager.GetLocalLanIDStr(jsonDevice);
		JSONObject localJSONDevice= localDevHashtable.get(idStr);
		devListLock.unlock();
		if( localJSONDevice != null ) {
			foundOnLocalLan = true;
		}
		return foundOnLocalLan;
	}
	
	/**
	 * @brief Get the address of the device on the local LAN if present on the local LAN.
	 * @param jsonDevice A JSONObject instance.
	 * @return The IP address of the device on the local LAN or null if not found on the local LAN.
	 */
	public String getLocalAddress(JSONObject jsonDevice) {
		String devAddress = null;
		if( isOnLocalLan(jsonDevice) ) {
			devAddress = JSONProcessor.GetIPAddress(jsonDevice);
		}
		return devAddress;
	}
	
	/**
	 * @brief Get the TCP port of the device on the local LAN if present on the local LAN.
	 * @param jsonDevice A JSONObject instance.
	 * @return The String that defines the services available on the device or null if not found on the local LAN.
	 */
	public String getLocalServiceList(JSONObject jsonDevice) {
		String servicesListStr = null;
		if( isOnLocalLan(jsonDevice) ) {
			servicesListStr = JSONProcessor.GetServicesList(jsonDevice);
		}
		return servicesListStr;
	}
	
	/**
	 * @brief Set direct connection to local devices.
	 * @param connectToLocalDevices If true attempt to connect to local devices directly. I.E don't go through ICONS.
	 */
	public void setDirectLocalDevConnect(boolean connectToLocalDevices) {
		discoverLocalDevices(connectToLocalDevices);
	}
	
}
