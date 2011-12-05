@echo off
java -jar "%~dp0weblogic.jar" %* -cfg "%~dp0environnements.conf"