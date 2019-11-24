# yView
yView is a framework that allows any device that offers a service over a TCP 
server connection to be connected to a private network using only an ssh server connected to the Internet.

The diagram below shows how the various parts of the yView system connect together.

![Overview](overview_diagram.png "yView Connected Network")

The ICON server is a virtual device has it's functionality is inside a docker container. Internally (not exposed to the Internet) this docker image executes an MQTT server which brokers all messages in the yView system.

The ICONS gateway program will run on the same machine running the above docker image. At least one instance of the ICONS gateway should be running in 
each (Home or Remote in the diagram, although many more networks can be connected) network. The ICONS gateway is responsible for sending are you there (AYT) broadcast messages to the local network. Devices and computers
respond to these messages with JSON messages that contain details of the device including what services they support (E.G http on port 80).

The ydev utility allows any device or computer to become the destination of a connection. The ydev utility is a python application. C libraries are also available so to add the functionality required to allow smaller (unable to run python) to become part of the yView network.

Computers, tablets and phones connected to the yView network can use the Java or Android GUI software to connect to devices and computers in the yView network.