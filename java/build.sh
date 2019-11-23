#!/bin/sh
# This will build a yview.jar frile that can be executed by running
# java -jar yview.jar
#
set -e

ant clean
ant
ant create_jar
cd packaging/linux && make
