#!/bin/bash
source $HOME/.bashrc

SCRIPT_DIR=$(dirname $0)
COMMAND_LINE_DEBUG="java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar *.war"
COMMAND_LINE_NORMAL="java -jar *.war"
cd ${SCRIPT_DIR}/../

if [ "$1" = "debug" ]; then
    echo "Starting in debug mode, use port: 5005 and IP: $(hostname -I | awk '{print $1}')"
    COMMAND_LINE=$COMMAND_LINE_DEBUG
else
    echo "Starting in normal mode"
    COMMAND_LINE=$COMMAND_LINE_NORMAL
fi

if [ -e homesystemserver.pid ]; then
	echo "Previous run detected."
	if [ -e /proc/`cat homesystemserver.pid` ]; then
		echo "Home System already running, exiting."
		exit 1
	else
		echo "Starting Home System..."
		$COMMAND_LINE &
	fi
else
	echo "Starting Home System..."
	$COMMAND_LINE &
fi
