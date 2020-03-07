#!/usr/bin/python

from    optparse import OptionParser
import  socket
from    pjalib.mqtt_rpc import MQTTRPCClient, MQTTRPCProviderClient
from    pjalib.uio import UIO as UO

class RPCMethodProvider(object):

    @staticmethod
    def GetFreeTCPPort():
        """@brief Get a free port and return to the client. If no port is available
                  then -1 is returned.
           @return the free TCP port number or -1 if no port is available."""
        tcpPort=-1
        try:
            #Bind to a local port to find a free TTCP port
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.bind(('', 0))
            tcpPort = sock.getsockname()[1]
            sock.close()
        except socket.error:
            pass
        return tcpPort

    def getFreeTCPPort(self):
        """@brief Get a free port and return to the client. If no port is available
                  then -1 is returned.
           @return the free TCP port number or -1 if no port is available."""
        return RPCMethodProvider.GetFreeTCPPort()

def main():
    uo = UO()

    opts=OptionParser(usage='An MQTT client that provides provides RPC services for ICONS connections.')
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--server",     help="The MQTT server address (default=%s)." % (MQTTRPCClient.DEFAULT_HOST) , default=MQTTRPCClient.DEFAULT_HOST)
    opts.add_option("--port",       help="The MQTT server port (default=%d)." % (MQTTRPCClient.DEFAULT_PORT) , type="int", default=MQTTRPCClient.DEFAULT_PORT)
    opts.add_option("--keepalive",  help="The number of seconds between each keepalive message (default=%d)." % (MQTTRPCClient.DEFAULT_KEEPALIVE_SECONDS) , type="int", default=MQTTRPCClient.DEFAULT_KEEPALIVE_SECONDS)
    opts.add_option("--sid",        help="The ID number that uniquely identifies the MQTT RPC server to send the RPC request to (default=%d)." % (MQTTRPCClient.DEFAULT_SRV_ID) , type="int", default=MQTTRPCClient.DEFAULT_SRV_ID)
    opts.add_option("--cid",        help="The ID number that uniquely identifies this MQTT RPC client from which the RPC request is made (default=%d)." % (MQTTRPCClient.DEFAULT_SRV_ID) , type="int", default=MQTTRPCClient.DEFAULT_SRV_ID)
    opts.add_option("--enable_auto_start",  help="Enable auto start this program when this computer starts.", action="store_true", default=False)
    opts.add_option("--disable_auto_start", help="Disable auto start this program when this computer starts.", action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()

        rpcMethodProvider = RPCMethodProvider()

        mqttRPCProviderClient = MQTTRPCProviderClient(uo, options, (rpcMethodProvider,), autoStartUser="root", allowRootAutoStartUser=True)

        if options.enable_auto_start:
            #This tool has no persistent config. All parameters are passed on the command line.
            mqttRPCProviderClient.enableAutoStart(argString="--server {} --port={} --keepalive={} --sid={} --cid={}".format(options.server, options.port, options.keepalive, options.sid, options.cid) )

        elif options.disable_auto_start:
            mqttRPCProviderClient.disableAutoStart()

        else:
            mqttRPCProviderClient.connect()
            mqttRPCProviderClient.loopForever()

    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except Exception as ex:
     if options.debug:
       raise

     else:
       uo.error( str(ex) )

if __name__== '__main__':
    main()
