#!/bin/sh
set -e

USER=pi
HOST=192.168.0.190

scp -r install.sh uninstall.sh clean.sh README.md LICENSE scripts/ setup.py ydev/ $USER@$HOST:/tmp/ydev
