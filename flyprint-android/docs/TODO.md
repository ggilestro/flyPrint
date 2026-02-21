# flyPrint Android - Implementation TODO

**Project Start Date**: 2026-02-06
**Estimated Completion**: 12 weeks (2026-04-30)

---

## Phase 0: Preparation ⏳ (Week 1)

### Hardware & Tools
- [ ] Choose printer brand
  - [ ] If budget-conscious: Brother QL-820NWB ($250)
  - [ ] If production-grade: Zebra ZD421 ($450)
- [ ] Order hardware
  - [ ] Android tablet (Android 8.0+, $200-300)
  - [ ] Thermal label printer
  - [ ] Bluetooth barcode scanner ($50-100)
- [ ] Set up development environment
  - [ ] Install Android Studio (latest version)
  - [ ] Install JDK 11+
  - [ ] Configure Android SDK (API 26+)

### SDK & Registration
- [ ] Register for printer SDK
  - [ ] Brother: [Developer Program](https://developerprogram.brother-usa.com/sdk-download)
  - [ ] Zebra: [Developer Portal](https://developer.zebra.com/)
- [ ] Download printer SDK (JAR/AAR)
- [ ] Review SDK documentation
  - [ ] Discovery API
  - [ ] Connection API
  - [ ] Print API

### Project Setup
- [ ] Create Android Studio project
  - [ ] Application name: "FlyPrint"
  - [ ] Package: `ro.gilest.flyprint`
  - [ ] Minimum SDK: API 26 (Android 8.0)
  - [ ] Language: Kotlin
- [ ] Configure build.gradle.kts
  - [ ] Add dependencies (Retrofit, Compose, WorkManager)
  - [ ] Add printer SDK JAR to `app/libs/`
- [ ] Create Git repository branch
  - [ ] Branch: `feature/android-print-agent`
  - [ ] Push initial project structure

---

## Phase 1: POC - Print Service 🔧 (Week 2-3)

**Goal**: Prove Android → printer communication works

### Minimal App
- [ ] Create single-activity app
  - [ ] MainActivity with Jetpack Compose
  - [ ] Button: "Discover Printers"
  - [ ] Button: "Print Test Label"
  - [ ] Status text display

### Printer SDK Integration
- [ ] Add printer SDK to project
  - [ ] Copy JAR to `app/libs/`
  - [ ] Add `implementation(files("libs/..."))` to build.gradle
- [ ] Implement printer discovery
  - [ ] Bluetooth discovery (scan for nearby printers)
  - [ ] WiFi discovery (network scan)
  - [ ] Display discovered printers in UI
- [ ] Implement connection logic
  - [ ] Connect to selected printer
  - [ ] Handle connection errors
  - [ ] Display connection status

### Test Printing
- [ ] Create test label PNG (54mm x 25mm)
  - [ ] Add to `app/src/main/res/raw/test_label.png`
  - [ ] QR code with text "TEST-001"
- [ ] Implement print function
  - [ ] Read PNG from resources
  - [ ] Send to printer via SDK
  - [ ] Handle print errors
- [ ] Test print quality
  - [ ] Verify label dimensions (54mm x 25mm)
  - [ ] Verify QR code readability
  - [ ] Test multiple copies

### Permissions
- [ ] Add required permissions to AndroidManifest.xml
  - [ ] `BLUETOOTH`
  - [ ] `BLUETOOTH_ADMIN`
  - [ ] `BLUETOOTH_SCAN` (API 31+)
  - [ ] `BLUETOOTH_CONNECT` (API 31+)
  - [ ] `ACCESS_FINE_LOCATION` (for Bluetooth discovery)
  - [ ] `INTERNET`
- [ ] Request runtime permissions

**Deliverable**: App that successfully prints a test label

---

## Phase 2: POC - API Integration 🌐 (Week 4)

**Goal**: Prove Android ↔ flyPush API communication

### Retrofit Setup
- [ ] Add Retrofit dependencies
  - [ ] `retrofit2:retrofit:2.9.0`
  - [ ] `retrofit2:converter-gson:2.9.0`
  - [ ] `okhttp3:okhttp:4.12.0`
  - [ ] `okhttp3:logging-interceptor:4.12.0`
- [ ] Create API module (`app/src/main/java/ro/gilest/flyprint/api/`)
  - [ ] `FlyPushApi.kt` - Retrofit interface
  - [ ] `ApiModels.kt` - Data classes
  - [ ] `AuthInterceptor.kt` - API key authentication
  - [ ] `ApiClient.kt` - Retrofit instance factory

### API Endpoint Definitions
Reference: `/app/labels/router.py` (lines 712-1007)

- [ ] Define data classes
  - [ ] `PrintJob(id, stock_ids, label_format, copies, code_type, status)`
  - [ ] `HeartbeatRequest(printer_name, printer_status)`
  - [ ] `JobResult(success, error_message)`
- [ ] Define API interface
  - [ ] `POST /api/labels/agent/heartbeat`
  - [ ] `GET /api/labels/agent/jobs`
  - [ ] `POST /api/labels/agent/jobs/{id}/claim`
  - [ ] `GET /api/labels/agent/jobs/{id}/image`
  - [ ] `POST /api/labels/agent/jobs/{id}/start`
  - [ ] `POST /api/labels/agent/jobs/{id}/complete`

### API Key Authentication
- [ ] Implement `AuthInterceptor`
  - [ ] Add `X-API-Key` header to all requests
  - [ ] Read API key from SharedPreferences
- [ ] Add OkHttp logging (debug builds only)

### Manual API Testing
- [ ] Create test UI with buttons
  - [ ] "Send Heartbeat"
  - [ ] "Fetch Pending Jobs"
  - [ ] "Download Job PDF"
- [ ] Test against development server
  - [ ] Create test print agent in flyPush web UI
  - [ ] Get API key
  - [ ] Verify heartbeat updates `last_seen` timestamp
  - [ ] Verify job fetch returns test jobs
  - [ ] Verify PDF/PNG download works

**Deliverable**: App that can fetch real jobs from server

---

## Phase 3: Core Agent Logic ⚙️ (Week 5-6)

**Goal**: Full polling and job processing

### Foreground Service
- [ ] Create `FlyPrintService.kt`
  - [ ] Extend `Service` class
  - [ ] Implement `onCreate()`, `onStartCommand()`, `onDestroy()`
  - [ ] Create persistent notification (required for foreground service)
  - [ ] Return `START_STICKY` for auto-restart

### Polling Loop
- [ ] Implement 5-second polling loop
  - [ ] Use `Handler.postDelayed()` for interval
  - [ ] Call `pollForJobs()` every 5 seconds
- [ ] Implement `pollForJobs()` function
  - [ ] Send heartbeat to server
  - [ ] Fetch pending jobs
  - [ ] Process each job sequentially

### Job Processing
Reference: `/flyprint/agent.py` (lines 235-307)

- [ ] Implement `processJob(job: PrintJob)`
  - [ ] Claim job via API
  - [ ] Download image via API
  - [ ] Mark as printing
  - [ ] Print via printer SDK
  - [ ] Mark as completed/failed
- [ ] Handle success case
  - [ ] Print all copies
  - [ ] Complete job with `success = true`
- [ ] Handle failure case
  - [ ] Catch printer errors
  - [ ] Complete job with `success = false, error = e.message`
  - [ ] Log error details

### Error Handling
- [ ] Network errors
  - [ ] Catch `IOException`, `HttpException`
  - [ ] Log error and retry on next poll
  - [ ] Don't crash service
- [ ] Printer errors
  - [ ] Catch printer SDK exceptions
  - [ ] Report error to server
  - [ ] Show notification to user
- [ ] Battery optimization warnings
  - [ ] Detect if app is battery-optimized
  - [ ] Show dialog requesting exemption

### Service Lifecycle
- [ ] Start service from MainActivity
  - [ ] `startForegroundService(Intent(this, FlyPrintService::class.java))`
- [ ] Stop service when configured
  - [ ] "Stop Service" button in UI
  - [ ] Save state before stopping

**Deliverable**: Fully functional background agent

---

## Phase 4: Configuration UI 🎨 (Week 7)

**Goal**: User-friendly setup

### SharedPreferences Storage
- [ ] Create `AppConfig.kt`
  - [ ] Wrapper for SharedPreferences
  - [ ] Encrypt sensitive data (API key)
- [ ] Store configuration
  - [ ] `server_url: String`
  - [ ] `api_key: String`
  - [ ] `printer_name: String`
  - [ ] `printer_address: String` (MAC or IP)
  - [ ] `printer_type: String` ("zebra" or "brother")

### Setup Wizard UI (Jetpack Compose)
- [ ] Create `SetupWizard` composable
  - [ ] Step 1: Welcome screen
  - [ ] Step 2: Server configuration
  - [ ] Step 3: Printer discovery
  - [ ] Step 4: Test connection
  - [ ] Step 5: Completion
- [ ] Step 2: Server Configuration
  - [ ] TextField for server URL
  - [ ] TextField for API key (password type)
  - [ ] Validation (URL format, non-empty)
- [ ] Step 3: Printer Discovery
  - [ ] "Scan for Printers" button
  - [ ] Show loading indicator during scan
  - [ ] Display list of discovered printers
  - [ ] Allow selection
- [ ] Step 4: Test Connection
  - [ ] Test server connection (send heartbeat)
  - [ ] Test printer connection (connect and disconnect)
  - [ ] Show success/failure status
- [ ] Step 5: Completion
  - [ ] Save configuration
  - [ ] "Start Service" button
  - [ ] Navigate to status screen

### First-Run Detection
- [ ] Check if config exists on app start
  - [ ] If no config: show SetupWizard
  - [ ] If configured: show StatusScreen
- [ ] Add "Reconfigure" option in settings

**Deliverable**: No-code-editing setup experience

---

## Phase 5: Status & Monitoring 📊 (Week 8)

**Goal**: Visibility into agent health

### Status Dashboard
- [ ] Create `StatusScreen` composable
  - [ ] Service status indicator (running/stopped)
  - [ ] Last heartbeat timestamp
  - [ ] Printer connection status
  - [ ] Server URL display
- [ ] Agent Status Card
  - [ ] 🟢 Green: Service running, heartbeat recent (<60s)
  - [ ] 🟡 Yellow: Service running, heartbeat delayed
  - [ ] 🔴 Red: Service stopped or connection lost
- [ ] Recent Jobs List
  - [ ] Show last 10 processed jobs
  - [ ] Display: stock_id, status, timestamp
  - [ ] Color-code by status (green=completed, red=failed)

### Notifications
- [ ] Job completed notification
  - [ ] Show number of labels printed
  - [ ] Low priority (no sound)
- [ ] Job failed notification
  - [ ] Show error message
  - [ ] High priority (sound + vibration)
- [ ] Printer error notification
  - [ ] Show printer status
  - [ ] Action: "Reconnect"
- [ ] Connection lost notification
  - [ ] Show when server unreachable
  - [ ] Action: "Check Network"

### Logs Viewer
- [ ] In-app log display
  - [ ] Show last 100 log lines
  - [ ] Scrollable list
  - [ ] Color-code by level (ERROR=red, WARN=yellow, INFO=gray)
- [ ] Export logs button
  - [ ] Save to file
  - [ ] Share via Android share sheet
- [ ] Clear logs button

**Deliverable**: Observable agent behavior

---

## Phase 6: PWA Enhancements 📱 (Week 9)

**Goal**: Optimize web UI for tablets

### Web App Manifest
- [ ] Create `/app/static/manifest.json`
  - [ ] Set name, short_name, description
  - [ ] Set start_url, display (standalone)
  - [ ] Set theme_color, background_color
  - [ ] Add icons array
- [ ] Create PWA icons
  - [ ] Generate 192x192 and 512x512 PNG icons
  - [ ] Save to `/app/static/icons/`
- [ ] Link manifest in base template
  - [ ] Edit `/app/templates/base.html`
  - [ ] Add `<link rel="manifest" href="/static/manifest.json">`

### Service Worker
- [ ] Create `/app/static/sw.js`
  - [ ] Cache static assets on install
  - [ ] Network-first strategy for HTML
  - [ ] Cache-first for CSS/JS/images
  - [ ] Skip caching API calls
- [ ] Register service worker
  - [ ] Add script to `/app/templates/base.html`
  - [ ] Check `'serviceWorker' in navigator`
  - [ ] Call `navigator.serviceWorker.register('/sw.js')`

### Tablet UI Improvements
- [ ] Increase touch target sizes
  - [ ] Find all buttons in templates
  - [ ] Change from `px-2 py-1` to `px-6 py-3`
  - [ ] Add `min-h-[48px] min-w-[48px]` classes
- [ ] Test responsive layouts
  - [ ] Stock list page (grid layout on tablets)
  - [ ] Stock detail page (two-column on tablets)
  - [ ] Labels page (larger fonts)
- [ ] Optimize landscape mode
  - [ ] Pin search bar to top
  - [ ] Ensure critical UI fits above fold

### Print Agent Status Widget
- [ ] Create widget component (`/app/templates/components/agent_status.html`)
  - [ ] Show online/offline indicator
  - [ ] Display last heartbeat time
  - [ ] Show pending job count
  - [ ] Auto-refresh every 10 seconds
- [ ] Add widget to dashboard
  - [ ] Edit `/app/templates/dashboard.html`
  - [ ] Include agent_status component
- [ ] Style widget
  - [ ] Green background when online
  - [ ] Gray background when offline
  - [ ] Large font for readability

### Bluetooth Scanner Guide
- [ ] Create help page (`/app/templates/help/bluetooth-scanner.html`)
  - [ ] Instructions for pairing Bluetooth scanner
  - [ ] Screenshot of Android Bluetooth settings
  - [ ] Testing instructions (scan barcode in search box)
- [ ] Link from settings page

**Deliverable**: Installable PWA optimized for tablets

---

## Phase 7: Testing & Polish 🧪 (Week 10-11)

**Goal**: Ensure reliability

### Hardware Testing
- [ ] Test on multiple Android versions
  - [ ] Android 8.0 (API 26)
  - [ ] Android 10 (API 29)
  - [ ] Android 12 (API 31)
  - [ ] Android 14 (API 34)
- [ ] Test on multiple tablets
  - [ ] Samsung Galaxy Tab
  - [ ] Lenovo Tab
  - [ ] Amazon Fire HD (if applicable)
- [ ] Test Bluetooth vs WiFi printer
  - [ ] Pair via Bluetooth
  - [ ] Connect via WiFi
  - [ ] Verify both work reliably
- [ ] Battery life test
  - [ ] Run for 8 hours continuous
  - [ ] Monitor battery drain
  - [ ] Optimize if drain > 10%/hour

### Edge Case Testing
- [ ] Network interruption during print
  - [ ] Disconnect WiFi mid-print
  - [ ] Verify job marked as failed
  - [ ] Verify retry on reconnect
- [ ] Printer out of labels
  - [ ] Trigger printer error
  - [ ] Verify error reported to server
  - [ ] Verify user notification shown
- [ ] Multiple jobs queued
  - [ ] Create 10 jobs at once
  - [ ] Verify processed sequentially
  - [ ] Verify all completed correctly
- [ ] App kill and restart
  - [ ] Force stop app
  - [ ] Verify service restarts (START_STICKY)
  - [ ] Verify configuration persists
- [ ] Tablet reboot
  - [ ] Reboot device
  - [ ] Verify service auto-starts on boot
  - [ ] Add RECEIVE_BOOT_COMPLETED permission if needed

### Performance Optimization
- [ ] Implement idle mode
  - [ ] If no jobs for 5 minutes, reduce poll to 30s
  - [ ] Resume 5s polling when job arrives
- [ ] Exponential backoff on errors
  - [ ] If network error, wait 5s, 10s, 20s, 40s, 60s (max)
  - [ ] Reset on successful poll
- [ ] Minimize battery drain
  - [ ] Use WakeLock only during print operation
  - [ ] Release locks immediately after print
  - [ ] Consider Doze mode exemption dialog

**Deliverable**: Production-ready app

---

## Phase 8: Deployment 🚀 (Week 12)

**Goal**: Deploy to lab

### Build Release APK
- [ ] Configure ProGuard/R8
  - [ ] Create `proguard-rules.pro`
  - [ ] Add keep rules for Retrofit, Gson
  - [ ] Enable minification in build.gradle
- [ ] Create signing keystore
  - [ ] Run `keytool -genkeypair -v -keystore flystocks.jks ...`
  - [ ] Store keystore securely (NOT in Git)
- [ ] Configure signing in build.gradle
  - [ ] Add release signing config
  - [ ] Use environment variables for passwords
- [ ] Build release APK
  - [ ] `./gradlew assembleRelease`
  - [ ] Test release build thoroughly

### Installation Guide
- [ ] Create user guide (`docs/INSTALLATION.md`)
  - [ ] Step 1: Enable "Install from Unknown Sources"
  - [ ] Step 2: Download APK
  - [ ] Step 3: Install APK
  - [ ] Step 4: Open flyPrint app
  - [ ] Step 5: Complete setup wizard
  - [ ] Step 6: Test print
- [ ] Create troubleshooting section
  - [ ] "Service not running" → Check battery optimization
  - [ ] "Printer not found" → Check Bluetooth pairing
  - [ ] "Connection failed" → Verify server URL and API key
- [ ] Create video walkthrough (optional)

### Deploy to Lab
- [ ] Install on tablet(s)
  - [ ] Download APK to tablet
  - [ ] Install via file manager
- [ ] Configure with production server
  - [ ] Get server URL from ops
  - [ ] Create print agent in web UI
  - [ ] Copy API key to Android app
- [ ] Pair Bluetooth scanner
  - [ ] Settings → Bluetooth → Pair scanner
  - [ ] Test scan in web UI search box
- [ ] Connect printer
  - [ ] Discover printer in app
  - [ ] Test print label
  - [ ] Verify dimensions and quality

### User Training
- [ ] Train lab members
  - [ ] How to use web UI on tablet
  - [ ] How to scan barcodes
  - [ ] How to print labels
  - [ ] How to check print agent status
- [ ] Distribute quick reference card
- [ ] Set up support channel (Slack/email)

### Monitoring
- [ ] Monitor for first week
  - [ ] Check server logs for agent errors
  - [ ] Ask users for feedback
  - [ ] Track print success/failure rates
- [ ] Fix critical bugs immediately
  - [ ] Hotfix release if needed
  - [ ] Update APK and redeploy
- [ ] Schedule weekly check-ins

**Deliverable**: Running system in production

---

## Phase 9: Future Enhancements 🚀 (Optional)

### Firebase Cloud Messaging (FCM)
- [ ] Set up Firebase project
  - [ ] Add app to Firebase console
  - [ ] Download `google-services.json`
- [ ] Implement push notifications
  - [ ] Server sends FCM notification when job created
  - [ ] Android receives and wakes service
  - [ ] Eliminates need for constant polling
- [ ] Benefits
  - [ ] Reduced battery drain
  - [ ] Instant job processing (no 5s delay)
  - [ ] Works even when app closed

### Multi-Printer Support
- [ ] Support multiple printers per tablet
  - [ ] Store array of printer configs
  - [ ] UI to add/remove printers
- [ ] Route jobs based on label format
  - [ ] Job metadata: "target_printer": "printer_A"
  - [ ] Agent routes to correct printer
- [ ] Load balancing
  - [ ] If multiple printers available, distribute jobs

### Offline Job Queue
- [ ] Store jobs locally when offline
  - [ ] SQLite database for job queue
  - [ ] Sync when connection restored
- [ ] Background sync
  - [ ] Use WorkManager for periodic sync
  - [ ] Retry failed jobs

### Google Play Store Release
- [ ] Create Play Console account ($25)
- [ ] Prepare store listing
  - [ ] App description, screenshots
  - [ ] Privacy policy
  - [ ] Content rating questionnaire
- [ ] Upload APK as internal testing
  - [ ] Add testers (lab members)
  - [ ] Share testing link
- [ ] Promote to production
  - [ ] After 2 weeks of stable testing
  - [ ] Enable auto-updates

---

## Current Status

- **Phase**: Phase 0 (Planning complete)
- **Next Action**: Order hardware and set up Android Studio
- **Blockers**: None

## Notes

- This TODO is a living document - update as phases complete
- Mark items complete with `[x]` when done
- Add discovered tasks under appropriate phase
- Track blockers and questions here
