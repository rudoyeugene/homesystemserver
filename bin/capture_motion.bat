echo off
rem HELP:
rem %1 - source, %2 - duration in seconds, %3 - location, %4 - lock name
ffmpeg -i "%1" -vcodec copy -c:a aac -ab 64 -strict -2 -t %2 "%3"
del /F %4.lock
