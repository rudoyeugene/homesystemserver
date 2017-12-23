#!/bin/sh
sshpass -p scienide ssh -o "StrictHostKeyChecking=no" master@192.168.1.1 "uptime"

