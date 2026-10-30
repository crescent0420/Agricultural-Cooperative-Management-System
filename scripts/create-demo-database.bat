@echo off
rem create-demo-database.bat - an empty database for the demonstration (default acms_demo), from the values in .env
rem It simply runs create-demo-database.ps1 (in the same folder) with PowerShell, allowing local scripts to run.
rem   %~dp0  = the folder this .bat file is in, so it works from anywhere
rem   %*     = any arguments you typed after the .bat name are passed on (for example -Name acms_demo2)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0create-demo-database.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
rem When started by double-click, keep the window open so you can read the result.
echo %CMDCMDLINE% | find /i "%~nx0" >nul && pause
exit /b %EXITCODE%
