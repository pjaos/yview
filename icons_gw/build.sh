#!/bin/bash
set -e
rm -rf dist
pyflakes3 icons_gw/*.py
poetry -vvv build

