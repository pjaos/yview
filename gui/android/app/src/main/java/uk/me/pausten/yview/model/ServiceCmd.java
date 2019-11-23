package uk.me.pausten.yview.model;

import java.io.File;

import org.json.JSONObject;

public class ServiceCmd {

    public static final String	HTTP_SERVICE_NAME        	   = "HTTP";
    public static final String	SERIAL_PORT_SERVICE_NAME 	   = "WYTERM_SERIAL_PORT";
    public static final String	SSH_SERVICE_NAME 	   		   = "ssh";
    public static final String	VNC_SERVICE_NAME 	   		   = "vnc";

    public static final int EXTERNAL_CMD_TYPE = 0;
    public static final int INTERNAL_VNC_CMD_TYPE = 1;
    public static final int INTERNAL_HTTP_CMD_TYPE = 2;
    public static final int INTERNAL_SERIAL_PORT_CMD_TYPE = 3;
    public static final int MAX_INTERNAL_TYPE = INTERNAL_SERIAL_PORT_CMD_TYPE;

    private String 	serviceName;
    private int 	servicePort;
    private int		cmdType;
    private String  menuName;
    private String  cmd;

    //Getters
    public String getServiceName() {
        return serviceName;
    }
    public int getServicePort() {
        return servicePort;
    }
    public String getMenuOptionName() {
        return menuName;
    }
    public int getCmdType() {
        return cmdType;
    }
    public String getCmd() {
        return cmd;
    }

    //Setters
    public void setMenuOptionName( String menuName ) {
        this.menuName=menuName;
    }
    public void setCmd( String cmd ) {
        this.cmd=cmd;
    }
    /**
     * @brief Constructor
     * @param serviceName
     * @param menuName
     * @param cmdType
     * @param cmd
     */
    public ServiceCmd(String serviceName, int servicePort, String menuName, int cmdType, String cmd) throws ServiceCmdException {
        if( cmd.length() == 0 ) {
            throw new ServiceCmdException("External command not definned.");
        }

        if( cmd.indexOf(',') != -1 ) {
            throw new ServiceCmdException("externalCmdline contains , character/s: "+cmd);
        }

        if( cmdType > MAX_INTERNAL_TYPE ) {
            throw new ServiceCmdException("Invalid internal type ("+cmdType+" max "+MAX_INTERNAL_TYPE+")");
        }

        if( menuName.length() == 0 ) {
            throw new ServiceCmdException("No menu name defined");
        }

        if( menuName.indexOf(',') != -1 ) {
            throw new ServiceCmdException("name contains , character/s: "+menuName);
        }

        this.serviceName=serviceName;
        this.servicePort=servicePort;
        this.cmdType=cmdType;
        this.menuName=menuName;
        this.cmd=cmd;
    }

    /**
     * @brief Convert object to string representation.
     */
    public String toString() {
        return serviceName+","+servicePort+","+menuName+","+cmdType+","+cmd;
    }

    /**
     * @brief Parse the line to return a ServiceMenuOption instance.
     * @param line The line if text
     * @return A ServiceMenuOption instance.
     * @throws ServiceCmdException
     */
    public static ServiceCmd GetServiceCmd(String line) throws ServiceCmdException {
        String 	serviceName;
        int 	servicePort;
        String  menuName;
        int		cmdType;
        String  cmd;

        //Remove EOL chars
        line = line.replace("\r", "");
        line = line.replace("\n", "");

        String elems[] = line.split(",");

        if( elems.length != 5 ) {
            throw new ServiceCmdException("Invalid ServiceMenuOption: "+line+": "+elems.length+" args. Expected 5 args.");
        }

        serviceName = elems[0];
        try {
            servicePort = Integer.parseInt(elems[1]);
        }
        catch(NumberFormatException e) {
            e.printStackTrace();
            throw new ServiceCmdException("Invalid service port number: "+line+" ("+elems[1]+" is an invalid port number)");
        }
        menuName = elems[2];

        try {
            cmdType = Integer.parseInt(elems[3]);
        }
        catch(NumberFormatException e) {
            throw new ServiceCmdException("Invalid service port number: "+line+" ("+elems[3]+" is not an integer)");
        }

        cmd = elems[4];

        return new ServiceCmd(serviceName, servicePort, menuName, cmdType, cmd);
    }

    /**
     * @brief Create a deep copy of the ServiceCmd
     * @param serviceCmd
     * @return
     * @throws ServiceCmdException
     */
    public static ServiceCmd GetCopy(ServiceCmd serviceCmd) throws ServiceCmdException {
        return new ServiceCmd( 	new String(serviceCmd.getServiceName()),
                serviceCmd.getServicePort(),
                new String(serviceCmd.getMenuOptionName()),
                serviceCmd.getCmdType(),
                new String(serviceCmd.getCmd())	);
    }

}
