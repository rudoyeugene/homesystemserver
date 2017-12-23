#!/bin/sh
df -h | grep storage | awk '{print $4}'

