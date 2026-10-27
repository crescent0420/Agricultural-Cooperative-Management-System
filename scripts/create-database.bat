@echo off
rem create-database.bat - first-time setup: create the login and both databases from the values in .env
rem It simply runs create-database.ps1 (in the same folder) with PowerShell, allowing local scripts to run.
rem   %~dp0  = the folder this .bat file is in, so it works from anywhere
rem   %*     = any arguments you typed after the .bat name are passed on
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0create-database.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
rem When started by double-click, keep the window open so you can read the result.
echo %CMDCMDLINE% | find /i "%~nx0" >nul && pause
exit /b %EXITCODE%
