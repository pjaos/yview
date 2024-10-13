#!/bin/bash
set -e
pyflakes3 icons_rpc_provider/*.py
poetry -vvv build

