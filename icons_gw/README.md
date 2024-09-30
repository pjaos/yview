# ICONS GW
The internet connection server gateway. This is installed on a machine that has access to an IP sub network. This subnetwork will have yView devices sitting on it. A yView device is any device that responds to a yView are you there (AYT) message. In order to use the icons_gw you should have previously started the [icon server](https://github.com/pjaos/icons).


# Building
You will need pipx and poetry installed on your machine to build the python wheel. Once you have done this run the build.sh script and the python wheel file will be created in the local dist folder.

E.G

```
ls dist
icons_gw-5.0.0-py3-none-any.whl  icons_gw-5.0.0.tar.gz
```

# Installation
- Ensure pipx is installed on the target machine.
- Copy the above python wheel (.whl extension) file to the target machine.
- Install the python wheel file

E.G

```
pipx install /tmp/icons_gw-5.0.0-py3-none-any.whl
  installed package icons-gw 5.0.0, installed using Python 3.12.3
  These apps are now globally available
    - icons_gw
    - icons_json_check
    - mqtt_subscribe
done! ✨ 🌟 ✨
```

  You may uninstall icons_gw using the following command.

```
pipx uninstall icons_gw
uninstalled icons-gw! ✨ 🌟 ✨
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

```
icons_gw --enable_auto_start
```

# Check that icons_gw is running.
You can check that the icons_gw app is running as shown below.
If you wish the icons_gw to be started every time the computer start up use the following command replacing USERNAME with your current username.

```
icons_gw --check_auto_start
INFO:  Loaded config from /home/username/.icons_gw.cfg
INFO:  ● icons_gw.service
INFO:       Loaded: loaded (/home/username/.config/systemd/user/icons_gw.service; enabled; preset: enabled)
INFO:       Active: active (running) since Mon 2024-09-30 16:07:41 BST; 6s ago
INFO:     Main PID: 20310 (icons_gw)
INFO:        Tasks: 8 (limit: 3854)
INFO:       Memory: 18.7M (peak: 19.3M)
INFO:          CPU: 1.031s
INFO:       CGroup: /user.slice/user-1002.slice/user@1002.service/app.slice/icons_gw.service
INFO:               └─20310 /home/username/.local/share/pipx/venvs/icons-gw/bin/python /home/username/.local/bin/icons_gw
INFO:  
INFO:  Sep 30 16:07:41 rpi-server systemd[1603]: Started icons_gw.service.
INFO:  
INFO:  
```

# Remove auto start
If you wish the icons_gw to be removed for the computer start up use the following command.

```
icons_gw --disable_auto_start
```
