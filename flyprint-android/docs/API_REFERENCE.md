# flyPush Agent API Reference

This document describes the API endpoints used by the flyPrint Android agent to communicate with the flyPush server.

**Base URL**: `https://your-server.com/api/labels/agent`

**Authentication**: All requests require `X-API-Key` header with agent's API key.

---

## Authentication

All agent endpoints require API key authentication via HTTP header:

```
X-API-Key: <agent_api_key>
```

The API key is generated when creating a print agent in the flyPush web UI (Settings → Print Agents → Add Agent).

**Security Note**: API keys should be stored securely (Android EncryptedSharedPreferences).

---

## Endpoints

### 1. Send Heartbeat

**Endpoint**: `POST /api/labels/agent/heartbeat`

**Purpose**: Update agent's `last_seen` timestamp and printer status. Server uses this to determine if agent is online (< 60 seconds since last heartbeat).

**Request Body**:
```json
{
  "printer_name": "Brother_QL820",
  "printer_status": "ready"
}
```

**Fields**:
- `printer_name` (optional string): Current printer name
- `printer_status` (optional string): Printer status ("ready", "offline", "busy", "unknown")

**Response**: `200 OK` (empty body)

**Error Responses**:
- `401 Unauthorized` - Invalid API key
- `404 Not Found` - Agent not found (deleted from server)

**Android Implementation**:
```kotlin
@POST("/api/labels/agent/heartbeat")
suspend fun sendHeartbeat(@Body request: HeartbeatRequest): Response<Unit>

data class HeartbeatRequest(
    @SerializedName("printer_name") val printerName: String?,
    @SerializedName("printer_status") val printerStatus: String?
)
```

**Example**:
```kotlin
try {
    val response = api.sendHeartbeat(
        HeartbeatRequest(
            printerName = "Brother_QL820",
            printerStatus = "ready"
        )
    )
    if (response.isSuccessful) {
        Log.d(TAG, "Heartbeat sent successfully")
    }
} catch (e: Exception) {
    Log.e(TAG, "Heartbeat failed: ${e.message}")
}
```

---

### 2. Get Pending Jobs

**Endpoint**: `GET /api/labels/agent/jobs`

**Purpose**: Fetch list of pending print jobs for this agent's tenant. Returns jobs with `status = PENDING` ordered by creation time (oldest first).

**Request**: No body

**Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "stock_ids": ["id1", "id2", "id3"],
    "label_format": "dymo_11352",
    "copies": 2,
    "code_type": "qr",
    "status": "pending",
    "created_at": "2026-02-06T14:30:00Z"
  }
]
```

**Fields**:
- `id` (string): Job UUID
- `stock_ids` (array): Stock IDs to print (or `["__TEST__"]` for test jobs)
- `label_format` (string): Label format key ("dymo_11352", "dymo_99010", etc.)
- `copies` (integer): Number of copies per label
- `code_type` (string): "qr" or "barcode"
- `status` (string): Job status ("pending", "claimed", "printing", "completed", "failed")
- `created_at` (string): ISO 8601 timestamp

**Error Responses**:
- `401 Unauthorized` - Invalid API key

**Android Implementation**:
```kotlin
@GET("/api/labels/agent/jobs")
suspend fun getPendingJobs(): List<PrintJob>

data class PrintJob(
    val id: String,
    @SerializedName("stock_ids") val stockIds: List<String>,
    @SerializedName("label_format") val labelFormat: String,
    val copies: Int,
    @SerializedName("code_type") val codeType: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)
```

---

### 3. Claim Job

**Endpoint**: `POST /api/labels/agent/jobs/{job_id}/claim`

**Purpose**: Claim a pending job for this agent. Sets `status = CLAIMED` and `agent_id = <this_agent>`. Only pending jobs can be claimed.

**Path Parameters**:
- `job_id` (string): Job UUID

**Request**: No body

**Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "claimed",
  "agent_id": "agent-uuid",
  "claimed_at": "2026-02-06T14:30:05Z",
  ...
}
```

