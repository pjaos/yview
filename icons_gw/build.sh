#!/bin/sh
set -e

#Check the python files and exit on error.
python3 -m pyflakes python/*.py

p3build --sitep --python=python3.9
