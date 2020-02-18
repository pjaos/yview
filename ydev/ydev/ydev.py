#!/usr/bin/env python

import  sys
import  socket
import  platform
from    time import time, sleep
from    pjalib.netif import NetIF
import  json
import  getpass
from    pjalib.uio import  UIO
from    optparse import OptionParser
from    pjalib.pconfig import ConfigManager
from    pjalib.boot_manager import BootManager

class AYTListener(object):
    """@brief Responsible listening for are you there messages (AYT) from the host and sending responses
              back."""

    IP_ADDRESS_KEY           = "IP_ADDRESS"
    OS_KEY                   = "OS"
    UDP_DEV_DISCOVERY_PORT   = 2934
    UDP_RX_BUFFER_SIZE       = 2048
    AYT_KEY                  = "AYT"

    def __init__(self, uo, options, deviceConfig):
        """@Constructor
            @param uo A UserOutput instance.
            @param options Command line options from OptionParser.
            @param deviceConfig The device configuration instance."""
        self._uio=uo
        self._options=options
        self._deviceConfig=deviceConfig
        self._sock=None
        self._user = getpass.getuser()

        self._osName = platform.system()

    def _listener(self):
        """@brief Listen for messages from the icons dest server.
           @return The message received."""
        self._uio.info("Listening on UDP port %d" % (AYTListener.UDP_DEV_DISCOVERY_PORT))

        try:
            while True:
                #Inside loop so we re read config if changed by another instance using --config option.
                jsonDict = self._deviceConfig.getConfigDict()

                #Wait for RX data
                rxData, addressPort = self._sock.recvfrom(AYTListener.UDP_RX_BUFFER_SIZE)

                try:
                    rxDict = json.loads(rxData)
                    if AYTListener.AYT_KEY in rxDict:
                        aytString = rxDict[AYTListener.AYT_KEY]
                        if jsonDict[DeviceConfig.AYT_MSG] == aytString:

                            self._lastAYTMsgTime = time()

                            #Get the name of the interface on which we received the rxData
                            ifName = self._netIF.getIFName(addressPort[0])

                            #Add the interface address on this machine as the source of the message for yview
                            jsonDict[AYTListener.IP_ADDRESS_KEY] = self._netIF.getIFIPAddress(ifName)
                            jsonDict[AYTListener.OS_KEY] = self._osName
                            jsonDictStr = json.dumps( jsonDict, sort_keys=True, indent=4, separators=(',', ': '))

                            if self._options.debug:
                                self._uio.info("%s: %s" % (self.__class__.__name__, jsonDictStr) )

                            self._sock.sendto( jsonDictStr, addressPort)

                        elif self._options.debug:
                            self._uio.error("AYT mismatch:")
                            self._uio.info("Expected: {}".format(jsonDict[DeviceConfig.AYT_MSG]) )
                            self._uio.info("Found:    {}".format(aytString) )

                except:
                    pass

        except:
            self._uio.errorException()
            self._uio.info("Shutdown device listener.")

    def run(self):
        """@brief Called to start sending UDP broadcast (beacon) messages."""

        self._netIF = NetIF()

        # Open UDP socket to be used for discovering devices
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self._sock.bind(('', AYTListener.UDP_DEV_DISCOVERY_PORT))

        self.initAYTTime()

        try:

            while True:

                self._listener()

                sleep(5)

        finally:
            self.shutDown()

    def shutDown(self):
        """@brief Shutdown the network connection if connected."""
        if self._sock:
            self._sock.close()

    def setDict(self, yviewDict):
        """@brief set the dict (data) to be sent to the yview network."""
        self._yviewDict = yviewDict

    def getSecsSinceAYTMsg(self):
        """@brief Get the number of seconds since we last received an Are You There Message."""
        seconds = time()-self._lastAYTMsgTime
        return seconds

    def initAYTTime(self):
        """@brief Init the AYT message received time to now."""
        self._lastAYTMsgTime=time()

    def enableAutoStart(self):
        """@brief Enable this program to auto start when the computer on which it is installed starts."""
        bootManager = BootManager()
        bootManager.add(user=self._user)

    def disableAutoStart(self):
        """@brief Enable this program to auto start when the computer on which it is installed starts."""
        bootManager = BootManager()
        bootManager.remove()
        

