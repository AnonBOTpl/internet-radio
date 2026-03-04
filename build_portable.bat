@echo off
echo ===================================================
echo   BUILDING INTERNET RADIO (PORTABLE APP-IMAGE)
echo ===================================================

echo.
echo STEP 1: Building the project with Maven...
call mvn clean package
if %ERRORLEVEL% neq 0 (
    echo[ERROR] Maven build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo STEP 2: Preparing files...
if not exist "target\jpackage_input" mkdir target\jpackage_input
copy target\radio-app-1.1.0.jar target\jpackage_input\ >nul
copy target\libs\*.jar target\jpackage_input\ >nul

echo.
echo STEP 3: Generating portable directory using jpackage...
:: Dodano: --icon "icon.ico"
jpackage ^
  --type app-image ^
  --name "InternetRadio" ^
  --dest release ^
  --input target\jpackage_input ^
  --main-jar radio-app-1.1.0.jar ^
  --main-class net.anonbot.radio.Main ^
  --icon "icon.ico"

echo.
echo ===================================================
echo DONE! 
echo Your portable app is located in the "release\InternetRadio" folder.
echo You can zip this folder and run InternetRadio.exe anywhere.
echo No Java installation is required on the target machine!
echo ===================================================
pause