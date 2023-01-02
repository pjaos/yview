#!/usr/bin/env python3

import  os
import  socket
import  sys
import  traceback
import  threading
import  _thread
import  json
from    optparse import OptionParser
from    time import sleep
import  getpass
import  paho.mqtt.client as mqtt
from    paramiko import AuthenticationException
from    texttable import Texttable

from    p3lib.helper import GetFreeTCPPort
from    p3lib.mqtt_rpc import MQTTRPCCallerClient
from    p3lib.uio import UIO as UO
from    p3lib.ssh import SSH, SSHTunnelManager
from    p3lib.pconfig import ConfigManager
from    p3lib.boot_manager import BootManager
from    p3lib.netif import NetIF

LOCALHOST = 'localhost'
LOCALHOST_IP = '127.0.0.1'

class IconsClientError(Exception):
  pass

class IconsGWError(Exception):
  pass

class IconsGWConfig(object):
    """@brief Responsible for managing the icons dest client configuration."""

    USERNAME                            = "username"
    SERVER                              = "server"
    SERVER_PORT                         = "server_port"
    LOCATION                            = "location"
    AYT_MSG                             = "are_you_there_message"
    PRIVATE_KEY_FILE                    = "private_key_file"
    AYT_DISCOVERY_INTERFACE             = "device_discovery_interface"    

    DEFAULT_GROUP_NAME       = "none"
    DISABLE_SYSLOG_FILE      = "disable_icons_gw_syslog"
    MQTT_SERVER_PORT         = 1883
    DEFAULT_DEV_POLL_PERIOD  = 10
    CONFIG_FILENAME          = "icons_gw.cfg"
    DEFAULT_AYT_MSG          = "-!#8[dkG^v's!dRznE}6}8sP9}QoIR#?O&pg)Qra"
    
    DEFAULT_CONFIG = {
        SERVER:                     "",
        SERVER_PORT:                "22",
        USERNAME:                   "",
        LOCATION:                   "",
        AYT_MSG:                    DEFAULT_AYT_MSG,
        PRIVATE_KEY_FILE:           "",
        AYT_DISCOVERY_INTERFACE:    ""
    }
    
    @staticmethod
    def GetHomePath():
        """Get the user home path as this will be used to store config files"""
        if "HOME" in os.environ:
            return os.environ["HOME"]

        elif "HOMEDRIVE" in os.environ and "HOMEPATH" in os.environ:
            return os.environ["HOMEDRIVE"] + os.environ["HOMEPATH"]

        elif "USERPROFILE" in os.environ:
            return os.environ["USERPROFILE"]

        return None


    @staticmethod
    def GetConfigFile(filename):
        """@brief Get the full path of a connfig file.
           @param The base filename for the config file."""
        homePath = IconsGWConfig.GetHomePath()

        if not filename.startswith("."):
            filename = "."+filename

        return os.path.join(homePath, filename)

    def __init__(self, uio, configFile):
        """@brief Constructor.
           @param uio UIO instance.
           @param configFile Config file instance."""
        self._uio = uio
        self._configManager = ConfigManager(uio, configFile, IconsGWConfig.DEFAULT_CONFIG)
        self._configManager.load()

    def configure(self):
        """@brief configure the required parameters for normal operation."""
        self._configManager.configure(self._editConfig)
        
    def _showAvalableNetworkInterfaces(self):
        """@param Show the use the available network interfaces.
           @return A list of network interface names in the order in which they 
                   appear in the displayed list."""
        self._uio.info("Available network interfaces.")
        self._uio.info("ID    Name            Address")
        netIF = NetIF()
        ifDict = netIF.getIFDict()
        nameList = []
        ifNameID = 1
        self._uio.info("{: <2d}    {}".format(ifNameID, "All Interfaces"))
        ifNameID = ifNameID + 1
        for ifName in list(ifDict.keys()):
            ipsStr = ",".join(ifDict[ifName])
            if ifName == "lo":
                continue
            self._uio.info("{: <2d}    {: <10s}      {}".format(ifNameID, ifName, ipsStr))
            nameList.append(ifName)
            ifNameID = ifNameID + 1
        return nameList
                 
    def _enterDiscoveryInterface(self):
        """@brief Allow the user to enter a network interface name to discover YView devices on.
                  This is optional. If the user enters none then AYT broadcast messages are sent 
                  over all network interfaces."""
        self._uio.info("Select the interface/s to find YView devices over.")
        self._uio.info("")
        ifNameList = self._showAvalableNetworkInterfaces()
        idSelected = ConfigManager.GetDecInt(self._uio, "Enter the ID from the above list", minValue=1, maxValue=len(ifNameList)+1)
        if idSelected == 1:
             self._configManager.addAttr(IconsGWConfig.AYT_DISCOVERY_INTERFACE, None)
        else:
            selectedIndex = idSelected-2
            if selectedIndex < len(ifNameList):
                self._configManager.addAttr(IconsGWConfig.AYT_DISCOVERY_INTERFACE, ifNameList[selectedIndex])
            else:
                raise Exception("{} ID is not valid for {} interface list.".format(idSelected, ",".join(ifNameList)))
         
    def _editConfig(self, key):
        """@brief Edit an icons_gw persistent config attribute.
           @param key The dictionary key to edit."""
        if key == IconsGWConfig.SERVER:
            self._configManager.inputStr(IconsGWConfig.SERVER, "The ICON server (ICONS) address", False)
            
        elif key == IconsGWConfig.SERVER_PORT:
            self._configManager.inputDecInt(IconsGWConfig.SERVER_PORT, "The ICON server TCP IP port", 1, 65535)

        elif key == IconsGWConfig.USERNAME:
            self._configManager.inputStr(IconsGWConfig.USERNAME, "The ICONS username", False)

        elif key == IconsGWConfig.LOCATION:
            self._configManager.inputStr(IconsGWConfig.LOCATION, "The location name for this ICONS destination client", False)

        elif key == IconsGWConfig.AYT_MSG:
            self._uio.info("Default AYT message: {}".format(IconsGWConfig.DEFAULT_AYT_MSG))
            self._configManager.inputStr(IconsGWConfig.AYT_MSG, "The devices 'Are You There' message text (min 8, max 64 characters)", False)

        elif key == IconsGWConfig.PRIVATE_KEY_FILE:
            privateKeyFile = self._configManager.getAttr(IconsGWConfig.PRIVATE_KEY_FILE)
            #If no key file has been defined then set the default key file
            if len(privateKeyFile) == 0:
                privateKeyFile = SSH.GetPrivateKeyFile()
                self._configManager.addAttr(IconsGWConfig.PRIVATE_KEY_FILE, privateKeyFile)

            self._configManager.inputStr(IconsGWConfig.PRIVATE_KEY_FILE, "The local ssh private key file to use when connecting to the ICONS", False)

        elif key == IconsGWConfig.AYT_DISCOVERY_INTERFACE:
            self._enterDiscoveryInterface()
            
    def loadConfigQuiet(self):
        """@brief Load the config without displaying a message to the user."""
        self._configManager.load(showLoadedMsg=False)

    def updateOptions(self, options):
        """@brief Update the options instance with the config parameters."""
        options.server      = self._configManager.getAttr(IconsGWConfig.SERVER)
        options.server_port = self._configManager.getAttr(IconsGWConfig.SERVER_PORT)
        options.username    = self._configManager.getAttr(IconsGWConfig.USERNAME)
        options.location    = self._configManager.getAttr(IconsGWConfig.LOCATION)
        options.ayt_msg     = self._configManager.getAttr(IconsGWConfig.AYT_MSG)
        options.private_key = self._configManager.getAttr(IconsGWConfig.PRIVATE_KEY_FILE)
        options.net_if      = self._configManager.getAttr(IconsGWConfig.AYT_DISCOVERY_INTERFACE)

