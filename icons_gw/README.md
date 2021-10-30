# ICONS GW
The internet connection server gateway. This is installed on a machine that has access to an IP sub network. This subnetwork will have yView devices sitting on it. A yView device is any device that responds to a yView are you there (AYT) message. In order to use the icons_gw you should have previously started the [icon server](https://github.com/pjaos/icons).

# Installation
- Copy the contents of the icons_gw folder to the target machine.
- Run the install.sh file.

E.G

```
./install.sh
Defaulting to user installation because normal site-packages is not writeable
Requirement already satisfied: pyflakes in /usr/lib/python3/dist-packages (2.1.1)
Processing /scratch/git_repos/yview.git/icons_gw
  Preparing metadata (setup.py) ... done
Requirement already satisfied: p3lib>=1.1.28 in /usr/local/lib/python3.8/dist-packages (from icons-gw==4.3) (1.1.28)
Requirement already satisfied: paho-mqtt in /usr/local/lib/python3.8/dist-packages (from icons-gw==4.3) (1.5.1)
Requirement already satisfied: paramiko in /usr/local/lib/python3.8/dist-packages (from icons-gw==4.3) (2.7.2)
Requirement already satisfied: texttable in /usr/local/lib/python3.8/dist-packages (from icons-gw==4.3) (1.6.4)
Requirement already satisfied: cryptography>=2.5 in /usr/lib/python3/dist-packages (from paramiko->icons-gw==4.3) (2.8)
Requirement already satisfied: bcrypt>=3.1.3 in /usr/lib/python3/dist-packages (from paramiko->icons-gw==4.3) (3.1.7)
Requirement already satisfied: pynacl>=1.0.1 in /usr/lib/python3/dist-packages (from paramiko->icons-gw==4.3) (1.3.0)
Building wheels for collected packages: icons-gw
  Building wheel for icons-gw (setup.py) ... done
  Created wheel for icons-gw: filename=icons_gw-4.3-py3-none-any.whl size=17002 sha256=668a850605c04f1d811e5bc83545b524f22957ff6914ec0152e5948496ab481e
  Stored in directory: /tmp/pip-ephem-wheel-cache-lg4cu_pb/wheels/5d/0c/1c/813dc6b19c29a2bcb271183c21a78d307d3756cbee504ca5e8
Successfully built icons-gw
Installing collected packages: icons-gw
Successfully installed icons-gw-4.3
WARNING: Running pip as the 'root' user can result in broken permissions and conflicting behaviour with the system package manager. It is recommended to use a virtual environment instead: https://pip.pypa.io/warnings/venv
Name: icons-gw
Version: 4.3
Summary: Register a device in the YView network.
Home-page: https://github.com/pjaos/yview/tree/master/icons_gw
Author: Paul Austen
Author-email: pausten.os@gmail.com
License: MIT License
Location: /usr/local/lib/python3.8/dist-packages
Requires: p3lib, paho-mqtt, paramiko, texttable
Required-by:
Files:
  ../../../bin/icons_gw
  ../../../bin/icons_json_check
  ../../../bin/mqtt_subscribe
  icons_gw-4.3.dist-info/INSTALLER
  icons_gw-4.3.dist-info/LICENSE
  icons_gw-4.3.dist-info/METADATA
  icons_gw-4.3.dist-info/RECORD
  icons_gw-4.3.dist-info/REQUESTED
  icons_gw-4.3.dist-info/WHEEL
  icons_gw-4.3.dist-info/direct_url.json
  icons_gw-4.3.dist-info/top_level.txt
  icons_gw/__init__.py
  icons_gw/__pycache__/__init__.cpython-38.pyc
  icons_gw/__pycache__/icons_gw.cpython-38.pyc
  icons_gw/__pycache__/icons_json_check.cpython-38.pyc
  icons_gw/__pycache__/mqtt_subscribe.cpython-38.pyc
  icons_gw/icons_gw.py
  icons_gw/icons_json_check.py
  icons_gw/mqtt_subscribe.py
```

  You may uninstall icons_gw using the following command.

```
./uninstall.sh
Found existing installation: icons-gw 4.3
Uninstalling icons-gw-4.3:
  Would remove:
    /usr/local/bin/icons_gw
    /usr/local/bin/icons_json_check
    /usr/local/bin/mqtt_subscribe
    /usr/local/lib/python3.8/dist-packages/icons_gw-4.3.dist-info/*
    /usr/local/lib/python3.8/dist-packages/icons_gw/*
Proceed (Y/n)? y
  Successfully uninstalled icons-gw-4.3
```

# Configuration
Run the `icons_gw --config' command. You will be prompted to enter a number of parameters as detailed below.

- `The ICON server (ICONS) address`
This is address (IP address or domain name) of the icon server over the internet. Typically this will be you routers IP address as provided via DHCP from you ISP. You may wish to setup a DDNS if you ISP does not provide a static IP address. In this case you'll need a DNS domain name to link your router IP address too.

- `The ICON server TCP IP port`
The TCP/IP port to connect to the icon server. By default this is 22 but it is recommended that this is changed from the default.

- `The ICONS username`
The ssh username on the icon server.

- `The location name for this ICONS destination client`
This location is used to identify the location (subnetwork) that the icons_gw is running (E.G HOME).

- `The local ssh private key file to use when connecting to the ICONS`
By default your local private ssh key file should be displayed but you may change this to other local private keys that you have. If you have no local private/public key pairs then use the `ssh-keygen -t RSA` command to generate a default pair of keys.

# Starting the icons_gw for the first time
Once configured open a terminal window and enter `icons_gw`. You will be prompted for the password of the ssh server. On successful connection to the ssh server your local ssh public key will be copied to the icons server. Once this is successfully completed will not need to enter your password again.

Enter the `icons_gw` command again and a successful connection to the icon server should be made.

Once connected, details of the local (to the sub network) yView devices will be forwarded to the icon server.

# Add auto start
If you wish the icons_gw to be started every time the computer start up use the following command replacing USERNAME with your current username.

 `sudo icons_gw --enable_auto_start --user USERNAME`

# Remove auto start
If you wish the icons_gw to be removed for the computer start up use the following command.

 `icons_gw --disable_auto_start`
