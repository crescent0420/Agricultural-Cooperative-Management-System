@echo off
rem rename-migrations-to-timestamps.bat - ONE-TIME: rename V1..V15 to timestamp names, keeping your data
rem It simply runs rename-migrations-to-timestamps.ps1 (in the same folder) with PowerShell, allowing local scripts to run.
rem   %~dp0  = the folder this .bat file is in, so it works from anywhere
rem   %*     = any arguments you typed after the .bat name are passed on
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rename-migrations-to-timestamps.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
rem When started by double-click, keep the window open so you can read the result.
echo %CMDCMDLINE% | find /i "%~nx0" >nul && pause
exit /b %EXITCODE%
