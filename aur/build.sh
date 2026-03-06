#!/usr/bin/env bash
# Build the AUR package for flyprint.
# Run from the aur/ directory or pass --here to build in place.
#
# Usage: ./build.sh [--install]
#   --install   Also install the package after building (makepkg -si)
set -euo pipefail

cd "$(dirname "$0")"

INSTALL_FLAG=""
if [[ "${1:-}" == "--install" ]]; then
    INSTALL_FLAG="-i"
fi

echo "==> Updating .SRCINFO..."
# Clean previous builds
rm -rf flyprint-git/ pkg/ src/ *.pkg.tar.*

echo "==> Building AUR package..."
makepkg -sf ${INSTALL_FLAG}

echo "==> Updating .SRCINFO..."
makepkg --printsrcinfo > .SRCINFO

echo "==> Done."
ls -lh *.pkg.tar.* 2>/dev/null || true
