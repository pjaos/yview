package uk.me.pausten.yview.controller;

import android.app.Activity;
import android.os.Build;

import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.view.MainActivity;
import uk.me.pausten.yview.view.Dialogs;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;

public class SSHWrapper {

    /**
     * @brief Get the rsa and dsa private key files
     * @return An array with two elements (rsa key, dsa key)
     */
    private static String[] GetDefaultSSHPrivateKeyFilenames() throws IOException {
        String  appStorageFolder = MainActivity.GetAppStorageFolder();

        String fileSeparator = System.getProperty("file.separator");
        File sshConfigFolder = new File( appStorageFolder + fileSeparator + Constants.DEFAULT_SSH_FOLDER);
        if( !sshConfigFolder.isDirectory() ) {
            boolean created = sshConfigFolder.mkdirs();
            if( !created ) {
                throw new IOException("Failed to create the "+sshConfigFolder+" folder.");
            }
        }
        File rsaPrivateKeyFile = new File( appStorageFolder + fileSeparator + Constants.DEFAULT_SSH_FOLDER, Constants.PRIVATE_RSA_KEY_FILENAME);
        //MainActivity.Log("rsaPrivateKeyFile.isFile()              ="+rsaPrivateKeyFile.isFile());

        File dsaPrivateKeyFile = new File( appStorageFolder + fileSeparator + Constants.DEFAULT_SSH_FOLDER, Constants.PRIVATE_DSA_KEY_FILENAME);
        //MainActivity.Log("dsaPrivateKeyFile.isFile()              ="+dsaPrivateKeyFile.isFile());

        String publicKeyFiles[] = new String[2];
        publicKeyFiles[0]=rsaPrivateKeyFile.getAbsolutePath();
        publicKeyFiles[1]=dsaPrivateKeyFile.getAbsolutePath();
        return publicKeyFiles;
    }

    /**
     * @brief Get the device name
     * @return
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        manufacturer=manufacturer.replace(" ", "_");
        model=model.replace(" ", "_");
        return manufacturer+ "_" + model;
    }

    /**
     * @brief Manage the key files used by the ssh connection.
     * @param jsch The JSCH object.
     * @param username The ssh username.
     * @param @param activity The activity associated with the call. Maybe null if no user messages are required.
     */
    @SuppressWarnings("deprecation")
    private static void ManageKeyFiles(JSch jsch, String username, Activity activity) throws JSchException, IOException {
        boolean atLeastOnePrivateKeyFileFound = false;

        String privateKeyFilenames[] = SSHWrapper.GetDefaultSSHPrivateKeyFilenames();
        for( String privateKeyFile : privateKeyFilenames ) {
            File pkf = new File(privateKeyFile);
            if( pkf.isFile() ) {
                jsch.addIdentity(privateKeyFile);
                atLeastOnePrivateKeyFileFound = true;
            }
        }

        if( !atLeastOnePrivateKeyFileFound ) {
            KeyPair kpair=KeyPair.genKeyPair(jsch, KeyPair.RSA, Constants.RSA_KEY_LENGTH);
            kpair.setPassphrase("");
            kpair.writePrivateKey(privateKeyFilenames[0]);
            kpair.writePublicKey(privateKeyFilenames[0]+".pub", username+"@"+getDeviceName());
            if( activity != null ) {
                Dialogs.Toast(activity, "Generated ssh key: "+username+"@"+getDeviceName());
            }
        }

    }

    /**
     * @brief Delete the local ssh keys.
     * @throws JSchException
     * @throws IOException
     */
    public static void DeleteLocalKeys(Activity activity) throws JSchException, IOException {
        boolean deleted = false;

        String privateKeyFilenames[] = SSHWrapper.GetDefaultSSHPrivateKeyFilenames();
        for( String privateKeyFile : privateKeyFilenames ) {
            File pkf = new File(privateKeyFile);
            if( pkf.isFile() ) {
                pkf.delete();
                deleted = true;
            }
            pkf = new File(privateKeyFile + ".pub");
            if( pkf.isFile() ) {
                pkf.delete();
            }
        }
        if( deleted && activity != null ) {
            Dialogs.Toast(activity, "Deleted the local ssh key");
        }
    }

