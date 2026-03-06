# FlyPrint

Local print agent for [FlyRoom](https://www.flyroom.net) label printing.

FlyPrint is a lightweight agent that runs on local machines (Raspberry Pi, desktop, etc.) and polls your FlyRoom server for print jobs. When a job is available, it downloads the label PDF and prints it locally.

## Installation

### Arch Linux (AUR)

```bash
yay -S flyprint-git
```

### Debian / Ubuntu / Raspberry Pi

Download the `.deb` from the [latest release](https://github.com/ggilestro/flyPrint/releases):

```bash
sudo dpkg -i flyprint_*.deb
```

### macOS (Homebrew)

```bash
brew tap ggilestro/flyprint
brew install flyprint
```

### Windows

Download `FlyPrint-Setup-*.exe` from the [latest release](https://github.com/ggilestro/flyPrint/releases) and run the installer. It includes [SumatraPDF](https://www.sumatrapdfreader.com/) for reliable PDF printing.

For portable use, download the `FlyPrint-Windows-Portable` artifact instead — no installation needed.

### Android

Download `FlyPrint-Android.apk` from the [latest release](https://github.com/ggilestro/flyPrint/releases).

### From source (any platform)

```bash
pip install ".[cups,gui]"     # Linux/macOS
pip install ".[windows,gui]"  # Windows
```

## Quick Start

### 1. Pair with your server

Start pairing from the FlyRoom web UI (Settings > Labels > Add Agent), then:

```bash
flyprint pair              # Auto-pairs by IP
flyprint pair AB3K9X       # Or use the 6-character code
```

### 2. Test the connection

```bash
flyprint test
```

### 3. Start the agent

```bash
flyprint start             # Headless CLI mode
flyprint gui               # System tray mode
```

## Commands

| Command | Description |
|---------|-------------|
| `flyprint pair [CODE]` | Pair with your FlyRoom server |
| `flyprint configure` | Manual setup (server URL + API key) |
| `flyprint test` | Test connection to server and printer |
| `flyprint start` | Start the print agent (headless) |
| `flyprint gui` | Start with system tray icon |
| `flyprint status` | Show current configuration |
| `flyprint printers` | List available printers |
| `flyprint install-service` | Install auto-start (systemd/registry) |

## Running as a Service

### Linux (systemd)

```bash
flyprint install-service --user
systemctl --user enable --now flyprint
journalctl --user -u flyprint -f   # View logs
```

### Windows

```bash
flyprint install-service   # Adds to registry Run key
```

Or check "Start on Login" during installation, or from the system tray menu.

### macOS

```bash
brew services start flyprint
```

## Building & Releasing

FlyPrint builds for 5 platforms via GitHub Actions. All builds trigger automatically on version tags and are also available via manual dispatch.

### Automated release

```bash
# Bump version in pyproject.toml and flyprint/__init__.py, then:
git tag v0.2.0
git push origin master --tags
```

This creates a draft GitHub Release with artifacts for all platforms attached.

### Manual builds

Each platform has a standalone build script:

| Platform | Build command | Output |
|----------|--------------|--------|
| AUR | `cd aur && bash build.sh` | `*.pkg.tar.zst` |
| Debian | `cd deb && bash build.sh` | `deb/out/*.deb` |
| Homebrew | `cd homebrew && bash build.sh` | Tests formula |
| Windows | `windows\build.bat` (on Windows) | `dist\FlyPrint\`, installer `.exe` |
| Android | `cd flyprint-android && bash build.sh` | `app-debug.apk` |
| All | `./build_all.sh` | Builds all (skips Windows on Linux) |

### CI workflows

| Workflow | Runner | What it builds |
|----------|--------|----------------|
| `build-deb.yml` | Ubuntu (Docker) | `.deb` package |
| `build-aur.yml` | Arch container | AUR package |
| `build-windows.yml` | Windows | PyInstaller `.exe` + Inno Setup installer |
| `build-android.yml` | Ubuntu | Gradle APK |
| `build-homebrew.yml` | macOS | Formula syntax validation |
| `release.yml` | All | Orchestrates all builds on tag push |

### Development

```bash
pip install -e ".[cups,gui,dev]"  # Install in dev mode
pytest                             # Run tests
black flyprint/                    # Format
ruff check flyprint/               # Lint
```

## Configuration

Configuration is stored in `~/.config/flyprint/`:

- `config.json` — Server URL and API key (user-editable)
- `cached_config.json` — Operational settings synced from server

## Printer Setup

### Dymo LabelWriter (Linux/macOS)

```bash
# Debian/Ubuntu
sudo apt install printer-driver-dymo cups

# Arch Linux
sudo pacman -S cups python-pycups
```

Connect via USB, then add to CUPS:
```bash
sudo lpadmin -p dymo400 -E -v usb://DYMO/LabelWriter%20400 \
  -P /usr/share/ppd/dymo/lw400.ppd
```

### Windows

Install the Dymo driver from [dymo.com](https://www.dymo.com/support). FlyPrint uses SumatraPDF (bundled with the installer) for reliable PDF printing with proper page size control.

## Troubleshooting

**"CUPS not available"** (Linux/macOS) — `sudo systemctl status cups`

**"Printer backend not available"** (Windows) — Install pywin32: `pip install pywin32`

**"No printers found"** — `lpstat -p` (Linux) or check Windows printer settings

**"Server rejected heartbeat"** — Verify API key and server URL

**Verbose logging:** `flyprint start --verbose`

## License

MIT License — see LICENSE file.
