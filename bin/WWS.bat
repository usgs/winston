@echo off

cd %~dp0\..
java -cp lib/winston.jar gov.usgs.winston.server.WWS %1 %2 %3 %4 %5 %6 %7 %8 %9