@echo off
setlocal
echo === GitHive Packager ===
echo.

REM ── 1. Build fat JAR ──────────────────────────────────────────
echo [1/3] Building fat JAR (this may take a minute)...
call mvn package -q
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Make sure Maven is on your PATH.
    pause
    exit /b 1
)
echo       OK

REM ── 2. Prepare clean input folder ─────────────────────────────
echo [2/3] Preparing input...
if exist "dist" rmdir /s /q dist
mkdir dist\input
copy "target\githive-shaded.jar" "dist\input\" > nul
echo       OK

REM ── 3. Package with jpackage ───────────────────────────────────
echo [3/3] Creating Windows executable...
jpackage ^
  --type app-image ^
  --input dist\input ^
  --main-jar githive-shaded.jar ^
  --name GitHive ^
  --app-version 1.0.0 ^
  --dest dist\output

if errorlevel 1 (
    echo.
    echo PACKAGING FAILED. Make sure JDK 14+ bin folder is on your PATH.
    echo Current Java: & java -version
    pause
    exit /b 1
)

rmdir /s /q dist\input

echo.
echo === Done! ===
echo.
echo Executable: dist\output\GitHive\GitHive.exe
echo.
echo You can copy the entire dist\output\GitHive\ folder anywhere and run GitHive.exe.
pause
