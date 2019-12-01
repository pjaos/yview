package yview.model;

/**
 * @brief Responsible for holding all yView constants.
 */
public class Constants
{
  public static double          VERSION                        		= 5.6;

  public static final String    APP_NAME                       		= "yView";
  public static final String    LOCAL_HOST					   		= "localhost";
  
  public static final int       UDP_MULTICAST_PORT             		= 2934;

  public static final int       MQTT_SERVER_PORT               		= 1883;
  public static final int       MAX_DEVICE_MSG_LENGTH          		= 4096;
  
  public static final int       HTTP_TCP_PORT                  		= 80;
  public static final int       WYTERM_TCP_PORT                		= 23;
  
  public static final String    WYTERM_PRODUCT_ID       	   		= "WyTerm";
  public static final String    WYSWITCH2_PRODUCT_ID    	   		= "WySwitch2";
  public static final String    WYSW_PRODUCT_ID         	   		= "WySw";
  public static final String    WYGPIO_PRODUCT_ID              		= "WyGPIO";
  public static final String    SERVER_ID              		   		= "Server";
  public static final String    OGSOLAR			               		= "OGSOLAR";
  
  
  public static final String    DEFAULT_SSH_FOLDER             		= ".ssh";
  public static final String    PRIVATE_RSA_KEY_FILENAME       		= "id_rsa";
  public static final String    PRIVATE_DSA_KEY_FILENAME       		= "id_rda";
  public static final int       DEFAULT_SSH_SERVER_PORT 	   		= 22;
  public static final int       MIN_TCP_PORT_NUMBER 		   		= 1;
  public static final int       MAX_TCP_PORT_NUMBER 		   		= 65535;
  
  public static final String    AYT_MESSAGE 				   		= "{\"AYT\":\"-!#8[dkG^v's!dRznE}6}8sP9}QoIR#?O&pg)Qra\"}";
  
  public static final String	NO_LOCATION					   		= "NO_LOCATION";
  public static final String	LOCAL_LOCATION				   		= "LAN";
  
  public static final int       DEFAULT_AYT_PERIOD_MS	       		= 2000;
  public static final int   	CHECK_DEVICE_POLL_MS 		   		= 1000;
  public static final float		DEFAULT_WARNING_DEVICE_TIMEOUT_SECS	= 60.0F;
  public static final float		DEFAULT_LOST_DEVICE_TIMEOUT_SECS	= 3600.0F;

  
  public static final String    CLIENT_CONFIGURATION           		= "Client Configuration";
  public static final String	WEB_BROWSER_MENU_ITEM          		= "Web Browser";
  public static final String	SERIAL_TERM_MENU_ITEM          		= "Terminal";
  public static final String	SSH_CLIENT_MENU_ITEM	       		= "SSH Client";
  public static final String	VNC_CLIENT_MENU_ITEM		   		= "VNC Client";
  public static final String	SSH_CLIENT_CONFIG_MENU_ITEM	   		= "SSH "+CLIENT_CONFIGURATION;
  public static final String	VNC_CLIENT_CONFIG_MENU_ITEM	   		= "VNC "+CLIENT_CONFIGURATION;

  public static final String    SUPPORTED_TERMNIALS[]          		= {"/usr/bin/terminator","/usr/bin/gnome-terminal"};    
  public static final String    SUPPORTED_BROWSERS[]           		= {"/usr/bin/firefox", "/snap/bin/firefox", "/usr/bin/google-chrome"};    
  
  public static final String 	PORT_STRING                    		= "$p";
  public static final String 	HOST_STRING                    		= "$h";

  public static final String 	CONFIGURE_OPTIONS			   		= "Configure options";
  
  public static final String    OPTIONS_CONFIG_FILE_PREFIX     		= "yview_popup_menu_";
  
  public static final int       INSTANCE_LOCK_TCP_PORT_NUMBER		= 37219;
  
}
