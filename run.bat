@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
javac -d bin src\*.java
if errorlevel 1 (
    echo Compilation failed! Make sure Java JDK is installed.
    pause
    exit /b 1
)
start "" javaw -cp bin Main
