#!/bin/sh
cat /sys/devices/virtual/thermal/thermal_zone0/temp | sed 's/$/ C/g'

