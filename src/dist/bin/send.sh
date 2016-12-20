#!/bin/sh

JAVA_OPTIONS="-Xms128M -Xmx512M -Xmx1024M"

# run dirname twice to get parent directory, use readlink for link case
APP_PATH=$(dirname `readlink -f $0`)
APP_PATH=$(dirname $APP_PATH)
CLASSPATH=$APP_PATH/conf:$APP_PATH/lib/*

java $JAVA_OPTIONS -classpath $CLASSPATH kr.gfex.main.GfexMain $@
