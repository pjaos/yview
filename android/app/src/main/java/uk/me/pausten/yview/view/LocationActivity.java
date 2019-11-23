package uk.me.pausten.yview.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import android.content.pm.PackageManager;

import java.util.Vector;
import java.util.Hashtable;
import java.net.ServerSocket;
import java.io.IOException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.json.JSONObject;
import org.json.JSONException;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.controller.JSONProcessor;
import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.controller.LocationListener;
import uk.me.pausten.yview.model.Service;

public class LocationActivity extends AppCompatActivity implements OnClickListener, LocationListener {
    public static final int             COL_PADDING_LEFT            = 10;
    public static final int             COL_PADDING_TOP             = 10;
    public static final int             COL_PADDING_RIGHT           = 10;
    public static final int             COL_PADDING_BOTTOM          = 10;
    public static final int             TABLE_GRAVITY               = Gravity.CENTER;
    public static final int             TABLE_FONT_SIZE             = 24;

    Handler                             uiHandler;
    TableLayout                         deviceTable;
    int                                 toolBarBackgroundColor;
    String                              location;
    boolean                             activityVisible;
    Hashtable<String, Integer>          remoteLocalPortHashtable;
    Vector<String>                      serviceList = new Vector<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_table);

        location = getIntent().getStringExtra(Constants.DEVICE_LIST_ACTIVITY);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.yview_icon);
        toolbar.setSubtitle(location);
        toolBarBackgroundColor = ((ColorDrawable) toolbar.getBackground()).getColor();

        remoteLocalPortHashtable = new Hashtable<String, Integer>();

        // Defines a Handler object that's attached to the UI thread
        uiHandler = new Handler(Looper.getMainLooper()) {

            /*
            * handleMessage() defines the operations to perform when
            * the Handler receives a new Message to process.
            */
            @Override
            public void handleMessage(Message inputMessage) {
                updateTable();
            }
        };

        updateTable();
    }

    /**
     * Called just before the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        MainActivity.Log("LocationActivity.onDestroy()");
    }

    /**
     * Called when the activity is about to become visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        enableLocationListener(true);
    }

    /**
     * Called when the activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        MainActivity.Log("LocationActivity.onStop()");
        enableLocationListener(false);
    }

    /**
     * Called when the activity has become visible.
     */
    @Override
    protected void onResume() {
        super.onResume();

        //If the ICONS connection has been disabled and we are not looking at the LAN go back to the main activity.
        if( !location.equals(Constants.LOCAL_LOCATION) && !MainActivity.IconsConnectionActive(this) ) {

            finish();

        }
        else {
            activityVisible = true;
        }
    }

    /**
     * Called when another activity is taking focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        activityVisible = false;
    }

    private void enableLocationListener(boolean enabled) {
        if( MainActivity.NetworkManager != null ) {
            if (enabled) {
                MainActivity.NetworkManager.addLocationListener(this);
            } else {
                MainActivity.NetworkManager.removeLocationListener(this);
            }
        }
    }

    /**
     * Called from the Network manager thread
     * @param updatedLocation
     */
    public void updated(String updatedLocation) {
        if( activityVisible ) {
            if( updatedLocation.equals(location) ) {
                Message completeMessage = uiHandler.obtainMessage(Constants.LOCATION_UPDATED_MSG_ID, location);
                completeMessage.sendToTarget();
            }
        }
    }

    /**
     * @brief Get the Group name preference
     * @return true if ssh connection is enabled.
     */
    public static String GetGroupNamePreference(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        return sharedPreferences.getString( activity.getResources().getString(R.string.pref_group), "" );
    }

    /**
     * @brief Called to update the view of the devices
     */
    public void updateTable() {
        TableRow row;
        TextView ttc1, ttc2, ttc3, ttc4, c1, c2, c3, c4;

        deviceTable = (TableLayout) findViewById(R.id.location_table);

        deviceTable.removeAllViews();

        if( MainActivity.NetworkManager == null ) {
            return;
        }

        row = new TableRow(this);

        //Add the table title lines
        ttc1 = new TextView(this);
        ttc1.setText("Type");

        ttc1.setGravity(TABLE_GRAVITY);
        ttc1.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
        ttc1.setTextSize(TABLE_FONT_SIZE);
        ttc1.setBackgroundColor(toolBarBackgroundColor);

        ttc2 = new TextView(this);
        ttc2.setText("Name");
        ttc2.setGravity(TABLE_GRAVITY);
        ttc2.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
        ttc2.setTextSize(TABLE_FONT_SIZE);
        ttc2.setBackgroundColor(toolBarBackgroundColor);

        ttc3 = new TextView(this);
        ttc3.setText("Info");
        ttc3.setGravity(TABLE_GRAVITY);
        ttc3.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
        ttc3.setTextSize(TABLE_FONT_SIZE);
        ttc3.setBackgroundColor(toolBarBackgroundColor);

        row.addView(ttc1);
        row.addView(ttc2);
        row.addView(ttc3);

        //Get current screen orientation
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        //In landscape mode we show an extra column
        if (rotation == Surface.ROTATION_90) {
            ttc4 = new TextView(this);
            ttc4.setText("IP Address");
            ttc4.setGravity(TABLE_GRAVITY);
            ttc4.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
            ttc4.setTextSize(TABLE_FONT_SIZE);
            ttc4.setBackgroundColor(toolBarBackgroundColor);

            row.addView(ttc4);

        }

        deviceTable.addView(row);
        addSeparator(5);

        String groupNamePreference = GetGroupNamePreference(this);

        if( MainActivity.NetworkManager != null ) {

            Vector<JSONObject> deviceList = MainActivity.NetworkManager.getDeviceList(location);

            if (deviceList != null) {
                deviceList = (Vector<JSONObject>) deviceList.clone();
                for (JSONObject device : deviceList) {
                    //If the user has entered a group preference and the group name of this device does not match then don't display this device.
                    if (groupNamePreference.length() > 0 && !groupNamePreference.equals(JSONProcessor.GetGroupName(device))) {
                        continue;
                    }

                    row = new TableRow(this);

                    c1 = new TextView(this);
                    c1.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
                    c1.setTextSize(TABLE_FONT_SIZE);

                    c2 = new TextView(this);
                    c2.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
                    c2.setTextSize(TABLE_FONT_SIZE);

                    c3 = new TextView(this);
                    c3.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
                    c3.setTextSize(TABLE_FONT_SIZE);

                    c1.setText( JSONProcessor.GetProductID(device) );
                    c2.setText( JSONProcessor.GetUnitName(device) );

                    String jsonPowerWatts = JSONProcessor.GetPowerWatts(device);
                    String serConf = JSONProcessor.GetSerialConfigString(device);
                    String infoStr = "";

                    if( JSONProcessor.GetProductID(device).equals(Constants.WYTERM_PRODUCT_ID) ) {
                        infoStr = serConf;
                    }
                    else if( JSONProcessor.GetProductID(device).equals(Constants.WYSW_PRODUCT_ID) ||
                            JSONProcessor.GetProductID(device).equals(Constants.WYSWITCH2_PRODUCT_ID) ) {
                        infoStr = getPowerKiloWattsString(jsonPowerWatts);
                    }
                    else if ( serConf != null ) {
                        infoStr = serConf;
                    }
                    c3.setText( infoStr );

                    c1.setGravity(TABLE_GRAVITY);
                    c2.setGravity(TABLE_GRAVITY);
                    c3.setGravity(TABLE_GRAVITY);

                    row.addView(c1);
                    row.addView(c2);
                    row.addView(c3);

                    //In landscape mode we show some extra columns
                    if (rotation == Surface.ROTATION_90) {

                        c4 = new TextView(this);
                        c4.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
                        c4.setTextSize(TABLE_FONT_SIZE);

                        c4.setText( JSONProcessor.GetIPAddress(device) );

                        c4.setGravity(TABLE_GRAVITY);

                        row.addView(c4);

                    }

                    row.setClickable(true);
                    row.setOnClickListener(this);
                    row.setTag(device);

                    deviceTable.addView(row);
                    addSeparator(1);
                }
            }
        }
    }

    /**
     * @brief Get the power in KW
     * @param powerWatts The value in watts
     * @return The Information String for power in KW
     */
    private String getPowerKiloWattsString(String powerWatts) {
        String pwrKWStr = "";
        try {
            int powerWattsInt = Integer.parseInt(powerWatts);
            pwrKWStr = ((float)powerWattsInt)/1000F + " kW";
        }
        catch(NumberFormatException e) {}

        return pwrKWStr;
    }

    /**
     * Add a separator line to the table
     * @param width The width of the separator line.
     */
    private void addSeparator(int width) {
        View line = new View(this);
        line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width));
        line.setBackgroundColor(Color.rgb(51, 51, 51));
        deviceTable.addView(line);
    }

    /**
     * Launch a browser connected to the selected Wy* unit
     *
     * @param v The View selected by the user in the bale of devices
     */
    public void onClick(View v) {
        TableRow row = (TableRow) v;
        final JSONObject device = (JSONObject) row.getTag();

        String services = JSONProcessor.GetServicesList(device);
        serviceList.removeAllElements();

        if( services != null ) {
            if ( services.toUpperCase().indexOf(Constants.HTTP_SERVICE_NAME) >= 0 || services.toUpperCase().indexOf(Constants.WEB_SERVICE_NAME) >= 0) {
                serviceList.add(Constants.WEB_SERVICE_DISPLAY_NAME);
            }
            if ( services.toUpperCase().indexOf(Constants.VNC_SERVICE_NAME) >= 0) {
                serviceList.add(Constants.VNC_SERVICE_DISPLAY_NAME);
            }
            if ( services.toUpperCase().indexOf(Constants.SERIAL_PORT_SERVICE_NAME) >= 0) {
                serviceList.add(Constants.SERIAL_PORT_DISPLAY_NAME);
            }
            if ( services.toUpperCase().indexOf(Constants.SSH_SERVICE_NAME) >= 0) {
                serviceList.add(Constants.SSH_SERVICE_DISPLAY_NAME);
            }
        }

        serviceList.add("Cancel");

        MainActivity.Log("onClick(): serviceList="+serviceList);

        //If this device supports any services we maybe able to connect to.
        if( serviceList.size() > 0 ) {
            //If the device only offers one service launch it now.
            if( serviceList.size() == 2 ) {
                String service = serviceList.get(0);

                if (service.equals(Constants.WEB_SERVICE_DISPLAY_NAME)) {

                    handleDevice(device, Constants.HTTP_SERVICE_NAME);

                } else if (service.equals(Constants.VNC_SERVICE_DISPLAY_NAME)) {

                    handleDevice(device, Constants.VNC_SERVICE_NAME);

                } else if (service.equals(Constants.SERIAL_PORT_DISPLAY_NAME)) {

                    handleDevice(device, Constants.SERIAL_PORT_SERVICE_NAME);

                } else if (service.equals(Constants.SSH_SERVICE_DISPLAY_NAME)) {

                    handleDevice(device, Constants.SSH_SERVICE_NAME);

                }
            }
            else {
                Dialogs.OptionsDialog(this,
                        "Select the service required.",
                        serviceList.toArray(new String[0]),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which < serviceList.size()) {
                                    String service = serviceList.get(which);

                                    if (service.equals(Constants.WEB_SERVICE_DISPLAY_NAME)) {

                                        handleDevice(device, Constants.HTTP_SERVICE_NAME);

                                    } else if (service.equals(Constants.VNC_SERVICE_DISPLAY_NAME)) {

                                        handleDevice(device, Constants.VNC_SERVICE_NAME);

                                    } else if (service.equals(Constants.SERIAL_PORT_DISPLAY_NAME)) {

                                        handleDevice(device, Constants.SERIAL_PORT_SERVICE_NAME);

                                    } else if (service.equals(Constants.SSH_SERVICE_DISPLAY_NAME)) {

                                        handleDevice(device, Constants.SSH_SERVICE_NAME);

                                    }

                                }

                            }

                        });
            }
        }
    }

    /**
     * @brief Handle the user selecting an application to connect to a device.
     * @param device The device object selected in the table by the user
     * @param serviceName The name of the service.
     */
    public void handleDevice(JSONObject device, String serviceName) {

        if( isLocalDevice(device) ) {
            handleLocalDevice(device, serviceName);
        }
        else {
            handleRemoteDevice(device, serviceName);
        }
    }

    /**
     * @brief If a device has been selected by the user is from the LAN table then handle
     *        connecting to the device here.
     * @param device The device to connect to.
     * @param serviceName The name of the service.
     */
    private void handleLocalDevice(JSONObject device, String serviceName) {
        if( serviceName.equals(Constants.HTTP_SERVICE_NAME) ) {
            startLocalWebBrowser(device);
        }
        else if( serviceName.equals(Constants.SERIAL_PORT_SERVICE_NAME) ) {

            startLocalTerminal(device);
        }
    }

    /**
     * @brief Open a Web broswer to a device connected to the local LAN.
     *
     * @param device The Device object that holds the unit details
     */
    void startLocalWebBrowser(JSONObject device) {
        String ipAddress = JSONProcessor.GetIPAddress(device);
        Toast.makeText(getApplicationContext(), "Opening browser @ " + ipAddress + ", please wait...", Toast.LENGTH_LONG).show();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ipAddress));
        startActivity(browserIntent);
    }

    /**
     * @brief Open a terminal session to a device connected to the local LAN.
     *
     * @param device The Device object that holds the unit details
     */
    void startLocalTerminal(JSONObject device) {
        String ipAddress = JSONProcessor.GetIPAddress(device);
        Toast.makeText(getApplicationContext(), "Opening terminal connection to " + ipAddress + ", please wait...", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, TermActivity.class);
        intent.putExtra(Constants.TERM_ACTIVITY_YTERM_ADDRESS, ipAddress);
        startActivity(intent);
    }

    /**
     * @brief Get the key format string for the location and serverPort.
     * @param location
     * @param serverPort
     * @return The string concatenating location and serverPort.
     */
    private String getRemoteLocalPortHashtableKey(String location, int serverPort) {
        return location+serverPort;
    }

    /**
     * @brief Get the local host forwarding port associated with the serverPort.
     *        If setup previously then we read this from storage. If not setup previously
     *        then a port is allocated.
     * @param location The location of the device.
     * @param serverPort
     * @return The local host forwarding port.
     */
    private int getLocalhostForwardingPort(String location, int serverPort) {
        int localhostPort = -1;
        String key =  getRemoteLocalPortHashtableKey(location, serverPort);

        if( remoteLocalPortHashtable.containsKey(key) ) {
            localhostPort = remoteLocalPortHashtable.get(key);
            MainActivity.Log("getLocalhostForwardingPort(): Using previously allocated local port="+localhostPort);
        }
        else {
            try {
                ServerSocket s = new ServerSocket(0);
                localhostPort = s.getLocalPort();
                s.close();
            } catch (IOException e) {}
            MainActivity.Log("getLocalhostForwardingPort(): Assigning new local port="+localhostPort);
        }
        return localhostPort;
    }

    /**
     * @brief Check if local host forwarding port associated with the serverPort has already been setup.
     * @param location The location of the device.
     * @param serverPort
     * @param localhostPort
     * @return true if already setup, false if not.
     */
    private boolean isPortForwardingAlreadySetup(String location, int serverPort, int localhostPort) {
        boolean portForwardingAlreadySetup = false;
        String key =  getRemoteLocalPortHashtableKey(location, serverPort);

        if ( remoteLocalPortHashtable.containsKey(key)) {
            int storedLocalhostPort = remoteLocalPortHashtable.get(key);
            if (storedLocalhostPort == localhostPort) {
                portForwardingAlreadySetup=true;
            }
        }
        return portForwardingAlreadySetup;
    }

    /**
     * @brief Store the local host forwarding port associated with the serverPort. This should be
     *        called once port forwarding has been setup.
     * @param location The location of the device.
     * @param serverPort
     * @param localhostPort
     */
    private void storeLocalhostForwardingPort(String location, int serverPort, int localhostPort) {
        String key =  getRemoteLocalPortHashtableKey(location, serverPort);

        if( remoteLocalPortHashtable.containsKey(key) ) {
            int storedLocalhostPort = remoteLocalPortHashtable.get(key);
            if( storedLocalhostPort != localhostPort ) {
                MainActivity.Log("ERROR serverPort="+serverPort+" should be associated with localhostPort="+localhostPort+" but is associated with storedLocalhostPort="+storedLocalhostPort);
            }
        }
        else {
            remoteLocalPortHashtable.put(key, localhostPort);
        }
    }

    /**
     * @brief If a device has been selected by the user is not from the LAN table then handle
     *        connecting to the device here. This involves connecting through the local and reverse
     *        ssh port forwarding connections via the ICON (Internet connection) server.
     * @param device The device to connect to.
     * @param serviceName The name of the service.
     */
    private void handleRemoteDevice(JSONObject device, String serviceName) {
        int localPort=-1;

        MainActivity.Log("handleRemoteDevice(): device     ="+device);
        MainActivity.Log("handleRemoteDevice(): serviceName="+serviceName);

        try {

            Service[] serviceArray = Service.GetServiceList( JSONProcessor.GetServicesList(device) );

            for( Service service : serviceArray ) {

                String location = JSONProcessor.GetLocation(device);
                localPort = getLocalhostForwardingPort(location, service.port );
                MainActivity.Log("handleRemoteDevice(): localPort="+localPort);
                if (localPort != -1) {

                    if( !isPortForwardingAlreadySetup(location, service.port, localPort) ) {

                        //Setup forwarding from a local TCP server port to the remote ssh server port = that connected to the remote device.
                        Session session = JSONProcessor.GetICONSSSHSession(device);
                        if( session != null ) {
                            MainActivity.Log("Setting up port forwarding from local port "+localPort+" to "+Constants.LOCAL_HOST+":"+service.port);
                            session.setPortForwardingL(Constants.LOCAL_HOST, localPort, Constants.LOCAL_HOST, service.port);
                        }
                        else {
                            try {
                                MainActivity.Log("No session object found for: " + device.toString(4));
                            }
                            catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        storeLocalhostForwardingPort(location, service.port, localPort);

                    }

                    if( (service.serviceName.toUpperCase().equals(Constants.WEB_SERVICE_NAME) || service.serviceName.equals(Constants.HTTP_SERVICE_NAME)) && serviceName.equals(Constants.HTTP_SERVICE_NAME) ) {

                        startIntent("http", localPort, JSONProcessor.GetUnitName(device) );
                        break;

                    }
                    else if( service.serviceName.toUpperCase().equals(Constants.SERIAL_PORT_SERVICE_NAME) && serviceName.equals(Constants.SERIAL_PORT_SERVICE_NAME) ) {

                        startRemoteTerminal(localPort, JSONProcessor.GetUnitName(device) );
                        break;

                    }
                    else if( service.serviceName.toUpperCase().equals(Constants.VNC_SERVICE_NAME) && serviceName.equals(Constants.VNC_SERVICE_NAME) ) {

                        startIntent("vnc", localPort, JSONProcessor.GetUnitName(device) );
                        break;

                    }
                    else if( service.serviceName.toUpperCase().equals(Constants.SSH_SERVICE_NAME) && serviceName.equals(Constants.SSH_SERVICE_NAME) ) {

                        startIntent("ssh", localPort, JSONProcessor.GetUnitName(device) );
                        break;

                    }

                }
            }
        }
        catch(JSchException e) {
        }
    }

    /**
     * @brief Open a Web broswer to a device connected to a remote network.
     * @param scheme The type of connection (E.G http)
     * @param localTCPPort The TCP port (on localhost) that will connect through to the web server on the device.
     * @param  deviceName The name of the device to connect to.
     */
    void startIntent(String scheme, int localTCPPort, String deviceName) {
        String uriString = scheme+"://" + Constants.LOCAL_HOST+":"+localTCPPort+"/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString) );
        MainActivity.Log("startIntent(): "+intent);
        PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            Toast.makeText(getApplicationContext(), "Opening "+scheme.toUpperCase()+" connection to "+deviceName+".", Toast.LENGTH_LONG).show();
            startActivity(intent);
        } else {
            String errString = "No application installed to handle "+uriString;
            MainActivity.Log("startIntent(): "+errString);
            Toast.makeText(getApplicationContext(), errString, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * @brief Open a terminal session to a device connected to a remote network.
     *
     * @param localTCPPort The TCP port (on localhost) that will connect through to the terminal interface server on the device.
     * @param  deviceName The name of the device to connect to.
     */
    void startRemoteTerminal(int localTCPPort, String deviceName) {
        Toast.makeText(getApplicationContext(), "Opening Terminal connection to "+deviceName+".", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, TermActivity.class);
        intent.putExtra(Constants.TERM_ACTIVITY_YTERM_ADDRESS, Constants.LOCAL_HOST+":"+localTCPPort);
        startActivity(intent);
    }

    /**
     * @brief Determine if a device is on the LAN or remote
     * @return true if on the LAN, else False
     */
    private  boolean isLocalDevice(JSONObject device) {

        if( JSONProcessor.GetLocation( device ).equals(Constants.LOCAL_LOCATION) ) {
            return true;
        }
        return false;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

