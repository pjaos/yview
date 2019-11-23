package uk.me.pausten.yview.model;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @brief Responsible for holding service details.
 *
 */
public class Service {
    public static String SERVICE_SEPARATOR 	= ",";
    public static String PORT_SEPARATOR 	= ":";

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
                //Convert to uppercase as they are compared with the upper case service types in Constants
                serviceName=serviceName.toUpperCase();
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
}