**Error Responses**:
- `401 Unauthorized` - Invalid API key
- `404 Not Found` - Job not found
- `409 Conflict` - Job already claimed by another agent

**Android Implementation**:
```kotlin
@POST("/api/labels/agent/jobs/{id}/claim")
suspend fun claimJob(@Path("id") jobId: String): PrintJob?
```

**Example**:
```kotlin
val job = api.claimJob(jobId)
if (job != null && job.status == "claimed") {
    // Proceed with processing
} else {
    // Job was claimed by another agent
    Log.w(TAG, "Failed to claim job $jobId")
}
```

---

### 4. Get Job Image

**Endpoint**: `GET /api/labels/agent/jobs/{job_id}/image`

**Purpose**: Download PNG image for the job. Returns a PNG file (54mm x 25mm, 300dpi) with all labels concatenated vertically.

**Path Parameters**:
- `job_id` (string): Job UUID

**Request**: No body

**Response**: `200 OK`
- Content-Type: `image/png`
- Body: PNG binary data

**Error Responses**:
- `401 Unauthorized` - Invalid API key
- `404 Not Found` - Job not found

**Android Implementation**:
```kotlin
@GET("/api/labels/agent/jobs/{id}/image")
suspend fun getJobImage(@Path("id") jobId: String): ResponseBody

// Usage:
val imageData = api.getJobImage(jobId).bytes()
```

**Alternative (PDF)**:

**Endpoint**: `GET /api/labels/agent/jobs/{job_id}/pdf`

Returns PDF instead of PNG. Same interface, different Content-Type (`application/pdf`).

**Note**: PNG is recommended for thermal printers (avoids CUPS scaling issues).

---

### 5. Start Printing

**Endpoint**: `POST /api/labels/agent/jobs/{job_id}/start`

**Purpose**: Mark job as currently printing. Sets `status = PRINTING`.

**Path Parameters**:
- `job_id` (string): Job UUID

**Request**: No body

**Response**: `200 OK` (job object with updated status)

**Error Responses**:
- `401 Unauthorized` - Invalid API key
- `404 Not Found` - Job not found or not claimed by this agent
- `400 Bad Request` - Job not in claimable state

**Android Implementation**:
```kotlin
@POST("/api/labels/agent/jobs/{id}/start")
suspend fun startPrinting(@Path("id") jobId: String): PrintJob
```

---

### 6. Complete Job

**Endpoint**: `POST /api/labels/agent/jobs/{job_id}/complete`

**Purpose**: Mark job as completed or failed. Sets `status = COMPLETED` or `FAILED`, and `completed_at = now()`.

**Path Parameters**:
- `job_id` (string): Job UUID

**Request Body**:
```json
{
  "success": true,
  "error_message": null
}
```

**Fields**:
- `success` (boolean): True if print succeeded, false if failed
- `error_message` (optional string): Error description if failed

**Response**: `200 OK` (job object with updated status)

**Error Responses**:
- `401 Unauthorized` - Invalid API key
- `404 Not Found` - Job not found or not claimed by this agent

**Android Implementation**:
```kotlin
@POST("/api/labels/agent/jobs/{id}/complete")
suspend fun completeJob(
    @Path("id") jobId: String,
    @Body result: JobResult
): PrintJob

data class JobResult(
    val success: Boolean,
    @SerializedName("error_message") val errorMessage: String? = null
)
```

**Example (Success)**:
```kotlin
api.completeJob(
    jobId,
    JobResult(success = true)
)
```

**Example (Failure)**:
```kotlin
api.completeJob(
    jobId,
    JobResult(
        success = false,
        errorMessage = "Printer out of paper"
    )
)
```

---

## Complete Job Processing Flow

