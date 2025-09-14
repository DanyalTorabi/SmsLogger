package com.example.smslogger.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Configuration manager for SMS Logger service
 * Handles server settings, credentials, and sync preferences
 * Uses EncryptedSharedPreferences for secure storage of sensitive data
 */
@Suppress("unused") // Configuration class for future integration
class SmsLoggerConfig private constructor(context: Context) {

    private val TAG = "SmsLoggerConfig"

    // Regular SharedPreferences for non-sensitive data
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Encrypted SharedPreferences for sensitive data (username, password, API keys)
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            // Fallback to regular SharedPreferences if encryption fails
            // This should only happen in extreme cases (device issues, etc.)
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    init {
        // Migrate existing plain text credentials to encrypted storage
        migrateCredentialsToEncryptedStorage()
    }

    // Server Configuration
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value) }

    // Sensitive data - stored in encrypted preferences
    var username: String
        get() = encryptedPrefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = encryptedPrefs.edit { putString(KEY_USERNAME, value) }

    var password: String
        get() = encryptedPrefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = encryptedPrefs.edit { putString(KEY_PASSWORD, value) }

    var apiKey: String
        get() = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = encryptedPrefs.edit { putString(KEY_API_KEY, value) }

    // Non-sensitive sync configuration - regular SharedPreferences
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

    /**
     * Migrate existing plain text credentials to encrypted storage
     * This ensures backward compatibility for existing users
     */
    private fun migrateCredentialsToEncryptedStorage() {
        try {
            // Check if migration is needed
            val migrationCompleted = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
            if (migrationCompleted) {
                return
            }

            Log.d(TAG, "Starting migration of credentials to encrypted storage")

            // Migrate username if it exists in plain text
            val plainUsername = prefs.getString(KEY_USERNAME, null)
            if (!plainUsername.isNullOrEmpty() && username.isEmpty()) {
                Log.d(TAG, "Migrating username to encrypted storage")
                username = plainUsername
                // Remove from plain text storage
                prefs.edit { remove(KEY_USERNAME) }
            }

            // Migrate password if it exists in plain text
            val plainPassword = prefs.getString(KEY_PASSWORD, null)
            if (!plainPassword.isNullOrEmpty() && password.isEmpty()) {
                Log.d(TAG, "Migrating password to encrypted storage")
                password = plainPassword
                // Remove from plain text storage
                prefs.edit { remove(KEY_PASSWORD) }
            }

            // Migrate API key if it exists in plain text
            val plainApiKey = prefs.getString(KEY_API_KEY, null)
            if (!plainApiKey.isNullOrEmpty() && apiKey.isEmpty()) {
                Log.d(TAG, "Migrating API key to encrypted storage")
                apiKey = plainApiKey
                // Remove from plain text storage
                prefs.edit { remove(KEY_API_KEY) }
            }

            // Mark migration as completed
            prefs.edit { putBoolean(KEY_MIGRATION_COMPLETED, true) }
            Log.d(TAG, "Migration to encrypted storage completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate credentials to encrypted storage", e)
        }
    }

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
            API Key: ${if (apiKey.isNotBlank()) "[SET]" else "[NOT SET]"}
            Sync Enabled: $syncEnabled
            Batch Size: $batchSize
            Sync Interval: ${syncInterval}ms
            Max Retries: $maxRetryAttempts
        """.trimIndent()
    }

    /**
     * Clear all sensitive data from encrypted storage
     * Useful for logout or security reset functionality
     */
    @Suppress("unused")
    fun clearSensitiveData() {
        try {
            encryptedPrefs.edit {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
                remove(KEY_API_KEY)
            }
            Log.d(TAG, "Sensitive data cleared from encrypted storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear sensitive data", e)
        }
    }

    companion object {
        private const val PREFS_NAME = "sms_logger_config"
        private const val ENCRYPTED_PREFS_NAME = "sms_logger_config_encrypted"

        // Keys for sensitive data (stored in encrypted preferences)
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_API_KEY = "api_key"

        // Keys for non-sensitive data (stored in regular preferences)
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_BATCH_SIZE = "batch_size"
        private const val KEY_MAX_RETRY_ATTEMPTS = "max_retry_attempts"
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"

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
