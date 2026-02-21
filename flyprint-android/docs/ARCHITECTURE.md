# flyPrint Android - Architecture

## Overview

flyPrint Android is a native Kotlin app that runs on Android tablets to poll the flyPush server for print jobs and print labels via Bluetooth or WiFi thermal printers. It's a rewrite of the Python/CUPS flyPrint agent specifically for Android.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Tablet                          │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │                  Web Browser (Chrome)                 │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │         flyPush PWA (Web UI)                    │ │ │
│  │  │  - Stock management                             │ │ │
│  │  │  - Barcode scanning (keyboard wedge)            │ │ │
│  │  │  - Create print jobs                            │ │ │
│  │  │  - View agent status                            │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                          │                            │ │
│  │                          │ HTTPS API                  │ │
│  │                          ↓                            │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │            flyPrint Android App (Native)              │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │           FlyPrintService (Foreground)          │ │ │
│  │  │                                                 │ │ │
│  │  │  while (running) {                              │ │ │
│  │  │    sendHeartbeat()                              │ │ │
│  │  │    jobs = getPendingJobs()                      │ │ │
│  │  │    jobs.forEach { processJob(it) }             │ │ │
│  │  │    sleep(5000)                                  │ │ │
│  │  │  }                                              │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                          │                            │ │
│  │                          ├─── Retrofit API Client     │ │
│  │                          │                            │ │
│  │                          ├─── PrinterManager         │ │
│  │                          │    (Zebra/Brother SDK)    │ │
│  │                          │                            │ │
│  │                          └─── AppConfig              │ │
│  │                               (SharedPreferences)    │ │
│  └───────────────────────────────────────────────────────┘ │
│                          │                                  │
│                          │ Bluetooth / WiFi                 │
│                          ↓                                  │
└─────────────────────────────────────────────────────────────┘
                           │
                           ↓
        ┌──────────────────────────────────────┐
        │     Thermal Label Printer            │
        │  (Brother QL-820NWB or Zebra ZD421)  │
        └──────────────────────────────────────┘
```

## Component Breakdown

### 1. MainActivity (UI Layer)

**Purpose**: Configuration and status display

**Responsibilities:**
- First-run detection (show setup wizard if not configured)
- Display agent status (online/offline, last heartbeat)
- Show recent job history
- Settings and logs viewer
- Start/stop foreground service

**Technology**: Jetpack Compose (declarative UI)

**Key Composables:**
- `SetupWizard` - Step-by-step configuration flow
- `StatusScreen` - Main screen showing agent status
- `JobHistoryList` - Recent jobs display
- `LogsViewer` - In-app log display

### 2. FlyPrintService (Background Service)

**Purpose**: Background polling and job processing

**Type**: Foreground Service (required for continuous background operation on Android 8+)

**Key Characteristics:**
- Shows persistent notification (Android requirement)
- Returns `START_STICKY` (auto-restarts if killed by OS)
- Uses `Handler.postDelayed()` for 5-second polling interval
- Runs until explicitly stopped or app uninstalled

**Lifecycle:**
```
onCreate() → startForeground(notification) → onStartCommand() → pollForJobs() [every 5s] → onDestroy()
```

**Polling Loop:**
```kotlin
private val pollingRunnable = object : Runnable {
    override fun run() {
        lifecycleScope.launch {
            pollForJobs()
        }
        handler.postDelayed(this, pollingInterval)
    }
}

