#!/bin/sh

set -e

PYTHON=python3.9

#Uninstall if only installed for current user
sudo $PYTHON -m pip uninstall icons_gw

#Uninstall if installed for all users
#sudo $PYTHON -m pip uninstall icons_gw

#Remove old build folders
sudo ./clean.sh
