#!/bin/sh

SCRIPT_DIR=$(dirname $0)
cd ${SCRIPT_DIR}/../

if [ -e pid ]; then
	echo "Previous run detected."
	if [ -e /proc/`cat pid` ]; then
		echo "Home System already running, exiting."
		exit 1
	else
		echo "Starting Home System..."
		java -jar *.war &
	fi
else
	echo "Starting Home System..."
	java -jar *.war &
fi