class ServiceConfigurator(object):
    """@brief Responsible for loading editing and saving service configurations"""

    CONFIG_FILENAME = '.icons_gw_service_list.cfg'
    LINUX = "linux"

    @staticmethod
    def GetConfigFile():
        if ServiceConfigurator.IsLinux():

            return "/etc/icons_gw_service.cfg"

        else:
            #For other OS we currently expect the config file to be in the users home folder
            return os.path.join( IconsGWConfig.GetHomePath() , ServiceConfigurator.CONFIG_FILENAME)

    @staticmethod
    def GetServiceList():
        """@brief Get the configured services"""
        serviceList = []

        try:
            fd = open( ServiceConfigurator.GetConfigFile(), 'r')
            lines = fd.readlines()
            fd.close()

            for line in lines:
                line = line.rstrip("\n")
                elems = line.split(',')
                #Originally we had not device type field in the service config
                #So handle old config files here.
                if len(elems) == 4:
                    serviceConfig = ServiceConfig("Server", elems[0], int(elems[1]), elems[2], elems[3])
                    serviceList.append(serviceConfig)
                elif len(elems) == 5:
                    serviceConfig = ServiceConfig(elems[0], elems[1], int(elems[2]), elems[3], elems[4])
                    serviceList.append(serviceConfig)

        except IOError:
            pass

        return serviceList

    @staticmethod
    def GetLocalServicePortList():
        """@brief Get a list of the local TCP ports configured on services provided by this machone"""
        portList = []
        srvList = ServiceConfigurator.GetServiceList()
        for serviceConfig in srvList:
            if serviceConfig.host == socket.gethostname() or serviceConfig.host == LOCALHOST_IP or serviceConfig.host == LOCALHOST:
                portList.append(serviceConfig.port)
        return portList

    @staticmethod
    def SaveService(serviceConfig, uio=None):
        """@brief Save the service attributes to the config file
           param uio Optionally a UIO instance."""
        serviceList = ServiceConfigurator.GetServiceList()
        serviceList.append(serviceConfig)
        ServiceConfigurator.SaveServiceList(serviceList, uio)

    @staticmethod
    def SaveServiceList(serviceList, uio=None):
        """@brief Save the serviceList to a file
           param uio Optionally a UIO instance."""
        cfgFile = ServiceConfigurator.GetConfigFile()
        fd = open( cfgFile, 'w')
        for service in serviceList:
            fd.write("%s\n" % ( str(service) ) )
        fd.close()
        if uio:
            uio.info("Saved service list to %s" % (cfgFile) )

    @staticmethod
    def IsLinux():
        """@Determine if running on a Linux system"""
        if sys.platform.startswith(ServiceConfigurator.LINUX):
            return True
        return False

    @staticmethod
    def EnsureSudo():
        """@brief Ensure that you are running as a sudo user."""
        if os.getuid() != 0:
            raise IconsGWError("Please run this command using sudo.")

    def __init__(self, uo):
        self._uo = uo

        if ServiceConfigurator.IsLinux():
            ServiceConfigurator.EnsureSudo()

    def showServiceList(self, serviceList):
        """@brief Show the services currently configured.
           @param The service list"""

        table = Texttable(max_width=0)
        table.set_deco(Texttable.BORDER | Texttable.VLINES)

        table.add_row(["ID","Device Type", "Service Name","TCP Port","Host Address", "Group"])

        id=1
        for service in serviceList:
            table.add_row([id, service.deviceType, service.serviceName, service.port, service.host, service.groupName])
            id=id+1

        tableText = table.draw() + "\n"
        lines = tableText.split('\n')
        for l in lines:
            self._uo.info(l)


    def getValidInput(self, prompt, allowNoInput=False, allowSpaceChars=False):
        """@brief Get valid input from the user"""
        response = self._uo.getInput(prompt)

        if not allowNoInput and len(response) == 0:
            self._uo.error("Invalid input")
            return None

        if not allowSpaceChars and " " in response:
            self._uo.error("Invalid input (space characters detected).")
            return None

        if "," in response:
            self._uo.error("Invalid input (may not include commas).")
            return None

        if ":" in response:
            self._uo.error("Invalid input (may not include colons).")
            return None

        return response

    def _GetYes(self, question):
        """@brief Get a yes/no response from the user.
                  If user responds quit or q then the program exists.
           @return True if user respons yes or y, False if the user responds no or n."""
        while True:
            response = self.getValidInput("%s [y]es, [n]o or [q]uit: " % (question) )
            response = response.lower()
            if response in ['y', 'yes']:
                return True
            elif response in ['n', 'no']:
                return False
            elif response in ['q', 'quit']:
                return sys.exit(0)

    def getInteger(self, prompt, minPort, maxPort):
        """@Get a valid port number"""
        while True:
            response = self.getValidInput(prompt)

            try:

                try:
                    port = int(response)

                    if port < minPort:
                        self._uo.error("%d is invalid (min=%d)." % (port, minPort) )

                    elif port > maxPort:
                        self._uo.error("%d is invalid (max=%d)." % (port, maxPort) )

                    else:
                        return port

                except ValueError:
                    self._uo.error("%s is an invalid number (not an integer value)." % (response))

            except TypeError:
                return None

    def addService(self):
        """@brief Add a service to the list of configured services"""
        self._uo.info("Device Type")
        self._uo.info("This can be any string (not containing commas) that describes the device.")
        self._uo.info("E.G Server, Heating Controller, etc...")
        deviceType = self.getValidInput("Enter the device type", allowNoInput=False, allowSpaceChars=True )
        if deviceType == None:
            return

        self._uo.info("Service Name")
        self._uo.info("This should identify the service being provided.")
        self._uo.info("E.G Server, ssh or http or vnc, etc...")
        serviceName = self.getValidInput("Enter service name")
        if serviceName == None:
            return
        serviceName=serviceName.upper()

        port = self.getInteger("The TCP IP port that the machine provides the service on.", 1, 65534)
        host = self._uo.getInput("The address of the machine providing this service.")

        groupName = self.getValidInput("Enter group name (default=%s)" % (IconsGWConfig.DEFAULT_GROUP_NAME), allowNoInput=True )
        if len(groupName) == 0 or groupName.lower() == "none":
            groupName = IconsGWConfig.DEFAULT_GROUP_NAME

        serviceConfig = ServiceConfig(deviceType, serviceName, port, host, groupName)
        ServiceConfigurator.SaveService(serviceConfig, uio=self._uo)

    def deleteService(self):
        """@brief Delete service from the list of configured services"""
        serviceList = ServiceConfigurator.GetServiceList()
        _id = self.getInteger("ID of service to delete", 1, len(serviceList) )
        if _id:
            self._uo.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            self._uo.warn("!!! Deleting the service will cause the icons dest client !!!")
            self._uo.warn("!!! to drop it's connection to the server and re connect. !!!")
            self._uo.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            if self._GetYes("Are you sure you wish to delete ID=%d" % (_id) ):
                del serviceList[_id-1]
                ServiceConfigurator.SaveServiceList(serviceList, uio=self._uo)

    def _getUpdatedServiceConfig(self, serviceConfig):
        """@brief Allow the user to change the service config.
           @param serviceConfig The ServiceConfig instance."""
        self._uo.info("Device Type")
        self._uo.info("This can be any string (not containing commas) that describes the device.")
        self._uo.info("E.G Server, Heating Controller, etc...")
        deviceType = self.getValidInput("Enter the device type [%s] " % (serviceConfig.deviceType), allowNoInput=True, allowSpaceChars=True )
        if not deviceType:
            deviceType = serviceConfig.deviceType

        self._uo.info("Service Name")
        self._uo.info("This should identify the service being provided.")
        self._uo.info("E.G Server, ssh or http or vnc, etc...")
        serviceName = self.getValidInput("Enter service name [%s] " % (serviceConfig.serviceName) )
        if serviceName == None:
            serviceName = serviceConfig.serviceName
        serviceName=serviceName.upper()

        minPort = 1
        maxPort = 65534
        while True:
            portStr = self.getValidInput("TCP IP port [%d] " % (serviceConfig.port), allowNoInput=True, allowSpaceChars=False )
            if not portStr:
                port = serviceConfig.port
                break
            else:
                try:
                    port = int(portStr)
                    if port < 1:
                        self._uo.error("%d is an invalid port (min=%d)." % (port, minPort))
                    elif port > 65535:
                        self._uo.error("%d is an invalid port (max=%d)." % (port, maxPort))
                    else:
                        break
                except ValueError:
                    self._uo.error("%s is an invalid port (not an integer value)." % (portStr))

        host = self._uo.getInput("The address of the machine providing this service [%s] " % (serviceConfig.host) )
        if not host:
            host = serviceConfig.host  

        groupName = self.getValidInput("Enter group name [%s] " % (serviceConfig.groupName), allowNoInput=True )
        if not groupName or len(groupName) == 0 or groupName.lower() == "none":
            groupName = IconsGWConfig.DEFAULT_GROUP_NAME

        return ServiceConfig(deviceType, serviceName, port, host, groupName)

    def editService(self):
        """@brief Edit a service in the list of configured services"""
        serviceList = ServiceConfigurator.GetServiceList()
        _id = self.getInteger("ID of service to edit", 1, len(serviceList) )
        if _id:
            serviceIndex = _id-1
            serviceList = ServiceConfigurator.GetServiceList()
            updatedService = self._getUpdatedServiceConfig(serviceList[serviceIndex])
            if updatedService != None:
                serviceList[serviceIndex]=updatedService
                ServiceConfigurator.SaveServiceList(serviceList, uio=self._uo)

    def displayServices(self):
        """@brief display a list of the configured services."""
        pass

    def editServices(self, uo):
        """@brief Allow the user to edit a list of services.
           @param UserOutput object"""
        while True:
            self.showServiceList( ServiceConfigurator.GetServiceList() )
            response = self._uo.getInput("[A]dd, [D]elete, [E]dit or [Q]uit service configuration")
            response = response.lower()

            if response == 'a':
                self.addService()

            elif response == 'd':
                self.deleteService()

            elif response == 'e':
                self.editService()

            elif response == 'q':
                break

