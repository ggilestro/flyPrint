@echo off
REM Build FlyPrint Windows installer
REM Run from the windows/ directory on a Windows machine
REM
REM Prerequisites:
REM   - Python 3.10+ with pip
REM   - Inno Setup 6 (iscc.exe in PATH or default install location)
REM   - (Optional) SumatraPDF portable in windows\SumatraPDF\

setlocal

cd /d "%~dp0\.."
echo === Installing FlyPrint with build dependencies ===
pip install -e ".[windows,gui,build]"
if errorlevel 1 (
    echo ERROR: pip install failed
    exit /b 1
)

echo === Building with PyInstaller ===
pyinstaller windows\flyprint.spec --distpath dist --workpath build\pyinstaller --noconfirm
if errorlevel 1 (
    echo ERROR: PyInstaller build failed
    exit /b 1
)

echo === Checking for Inno Setup ===
where iscc >nul 2>&1
if errorlevel 1 (
    REM Try default install location
    if exist "C:\Program Files (x86)\Inno Setup 6\iscc.exe" (
        set "ISCC=C:\Program Files (x86)\Inno Setup 6\iscc.exe"
    ) else (
        echo WARNING: Inno Setup not found. Skipping installer creation.
        echo Install from https://jrsoftware.org/isdl.php
        echo PyInstaller output is in dist\FlyPrint\
        exit /b 0
    )
) else (
    set "ISCC=iscc"
)

echo === Building installer with Inno Setup ===
"%ISCC%" windows\flyprint.iss
if errorlevel 1 (
    echo ERROR: Inno Setup build failed
    exit /b 1
)

echo === Build complete ===
echo Installer: Output\FlyPrint-Setup-*.exe
echo Portable:  dist\FlyPrint\
