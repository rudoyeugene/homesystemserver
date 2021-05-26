echo off
rem HELP:
rem %1 - source, %2 - duration in seconds, %3 - location, %4 - lock name
ffmpeg -i "%1" -vcodec copy -acodec copy -t %2 "%3"
del /F %4.lock