class ServiceConfig(object):
    """@brief Responsible for holding the attributes of a service"""

    def __init__(self, deviceType, serviceName, port, host, groupName):
        self.deviceType = deviceType
        self.serviceName = serviceName
        self.port = port
        self.host = host
        self.groupName = groupName

    def __str__(self):
        return "%s,%s,%d,%s,%s" % (self.deviceType, self.serviceName, self.port, self.host, self.groupName)

class AreYouThereThread(threading.Thread):
    """Responsible for sendng UDP messages that are intended to elict responses"""

    MULTICAST_ADDRESS       = "255.255.255.255"
    UDP_DEV_DISCOVERY_PORT  = 2934

    @staticmethod
    def GetJSONAYTMsg(ayt_msg_str):
        """@brief Get the JSON text string of the AYT message
           @return The AYT message text."""
        aytDict={"AYT": ayt_msg_str}
        return IconsClient.DictToJSON(aytDict)

    @staticmethod
    def IsAYTMsg(msg, exepectedAYTString):
        aytMsg = False
        try:
            aDict = IconsClient.JSONToDict(msg)
            if "AYT" in aDict:
                aytString = aDict["AYT"]
                if aytString == exepectedAYTString:
                    aytMsg = True
        except:
            pass
        return aytMsg

    @staticmethod
    def GetSubnetMultiCastAddress(ifName):
        """@brief Get the subnet multicast IP address for the given interface.
           @param ifName The name of a local network interface.
           @return A list of all the subnet multicast IP addresses."""
        subNetMultiCastAddressList = []
        netIF = NetIF()
        ifDict = netIF.getIFDict()
        if ifName in ifDict:
            ipList = ifDict[ifName]
            for elem in ipList:
                elems = elem.split("/")
                if len(elems) == 2:
                    # Extract the interface IP address. Calc the multicast IP address 
                    # for the subnet and add this to the list for the interface.
                    try:
                        ipAddress = elems[0]
                        subNetMaskBitCount = int(elems[1])
                        intIP = NetIF.IPStr2int(ipAddress)
                        subNetBits = (1<<(32-subNetMaskBitCount))-1
                        intMulticastAddress = intIP | subNetBits
                        subNetMultiCastAddress = NetIF.Int2IPStr(intMulticastAddress)
                        subNetMultiCastAddressList.append(subNetMultiCastAddress)
                    except ValueError:
                        pass
            
        else:
            raise Exception("{} is not a local network interface name.".format(ifName))
        
        return subNetMultiCastAddressList
    
    def __init__(self, uo, sock, options):
        """@brief Constructor
           @param uo The UserOutput object
           @param sock The UDP socket to send AYT messages on
           @param options Command line options."""
        threading.Thread.__init__(self)
        self._running = False
        self.setDaemon(True)
        self._uo = uo
        self._sock = sock
        self._options = options
        self._messagePeriod = self._options.dev_poll_period
        self._aytMsg = self._options.ayt_msg

    def run(self):
        self._running = True
        self._uo.info("Started the AYT thread")
        while self._running:
            if self._options.no_lan:
                destAddressList = (LOCALHOST_IP,)
            else:
                # If the user configured YView devices discovery on all interfaces.
                if self._options.net_if is None:
                    destAddressList = (AreYouThereThread.MULTICAST_ADDRESS,)
                else:
                    destAddressList = AreYouThereThread.GetSubnetMultiCastAddress(self._options.net_if)
            try:
                for destAddress in destAddressList:
                    self._sock.sendto( str.encode( AreYouThereThread.GetJSONAYTMsg(self._aytMsg) ), (destAddress, AreYouThereThread.UDP_DEV_DISCOVERY_PORT) )
            except:
                self._uo.error("Failed to send AYT message.")
                self._uo.errorException()

            sleep(self._messagePeriod)
        self._uo.info("Shutdown the AYT thread")

    def shutDown(self):
        """@brief shutdown the thread"""
        self._running = False

