#!/bin/bash
ROUTER_CPU_TEMP=`sshpass -p admin ssh -o "StrictHostKeyChecking=no" admin@192.168.1.1 "cat /proc/dmu/temperature | sed 's/..$/ C/g'" | awk '{print $4}'`
ROUTER_UPTIME=`sshpass -p admin ssh -o "StrictHostKeyChecking=no" admin@192.168.1.1 "uptime -p"`

echo "Router uptime: $ROUTER_UPTIME"
echo "Router CPU temperature: $ROUTER_CPU_TEMP °C"
