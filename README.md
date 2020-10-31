# Home System Project

Standalone full-stack application with Java based server and Android client
Easy configurable via single xml file
Preferable environment: Linux, Windows is also supported but network features are unstable

### Server purposes

* Take care about your IP cameras: monitor motions, upload to Cloud and notify the Clients ASAP, reboot if Camera has stuck
* Watch for your ISP failures and notify once Internet is back
* Run custom scripts on events ag. armed, disarmed, ISP changed, server started, server stopped
* Watch for your network devices, change state (arm/disarm) based on presence of the well known devices aka Master-devices
* Send you an hourly report with cameras snapshots, detailed report and any data you want to know via custom scripts

### How to setup the Server

* Copy the tar archive to the Server machine you like
* Extract contents into a separate folder
* _(OS dependent) Make scripts under bin dir executable if you are on the Linux_
* Modify the conf/system-settings.xml as you wish
* Run bin/start.* _(OS dependent)_
* Pair Client(s)

# Requirements
* Standalone x86/ARM PC
* 1GB of RAM
* Email account
* Some drive space to keep the videos and app (~60GB can keep tons of fragments)
* Internet
* Web Cams (Vstarcam tested)

# NOTES
- Normally ARM can process 1 cam per core and x86 2 cams per core (monitoring and recording)
- Personally I'm using Intel NUC based on Intel Celeron J3455 and 4GB of RAM
- If you need to add/change features fill free to contact me
- If you found any bug enable verbose logging on an any Client, try to reproduce and contact me

# EULA
The Server is provided as-is, Author have no any responsibility if something goes wrong due to this Server actions.
The Server purpose is NOT to protect your property but to catch the event was trying to disserve your property.
Every user of this Server have personal responsibility for the Server usage and the usages were applied to this Server.
The Server can contain some bugs, as soon you'll report them to the Author as soon they will be fixed.
