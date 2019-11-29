# ICONS RPC provider
Provides an MQTT RPC client that connects to the ICON server. This allows the ICONS GW to discover free TCP/IP ports inside the ICONS docker container.

# Building
This program is built into the ICONS docker image and should not need to be built. If you wish to build the Debian installer it can be built using pbuild [https://github.com/pjaos/pbuild](https://github.com/pjaos/pbuild). 
Once pbuild is installed the following commands will build and install the deb package.

git clone git@github.com:pjaos/yview.git

cd yview/icons_rpc_provider

sudo pbuild

sudo dpkg -i packages/python-icons-rpc-provider-1.0-all.deb

You may uninstall icons-rpc-provider using the following command.

`sudo dpkg -r python-icons-rpc-provider`
