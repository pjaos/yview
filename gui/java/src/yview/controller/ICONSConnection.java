package yview.controller;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

import org.json.JSONObject;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import yview.model.Constants;
import yview.model.DeviceMsgDebug;
import yview.model.ICONServer;
import yview.view.MainFrame;
import yview.view.StatusBar;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @brief Responsible for sending/receiving over an icon server connection.
 */
public class ICONSConnection extends Thread implements MqttCallback {
	public static final int ICONS_RECONNECT_DELAY_SECONDS = 5;
	public static final String JSON_CMD = "CMD";
	public static final String JSON_GET_DEVICES = "GET_DEVICES";
	public static final int SOCKET_READ_TIMEOUT_MS = 5000;
	public static final int RX_BUFFER_SIZE = 32768;
	public static final int SERVER_POLL_DELAY = 2000;
	private Session session;
	private ICONServer iconServer;
	private Vector<JSONListener> deviceListeners;
	private StatusBar statusBar;
	private boolean running;
	private Vector<DeviceMsgDebug> deviceMsgDebugList = new Vector<DeviceMsgDebug>();
	private long startMS = System.currentTimeMillis();
	private boolean shutdown;
	private MqttClient mqttClient;
	private String mqttSubscriptionTopic = "#";

	/**
	 * @brief Process a single device in messages received from the ICONS.
	 * @param json The JSON object to process.
	 */
	private void processDevice(String location, String ipAddress, JSONObject jsonDevice) {
		// Notify all device listeners
		if (deviceListeners != null) {
			for (JSONListener deviceListener : deviceListeners) {

				// Add the time we received this message from the server.
				long now = System.currentTimeMillis();
				jsonDevice.put(JSONProcessor.LOCAL_RX_TIME_MS, now);
				JSONProcessor.SetICONSSSHSession(session, jsonDevice);
				deviceListener.setJSONDevice(jsonDevice);

			}
		}
	}

