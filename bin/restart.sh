#!/bin/sh

SCRIPT_DIR=$(dirname $0)
cd ${SCRIPT_DIR}

. stop.sh
sleep 5
. start.sh
