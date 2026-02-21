package ro.gilest.flyprint.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persistent configuration backed by SharedPreferences.
 *
 * Sensitive data (API key) is stored in EncryptedSharedPreferences.
 * Everything else lives in regular SharedPreferences.
 */
class AppConfig(context: Context) {

    companion object {
        private const val TAG = "AppConfig"
        private const val PREFS_NAME = "flyprint_prefs"
        private const val ENCRYPTED_PREFS_NAME = "flyprint_secure"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val encryptedPrefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular prefs if encryption fails (e.g. emulator without KeyStore)
        Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back", e)
        context.getSharedPreferences("${ENCRYPTED_PREFS_NAME}_fallback", Context.MODE_PRIVATE)
    }

    // --- Core settings (user-configured) ---

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = prefs.edit().putString("server_url", normalizeUrl(value)).apply()

    var apiKey: String
        get() = encryptedPrefs.getString("api_key", "") ?: ""
        set(value) = encryptedPrefs.edit().putString("api_key", value).apply()

    var printerName: String
        get() = prefs.getString("printer_name", "") ?: ""
        set(value) = prefs.edit().putString("printer_name", value).apply()

    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) = prefs.edit().putString("printer_address", value).apply()

    // --- Operational settings (synced from server) ---

    var pollInterval: Long
        get() = prefs.getLong("poll_interval", 5000L)
        set(value) = prefs.edit().putLong("poll_interval", value).apply()

    var configVersion: Int
        get() = prefs.getInt("config_version", 0)
        set(value) = prefs.edit().putInt("config_version", value).apply()

    // --- Service state ---

    var serviceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", false)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()

    var jobsProcessed: Int
        get() = prefs.getInt("jobs_processed", 0)
        set(value) = prefs.edit().putInt("jobs_processed", value).apply()

    var lastHeartbeat: Long
        get() = prefs.getLong("last_heartbeat", 0L)
        set(value) = prefs.edit().putLong("last_heartbeat", value).apply()

    // --- Helpers ---

    fun isConfigured(): Boolean =
        serverUrl.isNotBlank() && apiKey.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
        encryptedPrefs.edit().clear().apply()
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim().trimEnd('/')
        if (normalized.startsWith("http://")) {
            normalized = "https://" + normalized.removePrefix("http://")
        } else if (!normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized
    }
}
