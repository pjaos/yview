#!/usr/bin/python3

import  argparse
import  socket
from    p3lib.mqtt_rpc import MQTTRPCClient, MQTTRPCProviderClient
from    p3lib.uio import UIO
from    p3lib.boot_manager import BootManager

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
    """@brief Program entry point"""
    uio = UIO()
    debug = False

    try:
        parser = argparse.ArgumentParser(description="A tool to do something.\n"\
                                                     "A description of what it does.",
                                         formatter_class=argparse.RawDescriptionHelpFormatter)
        parser.add_argument("-d", "--debug",    action='store_true', help="Enable debugging.")
        parser.add_argument("--server",     help="The MQTT server address (default=%s)." % (MQTTRPCClient.DEFAULT_HOST) , default=MQTTRPCClient.DEFAULT_HOST)
        parser.add_argument("--port",       help="The MQTT server port (default=%d)." % (MQTTRPCClient.DEFAULT_PORT) , type=int, default=MQTTRPCClient.DEFAULT_PORT)
        parser.add_argument("--keepalive",  help="The number of seconds between each keepalive message (default=%d)." % (MQTTRPCClient.DEFAULT_KEEPALIVE_SECONDS) , type=int, default=MQTTRPCClient.DEFAULT_KEEPALIVE_SECONDS)
        parser.add_argument("--sid",        help="The ID number that uniquely identifies the MQTT RPC server to send the RPC request to (default=%d)." % (MQTTRPCClient.DEFAULT_SRV_ID) , type=int, default=MQTTRPCClient.DEFAULT_SRV_ID)
        parser.add_argument("--cid",        help="The ID number that uniquely identifies this MQTT RPC client from which the RPC request is made (default=%d)." % (MQTTRPCClient.DEFAULT_SRV_ID) , type=int, default=MQTTRPCClient.DEFAULT_SRV_ID)
        BootManager.AddCmdArgs(parser)

        options = parser.parse_args()

        uio.enableDebug(options.debug)

        handled = BootManager.HandleOptions(uio, options, True)
        if not handled:
            rpcMethodProvider = RPCMethodProvider()
            mqttRPCProviderClient = MQTTRPCProviderClient(uio, options, (rpcMethodProvider,))
            mqttRPCProviderClient.connect()
            mqttRPCProviderClient.loopForever()

    #If the program throws a system exit exception
    except SystemExit:
        pass

    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
        pass

    except Exception as e:
        if debug:
            uio.errorException()
            raise

        else:
            uio.error(str(e))

if __name__== '__main__':
    main()
