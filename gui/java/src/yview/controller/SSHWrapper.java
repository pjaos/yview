package yview.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import yview.model.Constants;
import yview.view.StatusBar;

public class SSHWrapper {

	public SSHWrapper() {
		
	}
	

	/**
	 * @brief Get the rsa and dsa private key files
	 * @return An array with two elements (rsa key, dsa key)
	 */
	public static String[] GetDefaultSSHPrivateKeyFiles() {
		String homePath = System.getProperty("user.home");
		String fileSeparator = System.getProperty("file.separator");
		File sshConfigFolder = new File( homePath + fileSeparator + Constants.DEFAULT_SSH_FOLDER);
		if( !sshConfigFolder.isDirectory() ) {
		    int response = JOptionPane.showConfirmDialog(null, "The ssh configuration folder does not exists. Do you wish to create it ?", "Error", JOptionPane.YES_NO_OPTION);
	        if (response == JOptionPane.YES_OPTION) {
	        	sshConfigFolder.mkdirs();
	        }
	        else {
	           JOptionPane.showMessageDialog(null, "The ssh configuration is required.\nClose to exit program.");
	           System.exit(0);
	        }
		}
		File rsaPrivateKeyFile = new File( homePath + fileSeparator + Constants.DEFAULT_SSH_FOLDER, Constants.PRIVATE_RSA_KEY_FILENAME);
		File dsaPrivateKeyFile = new File( homePath + fileSeparator + Constants.DEFAULT_SSH_FOLDER, Constants.PRIVATE_DSA_KEY_FILENAME);
		String publicKeyFiles[] = new String[2];
		publicKeyFiles[0]=rsaPrivateKeyFile.getAbsolutePath();
		publicKeyFiles[1]=dsaPrivateKeyFile.getAbsolutePath();
		return publicKeyFiles;
	}
	
	
	/**
	 * @brief Return the string to represent the connection details
	 * @param username The ssh username
	 * @param host The ssh server address
	 * @param port The ssh port
	 * @return A String representing the connection
	 */
	public static String GetConnectionString(String username, String host, int port ) {
		return username + "@" + host + ":" +port;
	}
	
	/**
	 * @brief return the public key. We choose from the rsa and dsa keys and prefer the rsa key.
	 * 
	 * @return The Public ssh key or null if none found
	 */
	public static String GetPublicKey() throws IOException {
		String publicKey = null;
		
        String privateKeyFiles[] = SSHWrapper.GetDefaultSSHPrivateKeyFiles();
        for( String privateKeyFile : privateKeyFiles ) {
        	File pkf = new File(privateKeyFile);
        	if( pkf.isFile() ) {
        		Path publicKeyPath = Paths.get(pkf.getParent(), pkf.getName()+".pub");
        		publicKey = new String(Files.readAllBytes(publicKeyPath));
        	}
        }
        
        return publicKey;
	}
	

	/**
	 * @brief Manage the key files used by the ssh connection
	 */
	@SuppressWarnings("deprecation")
	public static void ManageKeyFiles(JSch jsch, String username, String host, StatusBar statusBar) throws JSchException, IOException{
		boolean atLeastOnePrivateKeyFileFound = false;
		
        String privateKeyFiles[] = SSHWrapper.GetDefaultSSHPrivateKeyFiles();
        for( String privateKeyFile : privateKeyFiles ) {
        	File pkf = new File(privateKeyFile);
        	if( pkf.isFile() ) {
        		jsch.addIdentity(privateKeyFile);
        		if( statusBar != null ) {
        			statusBar.println("Using " + privateKeyFile + " key file.");
        		}
        		atLeastOnePrivateKeyFileFound = true;
        	}
        }
        
        if( !atLeastOnePrivateKeyFileFound ) {
    		if( statusBar != null ) {
    			statusBar.println("No ssh key files found,. Generating " + privateKeyFiles[0]);
    		}
        	KeyPair kpair=KeyPair.genKeyPair(jsch, KeyPair.RSA);
        	kpair.setPassphrase("");
        	kpair.writePrivateKey(privateKeyFiles[0]);
    		if( statusBar != null ) {
    			statusBar.println("Created " + privateKeyFiles[0]);
    		}
        	kpair.writePublicKey(privateKeyFiles[0]+".pub", username+"@"+host);
    		if( statusBar != null ) {
    			statusBar.println("Created " + privateKeyFiles[0]+".pub");
    		}
        }
        
	}
	
