#!/bin/bash

# poetry env remove python3

pe=$(poetry env info -p)
if [ -n "$pe" ]; then
  echo "Removing $pe"
  rm -rf $pe
fi

date
poetry lock
poetry install
date