    /**
     * @brief return the public key. We choose from the rsa and dsa keys and prefer the rsa key.
     *
     * @return The Public ssh key or null if none found
     */
    public static String GetPublicKey() throws IOException {
        String publicKey = null;
        byte   rxBuffer[] = new byte[16384];

        String privateKeyFiles[] = SSHWrapper.GetDefaultSSHPrivateKeyFilenames();
        for( String privateKeyFile : privateKeyFiles ) {
            File pkf = new File(privateKeyFile);
            if( pkf.isFile() ) {
                FileInputStream fis = new FileInputStream(new File(pkf.getParent(), pkf.getName() + ".pub"));
                int byteCount = fis.read(rxBuffer);
                fis.close();
                if (byteCount > 0) {
                    publicKey = new String(rxBuffer, 0, byteCount);
                }
            }
        }
        //Remove trailing line feed
        if( publicKey.endsWith("\n")) {
            publicKey=publicKey.substring(0,publicKey.length()-1);
        }
        return publicKey;
    }

    private String [] readChannelOutput(Channel channel){
        Vector <String>lines = new Vector<String>();
        byte[] buffer = new byte[1024];

        try{
            InputStream in = channel.getInputStream();
            String line = "";
            while (true){
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    lines.add( new String(buffer, 0, i) );
                }

                if(line.contains("logout")){
                    break;
                }

                if (channel.isClosed()){
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee){}
            }
        }catch(Exception e){
            System.out.println("Error while reading channel output: "+ e);
        }

        //Buuld an array of Strings to return
        String lineArray[] = new String[lines.size()];
        int index=0;
        for( String line : lineArray ) {
            lineArray[index]=line;
            index++;
        }
        return lineArray;
    }

    /**
     * @brief Update the remote authorised keys file so that ssh connections do not require the
     *        user to enter a password.
     * @param @param activity The activity associated with the call. Maybe null if no user messages are required.
     */
    public static void UpdateRemoteAuthorisedKeys(Session session, Activity activity) throws JSchException, IOException {
        String publicSSHKey = SSHWrapper.GetPublicKey();

        if( publicSSHKey == null ) {

            throw new IOException("Failed to read public key file.");

        } else {
            String authKeysFile = "~/.ssh/authorized_keys";
            String command = "echo \"" + publicSSHKey + "\" >> "+authKeysFile;
            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            channel.connect();
            channel.disconnect();
            Dialogs.Toast(activity, "Added local SSH public key to server.");
        }
    }


    /**
     * @brief Disconnect a connected session
     * @param session The ssh Session object.
     * @param @param activity The activity associated with the call. Maybe null if no user messages are required.
     */
    public static void Disconnect(Session session,  Activity activity) {
        if( session != null && session.isConnected() ) {
            session.disconnect();
            Dialogs.Toast(activity, "Disconnected from "+session.getHost()+":"+session.getPort());
            session = null;
        }
    }

    /**
     * @brief Check if it's possible to connect to the ssh server
     * @param username The ssh username
     * @param host The ssh server address
     * @param port The ssh port
     * @param @param activity The activity associated with the call. Maybe null if no user messages are required.
     * @return true if connection successful.
     */
    public static boolean AbleToConnectToHost(JSch jsch, String username, String host, int port, Activity activity) {
        boolean connectable = false;

        try {
            Session session = Connect(jsch, host, port, username, null, activity);
            if (session != null) {
                if (session.isConnected()) {
                    connectable = true;
                    Disconnect(session, activity);
                }
            }
        }
        catch(IOException e) {

        }
        catch(JSchException e) {

        }
        return connectable;
    }

    /**
     * @brief Connect to an SSH server
     * @param jsch The JSCH object.
     * @param username The ssh username
     * @param host The ssh server address
     * @param port The ssh port
     * @param @param activity The activity associated with the call. Maybe null if no user messages are required.
     * @return An instance of a Session object or null if connect failed.
     */
    public static Session Connect(JSch jsch, String host, int port, String username, String password, Activity activity) throws IOException, JSchException {
        Session session=null;

        Dialogs.Toast(activity, "Connecting to " + username + "@" + host + ": " + port);

        session=jsch.getSession(username, host, port );
        session.setDaemonThread(true);

        SSHWrapper.ManageKeyFiles(jsch, username, activity);

        session.setConfig("StrictHostKeyChecking", "no");

        if( password != null && password.length() > 0 ) {
            session.setPassword(password);
        }

        session.connect();

        Dialogs.Toast(activity, "Connected to SSH Server");

        return session;
    }

}