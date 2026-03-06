#!/usr/bin/env bash
# Build FlyPrint packages for one or more distribution targets.
#
# Usage:
#   ./build_all.sh              # Build all targets
#   ./build_all.sh aur          # Build only AUR package
#   ./build_all.sh deb homebrew # Build deb and homebrew
#
# Targets: aur, deb, homebrew, android
set -euo pipefail

cd "$(dirname "$0")"

TARGETS=("${@:-aur deb homebrew android}")
# If no args, expand the default string into an array
if [[ $# -eq 0 ]]; then
    TARGETS=(aur deb homebrew android)
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
        *)
            echo "Unknown target: ${target}"
            echo "Available: aur, deb, homebrew, android"
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
