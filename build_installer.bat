@echo off
echo ===================================================
echo   BUILDING INTERNET RADIO (EXE INSTALLER)
echo ===================================================

echo.
echo STEP 1: Building the project with Maven...
call mvn clean package
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo STEP 2: Preparing files...
if not exist "target\jpackage_input" mkdir target\jpackage_input
copy target\radio-app-1.1.0.jar target\jpackage_input\ >nul
copy target\libs\*.jar target\jpackage_input\ >nul

echo.
echo STEP 3: Generating .exe installer using jpackage...
:: Dodano: --win-per-user-install (Omija koniecznosc bycia Administratorem!)
:: Dodano: --icon "icon.ico" (Wymaga pliku icon.ico w glownym folderze)
jpackage ^
  --type exe ^
  --name "InternetRadio" ^
  --dest release ^
  --input target\jpackage_input ^
  --main-jar radio-app-1.1.0.jar ^
  --main-class net.anonbot.radio.Main ^
  --icon "icon.ico" ^
  --win-per-user-install ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --app-version "1.1.0"

echo.
echo ===================================================
echo DONE! 
echo Installer (InternetRadio-1.1.0.exe) is ready in the "release" folder.
echo ===================================================
pause