class RPCCallerOptions(object):
    """@brief Responsible for holding the options required for the MQTT RPC caller"""
    def __init__(self, host, port, sid, cid, keepalive):
        """@brief Constructor
           @param host The MQTT server address
           @param port The TCPIP port of the MQTT server
           @param sid The MQTT RPC server identifier. This provides the RPC.
           @param cid The identifier of the MQTT client.
           @param keepalive The MQTT server connection keepalive time in seconds."""
        self.server=host
        self.port=port
        self.sid=sid
        self.cid=cid
        self.keepalive=keepalive

class IconsClient(object):

    ICONS_RECONNECT_DELAY           = 5
    MQTT_DEFAULT_KEEPALIVE_SECONDS  = 60

    JSON_UNIT_NAME           = "UNIT_NAME"
    JSON_GROUP_NAME          = "GROUP_NAME"
    JSON_PRODUCT_ID          = "PRODUCT_ID"
    JSON_IP_ADDRESS_KEY      = "IP_ADDRESS"
    JSON_SERVICE_LIST        = 'SERVICE_LIST'
    JSON_SERVER_SERVICE_LIST = 'SERVER_SERVICE_LIST'
    JSON_LOCATION            = "LOCATION"

    RPC_SERVER_ID            = 1

    @staticmethod
    def DictToJSON(aDict):
        """@brief convert a python dictionary into JSON text
           @param aDict The python dictionary to be converted
           @return The JSON text representing aDict"""
        return json.dumps(aDict)

    @staticmethod
    def JSONToDict(jsonText):
        """@brief Convert from JSON text to a python dict. Throws a ValueError
                  if the text is formatted incorrectly.
           @param jsonText The JSON text to be converted to a dict
           @return a python dict object"""
        return json.loads(jsonText)

    @staticmethod
    def ReportException(uo, ex, debug):
        """@brief Report an exception. If debug is set then a stack trasce is
                  reported.
           @param ex The exception to report"""
        if debug:
            lines = traceback.format_exc().splitlines()
            for line in lines:
                uo.error(line)
        else:
            uo.error(ex)

    @staticmethod
    def TX_UDP(theDict, addressPort):
        """"@brief Send the service details to the remote host in a UDP packet.
            @param theDict, The device dict
            @param addressPort A tuple of the address and port number to send the data to."""
        msg = IconsClient.DictToJSON(theDict)
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.sendto(msg.encode(), addressPort)

    def __init__(self, uo, options):
        """@brief Constructor
           @param uop A UIO instance
           @param options An options instance with the following attrs
                  options.location = A string that describes the location of this destination client (may not start /)
           """
        self._uo = uo
        self._options = options

        self._knownDeviceList = []
        self._ssh = None
        self._sshTunnelManager = None
        self._mqttClient = None
        self._mqttRPCCallerClient = None
        self._mqttClientConnected = False

    def on_connect(self, client, userdata, flags, rc):
        """@brief called on completion of the connection attempt."""
        if rc == 0:
            self._mqttClientConnected=True
            self._uo.info("Connected to the MQTT server via local port %d" % (self._localMQTTPort) )
        else:
            self._mqttClientConnected = False
            self._uo.error("Failed to connect to the MQTT server via local port %d (return code = %d)" % (self._localMQTTPort, rc) )

        self._connectAttemptInProgress = False

    def on_disconnect(self, client, userdata, rc):
        self._uo.info("MQTT client dissconnected")
        self._mqttClientConnected = False

    def _waitForConnectSuccess(self, pollSeconds=0.5):
        """@brief Wait for the connect attempt to succeed.
           @param pollSeconds he poll time in seconds."""
        while self._connectAttemptInProgress:
            sleep(pollSeconds)
            self._uo.info("Connecting to the MQTT server.")

        if not self._mqttClientConnected:
            raise IconsClientError("Failed to connect to MQTT server.")

    def isConnected(self):
        """@brief Get the connected status.
           @return True if the MQTT client is connected."""
        return self._mqttClientConnected

    def _connectToMQTTServer(self):
        """@brief Connect to the MQTT server through an ssh tunnel."""

        self._uo.info("Connecting to MQTT server on ssh server")
        self._localMQTTPort = GetFreeTCPPort() #Get a free TCPIP port on the local machine
        self._sshTunnelManager = SSHTunnelManager(self._uo, self._ssh, not self._options.no_comp )
        self._sshTunnelManager.startFwdSSHTunnel(self._localMQTTPort, LOCALHOST, self._options.mqtt_port)
        self._uo.info("Connecting local port %d to the %d port on the ssh server (%s:%d)" % (self._localMQTTPort, self._options.mqtt_port, self._options.server, self._options.server_port) )

        self._uo.info("Connecting to MQTT server (port %d) on ssh server" % (self._options.mqtt_port) )
        self._mqttClient = mqtt.Client()
        self._mqttClient.on_connect = self.on_connect
        self._mqttClient.on_disconnect = self.on_disconnect
        self._connectAttemptInProgress = True
        self._mqttClient.connect(LOCALHOST, self._localMQTTPort, self._options.keepalive)
        _thread.start_new_thread( self._mqttClient.loop_forever, () )
        self._waitForConnectSuccess()

        self._uo.info("Connecting to MQTT RPC server (port %d) on ssh server" % (self._localMQTTPort) )
        options = RPCCallerOptions(LOCALHOST, self._localMQTTPort, IconsClient.RPC_SERVER_ID, self._options.location, self._options.keepalive)
        self._mqttRPCCallerClient = MQTTRPCCallerClient(self._uo, options)
        self._mqttRPCCallerClient.connect()
        _thread.start_new_thread( self._mqttRPCCallerClient.loopForever, () )
        # PJA Not sure this is needed
        #self._mqttRPCCallerClient.waitForConnectSuccess() 

    def _startServerWithException(self):
        """@brief Called to connect to the ssh server running the ICONS MQTT server.
                  Exceptions will be thrown from this method in the event of errors (E.G network issues)"""
        self._ssh = None
        try:
            if not self._options.username:
                raise IconsClientError("Please configure a username and try again.")

            if not self._options.server:
                raise IconsClientError("Please configure a server and try again.")

            if not self._options.private_key:
                raise IconsClientError("Please configure a private ssh key file and try again.")

            if not os.path.isfile(self._options.private_key):
                raise IconsClientError("%s file not found. Please reconfigure and try again." % (self._options.private_key) )

            #Build an ssh connection to the ICON server
            self._ssh = SSH(self._options.server, 
                            self._options.username, 
                            port=self._options.server_port, 
                            useCompression=not self._options.no_comp, 
                            privateKeyFile=self._options.private_key, 
                            uio=self._uo)
            self._ssh.connect()
            self._locaIPAddress = self._ssh.getLocalAddress()
            self._uo.info("Connected to ssh server %s on port %d" % (self._options.server, self._options.server_port) )

            self._connectToMQTTServer()

            #This method must be implemented in a sub class
            self._handleConnection()

        finally:

            self._shutDown()

    def _shutDown(self):
        """@brief shutdown all connection used by the client."""
        self._uo.info("Shutting down client connections")

        if self._mqttClient:
            self._mqttClient.disconnect()
            self._mqttClient = None

        if self._mqttRPCCallerClient:
            self._mqttRPCCallerClient._client.disconnect()
            self._mqttRPCCallerClient = None

        self._knownDeviceList       = []

        if self._sshTunnelManager:
            self._sshTunnelManager.stopAllSSHTunnels()
            self._sshTunnelManager = None

        if self._ssh:
            self._ssh.close()
            self._ssh = None
            self._uo.info("Closed ssh connection to %s on port %d" % (self._options.server, self._options.server_port) )

    def setupAutologin(self):
      """Setup autologin on the ssh server."""

      publicKeyFile = "{}.pub".format(self._options.private_key)
      if not os.path.isfile(publicKeyFile):
          raise IconsClientError("{} public key file not found. Please create a public/private key pair and try again.".format(publicKeyFile) )

      self._uo.warn("Auto login to the ssh server failed authentication.")
      self._uo.info("Copying the local public ssh key to the ssh server for automatic login.")
      self._uo.info("Please enter the ssh server ({}) password for the user: {}".format(self._options.server, self._options.username))

      sshPassword = self._uo.getPassword()

      ssh = SSH(self._options.server,
          self._options.username,
          password=sshPassword,
          port=self._options.server_port,
          uio=self._uo)
      ssh._ensureAutoLogin()
      ssh.close()
      self._uo.info("Local public ssh key copied to the ssh server.")
      self._uo.info("Please restart to connect to the ssh server without a password.")
      sys.exit(0)

    def runClientConnection(self):
        """@brief Called to connect to the server and handle all responses from it."""
        while True:
            try:

                self._startServerWithException()

            except AuthenticationException:
                self.setupAutologin()

            except SystemExit:
              raise

            #Don't print error information if CTRL C pressed
            except KeyboardInterrupt:
                raise

            except Exception as ex:
                if self._options.debug:
                    self._uo.errorException()
                else:
                    self._uo.error(ex)

            self._uo.error("Waiting %d seconds before attempting to reconnect to ICON server." % (IconsClient.ICONS_RECONNECT_DELAY) )
            sleep(IconsClient.ICONS_RECONNECT_DELAY)


