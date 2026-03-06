# -*- mode: python ; coding: utf-8 -*-
"""PyInstaller spec for FlyPrint Windows distribution.

Builds two executables:
  - flyprint-gui.exe  (windowed, no console) — main distribution
  - flyprint.exe      (console) — CLI usage

Usage:
  pyinstaller windows/flyprint.spec
"""

import os
import sys

block_cipher = None

# Paths
spec_dir = os.path.dirname(os.path.abspath(SPEC))
project_dir = os.path.dirname(spec_dir)

# Common analysis options
common_kwargs = dict(
    pathex=[project_dir],
    binaries=[],
    datas=[
        (os.path.join(project_dir, "flyprint", "assets", "icon.png"), "flyprint/assets"),
    ],
    hiddenimports=[
        "pystray",
        "pystray._win32",
        "PIL",
        "PIL.Image",
        "PIL.ImageDraw",
        "PIL.ImageFont",
        "win32print",
        "win32api",
        "tkinter",
        "tkinter.ttk",
        "tkinter.messagebox",
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        "cups",
        "pycups",
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

# --- GUI executable (windowed, no console) ---

gui_a = Analysis(
    [os.path.join(project_dir, "flyprint", "app_entry.py")],
    **common_kwargs,
)

gui_pyz = PYZ(gui_a.pure, gui_a.zipped_data, cipher=block_cipher)

gui_exe = EXE(
    gui_pyz,
    gui_a.scripts,
    [],
    exclude_binaries=True,
    name="flyprint-gui",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    icon=os.path.join(spec_dir, "flyprint.ico"),
)

# --- CLI executable (console) ---

cli_a = Analysis(
    [os.path.join(project_dir, "flyprint", "cli.py")],
    **common_kwargs,
)

cli_pyz = PYZ(cli_a.pure, cli_a.zipped_data, cipher=block_cipher)

cli_exe = EXE(
    cli_pyz,
    cli_a.scripts,
    [],
    exclude_binaries=True,
    name="flyprint",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=True,
    icon=os.path.join(spec_dir, "flyprint.ico"),
)

# --- Collect into one folder ---

coll = COLLECT(
    gui_exe,
    gui_a.binaries,
    gui_a.zipfiles,
    gui_a.datas,
    cli_exe,
    cli_a.binaries,
    cli_a.zipfiles,
    cli_a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name="FlyPrint",
)
