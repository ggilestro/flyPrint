package ro.gilest.flyprint.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ro.gilest.flyprint.config.AppConfig

/**
 * Starts FlyPrintService on device boot if the service was previously enabled.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val config = AppConfig(context)
        if (config.isConfigured() && config.serviceEnabled) {
            Log.i(TAG, "Boot completed — starting FlyPrint service")
            FlyPrintService.start(context)
        } else {
            Log.d(TAG, "Boot completed — service not enabled or not configured")
        }
    }
}
