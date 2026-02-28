package com.example.smslogger.data.repository

import android.content.Context
import android.util.Log
import com.example.smslogger.api.ApiError
import com.example.smslogger.api.SmsApiClient
import com.example.smslogger.api.UserInfo
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Repository for authentication operations.
 *
 * Updated for #49/#50: calls the real POST /api/auth/login endpoint via
 * [SmsApiClient.login], parses the full [AuthResponse] (including [expires_at]
 * and [user] fields), and persists a real JWT token to [KeystoreCredentialManager].
 *
 * Dependencies: #46 (KeystoreCredentialManager), #48 (SessionManager),
 *               #49 (JWT injection), #50 (API models)
 */
class AuthRepository(
    private val credentialManager: KeystoreCredentialManager,
    private val serverUrl: String,
    context: Context? = null
) {
    private val TAG = "AuthRepository"
    private val sessionManager: SessionManager? =
        context?.let { SessionManager.getInstance(it.applicationContext) }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Authenticate user with username and password.
     * Optionally accepts a TOTP code for 2FA authentication (#50).
     *
     * Calls POST /api/auth/login, parses [AuthResponse], and stores the real JWT
     * (with its expiry) in [KeystoreCredentialManager] (#49).
     *
     * @param username User's username or email
     * @param password User's password
     * @param totpCode Optional 6-digit TOTP code for 2FA
     * @param rememberMe Whether to persist the password for future logins
     * @return [LoginResult] containing success status and optional error details
     */
    suspend fun login(
        username: String,
        password: String,
        totpCode: String? = null,
        rememberMe: Boolean = false
    ): LoginResult {
        return try {
            val result = withContext(Dispatchers.IO) {
                val apiClient = SmsApiClient(serverUrl, username, password)
                val authResponse = apiClient.login(totpCode)

                if (authResponse != null) {
                    // Resolve token expiry seconds from either field (#50)
                    val expiresInSeconds: Long = when {
                        authResponse.expiresIn != null -> authResponse.expiresIn
                        authResponse.expires_at != null -> {
                            try {
                                val expiryInstant = Instant.parse(authResponse.expires_at)
                                val remaining = (expiryInstant.toEpochMilli() - System.currentTimeMillis()) / 1000
                                if (remaining > 0) remaining else 3600L
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not parse expires_at '${authResponse.expires_at}', defaulting to 1h", e)
                                3600L
                            }
                        }
                        else -> 3600L // Sensible default when server omits expiry info
                    }

                    val userInfo = authResponse.user ?: UserInfo(
                        id = username,
                        username = username,
                        email = null,
                        createdAt = null
                    )

                    // Persist real JWT token and expiry (#49)
                    credentialManager.saveCredentials(
                        username = username,
                        password = if (rememberMe) password else null,
                        jwtToken = authResponse.token,
                        refreshToken = authResponse.refreshToken,
                        expiresInSeconds = expiresInSeconds,
                        userId = userInfo.id,
                        email = userInfo.email
                    )

                    // Schedule background token expiry monitoring (#48)
                    sessionManager?.scheduleTokenExpiryWorker()
                    Log.d(TAG, "Login successful for user: ${userInfo.username} – token valid for ${expiresInSeconds}s")
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
     * Logout – clears credentials and cancels token expiry monitoring (#48).
     * Falls back to clearing credentials directly if SessionManager is unavailable.
     */
    fun logout() {
        sessionManager?.invalidateSession(SessionManager.REASON_MANUAL_LOGOUT)
            ?: credentialManager.clearCredentials()
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
