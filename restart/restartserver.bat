@echo off
set serverPort=%1
echo %~dp0
echo Restart script will run with param serverPort: %serverPort%
java -cp %~dp0/* ParkNPark.middletier.Server -ORBInitialHost localhost -ORBInitialPort 5000 -ORBServerHost localhost -ORBServerPort %serverPort% --num-clients 10 --num-servers 3