private suspend fun pollForJobs() {
    try {
        // 1. Send heartbeat
        api.sendHeartbeat(HeartbeatRequest(printerName, printerStatus))

        // 2. Get pending jobs
        val jobs = api.getPendingJobs()

        // 3. Process each job
        jobs.forEach { job -> processJob(job) }
    } catch (e: Exception) {
        Log.e(TAG, "Polling error", e)
        // Don't crash - retry on next poll
    }
}
```

**Job Processing:**
```kotlin
private suspend fun processJob(job: PrintJob) {
    // 1. Claim job
    val claimed = api.claimJob(job.id)
    if (claimed == null) return

    // 2. Download image
    val imageData = api.getJobImage(job.id)
    if (imageData == null) {
        api.completeJob(job.id, success = false, error = "Download failed")
        return
    }

    // 3. Mark as printing
    api.startPrinting(job.id)

    // 4. Print
    try {
        printerManager.print(imageData, job.copies)
        api.completeJob(job.id, success = true)
        showNotification("Print completed", job.stockIds.size)
    } catch (e: PrinterException) {
        api.completeJob(job.id, success = false, error = e.message)
        showErrorNotification("Print failed", e.message)
    }
}
```

### 3. API Layer (Retrofit)

**Purpose**: Communication with flyPush server

**Key Files:**
- `FlyPushApi.kt` - Retrofit interface defining endpoints
- `ApiModels.kt` - Data classes (PrintJob, HeartbeatRequest, JobResult)
- `AuthInterceptor.kt` - Adds X-API-Key header to all requests
- `ApiClient.kt` - Retrofit instance factory

**API Endpoints (from `/app/labels/router.py`):**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/labels/agent/heartbeat` | Update last_seen timestamp |
| GET | `/api/labels/agent/jobs` | Fetch pending jobs for this agent |
| POST | `/api/labels/agent/jobs/{id}/claim` | Claim a job |
| GET | `/api/labels/agent/jobs/{id}/image` | Download PNG/PDF for job |
| POST | `/api/labels/agent/jobs/{id}/start` | Mark job as printing |
| POST | `/api/labels/agent/jobs/{id}/complete` | Mark job as completed/failed |

**Authentication:**
```kotlin
class AuthInterceptor(private val config: AppConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", config.apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
```

### 4. Printer Layer (SDK Abstraction)

**Purpose**: Abstract printer SDK differences between brands

**Interface:**
```kotlin
interface PrinterManager {
    fun discoverPrinters(): List<PrinterInfo>
    fun connect(printer: PrinterInfo): Boolean
    fun getStatus(): String
    fun print(data: ByteArray, copies: Int): Boolean
    fun disconnect()
}
```

**Implementations:**

#### ZebraPrinterManager (Zebra Link-OS SDK)
```kotlin
class ZebraPrinterManager : PrinterManager {
    private var connection: Connection? = null
    private var printer: ZebraPrinter? = null

    override fun discoverPrinters(): List<PrinterInfo> {
        val discoverer = BluetoothDiscoverer()
        return discoverer.getAvailablePrinters().map {
            PrinterInfo(it.name, it.macAddress, "Bluetooth")
        }
    }

    override fun print(data: ByteArray, copies: Int): Boolean {
        val tempFile = File(cacheDir, "label.pdf")
        tempFile.writeBytes(data)
        repeat(copies) {
            printer?.sendFileContents(tempFile.absolutePath)
        }
        tempFile.delete()
        return true
    }
}
```

#### BrotherPrinterManager (Brother Mobile SDK)
```kotlin
class BrotherPrinterManager : PrinterManager {
    private val printer = Printer()

    override fun print(data: ByteArray, copies: Int): Boolean {
        val tempFile = File(cacheDir, "label.pdf")
        tempFile.writeBytes(data)

        val settings = PrinterSettings(
            printerModel = QL_820NWB,
            port = PORT_BLUETOOTH,
            paperSize = BROTHER_QL_54x25MM
        )
        printer.setPrinterSettings(settings)
        printer.printPdfFile(tempFile.path, copies)

        tempFile.delete()
        return true
    }
}
```

### 5. Configuration Layer

**Purpose**: Persistent storage of app settings

**Implementation**: SharedPreferences with encryption for sensitive data

**Stored Data:**
- `server_url: String` - flyPush server URL (e.g., `https://fly.example.com`)
- `api_key: String` - Agent API key (encrypted)
- `printer_name: String` - Selected printer name
- `printer_address: String` - Bluetooth MAC or WiFi IP
- `printer_type: String` - "zebra" or "brother"

