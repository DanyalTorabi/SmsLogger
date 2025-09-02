package com.example.smslogger.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Configuration manager for SMS Logger service
 * Handles server settings, credentials, and sync preferences
 */
@Suppress("unused") // Configuration class for future integration
class SmsLoggerConfig private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Server Configuration
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value) }

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PASSWORD, value) }

    // Sync Configuration
    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SYNC_ENABLED, value) }

    var syncInterval: Long
        get() = prefs.getLong(KEY_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
        set(value) = prefs.edit { putLong(KEY_SYNC_INTERVAL, value) }

    var batchSize: Int
        get() = prefs.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
        set(value) = prefs.edit { putInt(KEY_BATCH_SIZE, value) }

    var maxRetryAttempts: Int
        get() = prefs.getInt(KEY_MAX_RETRY_ATTEMPTS, DEFAULT_MAX_RETRY_ATTEMPTS)
        set(value) = prefs.edit { putInt(KEY_MAX_RETRY_ATTEMPTS, value) }

    // Validation
    @Suppress("unused")
    fun isConfigurationValid(): Boolean {
        return serverUrl.isNotBlank() &&
               username.isNotBlank() &&
               password.isNotBlank() &&
               serverUrl.startsWith("http")
    }

    @Suppress("unused")
    fun getConfigSummary(): String {
        return """
            Server: $serverUrl
            Username: ${if (username.isNotBlank()) "[SET]" else "[NOT SET]"}
            Password: ${if (password.isNotBlank()) "[SET]" else "[NOT SET]"}
            Sync Enabled: $syncEnabled
            Batch Size: $batchSize
            Sync Interval: ${syncInterval}ms
            Max Retries: $maxRetryAttempts
        """.trimIndent()
    }

    companion object {
        private const val PREFS_NAME = "sms_logger_config"

        // Keys
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_BATCH_SIZE = "batch_size"
        private const val KEY_MAX_RETRY_ATTEMPTS = "max_retry_attempts"

        // Defaults
        private const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080"
        private const val DEFAULT_SYNC_INTERVAL = 30000L // 30 seconds
        private const val DEFAULT_BATCH_SIZE = 10
        private const val DEFAULT_MAX_RETRY_ATTEMPTS = 5

        @Volatile
        private var INSTANCE: SmsLoggerConfig? = null

        @Suppress("unused")
        fun getInstance(context: Context): SmsLoggerConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmsLoggerConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
