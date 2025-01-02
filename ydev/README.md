# YDev
Run on a machine to add a device to the YView network.

## Installing ydev
The python wheel package can be built by running the build.sh script.

### Installing pipx
pipx must be installed on your machine before proceeding with installation. Details of how to install pipx can be found [here](https://pipx.pypa.io/latest/installation/).

### Installing ydev
Copy the *.whl (E.G installers/ydev-3.8.3-py3-none-any.whl) to the target machine. Open a terminal session on your
computer and run the following command assuming the *.whl file is sitting in the /tmp folder.

```
pipx install /tmp/ydev-3.8.3-py3-none-any.whl
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
