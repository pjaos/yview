#!/bin/sh
set -e

#Remove dirs created during build
if [ -d "build" ]; then
    rm -rf build
fi

if [ -d "icons_gw.egg-info" ]; then
    rm -rf icons_gw.egg-info
fi