**AppConfig.kt:**
```kotlin
class AppConfig(context: Context) {
    private val prefs = context.getSharedPreferences("flyprint", Context.MODE_PRIVATE)
    private val encryptedPrefs = EncryptedSharedPreferences.create(...)

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = prefs.edit().putString("server_url", value).apply()

    var apiKey: String
        get() = encryptedPrefs.getString("api_key", "") ?: ""
        set(value) = encryptedPrefs.edit().putString("api_key", value).apply()

    fun isConfigured(): Boolean {
        return serverUrl.isNotEmpty() && apiKey.isNotEmpty()
    }
}
```

## Data Flow

### Print Job Lifecycle

```
[Web UI] User clicks "Print Labels"
   │
   ├─ POST /api/labels/print
   │  body: { stock_ids: ["id1", "id2"], copies: 1 }
   │
   ↓
[Server] Create PrintJob (status: PENDING)
   │
   ↓
[Android] Polls /api/labels/agent/jobs every 5s
   │
   ├─ Returns: [{ id: "job1", stock_ids: [...], ... }]
   │
   ↓
[Android] POST /api/labels/agent/jobs/job1/claim
   │
   ├─ Server updates: status = CLAIMED, agent_id = android_agent
   │
   ↓
[Android] GET /api/labels/agent/jobs/job1/image
   │
   ├─ Server generates PNG (54mm x 25mm, 300dpi)
   │
   ├─ Returns: PNG binary data
   │
   ↓
[Android] POST /api/labels/agent/jobs/job1/start
   │
   ├─ Server updates: status = PRINTING
   │
   ↓
[Android] printerManager.print(imageData, copies)
   │
   ├─ Send to Bluetooth printer via SDK
   │
   ↓
[Printer] Prints label
   │
   ↓
[Android] POST /api/labels/agent/jobs/job1/complete
   │  body: { success: true }
   │
   ↓
[Server] Updates: status = COMPLETED, completed_at = now()
   │
   ↓
[Web UI] Refreshes job status (shows green checkmark)
```

## Background Execution Strategy

### Android Background Restrictions

Android has increasingly strict background execution limits:

- **Android 8.0+**: Background services killed after a few minutes unless foreground
- **Android 12+**: Doze mode aggressively suspends apps
- **Manufacturer optimizations**: Samsung, Xiaomi add additional restrictions

### Our Strategy: Foreground Service

**Why Foreground Service:**
- Bypasses most background restrictions
- Can run indefinitely as long as notification is shown
- Survives Doze mode
- User-visible (transparency about background activity)

**Implementation:**
```kotlin
class FlyPrintService : Service() {
    override fun onCreate() {
        super.onCreate()

        // Create notification channel (required on Android 8+)
        createNotificationChannel()

        // Start as foreground service
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlyPrint Agent")
            .setContentText("Polling for print jobs...")
            .setSmallIcon(R.drawable.ic_printer)
            .setOngoing(true) // Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
```

**Trade-offs:**
- ✅ Reliable background execution
- ✅ Survives Doze mode
- ⚠️ Uses more battery than WorkManager
- ⚠️ Requires persistent notification (user-visible)

### Alternative: WorkManager (Not Recommended)

WorkManager has a **15-minute minimum interval** for periodic work, which is too slow for real-time printing (current system uses 5-second polling).

**When WorkManager makes sense:**
- If server implements Firebase Cloud Messaging (FCM) for push notifications
- For periodic status checks (not job polling)
- For background sync of failed jobs

## Security Considerations

### API Key Storage
- Stored in `EncryptedSharedPreferences` (Android Jetpack Security)
- Never logged or exposed in UI
- Transmitted via HTTPS only (TLS 1.2+)

### Network Security
- Enforce HTTPS for all API calls
- Certificate pinning (optional, for production)
- Timeout configuration (10s connect, 30s read)

