@echo off
setlocal
cd /d "%~dp0"
if not exist ".local\environment.cmd" (
    echo Missing .local\environment.cmd. See README.md for local setup.
    pause
    exit /b 1
)
call ".local\environment.cmd"
call "%~dp0gradlew.bat" --no-daemon runClient "-PsinguloJar=%ARSENAL_SINGULO%" "-PpythonExecutable=%ARSENAL_PYTHON%"
set "ARSENAL_EXIT=%ERRORLEVEL%"
if not "%ARSENAL_EXIT%"=="0" pause
exit /b %ARSENAL_EXIT%
