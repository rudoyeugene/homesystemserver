#!/bin/sh

SCRIPT_DIR=$(dirname $0)
cd ${SCRIPT_DIR}/../

if [ -e pid ]; then
	echo "Previous run detected."
	if [ -e /proc/`cat pid` ]; then
		echo "Closing Home System."
		kill `cat pid`
	else
		echo "Unclean stop detected, exiting."
		exit 1
	fi
else
	echo "Nothing to stop, exiting."
fi
