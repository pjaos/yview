package yview.controller;

import java.util.Hashtable;
import org.json.JSONObject;
import com.jcraft.jsch.Session;

import yview.model.Constants;
import yview.model.ServiceCmd;

/**
 * @brief Provides helper functions for processing JSON messages.
 */
public class JSONProcessor {
	private static Hashtable <String, Session>SessionsHashtable = new Hashtable<String, Session>();
	private static Session ICONSSSHSession;

	public static final String GROUP_NAME         = "GROUP_NAME";
	public static final String LOCATION           = "LOCATION";
    public static final String IP_ADDRESS         = "IP_ADDRESS";
    public static final String PRODUCT_ID         = "PRODUCT_ID";
    public static final String UNIT_NAME          = "UNIT_NAME";
    public static final String POWER_W       	  = "POWER_W";
    public static final String UART0_BAUD         = "UART0_BAUD";
    public static final String UART0_DATA_BITS    = "UART0_DATA_BITS";
    public static final String UART0_STOP_BITS    = "UART0_STOP_BITS";
    public static final String UART0_PARITY       = "UART0_PARITY";
    public static final String SERVER_SERVICE_LIST= "SERVER_SERVICE_LIST";
    public static final String LOCAL_RX_TIME_MS   = "LOCAL_RX_TIME_MS";
    public static final String SESSION_ID 		  = "SESSION_ID";
    
