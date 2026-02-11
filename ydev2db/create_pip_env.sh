#!/bin/sh
# poetry env remove python3
export PIPENV_VENV_IN_PROJECT=enabled
python3.9 -m pipenv install --python 3.9
