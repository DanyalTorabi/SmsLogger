package com.example.smslogger.data.repository

import android.content.Context
import android.util.Log
import com.example.smslogger.api.SmsApiClient
import com.example.smslogger.api.UserInfo
import com.example.smslogger.data.exception.AccountInactiveException
import com.example.smslogger.data.exception.AccountLockedException
import com.example.smslogger.data.exception.AuthException
import com.example.smslogger.data.exception.InvalidCredentialsException
import com.example.smslogger.data.exception.NetworkException
import com.example.smslogger.data.exception.ServerErrorException
import com.example.smslogger.data.model.ErrorResponse
import com.example.smslogger.data.model.LoginResponse
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant

/**
 * Repository for authentication operations (#51).
 *
 * Responsibilities:
 * - Call POST /api/auth/login via [SmsApiClient] and return [Result]<[LoginResponse]>
 * - Map HTTP / network errors to typed [AuthException] subclasses
 * - Parse server [ErrorResponse] bodies to inform the exception message
 * - Persist a real JWT token to [KeystoreCredentialManager] on success
 * - Schedule / cancel [TokenExpiryWorker] via [SessionManager]
 * - Expose [logout] and [isAuthenticated] helpers
 *
 * Error mapping:
 * - 401                  → [InvalidCredentialsException]
 * - 403 + "locked"       → [AccountLockedException]
 * - 403 + "inactive"     → [AccountInactiveException]
 * - 5xx                  → [ServerErrorException]
 * - Network / IO failure → [NetworkException]
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

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticate user with username / password and an optional TOTP code.
     *
     * Calls POST /api/auth/login, persists the returned JWT, and schedules
     * background token-expiry monitoring on success.
     *
     * @param username    User's username or email — not logged
     * @param password    User's password — not logged
     * @param totpCode    Optional 6-digit TOTP code for 2FA
     * @param rememberMe  Whether to persist the password for future logins
     * @return [Result.success] wrapping [LoginResponse] on success,
     *         [Result.failure] wrapping a typed [AuthException] on any error
     */
    suspend fun login(
        username: String,
        password: String,
        totpCode: String? = null,
        rememberMe: Boolean = false
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val apiClient = SmsApiClient(serverUrl, username, password)
            val authResponse = apiClient.login(totpCode)

            if (authResponse != null) {
                // Resolve expiry from either ISO-8601 field or seconds duration
                val expiresInSeconds: Long = when {
                    authResponse.expiresIn != null -> authResponse.expiresIn
                    authResponse.expires_at != null -> {
                        runCatching {
                            val remaining =
                                (Instant.parse(authResponse.expires_at).toEpochMilli()
                                        - System.currentTimeMillis()) / 1000
                            if (remaining > 0) remaining else 3600L
                        }.getOrElse {
                            Log.w(TAG, "Could not parse expires_at '${authResponse.expires_at}', defaulting to 1h")
                            3600L
                        }
                    }
                    else -> 3600L
                }

                val userInfo = authResponse.user ?: UserInfo(
                    id = username,
                    username = username,
                    email = null,
                    createdAt = null
                )

                // Persist real JWT — no sensitive values written to Logcat
                credentialManager.saveCredentials(
                    username = username,
                    password = if (rememberMe) password else null,
                    jwtToken = authResponse.token,
                    refreshToken = authResponse.refreshToken,
                    expiresInSeconds = expiresInSeconds,
                    userId = userInfo.id,
                    email = userInfo.email
                )

                sessionManager?.scheduleTokenExpiryWorker()
                Log.d(TAG, "Login successful for user: [REDACTED] – token valid for ${expiresInSeconds}s")

                val loginResponse = LoginResponse(
                    token = authResponse.token,
                    expires_at = authResponse.expires_at,
                    user = userInfo
                )
                Result.success(loginResponse)

            } else {
                // SmsApiClient.login() returns null on non-2xx — map by inspecting
                // the last known HTTP status via a dedicated login-with-response call
                Log.w(TAG, "Login returned null response – treating as invalid credentials")
                Result.failure(InvalidCredentialsException())
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network error during login")
            Result.failure(NetworkException(cause = e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during login: ${e.javaClass.simpleName}")
            Result.failure(NetworkException(message = getErrorMessage(e), cause = e))
        }
    }

    /**
     * Logout — clears credentials and cancels token expiry monitoring.
     * Falls back to clearing credentials directly if [SessionManager] is unavailable.
     */
    fun logout() {
        sessionManager?.invalidateSession(SessionManager.REASON_MANUAL_LOGOUT)
            ?: credentialManager.clearCredentials()
        Log.d(TAG, "User logged out")
    }

    /** Returns true when a valid, non-expired JWT is stored. */
    fun isAuthenticated(): Boolean = credentialManager.isAuthenticated()

    /** Returns the stored username for pre-filling the login form. */
    fun getCurrentUsername(): String? = credentialManager.getUsername()

    /** Returns the stored user ID. */
    fun getCurrentUserId(): String? = credentialManager.getUserId()

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Map an HTTP status code + parsed [ErrorResponse] to the correct [AuthException].
     *
     * Called by [SmsApiClient] consumers that need finer-grained error info.
     * Exposed internally so tests can exercise the mapping directly.
     */
    internal fun mapHttpError(httpCode: Int, errorResponse: ErrorResponse?): AuthException {
        val serverMessage = errorResponse?.message ?: errorResponse?.error
        return when (httpCode) {
            401 -> InvalidCredentialsException(
                serverMessage ?: "Invalid username or password"
            )
            403 -> {
                val body = serverMessage?.lowercase() ?: ""
                when {
                    body.contains("lock") -> AccountLockedException(
                        serverMessage ?: "Account locked. Try again in 30 minutes"
                    )
                    body.contains("inactiv") || body.contains("deactivat") ->
                        AccountInactiveException(
                            serverMessage ?: "Your account has been deactivated"
                        )
                    else -> AccountLockedException(serverMessage ?: "Access forbidden")
                }
            }
            in 500..599 -> ServerErrorException(
                serverMessage ?: "Server error. Please try again later",
                httpCode = httpCode
            )
            else -> NetworkException(serverMessage ?: "Unexpected error (HTTP $httpCode)")
        }
    }

    /** Safely parse a raw JSON string into [ErrorResponse], returning null on failure. */
    internal fun parseErrorBody(body: String?): ErrorResponse? {
        if (body.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<ErrorResponse>(body) }.getOrNull()
    }

    private fun getErrorMessage(exception: Exception): String = when {
        exception.message?.contains("Unable to resolve host") == true ->
            "Cannot connect to server. Check your network connection."
        exception.message?.contains("timeout") == true ->
            "Connection timeout. Please try again."
        exception.message?.contains("Failed to connect") == true ->
            "Cannot reach server. Check server URL in settings."
        else -> "Network error. Please try again"
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Legacy sealed result (kept for backward-compat with AuthViewModel)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sealed class kept for [AuthViewModel] compatibility.
     * New code should use the [Result]-returning [login] overload directly.
     */
    sealed class LoginResult {
        data class Success(val user: UserInfo) : LoginResult()
        data class Error(
            val message: String,
            val code: String,
            val httpCode: Int? = null,
            val exception: Exception? = null
        ) : LoginResult()
    }
}