    /**
     * @brief Get a value from a json object.
     * @param key The json key value.
     * @param json The JSON object that may contain the key
     * @return The String of the key value or null if not found.
     */
    private static String GetValue(String key, JSONObject json) {
    	String value = null;
    	
    	if( json.has(key) ) {
    		value = ""+json.get(key);
    	}
    	
    	return value;
    }

    
	/**
	 * @brief Get the location from the JSON message
	 * @param json The json message.
	 * return The location string or Null if not found.
	 */
	public static String GetLocation(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.LOCATION, json);
	}
	
	/**
	 * @brief Check for a location match
	 * @param location The location string.
	 * @param json The JSON object that may contain the location field.
	 * @return True if a match was found.
	 */
	public static boolean LocationMatch(String location, JSONObject json) {
		boolean match=false;
		String jsonLocation = JSONProcessor.GetLocation(json);
		
		if( jsonLocation != null && location != null && jsonLocation.equals(location) ) {
			match=true;
		}
		
		return match;
	}
	
	/**
	 * @brief Get the group name from the JSON message
	 * @param json The json message.
	 * return The group name string or Null if not found.
	 */
	public static String GetGroupName(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.GROUP_NAME, json);
	}
	
	/**
	 * @brief Check for a Group name match
	 * @param groupName The group name string.
	 * @param json The JSON object that may contain the group name field.
	 * @return True if a match was found.
	 */
	public static boolean GroupNameMatch(String groupName, JSONObject json) {
		boolean match=false;
		String jsonGroupName = JSONProcessor.GetGroupName(json);

		// If the device group name and the configured group name are set and they match
		if( jsonGroupName != null && groupName != null && jsonGroupName.equals(groupName) ) {
			match=true;
		}
		else {
			// Treat a 0 length strings and a null string the same
			if( groupName != null && groupName.length() == 0 ) {
				groupName = null;
			}
			if( jsonGroupName != null && jsonGroupName.length() == 0 ) {
				jsonGroupName = null;
			}
			// If neither the device group name or the configured group name are set they match
			if( jsonGroupName == null && groupName == null ) {
				match=true;
			}
		}
		
		return match;
	}

	/**
	 * @brief Get the IP address name from the JSON message
	 * @param json The json message.
	 * return The IP address or Null if not found.
	 */
	public static String GetIPAddress(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.IP_ADDRESS, json);
	}
	
	/**
	 * @brief Check for an IP address match
	 * @param ipAddress The IP address string.
	 * @param json The JSON object that may contain the IP address field.
	 * @return True if a match was found.
	 */
	public static boolean IPAddressMatch(String ipAddress, JSONObject json) {
		boolean match=false;
		String jsonIPAddress = JSONProcessor.GetIPAddress(json);
		
		if( jsonIPAddress != null && ipAddress != null && jsonIPAddress.equals(ipAddress) ) {
			match=true;
		}
		
		return match;
	}
	
	/**
	 * @brief Get the product ID from the JSON message
	 * @param json The json message.
	 * return The product ID or Null if not found.
	 */
	public static String GetProductID(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.PRODUCT_ID, json);
	}

	/**
	 * @brief Get the unit name from the JSON message
	 * @param json The json message.
	 * return The unit name or Null if not found.
	 */
	public static String GetUnitName(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.UNIT_NAME, json);
	}
	
	/**
	 * @brief Check for a unit name match
	 * @param unitName The Unit name string.
	 * @param json The JSON object that may contain the unit name field.
	 * @return True if a match was found.
	 */
	public static boolean UnitNameMatch(String unitName, JSONObject json) {
		boolean match=false;
		String jsonUnitName = JSONProcessor.GetUnitName(json);
		
		if( jsonUnitName != null && unitName != null && jsonUnitName.equals(unitName) ) {
			match=true;
		}
		
		return match;
	}
	
	/**
	 * @brief Get the power in watts from the JSON message
	 * @param json The json message.
	 * return The power in watts or Null if not found.
	 */
	public static String GetPowerWatts(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.POWER_W, json);
	}
	
	/**
	 * @brief Get the uart 0 baud rate from JSON message
	 * @param json The json message.
	 * return The value or Null if not found.
	 */
	public static String GetUart0Baud(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.UART0_BAUD, json);
	}
	
	/**
	 * @brief Get the uart 0 data bits from JSON message
	 * @param json The json message.
	 * return The value or Null if not found.
	 */
	public static String GetUart0DataBits(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.UART0_DATA_BITS, json);
	}	
	
	/**
	 * @brief Get the uart 0 stop bits from JSON message
	 * @param json The json message.
	 * return The value or Null if not found.
	 */
	public static String GetUart0StopBits(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.UART0_STOP_BITS, json);
	}	
	
	/**
	 * @brief Get the uart 0 parity from JSON message
	 * @param json The json message.
	 * return The value or Null if not found.
	 */
	public static String GetUart0Parity(JSONObject json) {
		return JSONProcessor.GetValue(JSONProcessor.UART0_PARITY, json);
	}	

    /**
     * @brief Get the string representing the serial config.
     * @param json The JSONObject instance.
     * @return The String representing the Uart settings or null if the req args are not in the json message.
     */
    public static String GetSerialConfigString(JSONObject jsonDevice) {
    	float stopBitsF=-1;
    	String serConfStr = null;
    	String baud = JSONProcessor.GetUart0Baud(jsonDevice);
    	String dataBits = JSONProcessor.GetUart0DataBits(jsonDevice);
    	String stopBits = JSONProcessor.GetUart0StopBits(jsonDevice);
    	String parity = JSONProcessor.GetUart0Parity(jsonDevice);
    	
    	if( stopBits != null ) {
	    	try {

	    		stopBitsF = Float.parseFloat(stopBits)/10;
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    	}
    	
	    	if( baud != null && dataBits != null && stopBits != null && parity != null ) {
	    		serConfStr = baud + ", " + dataBits + ", " + stopBitsF + ", " + parity;
	    	}
    	}
    	
        return serConfStr;
    }
    
	/**
	 * @brief Get the service services list from JSON message
	 * @param json The json message.
	 * return The value or Null if not found.
	 */
	public static String GetServicesList(JSONObject json) {
		String services = ServiceCmd.HTTP_SERVICE_NAME+":80";
		if( json.has(JSONProcessor.SERVER_SERVICE_LIST) ) {
			services = JSONProcessor.GetValue(JSONProcessor.SERVER_SERVICE_LIST, json);
		}
		//Local Yview (Old) devices don't advertise their services (The services tag is added by the icons_d gateway)  
		else {
			String productID = GetProductID(json);

			if( productID.equals(Constants.WYTERM_PRODUCT_ID) ) {
				services = ServiceCmd.HTTP_SERVICE_NAME+":80,"+ServiceCmd.SERIAL_PORT_SERVICE_NAME+":23";
			}
			else if( productID.equals(Constants.WYSWITCH2_PRODUCT_ID) ) {
				services = ServiceCmd.HTTP_SERVICE_NAME+":80";
			}
			else if( productID.equals(Constants.WYSW_PRODUCT_ID) ) {
				services = ServiceCmd.HTTP_SERVICE_NAME+":80";
			}
			else if( productID.equals(Constants.WYGPIO_PRODUCT_ID) ) {
				services = ServiceCmd.HTTP_SERVICE_NAME+":80";
			}
			else if( productID.equals(Constants.SERVER_ID) ) {
				services = ServiceCmd.SSH_SERVICE_NAME+":22";
			}
			else if( productID.equals(Constants.OGSOLAR) ) {
				services = ServiceCmd.SSH_SERVICE_NAME+":22,"+ServiceCmd.HTTP_SERVICE_NAME+":80";
			}
		}
		
		return services;
	}
	  public static final String    WYTERM_PRODUCT_ID       	   = "WyTerm";
	  public static final String    WYSWITCH2_PRODUCT_ID    	   = "WySwitch2";
	  public static final String    WYSW_PRODUCT_ID         	   = "WySw";
	  public static final String    WYGPIO_PRODUCT_ID              = "WyGPIO";
	  
	/**
	 * 
	 * @brief Get the time we received the message from the server as held in the message
	 * @param json The json message.
	 * return The value or -1 if not found.
	 */
	public static long GetLocalRxTimeMs(JSONObject json) {
		long msgRxTimeMS = -1;
		String msgRxTimeStr =  JSONProcessor.GetValue(JSONProcessor.LOCAL_RX_TIME_MS, json);
		try {
			if( msgRxTimeStr != null ) {
				msgRxTimeMS = Long.parseLong(msgRxTimeStr);
			}
		}
		catch(NumberFormatException e) {
			e.printStackTrace();
		}
		
		return msgRxTimeMS;
	}
	
	/**
	 * @brief Add the connected ICONS ssh session.
	 * @param session The JSCH session
	 * @param jsonDevice The json message.
	 */
	public static void SetICONSSSHSession(Session session, JSONObject jsonDevice) {
		String location = JSONProcessor.GetLocation(jsonDevice);
		String ipAddress = JSONProcessor.GetLocation(jsonDevice);
		
		if( location != null && ipAddress != null ) {
			SessionsHashtable.put(location+ipAddress, session);
		}
	}
	
	/**
	 * @brief Get the ICONS ssh session instance
	 * @param jsonDevice The json message.
	 * @return The session or null if not session reference exists.
	 */
	public static Session GetICONSSSHSession(JSONObject jsonDevice) {
		Session session = null;
		String location = JSONProcessor.GetLocation(jsonDevice);
		String ipAddress = JSONProcessor.GetLocation(jsonDevice);
		
		if( location != null && ipAddress != null ) {
			session = SessionsHashtable.get(location+ipAddress);
		}
		return session;
	}
	
	/**
	 * @brief Determine if the location of the devi ce is local
	 * @param jsonDevice
	 * @return true if local
	 */
	public static boolean IsLocalLocation(JSONObject jsonDevice) {
		boolean local=false;
		if( JSONProcessor.GetLocation(jsonDevice).equals( Constants.LOCAL_LOCATION ) ) {
			local=true;
		}
		return local;
	}
	
	/**
	 * @brief Determine if the devices Match
	 * @param jsonDeviceA
	 * @param jsonDeviceB
	 * @return true if they do
	 */
	public static boolean IsDeviceMatch(JSONObject jsonDeviceA, JSONObject jsonDeviceB)  {
		boolean match=false;
		String locationA = JSONProcessor.GetLocation(jsonDeviceA);
		String locationB = JSONProcessor.GetLocation(jsonDeviceB);
		String ipAddressA = JSONProcessor.GetIPAddress(jsonDeviceA);
		String ipAddressB = JSONProcessor.GetIPAddress(jsonDeviceA);
		
		if( locationA.equals(locationB) && ipAddressA.equals(ipAddressB) ) {
			match=true;
		}
		
		return match;
	}
	
}
