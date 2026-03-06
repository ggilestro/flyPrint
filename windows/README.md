# FlyPrint Windows Build

## Prerequisites

- Python 3.10+ with pip
- [Inno Setup 6](https://jrsoftware.org/isdl.php) (for creating the installer)
- (Optional) [SumatraPDF portable](https://www.sumatrapdfreader.com/download-free-pdf-viewer) — place `SumatraPDF.exe` in `windows/SumatraPDF/` to bundle it with the installer

## Building

From the `windows/` directory on a Windows machine:

```bat
build.bat
```

This will:
1. Install FlyPrint with Windows and GUI dependencies
2. Build the executables with PyInstaller (two entry points: `flyprint-gui.exe` and `flyprint.exe`)
3. Create the installer with Inno Setup (if available)

## Output

- `dist/FlyPrint/` — Portable distribution folder (can be zipped and distributed as-is)
- `Output/FlyPrint-Setup-x.y.z.exe` — Inno Setup installer

## What the installer does

- Installs to `%PROGRAMFILES%\FlyPrint` (or user-local if no admin)
- Creates Start Menu shortcuts for the GUI and CLI
- Optionally creates a Desktop shortcut
- Optionally adds a "Start on Login" registry entry
- Bundles SumatraPDF portable for reliable PDF printing (if present during build)
- Registers an uninstaller

## SumatraPDF

FlyPrint prefers SumatraPDF for printing PDFs on Windows because it:
- Supports explicit page size control via CLI
- Waits for printing to complete (no temp file race conditions)
- Has no dependency on a registered system PDF viewer
- Is free and open source (~6 MB)

If SumatraPDF is not found, FlyPrint falls back to `ShellExecute` (uses the system's default PDF handler).

## Manual / portable usage

You can also run FlyPrint without the installer:

```bat
dist\FlyPrint\flyprint-gui.exe   # GUI with system tray
dist\FlyPrint\flyprint.exe start  # CLI headless mode
```