	/**
	 * Update the remote authorised keys file so that ssh connections do not require the
	 * user to enter a password.
	 */
	public static void UpdateRemoteAuthorisedKeys(Session session, StatusBar statusBar) throws JSchException, IOException {
		String publicSSHKey = SSHWrapper.GetPublicKey();
		
		if( publicSSHKey == null ) {
			
			throw new IOException("Failed to read public key file.");

		} else {

			String authKeysFile = "~/.ssh/authorized_keys";
			if( statusBar != null ) {
				statusBar.println("Adding local public key to "+authKeysFile+" on the ssh server.");
			}
			String command = "echo \"" + publicSSHKey + "\" >> "+authKeysFile;
		    Channel channel=session.openChannel("exec");
		    ((ChannelExec)channel).setCommand(command);
		    channel.connect();
		    channel.disconnect();
		    if( statusBar != null ) {
		    	statusBar.println("Added local public key to "+authKeysFile+" on the ssh server.");
		    }

		}
	}
	
	/**
	 * @brief Check if it's possible to connect to the ssh server
	 * @param username The ssh username
	 * @param host The ssh server address 
	 * @param port The ssh port
	 * @param statusBar A StatusBar instance to show connection progress. If null then no attempt to display status information is made.
	 * @return true if connection successful.
	 */
	public static boolean AbleToConnectToHost(JSch jsch, String username, String host, int port, StatusBar statusBar) {
		boolean connectable = false;
		
		Session session = Connect(jsch, username, host, port, statusBar);
		if( session != null ) {
			if( session.isConnected() ) {
				connectable = true;
				Disconnect(session, statusBar);
			}
		}
		return connectable;
	}
	
	/**
	 * @brief Connect to an SSH server
	 * @param username The ssh username
	 * @param host The ssh server address 
	 * @param port The ssh port
	 * @param statusBar A StatusBar instance to show connection progress. If null then no attempt to display status information is made. 
	 * @return An instance of a Session object or null if connect failed.
	 */
	public static Session Connect(JSch jsch, String username, String host, int port, StatusBar statusBar) {
		Session session=null;
		Component parent=null;
		if( statusBar != null ) {
			parent = statusBar.getParent();
		}
		//Check we can connect to the server
    	try {
    		if( statusBar != null ) {
    			statusBar.setVisible(true);
    			statusBar.println("Checking SSH connectivity to " + username + "@" + host + ": " + port);
    			statusBar.println_persistent("Attempting to connect to " + username + "@" + host + ": " + port);
    		}
    		
    		session=jsch.getSession(username, host, port );
    		session.setDaemonThread(true);
    		
    		SSHWrapper.ManageKeyFiles(jsch, username, host, statusBar);

	        session.setConfig("StrictHostKeyChecking", "no");
	        
	        MyUserInfo userInfo = new MyUserInfo();
	        session.setUserInfo(userInfo);

	        session.connect();

	        if( statusBar != null ) {
	        	statusBar.println("Connected to " + username + "@" + host + ": " + port);
	        }
	        
    		//If user had to enter a password, add the public key to the server authorised keys file
    		if( ((MyUserInfo) userInfo).wasPasswordEntered() ) {
    			UpdateRemoteAuthorisedKeys(session, statusBar);
    		}
    		
		} 
    	catch( IOException ex) {
    		String msg = ex.getMessage()+" ("+username + "@" + host + ": " + port+")";
    		JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    		statusBar.println( msg );
        }
    	catch( JSchException ex) {

    		ex.printStackTrace();
    		//TODO UnknownHostException when can't connect to host present different message to user
    		String msg = "Failed to login to the ssh server "+username + "@" + host + ": " + port;
    		statusBar.println( msg );
        }
    	return session;

	}
	
	/**
	 * @brief Disconnect a connected session
	 * @param session The ssh Session object.
	 * @param statusBar A StatusBar instance to show connection progress. If null then no attempt to display status information is made. 
	 */
	public static void Disconnect(Session session, StatusBar statusBar) {
		if( session != null && session.isConnected() ) {
			session.disconnect();
			statusBar.println("Disconnected SSH session from "+session.getHost()+":"+session.getPort());
			session = null;
		}
	}
	
	/**
	 * @brief Callback to assist in building an ssh connection
	 */
	public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
		String 		passwd;
		JTextField 	passwordField = (JTextField) new JPasswordField(20);
		boolean 	passwordEntered;
		
		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {
			Object[] options = { "yes", "no" };
			int foo = JOptionPane.showOptionDialog(null, str, "Warning",
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[0]);
			return foo == 0;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			// Do this so that password are not prompted from the user
			// if( true )
			// return true;

			Object[] ob = { passwordField };
			int result = JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passwd = passwordField.getText();
				passwordEntered = true;
				return true;
			} else {
				return false;
			}
		}

		public boolean wasPasswordEntered() {
			return passwordEntered;
		}
		
		public void showMessage(String message) {
			JOptionPane.showMessageDialog(null, message);
		}

		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0);
		private Container panel;

		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;

			gbc.gridwidth = GridBagConstraints.RELATIVE;
			JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);

				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}

			if (JOptionPane.showConfirmDialog(null, panel, destination + ": "
					+ name, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			} else {
				return null; // cancel
			}
		}
	}

}
