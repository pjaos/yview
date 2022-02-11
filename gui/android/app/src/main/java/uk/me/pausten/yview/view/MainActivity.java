package uk.me.pausten.yview.view;

import android.content.Intent;
import 	android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.graphics.drawable.ColorDrawable;
import android.widget.Toast;
import android.content.SharedPreferences;
import 	android.app.Activity;
import android.app.AlertDialog;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Vector;
import java.net.SocketException;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.controller.JSONProcessor;
import uk.me.pausten.yview.controller.LocationListener;
import uk.me.pausten.yview.controller.NetworkingManager;
import uk.me.pausten.yview.model.Constants;

public class MainActivity extends AppCompatActivity implements OnClickListener, LocationListener, StringInputListener {
    public static final int             COL_PADDING_LEFT            = 10;
    public static final int             COL_PADDING_TOP             = 10;
    public static final int             COL_PADDING_RIGHT           = 10;
    public static final int             COL_PADDING_BOTTOM          = 10;
    public static final int             TABLE_GRAVITY               = Gravity.CENTER;
    public static final int             TABLE_FONT_SIZE             = 24;
    private static String               AppStorageFolder;
    private AlertDialog                 enterAYTMsgDialog;

    public static NetworkingManager     NetworkManager;

    TableLayout                         locationTable;
    int                                 toolBarBackgroundColor;
    Handler                             uiHandler;
    boolean                             activityVisible;
    boolean                             removeLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_table);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.yview_icon);
        //toolbar.setSubtitle("Locations");
        toolBarBackgroundColor = ((ColorDrawable) toolbar.getBackground()).getColor();
        MainActivity.Log("MainActivity.onCreate()");
        MainActivity.AppStorageFolder = getApplicationContext().getFilesDir().getAbsolutePath();

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
    }

    /**
     * Called just before the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        MainActivity.Log("MainActivity.onDestroy()");
        MainActivity.ShutDownNetworkManager();
    }

    /**
     * @brief Ensure that the location manager instance is present and running.
     */
    private void ensureNetworkManagerExists() {
        if( NetworkManager == null ) {
            MainActivity.Log("startNetworkingManager(");
            NetworkManager = new NetworkingManager(this, IconsConnectionActive(this));
            NetworkManager.setDaemon(true);
            NetworkManager.start();
	    NetworkManager.enableICONSConnection( IconsConnectionActive(this) );

            NetworkManager.removeAllDeviceLisenters();
            NetworkManager.addLocationListener(this);
        }
    }

    private static void ShutDownNetworkManager() {
        if( NetworkManager != null ) {
            NetworkManager.shutDown();
            NetworkManager.removeAllDeviceLisenters();
            NetworkManager = null;
        }
    }


    /**
     * Called when the activity is about to become visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        MainActivity.Log("MainActivity.onStart()");
        ensureNetworkManagerExists();
        //If not using an ICONS server allow the user to set the AYT message
        if( !NetworkManager.isICONSConnectionEnabled() ) {
            enterAYTMsgDialog = Dialogs.showInputDialog(this, "YView", Constants.AYT_MSG_PROMPT, Constants.AYT_MESSAGE_CONTENTS , false, this);
        }
    }

    /**
     * Called when the activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        MainActivity.Log("MainActivity.onStop()");
    }

    /**
     * @brief Check if the user preference has ssh (ICON server) connection enabled.
     * @return true if ssh connection is enabled.
     */
    public static boolean IconsConnectionActive(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        return sharedPreferences.getBoolean( activity.getResources().getString(R.string.pref_server_active), false );
    }

    /**
     * Called when the activity has become visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.Log("MainActivity.onResume()");
        activityVisible = true;

        ensureNetworkManagerExists();

        boolean networkManagerEnabled = NetworkManager.isICONSConnectionEnabled();

        //If networking is enabled and user has disabled ICONS connectionS
        if (networkManagerEnabled && !IconsConnectionActive(this)) {
            NetworkManager.enableICONSConnection(false);
        }

        //If networking is not enabled and the user has enabled the ICONS server.
        if (!networkManagerEnabled && IconsConnectionActive(this)) {
            NetworkManager.enableICONSConnection(true);
        }

    }

    /**
     * Called when another activity is taking focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.Log("MainActivity.onPause()");
        activityVisible = false;
    }

    //Debug messages logged here, used when debugging
    public static void Log(String message) {
        Log.e(Constants.APP_NAME, message);
    }

    /**
     * Called from the Network manager thread
     * @param updatedLocation
     */
    public void updated(String updatedLocation) {
        if( activityVisible ) {
            Message completeMessage = uiHandler.obtainMessage(Constants.LOCATION_UPDATED_MSG_ID, updatedLocation);
            completeMessage.sendToTarget();
        }
    }

    /**
     * @brief Called to update the location hashtable
     */
    public void updateTable() {
        TableRow row;
        TextView ttc1, c1;

        locationTable = (TableLayout) findViewById(R.id.location_table);

        locationTable.removeAllViews();
        if( removeLocations ) {
            removeLocations=false;
            return;
        }

        ttc1 = new TextView(this);
        ttc1.setText("Location");

        ttc1.setGravity(TABLE_GRAVITY);
        ttc1.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
        ttc1.setTextSize(TABLE_FONT_SIZE);
        ttc1.setBackgroundColor(toolBarBackgroundColor);

        row = new TableRow(this);
        row.addView(ttc1);
        addSeparator(5);
        locationTable.addView(row);

        Vector<String> locations = NetworkManager.getLocationList();
        for( String location : locations ) {
            if( NetworkManager.isICONSConnectionEnabled() ) {
                //If we have a configured ICONS then don't check for devices on the local LAN
                if (location.equals("LAN")) {
                    continue;
                }
            }

            Vector<JSONObject> locationDeviceList = NetworkManager.getDeviceList(location);

            row = new TableRow(this);

            c1 = new TextView(this);
            c1.setPadding(COL_PADDING_LEFT, COL_PADDING_TOP, COL_PADDING_RIGHT, COL_PADDING_BOTTOM);
            c1.setTextSize(TABLE_FONT_SIZE);

            c1.setText(location);

            c1.setGravity(TABLE_GRAVITY);

            row.addView(c1);
            row.setTag(locationDeviceList);
            row.setOnClickListener(this);

            locationTable.addView(row);

        }
    }

    /**
     * Add a separator line to the table
     * @param width The width of the separator line.
     */
    private void addSeparator(int width) {
        View line = new View(this);
        line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width));
        line.setBackgroundColor(Color.rgb(51, 51, 51));
        locationTable.addView(line);
    }

    /**
     * @brief When user selects a location open the activity for that location.
     * @param v The View selected by the user.
     */
    public void onClick(View v) {
        TableRow row = (TableRow) v;
        final Vector<JSONObject> deviceList = (Vector<JSONObject>) row.getTag();
        String location="";
        location = GetLocation(deviceList);

        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra(Constants.DEVICE_LIST_ACTIVITY, location);
        startActivity(intent);

    }

    /**
     * Get the location from a list of devices. This assumes that all devices
     * in the list are from the same location.
     *
     * @param deviceList
     * @return The location String
     */
    public static String GetLocation(Vector<JSONObject> deviceList) {
        String location="?";
        if( deviceList.size() > 0 ) {
            location = JSONProcessor.GetLocation( deviceList.get(0) );
        }
        return location;
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

    /**
     * @brief Get the storage folder where thios app may save files.
     * @return The storage folder
     * @throws IOException If unable to find the folder.
     */
    public static String GetAppStorageFolder() throws IOException {
        if (MainActivity.AppStorageFolder == null) {
            throw new IOException("MainActivity.AppStorageFolder not set !!!");
        }
        return MainActivity.AppStorageFolder;
    }

    @Override
    public void positiveInput(String title, String prompt, String input) {
        MainActivity.Log("PJA: input="+input);
        if( NetworkManager != null ) {
            MainActivity.Log("PJA: 1");
            NetworkManager.setAYTMsgContents(input);
            MainActivity.ShutDownNetworkManager();
            ensureNetworkManagerExists();
            removeLocations=true;
        }
    }

    @Override
    public void negativeInput(String title, String prompt) {

    }
}
