package uk.me.pausten.yview.controller;

import android.util.Log;

import java.util.Vector;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.controller.JSONListener;
import uk.me.pausten.yview.view.MainActivity;

import java.net.*;

/**
 * @author Paul Austen
 *
 * @brief Responsible for receiving UDP multicast messages from WyUnit hardware on the local area network (LAN).
 */
public class LanDeviceReceiver extends Thread
{
    byte[]                    deviceRXBuffer;
    DatagramPacket            deviceDataGram;
    Vector<JSONListener>      deviceListenerList;
    JSONObject				  json;
    DatagramSocket 			  lanDatagramSocket;

    /**
     * @brief Constructor
     */
    public LanDeviceReceiver(DatagramSocket lanDatagramSocket) {
        this.lanDatagramSocket=lanDatagramSocket;
        deviceRXBuffer = new byte[Constants.MAX_DEVICE_MSG_LENGTH];
        deviceDataGram = new DatagramPacket(deviceRXBuffer, deviceRXBuffer.length);
        deviceListenerList = new Vector<JSONListener>();
    }

    /**
     * @brief The thread method, only calls the listen() method.
     */
    public void run() {
        listen();
    }

    /**
     * @brief A blocking call that will listen for Beacon messages and notify beaconListener
     * objects when they are received.
     */
    public void listen() {

        try {
            //Loop to listen for beacon messages
            while(true) {
                java.util.Arrays.fill(deviceRXBuffer, (byte)0);

                if( lanDatagramSocket != null && lanDatagramSocket.isBound() ) {
                    lanDatagramSocket.receive(deviceDataGram);
                    try {
                        String rxData = new String(deviceDataGram.getData(), 0, deviceDataGram.getLength() );
                        json = new JSONObject(rxData);
                        String ipAddress = JSONProcessor.GetIPAddress(json);
                        if (ipAddress != null) {
                            //Notify all device listeners
                            if (deviceListenerList != null) {
                                for (JSONListener deviceListener : deviceListenerList) {
                                    json.put(JSONProcessor.LOCATION, Constants.LOCAL_LOCATION);
                                    json.put(JSONProcessor.IP_ADDRESS, ipAddress);
                                    deviceListener.setJSONDevice(json);
                                }
                            }
                        }
                    }
                    catch(Exception ex ) {
                    }
                }
            }
        }
        catch(Exception e) {
        }
    }

    /**
     * @brief Shutdown the server. This will close the server socket if we have a reference to a bound socket.
     */
    public void shutdown() {
        MainActivity.Log("LanDeviceReceiver.shutdown()");
        if( lanDatagramSocket != null ) {
            if( lanDatagramSocket.isBound() ) {
                lanDatagramSocket.close();
            }
            lanDatagramSocket=null;
        }
    }

    /**
     * @brief Add to the list of deviceListener objects.
     * @param deviceListener The object to be notified of messages received from devices.
     */
    public void addDeviceListener(JSONListener deviceListener) {
        deviceListenerList.add(deviceListener);
    }

    /**
     * @brief Remove a deviceListener object from the list of of deviceListener objects.
     * @param deviceListener The object to be notified of messages received from devices.
     */
    public void removeBeaconListener(JSONListener deviceListener) {
        deviceListenerList.remove(deviceListener);
    }

    /**
     * @brief Remove all deviceListener objects
     */
    public void removeAllDeviceLisenters() {
        deviceListenerList.removeAllElements();
    }

}
