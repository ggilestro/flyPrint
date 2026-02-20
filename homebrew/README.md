# FlyPrint Homebrew Formula

Install FlyPrint on macOS via [Homebrew](https://brew.sh).

## Installation

```bash
# Add the tap
brew tap ggilestro/flyprint

# Install
brew install flyprint
```

Or install directly:

```bash
brew install ggilestro/flyprint/flyprint
```

To install from the latest commit on `master`:

```bash
brew install --HEAD ggilestro/flyprint/flyprint
```

## Setup

1. **Pair** with your FlyRoom server:

   ```bash
   flyprint pair https://your-server.flyroom.net
   ```

2. **Test** that printing works:

   ```bash
   flyprint test
   ```

3. **Start** as a background service:

   ```bash
   brew services start flyprint
   ```

## GUI Mode

Launch the system tray app:

```bash
flyprint-gui
```

The GUI provides a menu bar icon for monitoring print jobs, changing settings, and pairing with servers.

## Useful Commands

```bash
flyprint status          # Show agent status and configuration
flyprint printers        # List available CUPS printers
flyprint start --verbose # Run in foreground with debug output
brew services stop flyprint   # Stop the background service
brew services restart flyprint
```

## Logs

When running as a service, logs are written to:

```
$(brew --prefix)/var/log/flyprint.log
```

## Updating

```bash
brew update
brew upgrade flyprint
```

## Maintaining the Formula

When dependencies change in `pyproject.toml`, regenerate the resource stanzas:

```bash
cd homebrew/
./generate_resources.sh
```

Then update `flyprint.rb` with the new output.

### Publishing a new version

1. Tag the release: `git tag v0.2.0 && git push --tags`
2. Get the tarball SHA256: `curl -sL https://github.com/ggilestro/flyPrint/archive/refs/tags/v0.2.0.tar.gz | sha256sum`
3. Update `url` and `sha256` in the formula
4. Push to the tap repo (`homebrew-flyprint`)
