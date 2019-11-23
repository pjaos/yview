package uk.me.pausten.yview.model;

/**
 * Responsible for holding an ICON server attributes.
 * @author pja
 *
 */
public class ICONServer {

    public static final int INVALID_ICONS_PORT = -1;
    private String 	username="";
    private String 	serverName="";
    private int 	port=Constants.DEFAULT_SSH_SERVER_PORT;
    private boolean active=true;
    private int iconsPort = INVALID_ICONS_PORT;

    /**
     * @brief Set the local TCP port that is forwarded to the ICON server port on the ssh/ICON server.
     *
     * @param iconsPort The TCP port.
     */
    public void setICONSPort(int iconsPort) {
        this.iconsPort = iconsPort;
    }

    /**
     * @brief Get the local TCP port that is forwarded to the ICON server port on the ssh/ICON server.
     * @return The TCP port.
     */
    public int getICONSPort()	{
        return iconsPort;
    }

    /**
     * @brief Set the ssh username associated with this ICON server connection.
     *
     * @param username The ssh username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @brief Set the ssh server name associated with this ICON server connection.
     *
     * @param serverName The ssh server name.
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @brief Set the ssh TCP port associated with this ICON server connection.
     *
     * @param port The ssh TCP port.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @brief Set this ICON server connection as active or inactive.
     *
     * @param username The ssh TCP port.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @brief Get the username associated with this ICON server connection.
     *
     * @return The ssh username.
     */
    public String getUsername() { return username; }

    /**
     * @brief Get the serverName associated with this ICON server connection.
     *
     * @return The ssh server name.
     */
    public String getServerName() { return serverName; }

    /**
     * @brief Get the TCP port associated with this ICON server connection.
     *
     * @return The ssh TCP port..
     */
    public int getPort() { return port; }

    /**
     * @brief Get the active/inactive status for this ICON server connection.
     *
     * @return The ssh server name.
     */
    public boolean getActive() { return active; }

    /**
     * @brief Determine if one ICON Server is the same as another.
     * @param iconServer The ICON server object being compared.
     * @return true if they are the same, false if not.
     */
    public boolean equals(ICONServer iconServer) {
        boolean equals = false;

        if( iconServer.getUsername() == username &&
                iconServer.getServerName() == serverName &&
                iconServer.getPort() == port ) {
            equals = true;
        }

        return equals;
    }

    /**
     * @brief Convert this object into its string representation
     */
    public String toString() {
        String repr = getUsername()+"@"+getServerName()+":"+getPort();

        if( active ) {
            repr = repr + " (active)";
        }
        else {
            repr = repr + " (inactive)";
        }

        return  repr;
    }

    /**
     * @brief Set the parameters based on the string representation of an object.
     * @param stringRepr The string representation of the ICONServer object.
     */
    public void setFromString(String stringRepr) {
        String elementsA[] = stringRepr.split("@");
        if( elementsA.length > 1 ) {
            setUsername(elementsA[0]);
            String elementsB[] = elementsA[1].split(":");
            if( elementsB.length > 1 ) {
                setServerName(elementsB[0]);
                String elementsC[] = elementsB[1].split(" ");
                if( elementsC.length > 1 ) {
                    setPort( Integer.parseInt(elementsC[0]) );
                    if( stringRepr.indexOf(" (active)") != -1 ) {
                        setActive(true);
                    }
                    else {
                        setActive(false);
                    }
                }
            }
        }
    }

}
