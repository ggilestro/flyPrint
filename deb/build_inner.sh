#!/usr/bin/env bash
# Build a .deb package for flyprint from the source tree mounted at /src.
# Designed to run inside the Docker container defined by deb/Dockerfile.
set -euo pipefail

# --- Extract version from pyproject.toml ---
VERSION=$(python3 -c "
import tomllib, pathlib
data = tomllib.loads(pathlib.Path('/src/pyproject.toml').read_text())
print(data['project']['version'])
")
echo "Building flyprint ${VERSION}"

PKG=/build/pkg
DEBIAN="${PKG}/DEBIAN"

# --- Build the wheel ---
cd /src
python3 -m build --wheel --outdir /build/dist

# --- Install the wheel into the package tree ---
# Debian's installer places files under /usr/local; we relocate to /usr
# so they integrate with the system Python and PATH as a proper .deb should.
python3 -m installer --destdir="${PKG}" /build/dist/*.whl

# Relocate /usr/local → /usr for proper Debian packaging
if [ -d "${PKG}/usr/local" ]; then
    mkdir -p "${PKG}/usr/bin"
    cp -a "${PKG}/usr/local/bin/"* "${PKG}/usr/bin/"
    rm -rf "${PKG}/usr/local/bin"
    PYVER=$(python3 -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')")
    if [ -d "${PKG}/usr/local/lib/python${PYVER}/dist-packages" ]; then
        mkdir -p "${PKG}/usr/lib/python3/dist-packages"
        cp -a "${PKG}/usr/local/lib/python${PYVER}/dist-packages/"* \
            "${PKG}/usr/lib/python3/dist-packages/"
        rm -rf "${PKG}/usr/local/lib"
    fi
    rmdir "${PKG}/usr/local" 2>/dev/null || true
fi

# --- Install desktop file ---
install -Dm644 /src/aur/flyprint.desktop \
    "${PKG}/usr/share/applications/flyprint.desktop"

# --- Install icon ---
install -Dm644 /src/flyprint/assets/icon.png \
    "${PKG}/usr/share/icons/hicolor/256x256/apps/flyprint.png"

# --- Install systemd user service ---
install -Dm644 /src/aur/flyprint.service \
    "${PKG}/usr/lib/systemd/user/flyprint.service"

# --- Install license as Debian copyright file ---
install -Dm644 /src/LICENSE \
    "${PKG}/usr/share/doc/flyprint/copyright"

# --- Calculate installed size (in KiB, as Debian expects) ---
INSTALLED_SIZE=$(du -sk "${PKG}" | cut -f1)

# --- Generate DEBIAN/control ---
mkdir -p "${DEBIAN}"
cat > "${DEBIAN}/control" <<EOF
Package: flyprint
Version: ${VERSION}
Architecture: all
Maintainer: Giorgio Gilestro <giorgio@gilest.ro>
Description: Local print agent for FlyPush label printing
 FlyPrint is a background agent that polls a FlyRoom server for
 print jobs and sends them to local Dymo thermal label printers.
 Supports headless mode and a system-tray GUI.
Depends: python3 (>= 3.10), python3-click, python3-requests, python3-pystray, python3-pil, python3-cups, python3-tk
Section: utils
Priority: optional
Homepage: https://github.com/ggilestro/flyPrint
Installed-Size: ${INSTALLED_SIZE}
EOF

# --- Build the .deb ---
dpkg-deb --build "${PKG}" "/out/flyprint_${VERSION}_all.deb"
echo "Done → /out/flyprint_${VERSION}_all.deb"
