#!/bin/sh
# HELP:
# $1 - source, $2 - duration in seconds, $3 - location, $4 - lock name $5 - rtsp transport type

if [ -f /usr/bin/ffmpeg ]; then
  ffmpeg -rtsp_transport "$5" -i "$1" -vcodec libx264 -acodec aac -t $2 "$3"
elif [ -f /usr/bin/avconv ]; then
  avconv -rtsp_transport "$5" -i "$1" -vcodec libx264 -acodec aac -t $2 "$3"
fi

rm $4.lock
