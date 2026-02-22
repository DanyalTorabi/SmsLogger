package com.example.smslogger.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential manager using Android Keystore
 * Stores sensitive authentication data encrypted at rest
 *
 * Stored Data:
 * - Username
 * - Password (optional, for remember me)
 * - JWT Access Token
 * - JWT Refresh Token
 * - Token Expiry Timestamp
 *
 * Thread-safe singleton implementation
 */
class KeystoreCredentialManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val encryptedPrefs: SharedPreferences

    companion object {
        private const val TAG = "KeystoreCredentialManager"
        private const val ENCRYPTED_PREFS_FILE = "sms_logger_secure_credentials"

        // Keys for encrypted storage
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"

        @Volatile
        private var INSTANCE: KeystoreCredentialManager? = null

        fun getInstance(context: Context): KeystoreCredentialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeystoreCredentialManager(context).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        // Initialize encrypted shared preferences with Android Keystore
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            Log.d(TAG, "Encrypted storage initialized successfully")

            // Migrate from old plaintext storage if needed
            migrateLegacyCredentials()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted storage", e)
            throw SecurityException("Failed to initialize secure credential storage", e)
        }
    }

    /**
     * Save login credentials securely
     * @param username User's username
     * @param password User's password (optional, can be null)
     * @param jwtToken JWT access token from server
     * @param refreshToken JWT refresh token (optional)
     * @param expiresInSeconds Token validity duration in seconds
     * @param userId User's unique identifier
     * @param email User's email address
     */
    fun saveCredentials(
        username: String,
        password: String? = null,
        jwtToken: String,
        refreshToken: String? = null,
        expiresInSeconds: Long,
        userId: String? = null,
        email: String? = null
    ) {
        try {
            val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)

            encryptedPrefs.edit().apply {
                putString(KEY_USERNAME, username)
                if (password != null) {
                    putString(KEY_PASSWORD, password)
                } else {
                    remove(KEY_PASSWORD)
                }
                putString(KEY_JWT_TOKEN, jwtToken)
                if (refreshToken != null) {
                    putString(KEY_REFRESH_TOKEN, refreshToken)
                }
                putLong(KEY_TOKEN_EXPIRY, expiryTime)
                if (userId != null) {
                    putString(KEY_USER_ID, userId)
                }
                if (email != null) {
                    putString(KEY_USER_EMAIL, email)
                }
                apply()
            }

            Log.d(TAG, "Credentials saved securely for user: $username")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save credentials", e)
            throw SecurityException("Failed to save credentials securely", e)
        }
    }

    /**
     * Get stored username
     */
    fun getUsername(): String? {
        return encryptedPrefs.getString(KEY_USERNAME, null)
    }

    /**
     * Get stored password (if saved)
     */
    fun getPassword(): String? {
        return encryptedPrefs.getString(KEY_PASSWORD, null)
    }

    /**
     * Get JWT access token
     */
    fun getToken(): String? {
        val token = encryptedPrefs.getString(KEY_JWT_TOKEN, null)

        // Check if token is expired
        if (token != null && isTokenExpired()) {
            Log.w(TAG, "Token is expired")
            return null
        }

        return token
    }

    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Get user ID
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Check if JWT token is expired
     */
    fun isTokenExpired(): Boolean {
        val expiryTime = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        if (expiryTime == 0L) {
            return true
        }
        return System.currentTimeMillis() >= expiryTime
    }

    /**
     * Get time remaining until token expires (in milliseconds)
     * Returns 0 if expired or not set
     */
    fun getTimeToTokenExpiry(): Long {
        val expiryTime = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        if (expiryTime == 0L) {
            return 0
        }
        val remaining = expiryTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Check if user is authenticated (has valid token)
     */
    fun isAuthenticated(): Boolean {
        val token = encryptedPrefs.getString(KEY_JWT_TOKEN, null)
        return token != null && !isTokenExpired()
    }

    /**
     * Check if credentials exist (regardless of token validity)
     */
    fun hasCredentials(): Boolean {
        return encryptedPrefs.getString(KEY_USERNAME, null) != null
    }

    /**
     * Clear all stored credentials (logout)
     */
    fun clearCredentials() {
        try {
            encryptedPrefs.edit().clear().apply()
            Log.d(TAG, "All credentials cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credentials", e)
        }
    }

    /**
     * Update just the JWT token (for token refresh scenarios)
     */
    fun updateToken(jwtToken: String, refreshToken: String? = null, expiresInSeconds: Long) {
        try {
            val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)

            encryptedPrefs.edit().apply {
                putString(KEY_JWT_TOKEN, jwtToken)
                if (refreshToken != null) {
                    putString(KEY_REFRESH_TOKEN, refreshToken)
                }
                putLong(KEY_TOKEN_EXPIRY, expiryTime)
                apply()
            }

            Log.d(TAG, "Token updated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update token", e)
            throw SecurityException("Failed to update token", e)
        }
    }

    /**
     * Migrate credentials from legacy SharedPreferences (plaintext) to encrypted storage
     * This is a one-time migration for existing users
     */
    private fun migrateLegacyCredentials() {
        try {
            // Check if migration already done
            if (hasCredentials()) {
                Log.d(TAG, "Credentials already exist in secure storage, skipping migration")
                return
            }

            // Check for legacy credentials
            val legacyPrefs = appContext.getSharedPreferences("sms_logger_config", Context.MODE_PRIVATE)
            val legacyUsername = legacyPrefs.getString("username", null)
            val legacyPassword = legacyPrefs.getString("password", null)

            if (!legacyUsername.isNullOrBlank() && !legacyPassword.isNullOrBlank()) {
                Log.d(TAG, "Migrating legacy credentials to secure storage")

                // Save to encrypted storage (without token since we need to re-authenticate)
                encryptedPrefs.edit().apply {
                    putString(KEY_USERNAME, legacyUsername)
                    putString(KEY_PASSWORD, legacyPassword)
                    apply()
                }

                // Clear legacy credentials
                legacyPrefs.edit().apply {
                    remove("username")
                    remove("password")
                    apply()
                }

                Log.d(TAG, "Legacy credentials migrated successfully")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate legacy credentials", e)
            // Don't throw - migration failure shouldn't break app
        }
    }

    /**
     * Get credential summary for debugging (no sensitive data)
     */
    fun getCredentialSummary(): String {
        return """
            Credentials Status:
            - Has Username: ${getUsername() != null}
            - Has Password: ${getPassword() != null}
            - Has Token: ${encryptedPrefs.getString(KEY_JWT_TOKEN, null) != null}
            - Token Expired: ${isTokenExpired()}
            - Time to Expiry: ${getTimeToTokenExpiry() / 1000}s
            - Is Authenticated: ${isAuthenticated()}
        """.trimIndent()
    }
}

