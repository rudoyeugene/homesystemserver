#!/bin/sh

# exact URL of camera
SOURCE=$1
# record duration in seconds
DURATION=$2
# output file incl extension to define container type
OUTPUT_FILE=$3
# lock file name to resume motion detection in the App
LOCK_FILE_NAME=$4
# rtsp transport type (udp|tcp), default - udp
RTSP_TRANSPORT=$5

BINARY=ffmpeg

if [ -f /usr/bin/avconv ]; then
  BINARY=avconv
fi

$BINARY -err_detect aggressive -fflags discardcorrupt -t $DURATION -rtsp_transport $RTSP_TRANSPORT -i "$SOURCE" -vcodec libx264 -acodec aac "$OUTPUT_FILE"
#$BINARY -err_detect aggressive -fflags discardcorrupt -t $DURATION -rtsp_transport $RTSP_TRANSPORT -i "$SOURCE" -vcodec copy -acodec aac "$OUTPUT_FILE"

rm $LOCK_FILE_NAME.lock