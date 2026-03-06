#!/usr/bin/env bash
# Build the FlyPrint Android APK using Docker.
#
# Usage: ./build.sh [--local]
#   --local   Build using local Gradle (requires Android SDK) instead of Docker
set -euo pipefail

cd "$(dirname "$0")"

OUTPUT_APK="app-debug.apk"

if [[ "${1:-}" == "--local" ]]; then
    echo "==> Building APK locally with Gradle..."
    ./gradlew assembleDebug --no-daemon
    cp app/build/outputs/apk/debug/app-debug.apk "${OUTPUT_APK}"
else
    echo "==> Building APK via Docker..."
    docker build -t flyprint-android-build .

    # Run the container and extract the APK
    CONTAINER_ID=$(docker create flyprint-android-build)
    docker start -a "${CONTAINER_ID}"
    docker cp "${CONTAINER_ID}:/app/app/build/outputs/apk/debug/app-debug.apk" "${OUTPUT_APK}"
    docker rm "${CONTAINER_ID}"
fi

echo "==> Done."
ls -lh "${OUTPUT_APK}"
