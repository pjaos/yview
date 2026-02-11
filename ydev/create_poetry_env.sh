#!/bin/bash

# If you wish to remove the poetry python env uncomment this
# poetry env remove python3

# Date just used to show how long the env takes to create
date
poetry lock
poetry install
date