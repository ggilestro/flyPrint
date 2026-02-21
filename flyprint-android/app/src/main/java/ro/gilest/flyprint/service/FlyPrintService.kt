package ro.gilest.flyprint.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ro.gilest.flyprint.R
import ro.gilest.flyprint.api.ApiClient
import ro.gilest.flyprint.api.FlyPrintApi
import ro.gilest.flyprint.api.HeartbeatRequest
import ro.gilest.flyprint.api.JobResult
import ro.gilest.flyprint.api.PrintJob
import ro.gilest.flyprint.config.AppConfig
import ro.gilest.flyprint.printer.BrotherPrinterManager
import ro.gilest.flyprint.printer.PrinterException
import ro.gilest.flyprint.printer.PrinterManager
import ro.gilest.flyprint.ui.MainActivity

/**
 * Foreground service that polls the FlyPush server for print jobs.
 *
 * Lifecycle:
 *   onCreate → startForeground(notification) → polling loop → onDestroy
 *
 * The polling loop mirrors Python agent.py:
 *   sendHeartbeat() → getPendingJobs() → for each: claim → download → print → complete
 */
class FlyPrintService : Service() {

    companion object {
        private const val TAG = "FlyPrintService"
        const val CHANNEL_ID = "flyprint_service"
        const val NOTIFICATION_ID = 1
        private const val JOB_NOTIFICATION_ID = 2

        /** Intent extras. */
        const val ACTION_STOP = "ro.gilest.flyprint.STOP"

        fun start(context: Context) {
            val intent = Intent(context, FlyPrintService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FlyPrintService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var config: AppConfig
    private lateinit var api: FlyPrintApi
    private lateinit var printerManager: PrinterManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    // Exponential backoff state
    private var backoffDelay = 5000L
    private val maxBackoffDelay = 60000L

    // --- Polling runnable ---

    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return
            serviceScope.launch {
                val success = pollOnce()
                // Reason: Reset backoff on success, double on failure (capped at 60s)
                if (success) {
                    backoffDelay = config.pollInterval
                    handler.postDelayed(this@pollingRunnable, config.pollInterval)
                } else {
                    handler.postDelayed(this@pollingRunnable, backoffDelay)
                    backoffDelay = (backoffDelay * 2).coerceAtMost(maxBackoffDelay)
                }
            }
        }
    }

    // --- Service lifecycle ---

    override fun onCreate() {
        super.onCreate()
        config = AppConfig(this)
        api = ApiClient.create(config)
        printerManager = BrotherPrinterManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!config.isConfigured()) {
            Log.e(TAG, "Not configured — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        startPolling()
        config.serviceEnabled = true
        Log.i(TAG, "Service started — server=${config.serverUrl}, printer=${config.printerName}")

        return START_STICKY
    }

    override fun onDestroy() {
        stopPolling()
        config.serviceEnabled = false
        serviceScope.cancel()
        printerManager.disconnect()
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Polling ---

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        backoffDelay = config.pollInterval
        handler.post(pollingRunnable)
        Log.i(TAG, "Polling started (interval=${config.pollInterval}ms)")
    }

    private fun stopPolling() {
        isPolling = false
        handler.removeCallbacks(pollingRunnable)
        Log.i(TAG, "Polling stopped")
    }

    /**
     * Execute one poll cycle: heartbeat → fetch jobs → process each.
     *
     * Returns true if the cycle completed without network errors.
     */
    private suspend fun pollOnce(): Boolean {
        return try {
            // 1. Heartbeat
            val heartbeatResp = api.sendHeartbeat(
                HeartbeatRequest(
                    printerName = config.printerName.ifBlank { null },
                    printerStatus = printerManager.getStatus()
                )
            )

            if (heartbeatResp.isSuccessful) {
                config.lastHeartbeat = System.currentTimeMillis()
                val body = heartbeatResp.body()

                // Check config version change
                val serverVersion = body?.configVersion
                if (serverVersion != null && serverVersion != config.configVersion) {
                    Log.i(TAG, "Config version changed: ${config.configVersion} → $serverVersion")
                    config.configVersion = serverVersion
                }
            } else if (heartbeatResp.code() == 401) {
                Log.e(TAG, "Authentication failed (401) — stopping service")
                updateNotification("Authentication failed — check API key")
                stopSelf()
                return false
            }

            // 2. Fetch pending jobs
            val jobs = api.getPendingJobs()
            if (jobs.isNotEmpty()) {
                updateNotification("Processing ${jobs.size} job(s)…")
                for (job in jobs) {
                    processJob(job)
                }
                updateNotification("Polling for print jobs…")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Poll error: ${e.message}", e)
            updateNotification("Connection error — retrying…")
            false
        }
    }

    // --- Job processing (mirrors Python agent.process_job) ---

    private suspend fun processJob(job: PrintJob) {
        val jobId = job.id
        val shortId = jobId.take(8)
        Log.i(TAG, "Processing job $shortId (${job.stockIds.size} labels, ${job.copies} copies)")

        // 1. Claim
        val claimResp = api.claimJob(jobId)
        if (!claimResp.isSuccessful) {
            Log.w(TAG, "Failed to claim job $shortId: ${claimResp.code()}")
            return
        }

        // 2. Download image (PNG for thermal printers — not PDF)
        val imageData: ByteArray
        try {
            imageData = api.getJobImage(jobId).bytes()
            Log.d(TAG, "Downloaded ${imageData.size} bytes for job $shortId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image for job $shortId: ${e.message}")
            reportJobComplete(jobId, success = false, error = "Download failed: ${e.message}")
            return
        }

        // 3. Mark as printing
        try {
            api.startPrinting(jobId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark job $shortId as printing: ${e.message}")
            // Non-fatal — continue with print attempt
        }

        // 4. Print
        try {
            printerManager.print(imageData, job.copies)
            reportJobComplete(jobId, success = true)
            config.jobsProcessed += 1
            Log.i(TAG, "Job $shortId printed successfully")
            showJobNotification("Print completed", "${job.stockIds.size} labels printed")
        } catch (e: PrinterException) {
            Log.e(TAG, "Print error for job $shortId: ${e.message}")
            reportJobComplete(jobId, success = false, error = e.message)
            showJobNotification("Print failed", e.message ?: "Unknown error")
        }
    }

    private suspend fun reportJobComplete(jobId: String, success: Boolean, error: String? = null) {
        try {
            api.completeJob(jobId, JobResult(success = success, errorMessage = error))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report job completion: ${e.message}")
        }
    }

    // --- Notifications ---

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FlyPrintService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_printer)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun showJobNotification(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_printer)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(JOB_NOTIFICATION_ID, notification)
    }
}
