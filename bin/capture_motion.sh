#!/bin/sh
# HELP:
# $1 - source, $2 - duration in seconds, $3 - location, $4 - lock name

if [ -f /usr/bin/ffmpeg ]; then
  ffmpeg -i "$1" -vcodec libx264 -acodec aac -t $2 "$3"
elif [ -f /usr/bin/avconv ]; then
  avconv -i "$1" -vcodec libx264 -acodec aac -t $2 "$3"
fi

rm $4.lock
