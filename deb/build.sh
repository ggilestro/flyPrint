#!/usr/bin/env bash
# Build the .deb package for flyprint using Docker.
#
# Usage: ./build.sh
# Output: out/flyprint_<version>_all.deb
set -euo pipefail

cd "$(dirname "$0")"
PROJECT_ROOT="$(cd .. && pwd)"

echo "==> Building Docker image for .deb packaging..."
docker build -t flyprint-deb-build .

echo "==> Building .deb package..."
mkdir -p out
docker run --rm \
    -v "${PROJECT_ROOT}:/src:ro" \
    -v "$(pwd)/out:/out" \
    flyprint-deb-build

echo "==> Done."
ls -lh out/*.deb 2>/dev/null || true
