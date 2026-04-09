@echo off
setlocal

set "MAVEN_CMD=C:\Program Files\Apache\Maven\mvn.cmd"

if not exist "%MAVEN_CMD%" (
  echo Maven introuvable: "%MAVEN_CMD%"
  exit /b 1
)

call "%MAVEN_CMD%" %*