	/**
	 * @brief Shutdown the associated connection to the ICON server. This may block
	 *        for up to 10 seconds waiting for the connection to drop.
	 */
	public void shutdown() {
		SSHWrapper.Disconnect(session, statusBar);
		session = null;
		running = false;
		long startShutdownMS = System.currentTimeMillis();
		while (!shutdown) {
			if (System.currentTimeMillis() > startShutdownMS + 10000) {
				statusBar.println("After 10 seconds ICONS connection has not shutdown");
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
		// Notify all device listeners of the ICONS shutdown
		if (deviceListeners != null) {
			for (JSONListener deviceListener : deviceListeners) {
				deviceListener.iconsShutdown();
			}
		}
	}

	/**
	 * @brief Thread to handle the icons server connection.
	 */
	public void run() {
		int freePort;
		JSch jsch = new JSch();

		shutdown = false;
		running = true;
		while (running) {

			try {
				// Build the ssh connection to the ssh server
				session = SSHWrapper.Connect(jsch, iconServer.getUsername(), iconServer.getServerName(),
						iconServer.getPort(), statusBar);
				MainFrame.SetStatus("Connected to ICON server: " + iconServer.getUsername() + "@"
						+ iconServer.getServerName() + ":" + iconServer.getPort());
				freePort = getAvailableTCPPort();
				iconServer.setICONSPort(freePort);
				session.setPortForwardingL(Constants.LOCAL_HOST, freePort, Constants.LOCAL_HOST,
						Constants.MQTT_SERVER_PORT);
				statusBar.println("MQTT server connection forwarding from local port " + freePort + " to "
						+ Constants.LOCAL_HOST + ":" + Constants.MQTT_SERVER_PORT);

				String broker = "tcp://" + Constants.LOCAL_HOST + ":" + freePort;
				String clientId = MqttClient.generateClientId();

				MemoryPersistence persistence = new MemoryPersistence();
				mqttClient = new MqttClient(broker, clientId, persistence);
				MqttConnectOptions connOpts = new MqttConnectOptions();
				mqttClient.setCallback(this);
				statusBar.println("Connecting to ICONS MQTT server");
				mqttClient.connect(connOpts);
				statusBar.println("Connected to " + broker);

				mqttClient.subscribe(mqttSubscriptionTopic);
				statusBar.println("Subscribed to " + mqttSubscriptionTopic);

				while (mqttClient.isConnected()) {
					Thread.sleep(500);
				}

				statusBar.println("Disconnected from MQTT server");
			} catch (Exception ex) {
				ex.printStackTrace();
				statusBar.println(ex.getLocalizedMessage());
				SSHWrapper.Disconnect(session, statusBar);
				session = null;
			}

			reconnectDelay();

		}
		shutdown = true;
	}

	/**
	 * @brief Present a status message to the user if we have a gui widet to send
	 *        the msg to.
	 * @param line The line of text to be displayed.
	 */
	private void status(String line) {
		if (statusBar != null) {
			statusBar.println(line);
		}
	}

	/**
	 * @brief Get the number of an unused TCP port on this machine.
	 * @return The free TCP port or -1 if unable to find a free TCP port.
	 */
	private int getAvailableTCPPort() {
		int freePort = -1;
		try {
			ServerSocket serverSocket = new ServerSocket(0);
			freePort = serverSocket.getLocalPort();
			serverSocket.close();
		} catch (IOException ex) {
		}
		return freePort;
	}

	/**
	 * @brief Perform a delay before reconnecting to the SSH server/ICONS
	 */
	private void reconnectDelay() {
		long runningMS = System.currentTimeMillis() - startMS;
		// If we've tried at least 3 times
		if (runningMS > ICONSConnection.ICONS_RECONNECT_DELAY_SECONDS * 1000 * 3) {
			status("Pausing for " + ICONSConnection.ICONS_RECONNECT_DELAY_SECONDS + " seconds before reconnecting to "
					+ iconServer.getServerName() + ":" + iconServer.getPort());
			try {
				Thread.sleep(ICONSConnection.ICONS_RECONNECT_DELAY_SECONDS * 1000);
			} catch (InterruptedException ex) {
			}
		}
	}
	
	/**
	 * @brief Reset the reconnect delay timer.
	 */
	public void resetReconnectTimer() {
		startMS=System.currentTimeMillis();
	}

	/**
	 * @brief Set the associated ICONServer object.
	 * @param ICONServer The ICONServer object.
	 */
	public void setICONServer(ICONServer iconServer) {
		this.iconServer = iconServer;
	}

	/**
	 * @brief Set the list of device listeners that is interested in device
	 *        messages.
	 * @param deviceListeners The Vector of DeviceListener objects.
	 */
	public void setDeviceListeners(Vector<JSONListener> deviceListeners) {
		this.deviceListeners = deviceListeners;
	}

	/**
	 * @brief Set the status bar associated with this ICONSConnection instance.
	 * @param statusBar A StatusBar instance.
	 */
	public void setStatusBar(StatusBar statusBar) {
		this.statusBar = statusBar;
	}

	/**
	 * @brief Called when a connection is lost to the ICONS.
	 */
	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("MQTT connectionLost(): " + arg0);
	}

	/**
	 * @brief Called when an ICONS message delivery is complete. 
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		System.out.println("deliveryComplete(): " + arg0);
	}

	/**
	 * @brief Called when a message is received from the ICONS MQTT server.
	 * @param topic   The topic that the message was received on.
	 * @param message The MQTT message received.
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {

			String rxData = new String(message.getPayload());
			JSONObject json = new JSONObject();
			json = new JSONObject(rxData);
			String location = JSONProcessor.GetLocation(json);
			String ipAddress = JSONProcessor.GetIPAddress(json);

			if (location != null && ipAddress != null) {
				processDevice(location, ipAddress, json);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
