package uk.me.pausten.yview.controller;

import 	android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.io.*;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.view.MainActivity;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;

import org.json.JSONObject;

/**
 * @brief Responsible for managing all networking connections used by YView. This includes
 * - The Are You There (AYT) message (UDP) transmitter to see if any devices are out there.
 * - The server (UDP) listening for responses from devices.
 * - The SSH connections to ICON server/s.
 */
public class NetworkingManager extends Thread implements JSONListener {
    Timer                               deviceTimeoutTimer;
    DatagramSocket                      lanDatagramSocket;
    LanDeviceReceiver                   lanDeviceReceiver;
    AreYouThereTransmitter              aytTransmitter;
    Hashtable<String,   Vector<JSONObject>> locationHashtable = new Hashtable<String, Vector<JSONObject>>();
    Timer                               aytTXTimer;
    boolean                             active;
    boolean                             iconsConnectionEnabled;
    Vector<LocationListener>            locationListenerList;
    Activity                            activity;
    ICONSConnection                     iconsConnection;
    SharedPreferences                   sharedPreferences;

    /**
     * @brief Constructor.
     * @param activity The associated activity referrence.
     * @param iconsConnectionEnabled If true then ICONS connection will be started initially.
     */
    public NetworkingManager(Activity activity, boolean iconsConnectionEnabled) {
        this.activity=activity;
        this.iconsConnectionEnabled = iconsConnectionEnabled;

        if( this.activity != null ) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        }
        locationListenerList = new Vector<LocationListener>();
    }

    public void run() {
        MainActivity.Log("START NETWORKING");

        active=true;
        try {

            startLocalNetworking();

            if( iconsConnectionEnabled ) {
                startRemoteNetworking();
            }

            int runCount=0;
            while(active) {

                synchronized(this) {
                    try {

                        wait();

                        if ( iconsConnectionEnabled ) {

                            startRemoteNetworking();

                        } else if (!iconsConnectionEnabled ) {

                            stopRemoteNetworking();

                        }

                    } catch (InterruptedException e) {

                        e.printStackTrace();

                    }
                }

            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        finally {
            shutDown();
        }

    }

    /**
     * @brief Shutdown all networking.
     */
    public void shutDown() {
        MainActivity.Log("SHUTDOWN ALL NETWORKING");
        active=false;
        stopRemoteNetworking();
        stopLocalNetworking();
        if( lanDatagramSocket != null ) {
            lanDatagramSocket.close();
        }
    }

    /**
     * @brief Start all networking associated with detecting devices on the LAN (local to the subnet that
     *        this device is connected to).
     *
     * @throws SocketException
     */
    private void startLocalNetworking() throws SocketException {
        MainActivity.Log("startLocalNetworking()");
        if( lanDatagramSocket != null ) {
            return;
        }

        try {
            lanDatagramSocket = new DatagramSocket(Constants.UDP_MULTICAST_PORT);
            lanDeviceReceiver = new LanDeviceReceiver(lanDatagramSocket);
            lanDeviceReceiver.addDeviceListener(this);
            lanDeviceReceiver.start();

            aytTransmitter = new AreYouThereTransmitter();

            //Setup a timer to timeout units we have lost contact with
            aytTXTimer = new Timer();
            AYTTXTask aytTXTask = new AYTTXTask();
            aytTXTimer.schedule(aytTXTask, Constants.DEFAULT_AYT_PERIOD_MS, Constants.DEFAULT_AYT_PERIOD_MS);

            //Setup a timer to timeout units we have lost contact with
            deviceTimeoutTimer = new Timer();
            DeviceTimeoutTask deviceTimeoutTask = new DeviceTimeoutTask();
            deviceTimeoutTimer.schedule(deviceTimeoutTask, Constants.DEVICE_TIMEOUT_PERIOD_MS, Constants.DEVICE_TIMEOUT_PERIOD_MS);

        }
        catch(IOException e ) {
            MainActivity.Log("LAN ERROR: "+e.getLocalizedMessage());
        }
    }

    /**
     * @brief Start all networking associated with detecting devices on networks connected to the
     *        ICON (internet connection) server.
     */
    private void startRemoteNetworking()  {

        if( activity != null && sharedPreferences != null ){
            boolean active = sharedPreferences.getBoolean( activity.getResources().getString(R.string.pref_server_active), false );
            if( active ) {

                String serverUsername = sharedPreferences.getString(activity.getResources().getString(R.string.pref_server_username), "");
                String serverAddress = sharedPreferences.getString(activity.getResources().getString(R.string.pref_server_address), "");
                int serverPort = sharedPreferences.getInt(activity.getResources().getString(R.string.pref_server_port), Constants.DEFAULT_SSH_SERVER_PORT);

                iconsConnection = new ICONSConnection(serverUsername, serverAddress, serverPort, this);
                iconsConnection.setActivity(activity);
                iconsConnection.start();

            }
        }
    }

    /**
     * @brief Stop all networking associated with detecting devices on the LAN (local to the subnet that
     *        this device is connected to).
     */
    private void stopLocalNetworking() {
        if( lanDeviceReceiver == null ) {
            return;
        }

        lanDeviceReceiver.shutdown();
        lanDeviceReceiver=null;

        if (aytTransmitter != null) {
            aytTransmitter = null;
        }

        if (aytTXTimer != null) {
            aytTXTimer.cancel();
            aytTXTimer = null;
        }

        if( deviceTimeoutTimer != null ) {
            deviceTimeoutTimer.cancel();
            deviceTimeoutTimer=null;
        }

    }

    private void stopRemoteNetworking() {

        if( iconsConnection != null ) {

            iconsConnection.shutdown();
            locationHashtable.clear();
            iconsConnection = null;

        }
    }

    /**
     * @brief Task called periodically to check if we've lost contact with devices.
     */
    class DeviceTimeoutTask extends TimerTask {

        public void run() {
            timeoutOldUnits();
        }

    }

    /**
     * @brief Task called periodically to broadcast and AYT message.
     */
    class AYTTXTask extends TimerTask {

        public void run() {

            //If we should send device AYT messages
            if( aytTransmitter != null ) {
                aytTransmitter.sendAYTMessage(lanDatagramSocket);
            }

        }

    }

    /**
     * @brief Timeout units we have not heard from for a while.
     */
    public void timeoutOldUnits() {
        Vector<JSONObject> devicesTimedOut = new Vector<JSONObject>();
        long now = System.currentTimeMillis();

        Enumeration locationList = locationHashtable.keys();
        while( locationList.hasMoreElements() ) {
            boolean updateTable = false;
            String location = (String)locationList.nextElement();
            Vector<JSONObject> deviceList = locationHashtable.get(location);
            for( JSONObject device : deviceList ) {
                long msAgo = now - JSONProcessor.GetLocalRxTimeMs(device);
                if( msAgo > Constants.DEVICE_TIMEOUT_PERIOD_MS ) {
                    devicesTimedOut.add(device);
                    updateTable = true;

                }
            }
            for( JSONObject device : devicesTimedOut ) {
                deviceList.remove(device);
            }

        }
    }

    /**
     * @brief This is called or every message received from a device.
     * @param device
     */
    public void setJSONDevice(JSONObject device) {
        String location = JSONProcessor.GetLocation(device);
        if( location == null ) {
            return;
        }
        //If we don't know about this location
        if( !locationHashtable.containsKey(location)) {
            //Add an empty location
            locationHashtable.put(location, new Vector<JSONObject>() );
        }
        Vector<JSONObject> deviceList = locationHashtable.get(location);

        int deviceIndex = -1, index=0;
        for( JSONObject jsonDev : deviceList ) {
            String devIPAddress = JSONProcessor.GetIPAddress(jsonDev);
            String deviceIPAddress = JSONProcessor.GetIPAddress(device);
            if( devIPAddress.equals(deviceIPAddress)) {
                deviceIndex=index;
                break;
            }
            index++;
        }
        if( deviceIndex >= 0 ) {
            deviceList.set(deviceIndex, device);
        }
        else {
            deviceList.add(device);
        }

        //Notify all location listeners of the update now that the device list has been updated.
        if (locationListenerList != null) {
            for (LocationListener locationListener : locationListenerList) {
                locationListener.updated(location);
            }
        }

    }

    /**
     * @brief Enable / Disable the ICON server connection.
     *
     * @param iconsConnectionEnabled If true then activate networking connections. If false disconnect networking connections.
     */
    public void enableICONSConnection(boolean iconsConnectionEnabled) {
        if( this.iconsConnectionEnabled == iconsConnectionEnabled ) {
            return;
        }
        synchronized(this) {
            this.iconsConnectionEnabled = iconsConnectionEnabled;
            this.notifyAll();
        }
    }

    /**
     * @brief Read the enable statuc of the network manager.
     * @return If true the ICON server connection is enabled.
     */
    public boolean isICONSConnectionEnabled() { return iconsConnectionEnabled; }

    /**
     * @brief Called to shutdown networking when the App is shutdown
     */
    /*
    public void shutDown() {
        active=false;
    }
    */

    /**
     * @brief Get a list of devices at the given location.
     * @param location
     * @return A Vector of Device objects
     */
    public Vector<JSONObject> getDeviceList(String location) {
        return locationHashtable.get(location);
    }

    /**
     * @brief Return a list of all the locations that we are aware of.
     * @return A Vector of strings of the names of all locations.
     */
    public Vector<String> getLocationList() {
        Vector<String> locations = new Vector<String>();

        Enumeration locationList = locationHashtable.keys();
        while( locationList.hasMoreElements() ) {
            String location = (String) locationList.nextElement();
            locations.add(location);
        }
        return locations;
    }

    /**
     * @brief Add to the list of deviceListener objects.
     * @param locationListener The location listener object to revice notification of location device list changes.
     */
    public void addLocationListener(LocationListener locationListener) {
        locationListenerList.add(locationListener);
    }

    /**
     * @brief Remove a deviceListener object from the list of of deviceListener objects.
     * @param locationListener The location listener object to revice notification of location device list changes.
     */
    public void removeLocationListener(LocationListener locationListener) {
        locationListenerList.remove(locationListener);
    }

    /**
     * @brief Remove all locationListener objects
     */
    public void removeAllDeviceLisenters() {
        locationListenerList.removeAllElements();
    }

}
