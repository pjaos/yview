#!/bin/sh
set -e

#Check the python files and exit on error.
python3 -m pyflakes *.py

python3 -m pipenv2deb