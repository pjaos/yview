package uk.me.pausten.yview.view;

import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.controller.SSHWrapper;
import uk.me.pausten.yview.model.Constants;

public class SettingsActivity extends AppCompatPreferenceActivity implements StringInputListener {

    Button                      testConnectionButton;
    Button                      deleteLocalSSHKeysButton;
    SharedPreferences           sharedPreferences;
    Handler                     uiHandler;
    AlertDialog                 enterServerPasswordDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        addPreferencesFromResource(R.xml.preferences);

        //Bind to value so that the current value is visible to the user before changing it
        //Also adds a change listener for the preference attribute.
        bindPreferenceSummaryToValue(findPreference( getResources().getString(R.string.pref_group) ));
        bindPreferenceSummaryToValue(findPreference( getResources().getString(R.string.pref_server_username) ));
        bindPreferenceSummaryToValue(findPreference( getResources().getString(R.string.pref_server_address) ));
        bindPreferenceSummaryToValue(findPreference( getResources().getString(R.string.pref_server_port) ));
        bindPreferenceSummaryToValue(findPreference( getResources().getString(R.string.pref_server_active) ));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        addTestConnectionButton();

        // Defines a Handler object that's attached to the UI thread
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                if( inputMessage.what == Constants.AUTH_FAIL_MSG_ID ) {
                    enterServerPassword();
                }
                else if ( inputMessage.what == Constants.SSH_CONNECTED_MSG_ID ) {
                    //Not currently used
                }
            }
        };

    }

    /**
     * @brief Setup the action bar for this activity.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * @brief Add the test SSH connection button to the settings activity.
     */
    private void addTestConnectionButton() {
        ListView v = getListView();
        MainActivity.Log("addTestConnectionButton()");
        testConnectionButton = new Button(this);
        testConnectionButton.setText(getResources().getString(R.string.test_connection_label));
        deleteLocalSSHKeysButton = new Button(this);
        deleteLocalSSHKeysButton.setText(getResources().getString(R.string.delete_ssh_key_label));

        testConnectionButton.setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
                testSShConnection(null);
            }
        });

        deleteLocalSSHKeysButton.setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
                askDeleteLocalSSHKey();
            }
        });

        //enable/disable button along with other connection parameters
        boolean active = sharedPreferences.getBoolean( getResources().getString(R.string.pref_server_active), false );
        testConnectionButton.setEnabled(active);
        deleteLocalSSHKeysButton.setEnabled(active);
        v.addFooterView(testConnectionButton);
        v.addFooterView(deleteLocalSSHKeysButton);

    }

    /**
     * @brief Add handler to return from settings using the back icon at the top left of the screen.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onStop() {
        super.onStop();
    }

    /**
     * @brief Update the state of the server active preference.
     * @param active
     */
    private void setServerActivePreference(boolean active) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getResources().getString(R.string.pref_server_active), active);
        editor.commit();
    }

    /**
     * @brief Called when any preference for which bindPreferenceSummaryToValue() has been called.
     */
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            //As we defined the test connection button outside the xml we need to update it's enabled status
            if( preference.getTitle().equals(getResources().getString(R.string.active_label)) ) {
                boolean sshActive = Boolean.parseBoolean(value.toString());
                testConnectionButton.setEnabled(sshActive);
                deleteLocalSSHKeysButton.setEnabled(sshActive);
            }
            else if( stringValue != null || stringValue.length() > 0 ) {
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    /**
     * @brief Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private void bindPreferenceSummaryToValue(Preference preference) {


        try {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
        catch(java.lang.ClassCastException e ) {
            try {
                //Server port throws cast exception os get an int
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext()).getInt(preference.getKey(), 1));
                //PJA
                e.printStackTrace();
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }







    /**
     * @brief Responsible for holding parameters required to test an ssh connection to a server.
     *        Instances of this class are passed to the TestConnection AsyncTask.
     */
    private static class TestConnectionParams {
        SettingsActivity    settingsActivity;
        String              username;
        String              server;
        int                 port;
        String              password;

        /**
         * @brief Define the parameters required to connect to the ICON ssh server.
         * @param settingsActivity
         * @param username
         * @param server
         * @param port
         * @param password
         */
        public TestConnectionParams(SettingsActivity settingsActivity, String username, String server, int port, String password) {
            this.settingsActivity   = settingsActivity;
            this.username           = username;
            this.server             = server;
            this.port               = port;
            this.password           = password;
        }

        public String toString() { return username + "@" + server + ":" + port; }

    }

    /**
     * @brief Called from the main UI thread to test an ssh connection.
     */
    class TestConnection extends AsyncTask<TestConnectionParams, Integer, String>
    {

        protected String doInBackground(TestConnectionParams...testConnectionParams) {
            JSch jsch = new JSch();
            Session session = null;
            try {
                MainActivity.Log("testSShConnection() Connecting to "+testConnectionParams[0].username+"@"+testConnectionParams[0].server+":"+testConnectionParams[0].port);

                session = SSHWrapper.Connect(jsch, testConnectionParams[0].server, testConnectionParams[0].port, testConnectionParams[0].username, testConnectionParams[0].password, testConnectionParams[0].settingsActivity);

                if( testConnectionParams[0].password != null && testConnectionParams[0].password.length() > 0 ) {
                    SSHWrapper.UpdateRemoteAuthorisedKeys(session, testConnectionParams[0].settingsActivity);
                }

                Message completeMessage = uiHandler.obtainMessage(Constants.SSH_CONNECTED_MSG_ID, null);
                completeMessage.sendToTarget();
            }
            catch(IOException e ) {
                Dialogs.Toast(testConnectionParams[0].settingsActivity, "Connect Failed");
            }
            catch(JSchException e ) {
                if( e.getLocalizedMessage().equals("Auth fail") ) {
                    Message completeMessage = uiHandler.obtainMessage(Constants.AUTH_FAIL_MSG_ID, null);
                    completeMessage.sendToTarget();
                }
                else {
                    Dialogs.Toast(testConnectionParams[0].settingsActivity, "SSH Connect Failed");
                }
            }
            if( session != null ) {
                session.disconnect();
            }

            return "";
        }

    }

    /**
     * @brief A callback method on an ssh connection. This is called is we fail to connect to the server due to authentication error.
     */
    void enterServerPassword() {
        enterServerPasswordDialog = Dialogs.showInputDialog(this, Constants.SSH_PW_TITLE, Constants.PASSWORD, "", true, this);
    }

    /**
     * @brief Check the ssh connection to the ICONS server.
     *
     * @param password The ssh password. This maybe null and if so then we expect to be able to connect to the ssh server because
     *                 it has our public ssh key in its authorised keys file.
     */
    public void testSShConnection(String password) {
        String serverUsername = sharedPreferences.getString(getResources().getString(R.string.pref_server_username), "");
        String serverAddress = sharedPreferences.getString(getResources().getString(R.string.pref_server_address), "");
        int serverPort = sharedPreferences.getInt(getResources().getString(R.string.pref_server_port), Constants.DEFAULT_SSH_SERVER_PORT);
        MainActivity.Log("testSShConnection()");

        TestConnectionParams testConnectionParams = new TestConnectionParams(this, serverUsername, serverAddress, serverPort, password);
        new TestConnection().execute(testConnectionParams);
    }

    /**
     * @brief Called if the user enteres an ssh password.
     * @param title
     * @param prompt
     * @param input
     */
    public void positiveInput(String title, String prompt, String input) {
        if (title.equals(Constants.SSH_PW_TITLE) && prompt.equals(Constants.PASSWORD) ) {
            testSShConnection(input);
        }
    }

    public void negativeInput(String title, String prompt) {}

    private void askDeleteLocalSSHKey() {
        Dialogs.OptionsDialog(this,
                              this.getResources().getString(R.string.dialog_delete_ssh_key),
                              this.getResources().getStringArray(R.array.dialog_yes_no_options),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        deleteLocalSSHKey();
                        break;

                }
            }
        });
    }

    /**
     * @brief Called to delete the local SSH key
     */
    public void deleteLocalSSHKey() {
        try {
            SSHWrapper.deleteLocalKeys(this);
        }
        catch(IOException e) {}
        catch(JSchException e) {}
    }

}