class IconsGW(IconsClient):

    UDP_RX_BUFFER_SIZE       = 2048

    def __init__(self, uo, options):
        """@brief Constructor
           @param uop A UIO instance
           @param options An options instance with the following attrs
                  options.location = A string that describes the location of this destination client (may not start /)
           """
        IconsClient.__init__(self, uo, options)

        self._shutdownServer        = False
        self._knownDeviceList       = []

        self._user = getpass.getuser()

    def runClient(self):
        """@brief Start the dest client."""

        if not self._options.username:
            raise IconsGWError("Please configure a username and try again.")

        if not self._options.location:
            raise IconsGWError("Please configure a location and try again.")

        if not self._options.server:
            raise IconsGWError("Please configure a server and try again.")

        if not self._options.private_key:
            raise IconsGWError("Please configure a private ssh key file and try again.")

        self.runClientConnection()

    def _handleConnection(self):
        """@brief Discover devices on the local LAN.
                If a device is found then
                    - Let the ICON server know about the device
                    - Setup ssh port forwarding to the server
                If communication with a device is lost then
                    - Send a command to remove it from the list that the ICON server is aware of
                    - shutdown the associated ssh port forwarding connection"""
        sock                = None
        areYouThereThread   = None
        try:
            #Open UDP socket to be used for discovering devices
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            sock.bind(('', AreYouThereThread.UDP_DEV_DISCOVERY_PORT))

            #Start the thread that sends AYT messages to elicit device responses
            areYouThereThread = AreYouThereThread(self._uo, sock, self._options)
            areYouThereThread.start()

            self._listenForDevResponses(sock)

        finally:

            if areYouThereThread:
                areYouThereThread.shutDown()
                areYouThereThread = None

            if sock:
                sock.close()
                sock=None
                self._uo.info("Closed UDP device discovery socket.")

    def _isValidDevice(self, devDict):
        """@brief Determine if the device is valid.
           @param devDict The dictionary of the devices parameters."""
        if IconsGW.JSON_IP_ADDRESS_KEY in devDict:
            return True
        return False

    def _getKnownDevDict(self, localIPAddress):
        """@brief Get a known device dict from the know dev dict or return None
                  if unknown.
           @param localIPAddress The local IP address"""
        for knownDev in self._knownDeviceList:
            if knownDev[IconsGW.JSON_IP_ADDRESS_KEY] == localIPAddress:
                return knownDev
        return None

    def _isKnownDevice(self, devDict):
        """@brief Determine if we know about this device
           @param devDict The dictionary of the devices parameters
           @return True if the ssh TCP reverse forwarding server has been setup
                   for this device, False if not."""
        isKnownDev = False
        if self._getKnownDevDict(devDict[IconsGW.JSON_IP_ADDRESS_KEY]):
            isKnownDev = True

        return isKnownDev

    def _serverServiceListCountChanged(self, devDict):
        """@brief Determine if this the number of services provided by this dest client has changed.
           I.E the user has manually changed the number of services by using the --services command line option.
           @param devDict The dictionary of the devices parameters.
           @return True if updated, false if not."""
        knownDev = self._getKnownDevDict( devDict[IconsGW.JSON_IP_ADDRESS_KEY] )
        if knownDev:
            if IconsGW.JSON_SERVER_SERVICE_LIST in knownDev and IconsGW.JSON_SERVICE_LIST in devDict:
                oldSrvCount = len( knownDev[IconsGW.JSON_SERVER_SERVICE_LIST].split(',') )
                newSrvCount = len( devDict[IconsGW.JSON_SERVICE_LIST].split(',') )
                #If the number of services  has changed
                if oldSrvCount != newSrvCount:
                    return True
        return False

    def _updateServiceList(self, rxDict, serviceName, servicePort, server=False):
        """@brief Update the service list field in the rxDict to include details of
                  service
           @param rxDict The device dict to be updated.
           @param serviceName The name of the service.
           @param The TCP port that the service is provided on.
           @param server If True then the ports are added to the server service
                         list, else they are added to the local port list"""

        if server:
            keyName = IconsGW.JSON_SERVER_SERVICE_LIST
        else:
            keyName = IconsGW.JSON_SERVICE_LIST

        if not keyName in rxDict:
            rxDict[keyName]="%s:%d" % (serviceName, servicePort)
        else:
            serviceStr = rxDict[keyName]
            serviceStr = "%s,%s:%d" % (serviceStr, serviceName, servicePort)
            rxDict[keyName]=serviceStr

    def _getFreeServerPort(self):
        """@brief Get a free server TCP port
           @return The free TCP port number"""
        return self._mqttRPCCallerClient.rpcCall("getFreeTCPPort", "")

    def _setupDeviceReverseForwarding(self, devDict):
        """@brief Setup an ssh reverse port forwarding connection.
           @param devDict The dictionary of the devices parameters."""

        #The device dict must have a service list
        if IconsGW.JSON_SERVICE_LIST in devDict:
            serviceList = devDict[IconsGW.JSON_SERVICE_LIST].split(',')
            for service in serviceList:
                elems = service.split(':')
                #If incorrectly formatted service ignore it.
                if len(elems) != 2:
                    continue
                serviceName = elems[0]
                servicePortStr = elems[1]
                try:
                    servicePort = int(servicePortStr)

                    #Obtain a free port on the ICONS server`
                    freeServerTCPPort = self._getFreeServerPort()
                    if not freeServerTCPPort:
                        raise IconsGWError("Failed to get an available TCPIP port. Check MQTT RPC server is running.")

                    #Use it for a TCP server on the SSH server (reverse forwarded to the local network)
                    self._sshTunnelManager.startRevSSHTunnel(freeServerTCPPort, devDict[IconsGW.JSON_IP_ADDRESS_KEY], servicePort)

                    self._updateServiceList(devDict, serviceName, freeServerTCPPort, server=True)

                except ValueError:
                    pass

        self._knownDeviceList.append(devDict)

    def _updateServerPorts(self, devDict):
        """@brief Update the server port in the devDict as server port/s should
                  have been allocated and already be known but at this point will
                  only be present in the devDict the first time around when
                  _setupDeviceReverseForwarding() was called to allocated it/them.
           @param devDict The dictionary of the devices parameters."""
        knownDevDict = self._getKnownDevDict(devDict[IconsGW.JSON_IP_ADDRESS_KEY])
        if knownDevDict:

            if IconsGW.JSON_SERVER_SERVICE_LIST in knownDevDict:
                devDict[IconsGW.JSON_SERVER_SERVICE_LIST] = knownDevDict[IconsGW.JSON_SERVER_SERVICE_LIST]

    def _shutdownRevSSHTunnel(self, devDict):
        """@brief shutdown the reverse SSH tunnel
           @param devDict The dictionary of the devices parameters."""

        #The device dict must have a service list
        if IconsGW.JSON_SERVER_SERVICE_LIST in devDict:
            serviceList = devDict[IconsGW.JSON_SERVER_SERVICE_LIST].split(',')
            for service in serviceList:
                serviceName, serverServicePortStr = service.split(':')
                try:
                    serverServicePort = int(serverServicePortStr)
                    self._sshTunnelManager.stopRevSSHTunnel(serverServicePort)

                except ValueError:
                    pass

    def _removeKnownDev(self, devDict):
        """@brief Remove a device from the known devices list if present.
           @param devDict A dict of device data sent by the device to the
                  icons gateway."""
        indexToDelete = -1
        index = 0

        if self._isValidDevice(devDict):
            devIPAddress = devDict[IconsClient.JSON_IP_ADDRESS_KEY]
            for knownDev in self._knownDeviceList:
                if IconsClient.JSON_IP_ADDRESS_KEY in knownDev:
                    if devIPAddress == knownDev[IconsClient.JSON_IP_ADDRESS_KEY]:
                        indexToDelete=index
                        break
                index=index+1

            if indexToDelete >= 0:
                self._uo.debug("Removing from known device list: %s" % (devDict))
                del self._knownDeviceList[indexToDelete]

    def _getValidTopic(self, topic):
        """@brief Check the topic is valid and remove invalid characters.
           @return The valid topic."""
