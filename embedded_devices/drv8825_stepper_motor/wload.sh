#!/bin/bash

curl -v -i -F filedata=@./build/fw.zip http://$1/update
