# YDev
Run in a Linux machine to add a device (Linix machine) to the YView networking platform.

## Installing on Linux
The git repo must be present on the target machine. The following command can
then be executed.

```
sudo ./install.sh
```

If you do not have python3.8.2 or greater installed on the target machine
you will be prompted to indicate that the package requires python3.8.2 or
greater.

The following commands can be used to install python3.10.4 onto Debian based Linux systems.

```
sudo apt install build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev libssl-dev libreadline-dev libffi-dev libsqlite3-dev wget curl
sudo apt install libssl-dev libffi-dev
cd /tmp
curl -O https://www.python.org/ftp/python/3.10.4/Python-3.10.4.tar.xz
tar -xf Python-3.10.4.tar.xz
cd Python-3.10.4
./configure --enable-optimizations
sudo make altinstall
sudo python3.10 -m pip install --upgrade pip
```
If no python3 version is installed then The commands to do this on Linux machines will be provided.

## Installing on non Linux operating systems

- Install python3.8 or greater on the target machine if not already installed.
- Run the following command to install ydev from this folder.

```
python3 -m pip install .
```

## Configuration
The ydev program requires configuration on the command line as shown below.

E.G

```
ydev --config
ydev --config
INFO:  Loaded config from /home/pja/.ydev.cfg
INFO:  ID  PARAMETER     VALUE
INFO:  1   UNIT_NAME     My Linux Laptop
INFO:  2   PRODUCT_ID    Server
INFO:  3   SERVICE_LIST  ssh:22,vnc:5900
INFO:  4   GROUP_NAME    
INFO:  5   AYT_MSG       -!#8[dkG^v's!dRznE}6}8sP9}QoIR#?O&pg)Qra
INPUT: Enter 'E' to edit a parameter, 'S' to save and quit or 'Q' to quit:
```

The AYT_MSG message should be left with the default value.


## Running ydev

ydev may be started from the command line

```
ydev
INFO:  Loaded config from /home/pja/.ydev.cfg
INFO:  Listening on UDP port 2934
```

## Auto start ydev
You may want ydev to startup when the Linux machine starts up if so then the following
command may be used.

```
sudo ydev --user USERNAME --enable_auto_start
INFO:  Loaded config from /root/.ydev.cfg
```

USERNAME must be the username that you are logged into the Linux computer as.

You may disable auto start using the following command.

```
sudo ydev --user pja --disable_auto_start

```
