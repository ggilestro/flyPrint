# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is FlyPrint

FlyPrint is a distributed label printing agent for the [FlyRoom](https://www.flyroom.net) server. It polls a FlyRoom server for print jobs and prints them on local printers (Dymo thermal label printers via CUPS on Linux/macOS, or Win32 on Windows). There is also a planned Android companion app for Bluetooth thermal printers.

## Build & Development Commands

```bash
# Install in development mode (with all extras)
source .venv/bin/activate && pip install -e ".[cups,gui,dev]"

# Run the agent
flyprint start --verbose

# Run the GUI (system tray mode)
flyprint gui

# Run tests
source .venv/bin/activate && pytest

# Run a single test
source .venv/bin/activate && pytest tests/test_foo.py::test_bar -v

# Linting & formatting
source .venv/bin/activate && black --check flyprint/
source .venv/bin/activate && ruff check flyprint/

# Build AUR package
cd aur && makepkg -si
```

## Architecture

**Polling agent** that runs a loop: heartbeat → check config version → fetch pending jobs → claim/download/print/report each job. The server generates PDFs; the agent downloads and prints them.

### Key modules

- **`cli.py`** — Click CLI entry point. Commands: `configure`, `pair`, `test`, `start`, `status`, `printers`, `install-service`, `gui`
- **`agent.py`** — `FlyPrintAgent` class. Core polling loop (`run_once()`), job processing, heartbeat, config sync with server
- **`config.py`** — Two-file split: `config.json` (user-editable: server_url, api_key) and `cached_config.json` (server-synced: printer_name, poll_interval, cups_page, orientation, etc.) in `~/.config/flyprint/`
- **`printing/`** — Factory pattern via `get_printer()`. `PrinterBackend` protocol in `base.py`, implementations in `cups_printer.py` (pycups with `lp` fallback) and `win32_printer.py`
- **`gui/`** — `tray.py` (pystray system tray), `pairing_dialog.py` (Tkinter first-run wizard), `autostart.py` (systemd service setup)
- **`app_entry.py`** — GUI launcher: shows pairing dialog if unconfigured, starts agent in background thread, runs tray on main thread

### Entry points

- `flyprint` CLI → `flyprint.cli:main` (headless)
- `flyprint-gui` → `flyprint.app_entry:main` (GUI with system tray)
- `python -m flyprint` → `__main__.py`

### Job processing flow

```
send_heartbeat() → get_pending_jobs() → for each job:
  claim_job() → get_job_pdf() → start_job() → printer.print_pdf() → complete_job()
```

## Critical: PDF Printing Rules (DO NOT CHANGE)

Read `PRINTING_NOTES.md` before touching any printing code. Key rules:

1. **Always use PDF, never PNG** for CUPS printing on Dymo. PDF embeds 300 DPI content at correct physical dimensions; CUPS rasterizes at printer-native 300 DPI.
2. **`PageSize` must be set explicitly** in CUPS options (e.g., `w72h154` for Dymo 11352).
3. PNG approaches fundamentally cannot achieve 300 DPI on Dymo via CUPS — every combination was tested and failed (see the table in PRINTING_NOTES.md).

## Code Style

- Python 3.10+, line length 100 (`black` + `ruff`)
- Ruff rules: E, F, W, I, N, B
- Build system: hatchling (PEP 517/518)
- Core deps are minimal: `click` + `requests`. Everything else is optional extras (`cups`, `gui`, `windows`, `dev`)

## AUR Package

The `aur/` directory contains the Archlinux PKGBUILD. The package installs a systemd user service (`flyprint.service`), desktop entry, and icon. Use `master` branch (not `main`) per Arch convention.