class DeviceConfig(object):
    """@brief Responsible for managing the configuration used by the ydev application."""

    UNIT_NAME                           = "UNIT_NAME"
    PRODUCT_ID                          = "PRODUCT_ID"
    SERVICE_LIST                        = "SERVICE_LIST"
    GROUP_NAME                          = "GROUP_NAME"
    AYT_MSG                             = "AYT_MSG"

    DEFAULT_CONFIG = {
        UNIT_NAME:    "",
        PRODUCT_ID:   "",
        SERVICE_LIST: "",
        GROUP_NAME:   "",
        AYT_MSG:      ""
    }

    def __init__(self, uio, configFile):
        """@brief Constructor.
           @param uio UIO instance.
           @param configFile Config file instance."""
        self._uio     = uio

        self._configManager = ConfigManager(self._uio, configFile, DeviceConfig.DEFAULT_CONFIG)
        self._configManager.load()

    def configure(self):
        """@brief configure the required parameters for normal opperation."""
        configOK = True
        invalidInitialCharList = ('+','#','/')
        while True:
            self._configManager.inputStr(DeviceConfig.UNIT_NAME, "Enter the name of the device", False)
            unitName = self._configManager.getAttr(DeviceConfig.UNIT_NAME)
            if len(unitName) > 0 and unitName[0] in invalidInitialCharList:
                self._uio.warn("The name of a device may not start with a '%s' character." % (unitName[0]) )
            else:
                break

        self._configManager.inputStr(DeviceConfig.PRODUCT_ID, "Product identifier for the device", False)

        while True:
           self._configManager.inputStr(DeviceConfig.SERVICE_LIST, "The service list string for the device", False)

           srvListStr = self._configManager.getAttr(DeviceConfig.SERVICE_LIST)
           try:
               elems = srvListStr.split(",")
               for service in elems:
                   name, port = service.split(":")
                   if len(name) == 0:
                       raise ValueError("")
                   portValue = int(port)
                   if portValue < 1 or portValue > 65535:
                       raise ValueError("")
               break

           except ValueError:
               self._uio.error("%s is not a valid service string (E.G 'ssh:22,web:80')." % (srvListStr) )

        self._configManager.inputStr(DeviceConfig.AYT_MSG, "The devices 'Are You There' message text (min 8, max 64 characters)", False)

        self._configManager.inputStr(DeviceConfig.GROUP_NAME, "The group name (enter none for no/default group)", False)
        groupName = self._configManager.getAttr(DeviceConfig.GROUP_NAME)
        if groupName.lower() == 'none':
            self._configManager.addAttr(DeviceConfig.GROUP_NAME, "")

        if configOK:
            self._configManager.store()
        else:
            self._uio.error("Configuration aborted.")

    def show(self):
        """@brief Show the current configuration parameters."""
        attrList = self._configManager.getAttrList()
        attrList.sort()

        maxAttLen=0
        for attr in attrList:
            if len(attr) > maxAttLen:
                maxAttLen=len(attr)

        for attr in attrList:
            padding = " "*(maxAttLen-len(attr))
            self._uio.info("%s%s = %s" % (attr, padding, self._configManager.getAttr(attr)) )

    def loadConfigQuiet(self):
        """@brief Load the config without displaying a message to the user."""
        self._configManager.load(showLoadedMsg=False)

    def getAttr(self, key):
        """@brief Get an attribute value.
           @param key The key for the value we're after."""

        #If the config file has been modified then read the config to get the updated state.
        if self._configManager.isModified():
            self._configManager.load(showLoadedMsg=False)
            self._configManager.updateModifiedTime()

        return self._configManager.getAttr(key)

    def getConfigDict(self):
	return self._configManager._configDict

#Very simple cmd line template using optparse
def main():
    uio = UIO()

    opts=OptionParser(usage="Responsible for responding to AYT messages with the details of the Y device and providing the ability to configure device details.")
    opts.add_option("--config",     help="Configure the local Y device/s.", action="store_true", default=False)
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--enable_auto_start",  help="Enable auto start this program when this computer starts.", action="store_true", default=False)
    opts.add_option("--disable_auto_start", help="Disable auto start this program when this computer starts.", action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()

        deviceConfig = DeviceConfig(uio, "ydev.cfg")
        if options.config:
            deviceConfig.configure()

        aytListener = AYTListener(uio, options, deviceConfig)
        if options.enable_auto_start:
            aytListener.enableAutoStart()

        elif options.disable_auto_start:
            aytListener.disableAutoStart()
                
        else:
            aytListener.run()

    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except:
     if options.debug:
       raise

     else:
       uio.error(sys.exc_value)


if __name__== '__main__':
    main()