#PJA not sure these are the best replacement chars ?
        if topic.find('#') >= 0:
            topic = topic.replace('#', '>>1')

        if topic.find('+') >= 0:
            topic = topic.replace('+', '>>2')

        return topic

    def _processDevDict(self, devDict):
        """@brief Process the device dict from a single device.
           @param devDict A dict of device data sent by the device to the
                  icons gateway."""

        if self._isValidDevice(devDict):

            #Deal with old hardware here
            if IconsGW.JSON_SERVICE_LIST not in devDict:
                #Early hardware did not advertise supported services
                #However they all supported a web interface.
                devDict[IconsGW.JSON_SERVICE_LIST]="WEB:80"
                #WyTerm product also supported connection to it's serial port.
                if "PRODUCT_ID" in devDict:
                    productID = devDict["PRODUCT_ID"]
                    if productID == "WyTerm" and devDict[IconsGW.JSON_SERVICE_LIST].find("WYTERM_SERIAL_PORT") == -1:
                        devDict[IconsGW.JSON_SERVICE_LIST]=devDict[IconsGW.JSON_SERVICE_LIST]+",WYTERM_SERIAL_PORT:23"

            if not self._isKnownDevice(devDict):

                self._setupDeviceReverseForwarding(devDict)

            elif self._serverServiceListCountChanged(devDict):

                self._shutdownRevSSHTunnel(devDict)
                self._removeKnownDev(devDict)

            self._updateServerPorts(devDict)

            unitName=""
            if "UNIT_NAME" in devDict:
                unitName=devDict["UNIT_NAME"]
            mqttTopic = "%s/%s" % (self._options.location, unitName)

            devDict[IconsClient.JSON_LOCATION]= self._options.location
            json = IconsClient.DictToJSON(devDict)

            mqttTopic = self._getValidTopic(mqttTopic)

            self._mqttClient.publish(mqttTopic, json)

    def _getServiceDevDict(self, serviceDeviceList, ipAddress):
        """@brief Get the deviceDict object that has the given IP address
           @brief serviceDeviceList List of deviceDict objects.
           @brief ipAddress The device IP address
           @return The deviceDict object that has the ipAddress or None"""

        for serviceDevice in serviceDeviceList:
            if IconsGW.JSON_IP_ADDRESS_KEY in serviceDevice and serviceDevice[IconsGW.JSON_IP_ADDRESS_KEY] == ipAddress:
                return serviceDevice

        return None

    def _getServiceDeviceList(self, serviceConfigList):
        """@brief Get a list of devices for which services are defined statically
                  in a local config file.
           @brief serviceConfigList A List of serviceConfig objects loaded from local persistent storage."""

        serviceDeviceList = []

        for service in serviceConfigList:
            groupName = service.groupName
            groupName=groupName.lower()
            if groupName == IconsGWConfig.DEFAULT_GROUP_NAME:
                groupName=""

            serviceDevDict = self._getServiceDevDict(serviceDeviceList, socket.gethostbyname(service.host))

            if not serviceDevDict:

                serviceDevDict = {}
                serviceDevDict[IconsGW.JSON_UNIT_NAME] = service.host
                serviceDevDict[IconsGW.JSON_GROUP_NAME] = groupName
                serviceDevDict[IconsGW.JSON_PRODUCT_ID] = service.deviceType
                serviceDevDict[IconsGW.JSON_IP_ADDRESS_KEY] = socket.gethostbyname(service.host)
                self._updateServiceList(serviceDevDict, service.serviceName, service.port)
                serviceDeviceList.append(serviceDevDict)

            else:
                self._updateServiceList(serviceDevDict, service.serviceName, service.port)

        return serviceDeviceList

    def _sendServicesResponse(self, addressPort):
        """@brief Send a services response message.
                  This message details the locally configured services. These are typically
                  fixed address servers on this machine or the local network broadcast domain.
           @param addressPort A tuple of the IP address and port from which the AYT message was recieved."""
        self._serviceConfigList = ServiceConfigurator.GetServiceList()
        if len(self._serviceConfigList) > 0:
            serviceDeviceList = self._getServiceDeviceList(self._serviceConfigList)
            for serviceDevice in serviceDeviceList:
                IconsGW.TX_UDP(serviceDevice, addressPort)

    def _listenForDevResponses(self, sock):
        """@brief Listen for the UDP JSON messages sent by devices in response
                  to the discovery messages and add these responses to a queue.
           @param sock The socket to listen for UDP device response messages."""

        self._uo.info("Listening on UDP port %d" % (AreYouThereThread.UDP_DEV_DISCOVERY_PORT) )
        try:
            while self.isConnected():

                rxData, addressPort = sock.recvfrom(IconsGW.UDP_RX_BUFFER_SIZE)

                #Convert bytes received to a string instance
                rxData = rxData.decode("utf-8")

                if self._options.debug:
                    self._uo.debug("%s: DEVICE RX DATA: %s" % (str(addressPort), rxData))

                # If weve received an AYT message then send response.
                if AreYouThereThread.IsAYTMsg(rxData, self._options.ayt_msg):
                    self._sendServicesResponse(addressPort)

                else:
                    rxDict = None
                    #Try/except so that non json data can't crash the server
                    try:
                        rxDict = IconsGW.JSONToDict(rxData)
                        self._uo.info("Valid JSON data received from %s: %s" % (str(addressPort), str(rxData) ) )

                    except ValueError:
                        self._uo.error("Non JSON data received from %s: %s" % (str(addressPort), str(rxData) ) )

                    if rxDict:
                        self._processDevDict(rxDict)

        except:
            self._uo.info("Shutdown device listener (MQTT client connected = %d)" % (self.isConnected()) )
            self._uo.errorException()

        self._uo.info("Stopped listening for device responses.")

    def enableAutoStart(self, user):
        """@brief Enable this program to auto start when the computer on which it is installed starts.
           @param user The username which which you wish to execute on autostart."""
        bootManager = BootManager()
        if user:
            arsString = ""
            if self._options.log_file:
                arsString = "--log_file {}".format(self._options.log_file)

            if self._options.debug:
                arsString = "{} --debug".format(arsString)

            bootManager.add(user=user, argString=arsString, enableSyslog=self._options.enable_syslog)
        else:
            raise Exception("--user not set.")

    def disableAutoStart(self):
        """@brief Enable this program to auto start when the computer on which it is installed starts."""
        bootManager = BootManager()
        bootManager.remove()
        
    def checkAutoStartStatus(self):
        """@brief Check the status of a process previously set to auto start."""
        bootManager = BootManager()
        lines = bootManager.getStatus()
        if lines and len(lines) > 0:
            for line in lines:
                self._uo.info(line)
        

