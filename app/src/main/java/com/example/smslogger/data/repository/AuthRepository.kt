package com.example.smslogger.data.repository

import android.util.Log
import com.example.smslogger.api.SmsApiClient
import com.example.smslogger.api.UserInfo
import com.example.smslogger.security.KeystoreCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication operations
 * Abstracts API calls and credential management for AuthViewModel
 *
 * Responsibilities:
 * - Authenticate users with server
 * - Manage credential storage via KeystoreCredentialManager
 * - Handle server error responses
 * - Provide clean API for ViewModel layer
 *
 * Dependencies: #46 (KeystoreCredentialManager), #49/#50 (API models)
 */
class AuthRepository(
    private val credentialManager: KeystoreCredentialManager,
    private val serverUrl: String
) {

    private val TAG = "AuthRepository"

    /**
     * Authenticate user with username and password
     * Optionally accepts TOTP code for 2FA authentication
     *
     * @param username User's username or email
     * @param password User's password
     * @param totpCode Optional TOTP code for 2FA (6-digit code)
     * @param rememberMe Whether to save password for future logins
     * @return LoginResult containing success status and optional error details
     */
    suspend fun login(
        username: String,
        password: String,
        totpCode: String? = null,
        rememberMe: Boolean = false
    ): LoginResult {
        return try {
            // Perform authentication on IO dispatcher
            val result = withContext(Dispatchers.IO) {
                val apiClient = SmsApiClient(serverUrl, username, password)
                val success = apiClient.testConnection()

                if (success) {
                    // Create mock user info (will be properly handled when API is updated in #49/#50)
                    val userInfo = UserInfo(
                        id = username,
                        username = username,
                        email = null,
                        createdAt = null
                    )

                    // Save credentials to secure storage
                    credentialManager.saveCredentials(
                        username = username,
                        password = if (rememberMe) password else null,
                        jwtToken = "legacy_token_${System.currentTimeMillis()}", // Placeholder
                        refreshToken = null,
                        expiresInSeconds = 3600, // 1 hour
                        userId = username,
                        email = null
                    )

                    Log.d(TAG, "Login successful for user: $username")
                    LoginResult.Success(userInfo)
                } else {
                    Log.w(TAG, "Login failed for user: $username")
                    LoginResult.Error(
                        message = "Invalid username or password",
                        code = "INVALID_CREDENTIALS",
                        httpCode = 401
                    )
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            LoginResult.Error(
                message = getErrorMessage(e),
                code = "NETWORK_ERROR",
                httpCode = null,
                exception = e
            )
        }
    }

    /**
     * Check if user is currently authenticated
     */
    fun isAuthenticated(): Boolean {
        return credentialManager.isAuthenticated()
    }

    /**
     * Get currently authenticated user ID
     */
    fun getCurrentUserId(): String? {
        return credentialManager.getUserId()
    }

    /**
     * Get currently authenticated username
     */
    fun getCurrentUsername(): String? {
        return credentialManager.getUsername()
    }

    /**
     * Logout user - clear all stored credentials
     */
    fun logout() {
        credentialManager.clearCredentials()
        Log.d(TAG, "User logged out")
    }

    /**
     * Convert exception to user-friendly error message
     */
    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("Unable to resolve host") == true ->
                "Cannot connect to server. Check your network connection."
            exception.message?.contains("timeout") == true ->
                "Connection timeout. Please try again."
            exception.message?.contains("Failed to connect") == true ->
                "Cannot reach server. Check server URL in settings."
            else ->
                "Network error. Please try again"
        }
    }

    /**
     * Sealed class representing authentication result
     */
    sealed class LoginResult {
        /**
         * Successful authentication
         * @param user User information from server
         */
        data class Success(val user: UserInfo) : LoginResult()

        /**
         * Authentication failed
         * @param message User-friendly error message
         * @param code Error code (e.g., "INVALID_CREDENTIALS", "ACCOUNT_LOCKED", "INVALID_TOTP")
         * @param httpCode HTTP status code from server (if applicable)
         * @param exception Original exception (if applicable)
         */
        data class Error(
            val message: String,
            val code: String,
            val httpCode: Int? = null,
            val exception: Exception? = null
        ) : LoginResult()
    }
}

