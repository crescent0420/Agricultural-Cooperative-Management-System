@echo off
rem new-migration.bat - create a new timestamp-named migration, e.g.: new-migration.bat create programme offerings
rem It simply runs new-migration.ps1 (in the same folder) with PowerShell, allowing local scripts to run.
rem   %~dp0  = the folder this .bat file is in, so it works from anywhere
rem   %*     = any arguments you typed after the .bat name are passed on
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0new-migration.ps1" -Name "%*"
set "EXITCODE=%ERRORLEVEL%"
rem When started by double-click, keep the window open so you can read the result.
echo %CMDCMDLINE% | find /i "%~nx0" >nul && pause
exit /b %EXITCODE%
