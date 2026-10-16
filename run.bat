@echo off
REM Compiles and runs the AWT Donation Management System (Windows)
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    del sources.txt
    echo.
    echo Compilation failed. Check that JDK is installed and "javac" works.
    pause
    exit /b 1
)
del sources.txt
java -Dfile.encoding=UTF-8 -cp out com.awt.donation.Main
pause
