package uk.me.pausten.yview.controller;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.view.Dialogs;
import uk.me.pausten.yview.controller.JSONListener;
import uk.me.pausten.yview.view.MainActivity;

/**
 * @brief Responsible for sending/receiving over an icon server connection.
 *
 */
public class ICONSConnection extends Thread implements MqttCallback {
    private Session session;
    private JSONListener deviceListener;
    private boolean running;
    private Activity activity;
    private String serverUsername;
    private String serverAddress;
    private int serverPort;
    private int localIconServerPort;
    private long startMS = System.currentTimeMillis();
    private MqttClient mqttClient;
    private String mqttSubscriptionTopic = "#";

    /**
     * @brief ICONSConnection Constructor.
     * @param serverUsername
     * @param serverAddress
     * @param serverPort
     * @param deviceListener
     */
    public ICONSConnection(String serverUsername, String serverAddress, int serverPort, JSONListener deviceListener) {
        this.serverUsername=serverUsername;
        this.serverAddress=serverAddress;
        this.serverPort=serverPort;
        this.deviceListener=deviceListener;
    }

    /**
     * @brief Process a single device in messages received from the ICONS.
     * @param json The JSON object to process.
     */
    private void processDevice(String location, String ipAddress, JSONObject jsonDevice) throws JSONException {
        //Notify device listener
        if( deviceListener != null ) {
            // Add the time we received this message from the server.
            long now = System.currentTimeMillis();
            jsonDevice.put(JSONProcessor.LOCAL_RX_TIME_MS, now);
            JSONProcessor.SetICONSSSHSession(session, jsonDevice);
            deviceListener.setJSONDevice(jsonDevice);
        }
    }

    /**
     * @brief Set the activity associated with the networking manager.
     * @param activity The Activity referrence.
     */
    public void setActivity(Activity activity) {
        this.activity=activity;
    }

    /**
     * @brief Shutdown the associated connection to the ICON server
     */
    public void shutdown() {
        SSHWrapper.Disconnect(session, activity);
        session = null;
        running = false;
    }

    /**
     * @brief Thread to handle the icons server connection.
     */
    public void run() {
        JSch 	jsch = new JSch();

        running = true;
        while(running) {

            try {
                MainActivity.Log("Connecting to SSH server "+serverUsername+"@"+serverAddress+":"+serverPort);

                //Build the ssh connection to the ssh server
                session = SSHWrapper.Connect(jsch, serverAddress, serverPort, serverUsername, null, activity );
                localIconServerPort = getAvailableTCPPort();
                MainActivity.Log("Connected to ICON server: " + serverUsername + "@"
                        + serverAddress + ":" + serverPort);

                session.setPortForwardingL(Constants.LOCAL_HOST, localIconServerPort, Constants.LOCAL_HOST, Constants.MQTT_SERVER_PORT);

                MainActivity.Log("MQTT server connection forwarding from local port " + localIconServerPort + " to "
                        + Constants.LOCAL_HOST + ":" + Constants.MQTT_SERVER_PORT);

                String broker = "tcp://" + Constants.LOCAL_HOST + ":" + localIconServerPort;
                String clientId = MqttClient.generateClientId();

                MemoryPersistence persistence = new MemoryPersistence();
                mqttClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                mqttClient.setCallback(this);
                MainActivity.Log("Connecting to ICONS MQTT server");
                mqttClient.connect(connOpts);
                MainActivity.Log("Connected to " + broker);

                mqttClient.subscribe(mqttSubscriptionTopic);
                MainActivity.Log("Subscribed to " + mqttSubscriptionTopic);

                while (mqttClient.isConnected()) {
                    Thread.sleep(500);
                }

                MainActivity.Log("Disconnected from MQTT server");

            }
            catch(Exception ex) {
                MainActivity.Log("EXCEPTION: "+Log.getStackTraceString(ex));
                status("ICON server connection failed");
                SSHWrapper.Disconnect(session, activity);
                session = null;
            }

            if( running ) {
                reconnectDelay();
            }

        }

    }

    /**
     * @brief present a status message to the user if we have a gui wigdet to send the msg to.
     * @param line
     */
    private void status(String line) {
        if( activity != null ) {
            Dialogs.Toast(activity, line);
        }
    }

    /**
     * @brief Get the number of an unused TCP port on this machine.
     * @return The free TCP port or -1 if unable to find a free TCP port.
     */
    private int getAvailableTCPPort() {
        int freePort=-1;
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            freePort = serverSocket.getLocalPort();
            serverSocket.close();
        }
        catch(IOException ex) {}
        return freePort;
    }

    /**
     * @brief Perform a delay when polling the server for device lists
     */
    private void pollDelay() {
        try {
            Thread.sleep(Constants.SERVER_POLL_DELAY);
        }
        catch(InterruptedException ex) {}
    }

    /**
     * @brief Perform a delay before reconnecting to the SSH server/ICONS
     */
    private void reconnectDelay() {
        long runningMS = System.currentTimeMillis()-startMS;
        //If we've tried at least 3 times
        if( runningMS > Constants.ICONS_RECONNECT_DELAY_SECONDS*1000*3 ) {
            status("Pausing for " + Constants.ICONS_RECONNECT_DELAY_SECONDS + " seconds before reconnecting to " + serverAddress + ":" + serverPort);
            try {
                Thread.sleep(Constants.ICONS_RECONNECT_DELAY_SECONDS * 1000);
            } catch (InterruptedException ex) {}
        }
    }

    @Override
    public void connectionLost(Throwable arg0) {
        MainActivity.Log("MQTT connectionLost(): " + arg0);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        MainActivity.Log("MQTT deliveryComplete(): " + arg0);
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
