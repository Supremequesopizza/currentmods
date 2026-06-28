@echo off
setlocal

echo ================================================
echo AoTD Industry Patch Installer
echo MAKE SURE YOU BACKUP starfarer.api.jar MANUALLY
echo (Copy it to a safe backup folder before running)
echo ================================================
echo.

set SCRIPT_DIR=%~dp0
set PATCHER_JAR=%SCRIPT_DIR%IndustryPatcher.jar
set ASM_JAR=%SCRIPT_DIR%asm-9.1.jar
set API_JAR=%SCRIPT_DIR%starfarer.api.jar

if not exist "%PATCHER_JAR%" (
  echo Missing IndustryPatcher.jar
  goto fail
)

if not exist "%ASM_JAR%" (
  echo Missing asm-9.1.jar
  goto fail
)

if not exist "%API_JAR%" (
  echo Missing starfarer.api.jar
  goto fail
)

cd /d "%SCRIPT_DIR%"

echo Running patcher...
java -cp "%ASM_JAR%;%PATCHER_JAR%;%API_JAR%" IndustryPatch

if errorlevel 1 (
  echo Java patcher failed!
  goto fail
)

if not exist "starfarer.api.jar.tmp" (
  echo Temporary patched jar not found!
  goto fail
)

echo Replacing original jar...
del /f /q "starfarer.api.jar" >nul 2>nul
move /y "starfarer.api.jar.tmp" "starfarer.api.jar"

echo.
echo Patch completed successfully.
echo Press ENTER to exit.
pause >nul
exit /b 0

:fail
echo.
echo Patch FAILED.
echo Press ENTER to exit.
pause >nul
exit /b 1
