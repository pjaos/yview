package yview.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import yview.model.Constants;

import java.util.Iterator;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InterfaceAddress;
import javax.swing.*;

/**
 * @brief REsponsible for the discovery of devices on the local IP sub network.
 */
public class AreYouThereTXThread extends Thread {
	private boolean running;
	DatagramSocket lanDatagramSocket;
	String aytMsg;
	int runTimeSeconds;
	long startTimeMS;
	
	/**
	 * @brief Constructor
	 * @param lanDatagramSocket The socket to send UDP are you there messages.
	 */
	public AreYouThereTXThread(DatagramSocket lanDatagramSocket, String aytMsg) {
		this.lanDatagramSocket=lanDatagramSocket;
		this.aytMsg=aytMsg;
	}
	
	/***
	 * @brief  Get a list of interface IP addresses we can send for the interfaces
	 *         on this machine.
	 * @return A Vector of Strings each element of which is the IPV4 IP address.
	 */
	public static Vector<String> GetMulticastIPAddressList() {
		
		Vector<String> multicastIPList  = new Vector<String>();

		try {
			Enumeration e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements())
			{
			    NetworkInterface networkInterface = (NetworkInterface) e.nextElement();
			    
			    if( !networkInterface.isLoopback() && networkInterface.isUp() && networkInterface.supportsMulticast() && !networkInterface.isVirtual()) {
			    
                    Iterator it = networkInterface.getInterfaceAddresses().iterator();
                    while (it.hasNext()) {
                        InterfaceAddress address = (InterfaceAddress) it.next();
                        InetAddress multicastAddress = address.getBroadcast();
                        if(multicastAddress != null) 
                        {
                            multicastIPList.add(multicastAddress.getHostAddress());
                        }
                    }
			    }
			}
		}
		catch(SocketException e) {}

		return multicastIPList;
	}
	
	/**
	 * @brief Define the run time for the AYT TX process.
	 *        By default the AYT TX process runs until shutdown() is called.
	 * @param seconds The number of seconds the process should run for.
	 */
	public void setRuntime(int seconds) {
		this.runTimeSeconds = seconds;
	}
	
	/**
	 * @brief Thread method runs until shutdown() is called.
	 */
	public void run() {
		long activeTimeSeconds = 0;
		long nowMs;
		InetAddress mcIPAddress;
		running = true;
		startTimeMS = System.currentTimeMillis();
		
		
		Vector<String> multicastIPList = GetMulticastIPAddressList();
		
	    try {
		    byte[] msg = this.aytMsg.getBytes();
		    
			while(running) {

				//Send the broadcast on each interface capable of sending multicast messages
				for( String ipAddress : multicastIPList ) {
				    DatagramPacket packet = new DatagramPacket(msg, msg.length);
				    packet.setPort(Constants.UDP_MULTICAST_PORT);
					mcIPAddress = InetAddress.getByName(ipAddress);
				    packet.setAddress(mcIPAddress);
				    lanDatagramSocket.send(packet);
				}
			    
				//Wait between TX of multicast messages
				Thread.sleep(Constants.DEFAULT_AYT_PERIOD_MS);
				
				nowMs = System.currentTimeMillis();
				activeTimeSeconds = (nowMs-startTimeMS)/1000;

				//If it's time to shutdown the AYT TX message process.
				if(runTimeSeconds != 0 &&  activeTimeSeconds > runTimeSeconds ) {
					running = false;
				}
			}
		
	    }
	    catch(InterruptedException e) {
	    	e.printStackTrace();
	    }
	    catch(IOException e) {
	    	e.printStackTrace();
	    }
	    finally {
	    	if( lanDatagramSocket != null ) {
	    		lanDatagramSocket.close();
	    		lanDatagramSocket = null;
	    	}
	    }
	    
	}
			
	/**
	 * @brief Shutdown the AYT thread. 
	 *        After this is called the AYT thread will shutdown up to DEFAULT_AYT_PERIOD_MS later. 
	 */
	public void shutdown() {
		running = false;
	}

}
