#!/usr/bin/env bash
# Build FlyPrint packages for one or more distribution targets.
#
# Usage:
#   ./build_all.sh              # Build all targets
#   ./build_all.sh aur          # Build only AUR package
#   ./build_all.sh deb homebrew # Build deb and homebrew
#
# Targets: aur, deb, homebrew, android, windows
set -euo pipefail

cd "$(dirname "$0")"

TARGETS=("${@:-aur deb homebrew android}")
# If no args, expand the default string into an array
if [[ $# -eq 0 ]]; then
    TARGETS=(aur deb homebrew android windows)
fi

FAILED=()

for target in "${TARGETS[@]}"; do
    echo
    echo "========================================="
    echo "  Building: ${target}"
    echo "========================================="
    echo

    case "${target}" in
        aur)
            (cd aur && bash build.sh) || FAILED+=("${target}")
            ;;
        deb)
            (cd deb && bash build.sh) || FAILED+=("${target}")
            ;;
        homebrew|brew)
            (cd homebrew && bash build.sh) || FAILED+=("${target}")
            ;;
        android)
            (cd flyprint-android && bash build.sh) || FAILED+=("${target}")
            ;;
        windows|win)
            if [[ "$(uname -s)" == MINGW* ]] || [[ "$(uname -s)" == MSYS* ]] || [[ "$(uname -s)" == CYGWIN* ]]; then
                (cd windows && cmd //c build.bat) || FAILED+=("${target}")
            else
                echo "WARNING: Windows build must run on Windows. Skipping."
                echo "  Run 'windows\\build.bat' on a Windows machine."
            fi
            ;;
        *)
            echo "Unknown target: ${target}"
            echo "Available: aur, deb, homebrew, android, windows"
            FAILED+=("${target}")
            ;;
    esac
done

echo
echo "========================================="
if [[ ${#FAILED[@]} -eq 0 ]]; then
    echo "  All builds completed successfully."
else
    echo "  Failed targets: ${FAILED[*]}"
    exit 1
fi
echo "========================================="
