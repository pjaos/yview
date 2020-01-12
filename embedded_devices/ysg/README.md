## RF Signal Generator (YSG)
A signal generator that integrates with the YView network although it will run autonomously.

## Features
 * Provides a signal from 137.5 MHz to 4400 MHz with an accuracy of +/- 0.5 ppm.
 * Powered from a micro USB connector.
 * Connects to a WiFi network.
 * Control via a web interface.
 * The YSG command line tool allows remote control of the device to enable it's use in an automated test environment.

## Web Interface
The web interface can be accessed using a web browser over the WiFi interface.

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

A nodeMCU ESP 32 board is the CPU platform and provides WiFi connectivity.

![NodeMCU ESP32 Board](doc/nodemcu_esp32.jpg)

An ADF4350 development board was connected to the above board via an SPI bus. The following is a picture of the board as supplied.

![ADF4350 Development Board](doc/adf4350_dev_board.jpg)

I modified this board as follows in order to increase the accuracy and stability of the clocking on the device. This also allows the ESP32 platform to calibrate the clock.

* Replaced the 10 MHZ OSC with an IQD CFTP-126 TCXO (RS part number 148-5365).
* Connect the control voltage pin of this TCXO to Pin 3 (next to VCC pin) on the interface connector.

The following are picture of the board after modification.
![ADF4350 Development Board](doc/adf4350_dev_board_moda.jpg)
![ADF4350 Development Board](doc/adf4350_dev_board_modb.jpg)

The schematic folder contains the Kicad schematic of the connections between the above modules. Click ![here](doc/schematic.pdf) for a PDF of the schematic.

Once the electronics and software was complete I designed and 3D printed an ABS case as shown in the following pictures of the finished product.

![](doc/ysg3.jpg)
![](doc/ysg1.jpg)
![](doc/ysg2.jpg)

The 3D printed case includes light pipes to bring out the LED's. The Red LED indicates power to the device. The Blue LED indicates the WiFi is connected. The hole nearest the LED's can be used to  reset the device. The hole furthest from the LED's can be used to setup the WiFi on the device if held down for 5 seconds.

## Software
The device software is written in C ([the source is available here](mgos)). As part of this project I wrote the [ADF4350 device driver](https://github.com/pjaos/adf4350) from scratch.
The software makes extensive use of [Mongoose OS](https://mongoose-os.com/). Mongoose OS was chosen because as it provides many features (libraries including configuratin management, RPC etc) on top of the FreeRTOS SDK from [Esspessif](https://www.espressif.com/en/products/software/esp-sdk/overview) which allows faster product development.
