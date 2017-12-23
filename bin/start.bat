setlocal
cd /d %~dp0
cd /d ..

if exist pid (
	echo "Previous run detected."
	echo "Starting Home System..."
	start java -jar *.war)
else (
	echo "Starting Home System..."
	start java -jar *.war
)