package uk.me.pausten.yview.model;

/**
 * Created by pja on 31/07/15.
 * Responsible for holding all constants.
 *
 * @author Paul Austen
 */
public class Constants {
    public static final String    APP_NAME                      = "YView";

    public static final String    LOCAL_HOST					= "localhost";

    public static final int       UDP_MULTICAST_PORT           = 2934;
//  public static final int       UDP_MULTICAST_PORT           = 8737;

    public static final int       MQTT_SERVER_PORT             = 1883;
    public static final int       MAX_DEVICE_MSG_LENGTH        = 4096;

    public static final int       HTTP_TCP_PORT                = 80;
    public static final int       WYTERM_TCP_PORT              = 23;

    public static final String    WYTERM_PRODUCT_ID            = "WyTerm";
    public static final String    WYSWITCH2_PRODUCT_ID         = "WySwitch2";
    public static final String    WYSW_PRODUCT_ID              = "WySw";
    public static final String    WYGPIO_PRODUCT_ID            = "WyGPIO";
    public static final String	  SERVER_PRODUCT_ID            = "Server";

    public static final String    DEFAULT_SSH_FOLDER           = ".ssh";
    public static final String    PRIVATE_RSA_KEY_FILENAME     = "id_rsa";
    public static final String    PRIVATE_DSA_KEY_FILENAME     = "id_rda";
    public static final int       DEFAULT_SSH_SERVER_PORT 	   = 22;


    public static final String    AYT_MESSAGE 				   = "{\"AYT\":\"-!#8[dkG^v's!dRznE}6}8sP9}QoIR#?O&pg)Qra\"}";

    public static final String	  NO_LOCATION				   = "NO_LOCATION";
    public static final String	  LOCAL_LOCATION			   = "LAN";

    public static final int       DEFAULT_AYT_PERIOD_MS	       = 10000;
    public static final int		  DEVICE_TIMEOUT_PERIOD_MS 	   = 6*DEFAULT_AYT_PERIOD_MS;

    public static final int       YTERM_RX_BUFFER_SIZE         = 4096;
    public static final String    TERM_ACTIVITY_YTERM_ADDRESS  = "YTerm";
    public static final int       MAX_TEXT_VIEW_BUFFER_SIZE    = 131072;

    public static final int       LOCATION_UPDATED_MSG_ID      = 1;
    public static final int       AUTH_FAIL_MSG_ID             = 2;
    public static final int       SSH_CONNECTED_MSG_ID         = 3;

    public static final String    DEVICE_LIST_ACTIVITY         = "DevList";

    public static final String    SSH_PW_TITLE                 = "SSH Password Required";
    public static final String    PASSWORD                     = "Password";

    //USed in ICONSConnection
    public static final int ICONS_RECONNECT_DELAY_SECONDS       = 5;
    public static final String JSON_CMD                         = "CMD";
    public static final String JSON_GET_DEVICES                 = "GET_DEVICES";
    public static final int SOCKET_READ_TIMEOUT_MS              = 5000;
    public static final int RX_BUFFER_SIZE                      = 32768;
    public static final int SERVER_POLL_DELAY                   = 2000;
    public static final int INVALID_ICONS_PORT                  = -1;

    public static final int RSA_KEY_LENGTH                      = 2048;

    public static final String	HTTP_SERVICE_NAME        	    = "HTTP";
    public static final String	WEB_SERVICE_NAME        	    = "WEB";
    public static final String	VNC_SERVICE_NAME        	    = "VNC";
    public static final String	SERIAL_PORT_SERVICE_NAME 	    = "WYTERM_SERIAL_PORT";
    public static final String	SSH_SERVICE_NAME        	    = "SSH";

    public static final String	WEB_SERVICE_DISPLAY_NAME        = "Browser";
    public static final String	VNC_SERVICE_DISPLAY_NAME        = "VNC";
    public static final String	SERIAL_PORT_DISPLAY_NAME 	    = "Serial Port";
    public static final String	SSH_SERVICE_DISPLAY_NAME        = "SSH";

}
