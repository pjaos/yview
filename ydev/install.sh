#!/bin/sh
set -e

APP_NAME=ydev

check_internet_connectivity()
{
echo "Checking Internet connection"
#8.8.8.8 is used as it's googles dns server which should be available on the internet
if ping -q -c 1 -W 1 8.8.8.8 >/dev/null; then
  echo "Connected to the Internet"
else
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "! This computer is not connected to the internet !"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "Connect this computer to the Internet and then try again."
  exit 1
fi
}

if !(which python3.8) > /dev/null ; then
  echo "****************************************************************"
  echo "* Python3.8 is not installed is not installed on your computer *"
  echo "****************************************************************"
  check_internet_connectivity
  echo "Run the following commands to install python3.8."
  echo "    sudo apt install build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev libssl-dev libreadline-dev libffi-dev libsqlite3-dev wget curl"
  echo "    sudo apt install libssl-dev libffi-dev"
  echo "    cd /tmp"
  echo "    curl -O https://www.python.org/ftp/python/3.8.2/Python-3.8.2.tar.xz"
  echo "    tar -xf Python-3.8.2.tar.xz"
  echo "    cd Python-3.8.2"
  echo "    ./configure --enable-optimizations"
  echo "    sudo make altinstall"
  echo "    sudo python3.8 -m pip install --upgrade pip"
  echo "When the above steps are complete try runing this install script again."
  exit 1
fi

#Remove old build folders
sudo ./clean.sh

#Check the code using pyflakes
python3.8 -m pip install pyflakes
#Check the python files and exit on error.
python3.8 -m pyflakes $APP_NAME/*.py

# Install. sudo is required so that the package is installed for all users.
sudo python3.8 -m pip install .

# Upgrade pip to the latest if required
#python3.8 -m pip install --upgrade build
# Build a python whl (wheel) package if required
#python3.8 -m build

#Ensure all files are flush to disk/flash as the raspberry PI
# normally has flash storage.
sync

# Show a list of the installed files
python3.8 -m pip show -f $APP_NAME
