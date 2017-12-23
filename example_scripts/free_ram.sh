#!/bin/sh
free -h | grep Mem | awk '{print $4}' | sed 's/$/B/g'

