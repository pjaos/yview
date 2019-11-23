#!/usr/bin/python

from    optparse import OptionParser
from    pjalib.uio import UIO as UO
from    time import sleep
import  paho.mqtt.client as mqtt
#from    mqtt_rpc import MQTTRPCClient

DEFAULT_HOST = "localhost"
DEFAULT_PORT = 1883
DEFAULT_KEEPALIVE_SECONDS = 60
DEFAULT_SUBSCRIBE_TOPIC = "#"   #MQTT subscribe all topic

uo = UO(syslogEnabled=True)

def onConnect(client, userdata, flags, rc):
    uo.info("Connected to MQTT server")

def onMessage(client, userdata, msg):
    rxStr = msg.payload.decode()
    uo.info( "RX: %s" % (rxStr) )

def main():

    opts=OptionParser(usage='An MQTT client that allows the user to subscribe to a topic primarily for debug pusposess.')
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--server",     help="The MQTT server address (default=%s)." % (DEFAULT_HOST) , default=DEFAULT_HOST)
    opts.add_option("--port",       help="The MQTT server port (default=%d)." % (DEFAULT_PORT) , type="int", default=DEFAULT_PORT)
    opts.add_option("--keepalive",  help="The number of seconds between each keepalive message (default=%d)." % (DEFAULT_KEEPALIVE_SECONDS) , type="int", default=DEFAULT_KEEPALIVE_SECONDS)
    opts.add_option("--topic",      help="The topic to subscribe to on the MQTT server (default=%s)." % (DEFAULT_SUBSCRIBE_TOPIC) , default=DEFAULT_SUBSCRIBE_TOPIC)

    try:
        (options, args) = opts.parse_args()

        client = mqtt.Client()
        client.on_connect = onConnect
        client.on_message = onMessage
        uo.info("Connecting to %s:%d" % (options.server, options.port) )
        client.connect(options.server, options.port, options.keepalive)
        client.subscribe(options.topic)
        client.loop_forever()

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
