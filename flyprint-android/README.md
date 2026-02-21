# flyPrint Android

Native Android app for printing labels from flyPush server on Android tablets.

## Project Overview

This is the Android companion to the flyPrint Python agent, designed to run on Android tablets with Bluetooth barcode scanners and thermal label printers (Brother QL-820NWB).

**Architecture**: Native Kotlin app with Foreground Service for background print job polling (5s interval), Jetpack Compose UI, and Retrofit API layer.

## Hardware Requirements

- Android tablet (Android 8.0+)
- Bluetooth barcode scanner (keyboard wedge type)
- Thermal label printer:
  - **Recommended**: Brother QL-820NWB ($250) or Zebra ZD421 ($450)
  - Bluetooth or WiFi connectivity
  - 54mm x 25mm label support

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Technical architecture and design decisions
- [MIGRATION_PLAN.md](docs/MIGRATION_PLAN.md) - Full migration plan from Python to Android
- [TODO.md](docs/TODO.md) - Implementation checklist and progress tracking
- [API_REFERENCE.md](docs/API_REFERENCE.md) - flyPush API endpoints used by this app

## Quick Start (After Build)

1. Download and install APK on Android tablet
2. Open FlyPrint app
3. Enter server URL and API key (from flyPush web UI → Settings → Print Agents)
4. Discover and select Bluetooth printer
5. Tap "Save & Start Service"
6. App polls for print jobs every 5 seconds in the background

## Development Setup

### Prerequisites

- Android Studio (latest version)
- JDK 11+
- Android SDK (API 26+, target 34)
- Brother Mobile SDK JAR (optional — app compiles without it using stub)

### Building with Docker (recommended)

No Android SDK required on the host machine:

```bash
cd flyprint-android

# Build Docker image (one-time)
docker build -t flyprint-android-build .

# Build APK
docker run --rm --user root -e GRADLE_USER_HOME=/tmp/gradle \
  -v "$(pwd):/app" -w /app flyprint-android-build \
  sh -c "gradle assembleDebug --no-daemon --project-cache-dir=/tmp/project-cache && chown -R $(id -u):$(id -g) /app/app/build"
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

### Building with Android Studio

```bash
cd flyprint-android

# 1. Generate Gradle wrapper (requires Gradle installed)
gradle wrapper

# 2. Copy local.properties template and set SDK path
cp local.properties.template local.properties
# Edit local.properties: sdk.dir=/path/to/Android/Sdk

# 3. (Optional) Add Brother SDK JAR
mkdir -p app/libs
# Download from: https://developerprogram.brother-usa.com/sdk-download
# Place JAR in app/libs/
# Uncomment the dependency line in app/build.gradle.kts

# 4. Build
./gradlew assembleDebug

# 5. Install to connected device
./gradlew installDebug
```

## Project Structure

```
flyprint-android/
├── .gitignore
├── build.gradle.kts                        # Root: AGP 8.2.2 + Kotlin 1.9.22
├── settings.gradle.kts                     # Project repos + modules
├── gradle.properties                       # JVM args, AndroidX flags
├── local.properties.template               # SDK path placeholder
├── gradle/wrapper/
│   └── gradle-wrapper.properties           # Gradle 8.5
├── app/
│   ├── build.gradle.kts                    # Deps: Retrofit, Compose, Security, Coroutines
│   ├── proguard-rules.pro                  # Keep rules for Retrofit, Gson, Brother
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions, activity, service, receiver
│       ├── java/ro/gilest/flyprint/
│       │   ├── FlyPrintApplication.kt      # App class, notification channel
│       │   ├── api/
│       │   │   ├── ApiModels.kt            # PrintJob, HeartbeatRequest, JobResult
│       │   │   ├── FlyPrintApi.kt          # Retrofit interface (6 endpoints)
│       │   │   ├── AuthInterceptor.kt      # X-API-Key header injection
│       │   │   └── ApiClient.kt            # Retrofit/OkHttp singleton
│       │   ├── config/
│       │   │   └── AppConfig.kt            # SharedPreferences + encrypted storage
│       │   ├── printer/
│       │   │   ├── PrinterInfo.kt          # Printer data class
│       │   │   ├── PrinterManager.kt       # Interface + PrinterException
│       │   │   └── BrotherPrinterManager.kt # Brother SDK stub
│       │   ├── service/
│       │   │   ├── FlyPrintService.kt      # Foreground service, polling loop
│       │   │   └── BootReceiver.kt         # Auto-start on device boot
│       │   └── ui/
│       │       ├── MainActivity.kt         # Single activity host
│       │       ├── FlyPrintApp.kt          # Root composable (setup ↔ status)
│       │       ├── SetupWizardScreen.kt    # Server URL, API key, printer selection
│       │       ├── StatusScreen.kt         # Dashboard: status, printer, jobs
│       │       └── theme/
│       │           ├── Color.kt            # Color constants
│       │           ├── Type.kt             # Typography
│       │           └── Theme.kt            # Material3 theme
│       └── res/
│           ├── drawable/ic_printer.xml     # Notification icon vector
│           ├── mipmap-anydpi-v26/          # Adaptive launcher icons
│           ├── mipmap-xxhdpi/              # Raster launcher icons
│           ├── values/                     # strings.xml, colors.xml, themes.xml
│           └── xml/network_security_config.xml
└── docs/
    ├── ARCHITECTURE.md
    ├── MIGRATION_PLAN.md
    ├── TODO.md
    └── API_REFERENCE.md
```

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Image format** | PNG (not PDF) | Brother SDK prints bitmaps natively; no CUPS involved |
| **Background strategy** | Foreground Service | WorkManager's 15-min minimum is too slow for 5s polling |
| **Printer SDK** | Stub implementation | App compiles without JAR; swap in real SDK calls later |
| **Service type** | `connectedDevice` | Required on Android 14+ for Bluetooth foreground services |
| **API key storage** | EncryptedSharedPreferences | Android equivalent of Python agent's `chmod 600` |

## Key Technologies

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose (Material3)
- **Background**: Foreground Service with Handler-based polling
- **Networking**: Retrofit 2.9 + OkHttp 4.12
- **Security**: AndroidX Security Crypto (EncryptedSharedPreferences)
- **Build**: Gradle 8.5 + AGP 8.2.2

## Status

**Current Phase**: Scaffold complete — all source files, build system, and resources in place.

**Next steps**:
1. Install Android SDK and run `gradle wrapper` to generate wrapper scripts
2. `./gradlew assembleDebug` to verify compilation
3. Add Brother Mobile SDK JAR and replace stub methods in `BrotherPrinterManager.kt`
4. Test on physical tablet with printer

## Related Projects

- [flyPush](../) - Main web application (FastAPI + HTMX)
- [flyPrint Python](../flyprint/) - Original Linux/CUPS agent

## License

MIT License - see LICENSE file
