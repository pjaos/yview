package uk.me.pausten.yview.controller;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InterfaceAddress;
import uk.me.pausten.yview.model.Constants;

/**
 * @brief Responsible for sending broadcast messages to elicit a response from devices.
 */
public class AreYouThereTransmitter {

    private String aytMsgContents = Constants.AYT_MESSAGE_CONTENTS;
    /***
     * @brief   Get a list of Multicast IP addresses for this machine.
     *          This will not include localhost or any virtual interfaces.
     *          All interfaces returned support sending multicast addresses.
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
        catch(SocketException e) {
        }

        return multicastIPList;
    }

    /**
     * @brief Send an are you there broadcast message.
     * @param lanDatagramSocket The socket to send the broadcast messages on.
     */
    public void sendAYTMessage(DatagramSocket lanDatagramSocket) throws IOException {
        InetAddress mcIPAddress;

        Vector<String> multicastIPList = GetMulticastIPAddressList();

//        try {
            String aytMsg = "{\"AYT\":\""+aytMsgContents+"\"}";
            byte[] msg = aytMsg.getBytes();

            //Send the broadcast on each interface capable of sending multicast messages
            for( String ipAddress : multicastIPList ) {
                DatagramPacket packet = new DatagramPacket(msg, msg.length);
                packet.setPort(Constants.UDP_MULTICAST_PORT);
                mcIPAddress = InetAddress.getByName(ipAddress);
                packet.setAddress(mcIPAddress);
                lanDatagramSocket.send(packet);
            }
/*
        }
        catch(IOException e) {
            e.printStackTrace();
        }
*/
    }

    /**
     * @brief Set the contents of the AYT message
     * @param aytMsgContents The AYT message contents string.
     */
    public void setAYTMsgContents(String aytMsgContents) {
        this.aytMsgContents=aytMsgContents;
    }

}
