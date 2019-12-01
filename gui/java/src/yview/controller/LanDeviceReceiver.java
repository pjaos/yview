package yview.controller;
import java.util.Vector;

import org.json.JSONObject;

import yview.model.Constants;

import java.io.IOException;
import java.net.*;
  
/**
 * @brief Responsible for receiving UDP multicast messages from devices on the local area network (LAN).
 */
public class LanDeviceReceiver extends Thread
{
  private static final Logger logger = Logger.GetLogger(LanDeviceReceiver.class);
  boolean                   listening;
  byte[]                    deviceRXBuffer;
  DatagramPacket            deviceDataGram;
  Vector<JSONListener>    	jsonListenerList;
  JSONObject				json;
  DatagramSocket 			lanDatagramSocket;

  /**
   * @brief Constructor
   */
  public LanDeviceReceiver(DatagramSocket lanDatagramSocket) {
	this.lanDatagramSocket=lanDatagramSocket;
	deviceRXBuffer = new byte[Constants.MAX_DEVICE_MSG_LENGTH];
	deviceDataGram = new DatagramPacket(deviceRXBuffer, deviceRXBuffer.length);
    jsonListenerList = new Vector<JSONListener>();
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
    listening=true;
    
    try {
	    //Loop to listen for beacon messages
	    while(listening) {
	      java.util.Arrays.fill(deviceRXBuffer, (byte)0);
	      lanDatagramSocket.receive(deviceDataGram);

	      String rxData = new String( deviceDataGram.getData(), 0 , deviceDataGram.getLength() );
	      json = new JSONObject( rxData );
	      json.put(JSONProcessor.LOCATION, Constants.LOCAL_LOCATION);
		  long now = System.currentTimeMillis();
		  json.put(JSONProcessor.LOCAL_RX_TIME_MS, now );
		
	      String ipAddress = getIPAddress(json);
	      if( ipAddress != null ) {
	  		//Notify all device listeners
	  		if( jsonListenerList != null ) {
	  			for( JSONListener jsonListener : jsonListenerList ) {
	  				jsonListener.setJSONDevice(json);
	  			}
	  		}
	      }
	    }
	  }
	  catch(Exception e) {
	    e.printStackTrace();
	  }
  }
  
  /**
   * Shutdown the server listening for device messages.
   */
  public void shutdown() {
	  if( listening ) {
		  lanDatagramSocket.close();
	  }
  }
  
  /**
   * Get the IP address of the device.
   * @param json The json object built from the data received from the device.
   * @return The IP address of the device or null if the json object does not contain an IP address.
   */
  private String getIPAddress(JSONObject json) {
	  String ipAddress=null;
	  
	  if( json.has(JSONProcessor.IP_ADDRESS) ) {
		  ipAddress=json.getString(JSONProcessor.IP_ADDRESS);
	  }
	  
	  return ipAddress;
  }

  /**
   * @brief Add to the list of JSON Listener objects.
   * @param jsonListener The object to be notified of received JSON messages.
   */
  public void addJSONListener(JSONListener jsonListener) {
	  jsonListenerList.add(jsonListener);
  }

}
