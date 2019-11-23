package yview.model;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.json.JSONObject;

import pja.io.FileIO;
import pja.io.SimpleConfig;
import yview.controller.JSONProcessor;
import yview.view.MainFrame;

/**
 * @brief Responsible for holding service details.
 *
 */
public class Service {	
	public static String SERVICE_SEPARATOR 	= ",";
	public static String PORT_SEPARATOR 	= ":";
	public static final String 	DEFAULT_SERVICE_CFG_FILE   = "yview_default_srv_map.json";
	public static final String 	LAN_SERVICE_DEFAULTS	   = "{\"WySw\": \"HTTP:80\",\"YSmartM\": \"HTTP:80\",\"OGSOLAR\": \"HTTP:80,SSH:22\",\"DEFAULT\": \"HTTP:80\",\"Server\": \"SSH:22\",\"WyTerm\":\"HTTP:80,WYTERM_SERIAL_PORT:23\"}";
	public static final String 	HTTP_SERVICE_STRING		   = "HTTP:80";  
	public String serviceName;
	public int    port;
	
	/**
	 * @brief Constructor.
	 * @param serviceName The name of the service
	 * @param port        The TCP port number that the service is offered on.
	 */
	public Service(String serviceName, int port) {
		this.serviceName=serviceName;
		this.port=port;
	}
	
	public String toString() {
		return serviceName+PORT_SEPARATOR+port;
	}
	
	/**
	 * @brief Convert a service string into a list of services.
	 * @param serviceString The service string (E.G ssh:22,vnc:5900).
	 * @return A list of Service objects.
	 */
	public static Service[] GetServiceList(String serviceString) {
		String 	serviceName;
		int    	port;
		Service	service;

		Vector<Service> serviceList = new Vector<Service>();
		
		StringTokenizer serviceTokenizer = new StringTokenizer(serviceString, SERVICE_SEPARATOR);
		while( serviceTokenizer.hasMoreElements() ) {
			StringTokenizer portTokenizer = new StringTokenizer(serviceTokenizer.nextToken(), PORT_SEPARATOR);
			if( portTokenizer.countTokens() == 2 ) {
				serviceName = portTokenizer.nextToken();
				try {
					port = Integer.parseInt( portTokenizer.nextToken() );
					service = new Service(serviceName, port);
					serviceList.add(service);
				}
				catch(NumberFormatException e) {}
			}
		}
		
		Service[] serviceArray = new Service[serviceList.size()];
		int index=0;
		for(Service serv : serviceList ) {
			serviceArray[index]=serv;
			index++;
		}
		
		return serviceArray;
	}
	
	/**
	 * @brief Compare the names of the services to see if they match.
	 * @param service
	 * @param serviceName
	 * @return true if they match, false if not.
	 */
	public static boolean ServiceMatch(Service service, String serviceName) {
		if( service.serviceName.toLowerCase().equals(serviceName.toLowerCase())) {
			return true;
		}
		return false;
	}

	/**
	 * @brief Compare the names of the services to see if they match.
	 * @param serviceName
	 * @return true if they match, false if not.
	 */
	public boolean serviceMatch(String serviceName) {
		return ServiceMatch(this, serviceName);
	}
	
	/**
	 * @brief Compare the names of the services to see if they match.
	 * @param serviceNameA
	 * @param serviceNameB
	 * @return true if they match, false if not.
	 */
	public static boolean ServiceNameMatch(String serviceNameA, String serviceNameB) {
		if( serviceNameA.toLowerCase().equals(serviceNameB.toLowerCase())) {
			return true;
		}
		return false;
	}

	/**
	 * @brief Compare the names of the services to see if they match.
	 * @param serviceNameToMatch
	 * @return true if they match, false if not.
	 */
	public boolean serviceNameMatch(String serviceNameToMatch) {
		return ServiceNameMatch(this.serviceName, serviceNameToMatch);
	}
	
	/**
	 * @brief Determine if the services string holds the service
	 * @param servicesString
	 * @param service
	 * @return true if so, false if not.
	 */
	public static boolean InServicesString(String servicesString, String service) {
		if( servicesString.toLowerCase().indexOf(service.toLowerCase()) != -1) {
			return true;
		}
		return false;
	}
	
	/**
	 * @brief Get the file that holds the service configuration for devices found on the LAN.
	 * @return
	 */
	public static File GetDefaultSrvConfigFile() {
		File configFile = new File( SimpleConfig.GetTopLevelConfigPath(Constants.APP_NAME), Service.DEFAULT_SERVICE_CFG_FILE);
		return configFile;
	}
	
	/**
	 * @brief Get the service list for the jsonDevice on the LAN.
	 * @param jsonDevice The message received from the device.
	 * @return A Service array.
	 */
	public static Service[] GetDefaultDeviceServiceList(JSONObject jsonDevice) {
		Service[] srvList = GetServiceList(Service.HTTP_SERVICE_STRING);
		String serviceString = null;
		File configFile = GetDefaultSrvConfigFile();
		try {
			String jsonText = FileIO.Get(configFile.getAbsolutePath());
			JSONObject jsonConfigObject = new JSONObject(jsonText);
			if( jsonDevice.has(JSONProcessor.PRODUCT_ID) ) {
				String productID = jsonDevice.getString(JSONProcessor.PRODUCT_ID);
				if( jsonConfigObject.has(productID) ) {
					serviceString = (String)jsonConfigObject.get(productID);
				}
			}
		}
		catch( IOException e) {
			
		}
		if( serviceString != null ) {
			srvList = GetServiceList(serviceString);
		}
		return srvList;
	}
}
