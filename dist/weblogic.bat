@echo off
java -jar "%~dp0weblogic.jar" %* -cfg "%~dp0environments.conf"