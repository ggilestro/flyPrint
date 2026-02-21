package ro.gilest.flyprint

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import ro.gilest.flyprint.service.FlyPrintService

/**
 * Application class — creates the notification channel on app startup.
 */
class FlyPrintApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FlyPrintService.CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
