#!/bin/sh
set -e

#Remove dirs created during build
if [ -d "build" ]; then
    rm -rf build
fi

if [ -d "ydev.egg-info" ]; then
    rm -rf ydev.egg-info
fi
