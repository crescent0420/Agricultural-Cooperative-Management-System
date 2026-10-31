@echo off
rem export-sql.bat - export every migration and the current schema to database\exports
rem It simply runs export-sql.ps1 (in the same folder) with PowerShell, allowing local scripts to run.
rem   %~dp0  = the folder this .bat file is in, so it works from anywhere
rem   %*     = any arguments you typed after the .bat name are passed on
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0export-sql.ps1" %*
set "EXITCODE=%ERRORLEVEL%"
rem When started by double-click, keep the window open so you can read the result.
echo %CMDCMDLINE% | find /i "%~nx0" >nul && pause
exit /b %EXITCODE%
