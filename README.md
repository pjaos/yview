# ICONS GW
The internet connection server gateway. This is installed on a machine that has access to an IP sub network that the devices sit on. A device is any device that responds to a yView are you there (AYT) message. In order to use the icons_gw you should have previously started the [icon server](https://github.com/pjaos/icons).

#Installation
This can either be installed via pip or using a deb package.

## pip installation

 `git clone git@github.com:pjaos/icons_gw.git`
 `cd icons_gw`
 `sudo pip install`
 
  You may uninstall icons_gw using the following command.
  
  sudo pip uninstall icons_gw
 
## deb installation
 In order to build the deb package you must install [pbuild](git@github.com:pjaos/). Once pbuild is installed the following commands will build an install the deb package.
  
   `git clone git@github.com:pjaos/icons_gw.git`
   `cd icons_gw`
   `sudo pbuild`
   `sudo dpkg -i packages/python-icons-3.5-all.deb`

    You may uninstall icons_gw using the following command.
    
    `sudo dpkg -r python-icons`
 
#Configuration
Run the `icons_gw config' command. You will be prompted to enter a number of parameters as detailed below.

- `The ICON server (ICONS) address`
This is address of the icon server over the internet. You may wish to setup DDNS if you ISP does not have a static IP address. In this case you'll need a DNS domain name to assign your router IP address to.

- `The ICON server TCP IP port`
The TCP/IP port to connect to the icon server. By default this is 22 but it is recommended that this is change from the default

- `The ICONS username`
The ssh username on the icon server.

- `The location name for this ICONS destination client`
This location is used to identify the location (subnetwork) that the icons_gw is running (E.G HOME).

- `The local ssh private key file to use when connecting to the ICONS`
By default your local private key file should be displayed but you may change this to other local private keys that you have. If you have no local private/public key pairs then use the `ssh-keygen -t RSA` command to generate a default pair of keys.

#Starting the icons_gw for the first time
Once configured open a terminal window and enter `icons_gw`. You will be prompted for the password of the ssh server. On successful connection to the ssh server the local public key will be copied to the icons sever and you will not need to enter it again. Restart the connection and a successful connection to the icon server should be made.
Once connected details of the local (to the sub network) yView devices will be forwarded to the icon server. 

# Add auto start
If you wish the icons_gw to be started every time the computer start up use the following command.

 `icons_gw --enable_auto_start`

# Remove auto start
If you wish the icons_gw to be removed for the computer start up use the following command.

 `icons_gw --disable_auto_start`


