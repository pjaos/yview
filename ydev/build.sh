#!/bin/bash
# Ensure we remove old installer versions.
rm -rf dist
set -e
# syntax checking
pyflakes3 ydev/*.py
# code style checking
pycodestyle --max-line-length=250 ydev/*.py
poetry -vvv build
cp dist/*.whl installers


