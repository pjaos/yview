#!/bin/sh
set -e

#Check the python files and exit on error.
python3.9 -m pyflakes ydev2db.py dba.py

sudo python3.9 -m pipenv2deb pipenv2deb