```kotlin
suspend fun processJob(job: PrintJob) {
    // 1. Claim job
    val claimed = api.claimJob(job.id)
    if (claimed == null) {
        Log.w(TAG, "Failed to claim job ${job.id}")
        return
    }

    // 2. Download image
    val imageData = try {
        api.getJobImage(job.id).bytes()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to download image: ${e.message}")
        api.completeJob(job.id, JobResult(
            success = false,
            errorMessage = "Download failed: ${e.message}"
        ))
        return
    }

    // 3. Mark as printing
    api.startPrinting(job.id)

    // 4. Print
    try {
        printerManager.print(imageData, job.copies)

        // 5. Mark as completed
        api.completeJob(job.id, JobResult(success = true))

        // 6. Notify user
        showNotification(
            "Print completed",
            "${job.stockIds.size} labels printed"
        )

    } catch (e: PrinterException) {
        Log.e(TAG, "Print failed: ${e.message}")

        // Mark as failed
        api.completeJob(job.id, JobResult(
            success = false,
            errorMessage = e.message
        ))

        // Notify user
        showErrorNotification(
            "Print failed",
            e.message ?: "Unknown error"
        )
    }
}
```

---

## Error Handling

### Network Errors

**IOException** (network timeout, connection refused):
```kotlin
try {
    api.sendHeartbeat(...)
} catch (e: IOException) {
    // Log and retry on next poll
    Log.e(TAG, "Network error: ${e.message}")
    // Don't mark job as failed - might be temporary
}
```

**HttpException** (4xx, 5xx HTTP errors):
```kotlin
try {
    api.claimJob(jobId)
} catch (e: HttpException) {
    when (e.code()) {
        401 -> {
            // Invalid API key - stop service
            stopSelf()
            showNotification("Authentication failed", "Check API key")
        }
        404 -> {
            // Job not found - skip
            Log.w(TAG, "Job $jobId not found")
        }
        409 -> {
            // Job already claimed - skip
            Log.w(TAG, "Job $jobId already claimed")
        }
        else -> {
            // Server error - log and retry
            Log.e(TAG, "HTTP error ${e.code()}: ${e.message}")
        }
    }
}
```

### Retry Strategy

**Exponential Backoff** for network errors:
```kotlin
var backoffDelay = 5000L // Start with 5s
val maxDelay = 60000L // Max 60s

private fun scheduleNextPoll() {
    if (lastPollSucceeded) {
        // Reset backoff on success
        backoffDelay = 5000L
        handler.postDelayed(pollingRunnable, 5000L)
    } else {
        // Exponential backoff on failure
        handler.postDelayed(pollingRunnable, backoffDelay)
        backoffDelay = min(backoffDelay * 2, maxDelay)
    }
}
```

---

## Rate Limiting

The server does **not** implement rate limiting for agent endpoints, but agents should:

- Poll at **5-second intervals** (not faster)
- Send heartbeat on **every poll** (not more frequently)
- Implement **exponential backoff** on errors

---

## Server-Side Reference

The agent API endpoints are defined in:
- **File**: `/app/labels/router.py`
- **Lines**: 712-1007
- **Authentication**: `get_agent_from_api_key()` dependency

---

## Testing API Endpoints

### Using curl

```bash
# Set variables
API_KEY="your_api_key_here"
SERVER="https://your-server.com"

# Send heartbeat
curl -X POST "$SERVER/api/labels/agent/heartbeat" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"printer_name":"TestPrinter","printer_status":"ready"}'

# Get pending jobs
curl -X GET "$SERVER/api/labels/agent/jobs" \
  -H "X-API-Key: $API_KEY"

# Claim job
JOB_ID="550e8400-e29b-41d4-a716-446655440000"
curl -X POST "$SERVER/api/labels/agent/jobs/$JOB_ID/claim" \
  -H "X-API-Key: $API_KEY"

# Download image
curl -X GET "$SERVER/api/labels/agent/jobs/$JOB_ID/image" \
  -H "X-API-Key: $API_KEY" \
  -o label.png

# Complete job
curl -X POST "$SERVER/api/labels/agent/jobs/$JOB_ID/complete" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"success":true}'
```

### Using Android Logging

Enable OkHttp logging interceptor to see all API calls:

```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(config))
    .addInterceptor(loggingInterceptor) // Add this
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()
```

---

## References

- Server-side code: `/app/labels/router.py` (lines 712-1007)
- Print service: `/app/labels/print_service.py`
- Data models: `/app/db/models.py` (PrintJob, PrintAgent)
