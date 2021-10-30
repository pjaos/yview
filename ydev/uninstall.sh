#!/bin/sh
set -e

#Uninstall if only installed for current user
sudo python3.8 -m pip uninstall ydev

#Uninstall if installed for all users
#sudo python3.8 -m pip uninstall ydev

#Remove old build folders
./clean.sh