### Permissions
Required permissions:
- `INTERNET` - API communication
- `BLUETOOTH` / `BLUETOOTH_ADMIN` - Printer discovery (Android < 12)
- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` - Printer discovery (Android 12+)
- `ACCESS_FINE_LOCATION` - Required for Bluetooth discovery on Android
- `FOREGROUND_SERVICE` - Background service
- `POST_NOTIFICATIONS` - Show notifications (Android 13+)

Runtime permissions (must request):
- Bluetooth
- Location (for Bluetooth discovery)
- Notifications

## Performance Characteristics

### Battery Usage
- **Foreground service**: ~5-10% battery per 8-hour shift (estimated)
- **Network polling**: ~1% per hour (5-second interval)
- **Idle optimization**: Reduce polling to 30s when no jobs for 5+ minutes

### Network Bandwidth
- **Heartbeat**: ~100 bytes every 5s = 1.7 KB/minute
- **Job list**: ~500 bytes every 5s (when jobs exist)
- **Image download**: ~50 KB per label (PNG, 300dpi)
- **Total**: ~2-3 MB per 8-hour shift (assuming 100 labels printed)

### Memory Usage
- **Base app**: ~30 MB (Kotlin runtime + libraries)
- **Printer SDK**: ~10-20 MB (Zebra/Brother SDK)
- **Image cache**: ~10 MB (temporary PNG storage)
- **Total**: ~50-60 MB

## Error Handling

### Network Errors
```kotlin
try {
    api.sendHeartbeat(...)
} catch (e: IOException) {
    // Network error - log and retry next poll
    Log.e(TAG, "Network error: ${e.message}")
    // Don't crash - polling will retry in 5s
} catch (e: HttpException) {
    // Server error (4xx, 5xx)
    if (e.code() == 401) {
        // Invalid API key - stop service and notify user
        showNotification("Authentication failed", "Check API key")
        stopSelf()
    }
}
```

### Printer Errors
```kotlin
try {
    printerManager.print(data, copies)
} catch (e: PrinterConnectionException) {
    // Printer disconnected - try reconnect
    printerManager.disconnect()
    printerManager.connect(currentPrinter)
} catch (e: PrinterOutOfPaperException) {
    // Notify user
    showNotification("Printer error", "Out of labels")
    api.completeJob(jobId, success = false, error = "Out of labels")
}
```

### Android System Errors
```kotlin
// Service killed by system - restart
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Restore state from SharedPreferences
    // Restart polling
    return START_STICKY // System will restart service
}

// Low memory - reduce cache
override fun onLowMemory() {
    // Clear image cache
    cacheDir.listFiles()?.forEach { it.delete() }
}
```

## Testing Strategy

### Unit Tests
- `AppConfig` - SharedPreferences logic
- API models - JSON serialization
- PrinterManager interface - Mock implementations

### Integration Tests
- Retrofit API calls - Mock server responses
- Printer SDK integration - Requires physical printer or emulator

### End-to-End Tests
- Full job flow: create → claim → download → print → complete
- Requires: running flyPush server + physical printer

### Manual Testing Checklist
- [ ] Bluetooth printer discovery
- [ ] WiFi printer discovery
- [ ] Job polling and processing
- [ ] Network interruption handling
- [ ] Printer error handling
- [ ] App kill and restart
- [ ] Battery optimization impact
- [ ] Multiple jobs queued

## Future Enhancements

### Firebase Cloud Messaging (FCM)
Replace polling with push notifications:
- Server sends FCM message when job created
- Android wakes service instantly
- Process job immediately
- Benefits: Lower battery usage, instant processing

### Multi-Printer Support
- Store array of printer configs
- Route jobs based on label format or load balancing
- UI to manage multiple printers

### Offline Queue
- Store jobs locally when network unavailable
- Sync when connection restored
- SQLite database for job storage

## References

- [Android Foreground Services](https://developer.android.com/develop/background-work/services/foreground-services)
- [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Brother Print SDK Documentation](https://support.brother.com/g/s/es/dev/en/mobilesdk/android/index.html)
- [Zebra Link-OS SDK Documentation](https://techdocs.zebra.com/link-os/)