def main():
    uo = UO()
    debug = False

    try:

        opts=OptionParser(usage='Connects (via ssh) to an ICONS (internet connection server) and provides a gateway from the local network.')
        opts.add_option("--debug",              help="Enable debugging. If enabled then all RX device will be displayed on stdout.", action="store_true", default=False)
        opts.add_option("--config",             help="Configure the ICONS destination client.", action="store_true", default=False)
        opts.add_option("--mqtt_port",          help="The MQTT server TCPIP port on the ssh server port (default=%d)" % (IconsGWConfig.MQTT_SERVER_PORT) , type="int", default=IconsGWConfig.MQTT_SERVER_PORT)
        opts.add_option("--log_file",           help="A log file to save all output to (default=None)" , default=None)
        opts.add_option("--dev_poll_period",    help="The device poll period in seconds (default=%d)" % (IconsGWConfig.DEFAULT_DEV_POLL_PERIOD) , type="float", default=IconsGWConfig.DEFAULT_DEV_POLL_PERIOD)
        opts.add_option("--no_comp",            help="Disable SSH data compression. By default compression is used.", action="store_true", default=False)
        opts.add_option("--services",           help="Configure services to be provided by machines. These can be any network device that runs a TCP server (E.G SSH, VNC etc).", action="store_true", default=False)
        opts.add_option("--no_lan",             help="Do not attempt to discover devices on the LAN.", action="store_true", default=False)
        opts.add_option("--enable_syslog",      help="Enable syslog on this instance of icons_gw. By default syslog is disabled.", action="store_true", default=False)
        opts.add_option("--keepalive",          help="The number of seconds between each MQTT keepalive message (default=%d)." % (IconsClient.MQTT_DEFAULT_KEEPALIVE_SECONDS) , type="int", default=IconsClient.MQTT_DEFAULT_KEEPALIVE_SECONDS)
        opts.add_option("--enable_auto_start",  help="Auto start when this computer starts.", action="store_true", default=False)
        opts.add_option("--disable_auto_start", help="Disable auto starting when this computer starts.", action="store_true", default=False)
        opts.add_option("--check_auto_start",   help="Check the status of an auto started icons_gw instance.", action="store_true", default=False)
        opts.add_option("--user",               help="Set the user for auto start.")

        (options, args) = opts.parse_args()

        debug = options.debug
        uo.enableDebug(debug)

        if options.log_file:
            uo.setLogFile(options.log_file)

        if options.enable_syslog:
            uo.enableSyslog(True)
            
        iconsGWConfig = IconsGWConfig(uo, IconsGWConfig.CONFIG_FILENAME)

        if options.services:

            serviceConfigurator = ServiceConfigurator(uo)
            serviceConfigurator.editServices(uo)

        elif options.config:

            iconsGWConfig.configure()

        elif options.check_auto_start:
            iconsGW = IconsGW(uo, options)
            iconsGW.checkAutoStartStatus()            
            
        else:

            iconsGWConfig.updateOptions(options)

            iconsGW = IconsGW(uo, options)

            if options.enable_auto_start:
                iconsGW.enableAutoStart(options.user)

            elif options.disable_auto_start:
                iconsGW.disableAutoStart()

            else:
                iconsGW.runClient()

    #If the program throws a system exit exception
    except SystemExit:
      pass

    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass

    except Exception as e:
         if debug:
           uo.errorException()
           raise

         else:
           uo.error(str(e))

if __name__== '__main__':
    main()
