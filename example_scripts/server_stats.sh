#!/bin/bash
power_stats () {
upsc apc 2>&1 | grep -v '^Init SSL'> /tmp/ups_status
}

power_stats

BOARD_UPTIME=`uptime -p`
BOARD_TEMP=`sensors | grep -A 1 ACPI | grep -v ACPI | awk '{ print $2 }'`
CPU_TEMP=`sensors | grep Package | awk '{ print $4 }'`
FREE_RAM=`free -m | grep Mem | awk '{print $4}'`
USED_SWAP=`free -m | grep Swap | awk '{print $3}'`
FREE_STORAGE=`df -h | grep storage | awk '{print $4}'`
HDD_TEMP=`hddtemp /dev/sda | awk '{ print $4 }'`

#Power stats
BATTERY_LEVEL=`cat /tmp/ups_status | egrep 'battery.charge' | egrep -v 'SSL' | sed 's/battery.charge/Battery level/' | awk '{ print $3 }'`
INPUT_VOLTAGE=`cat /tmp/ups_status | egrep 'input.voltage' | egrep -v 'input.voltage.fault' | egrep -v 'SSL' | sed 's/input.voltage/Line voltage/' | awk '{ print $3 }'`
BATTERY_LOAD=`cat /tmp/ups_status | egrep 'ups.load' | egrep -v 'SSL' | sed 's/ups.load/UPS load/' | awk '{ print $3 }'`

echo "Server uptime: $BOARD_UPTIME"
echo "Board temperature: $BOARD_TEMP"
echo "CPU temperature: $CPU_TEMP"
echo "Free RAM: $FREE_RAM MB"
echo "Used swap: $USED_SWAP MB"
echo "Free storage: $FREE_STORAGE"
echo "HDD temperature: $HDD_TEMP"
echo -e "\n"
echo "Power statistics:"
echo "Battery level: $BATTERY_LEVEL%"
echo "Input voltage: $INPUT_VOLTAGE V"
echo "Battery load: $BATTERY_LOAD%"