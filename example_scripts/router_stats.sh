#!/bin/bash

function router_free_ram_raw (){
sshpass -p admin ssh -o "StrictHostKeyChecking=no" admin@192.168.1.1 "free | grep Mem"
}

function router_free_ram_kb (){
FREE_RAM_KB=`router_free_ram_raw | awk '{print $4}'`
echo $(($FREE_RAM_KB/1024))
}

ROUTER_CPU_TEMP=`sshpass -p admin ssh -o "StrictHostKeyChecking=no" admin@192.168.1.1 "cat /sys/class/thermal/thermal_zone0/temp | sed 's/.\{3\}$/.&/'"`
ROUTER_UPTIME=`sshpass -p admin ssh -o "StrictHostKeyChecking=no" admin@192.168.1.1 "uptime -p"`
FREE_RAM=`router_free_ram_kb`


echo -e "\n"
echo "Router statistics:"
echo "Router uptime: $ROUTER_UPTIME"
echo "Router CPU temperature: $ROUTER_CPU_TEMP°C"
echo "Router free RAM: $FREE_RAM MB"
