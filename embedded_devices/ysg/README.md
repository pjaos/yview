## RF Signal Generator (YSG)
A signal generator that integrates with the YView network although it will run autonomously.

## Features
 * Provides a signal from 137.5 MHz to 4400 MHz.
 * Powered from a micro USB connector.
 * Connects to a WiFi network.
 * Presents a web interface.
 * The YSG command line tool allows the device to be included in
automated test environment.

## Web Interface
The web interface show a web page with to tabs

### RF outputs Tab
This tab allows the user to set

* Set the output frequency
* Set the output level
* Turn the output on and off

![webtab1](doc/webtab1.png)

### Configuration Tab
This tab allows the user to set

* The device name (this appears at the top of the web page).
* The group name. This is a YView network function (see [YView GUI](https://github.com/pjaos/yview/tree/master/gui/java)) for more details. If the device is not in a YView network then this option can be left blank.
* Enable syslog. Syslog output is sent to the ICONS GW server (See https://github.com/pjaos/yview/tree/master/icons_gw) for more details. If the device is not in a YView network then this option can be left unselected.

![webtab2](doc/webtab2.png)


# Hardware

![](doc/ysg3.jpg)
![](doc/ysg1.jpg)
![](doc/ysg2.jpg)

The Red LED indicates power to the device.
The Blue LED indicates the WiFi is connected.



A nodeMCU ESP 32 board provides WiFi connectivity.

![NodeMCU ESP32 Board](doc/nodemcu_esp32.jpg)

The nodeMCU ESP32 device is connected to an ADF4350 development board via an SPI bus.

The schematic folder contains the Kicad schematic of the connections between the above modules.

![Module Connectivity](doc/schematic.pdf)

## Software
The device software is written in C ([here](mgos)). As part of this project I wrote the [ADF4350 device driver](https://github.com/pjaos/adf4350) from scratch.
The software makes extensive use of [Mongoose OS](https://mongoose-os.com/). Mongoose OS was chosen because as it provides many features (on top of the FreeRTOS SDK from [Esspessif](https://www.espressif.com/en/products/software/esp-sdk/overview) ) which allows faster product development.
