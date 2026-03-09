package com.example.smslogger.api

import android.content.Context
import android.util.Log
import com.example.smslogger.BuildConfig
import com.example.smslogger.data.exception.CertificatePinningException
import com.example.smslogger.network.AuthInterceptor
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * API client for communicating with SMS sync server.
 *
 * When [context] is provided, [AuthInterceptor] is added to the OkHttpClient so that:
 *  - The JWT Bearer token is attached automatically to every request
 *  - HTTP 401 responses trigger auto-logout via [SessionManager]
 *
 * [username] and [password] are only needed for the initial login flow
 * ([AuthRepository]). When instantiated for SMS syncing ([SmsSyncService]),
 * omit them — [AuthInterceptor] will inject the stored token instead (#49).
 *
 * Certificate pinning (#56):
 *  - In release builds, [BuildConfig.CERT_HOSTNAME], [BuildConfig.CERT_PIN_PRIMARY]
 *    and [BuildConfig.CERT_PIN_BACKUP] must be set to real values.
 *  - In debug builds those fields are empty strings → pinning is skipped.
 *  - If the server URL starts with "http://" (cleartext), pinning is also skipped
 *    with a warning (pinning only works over TLS).
 *
 * See [docs/security/CERTIFICATE_PINNING.md] for local dev setup, staging/production
 * configuration, and the certificate rotation workflow.
 *
 * Related: #48 (Auto-Logout), #49 (JWT header injection), #50 (API models), #56 (cert pinning)
 */
class SmsApiClient(
    private val baseUrl: String,
    private val username: String = "",
    private val password: String = "",
    context: Context? = null
) {
    private val TAG = "SmsApiClient"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply { if (context != null) addInterceptor(AuthInterceptor(context)) }
        .apply { configureCertificatePinning(this) }
        .build()

    /**
     * Attaches a [CertificatePinner] when pinning is configured for the release build.
     *
     * Pinning is skipped when:
     * - [BuildConfig.CERT_HOSTNAME] is blank (debug builds)
     * - The [baseUrl] uses plain HTTP (pinning has no effect over cleartext)
     */
    private fun configureCertificatePinning(builder: OkHttpClient.Builder) {
        val hostname = BuildConfig.CERT_HOSTNAME
        val primaryPin = BuildConfig.CERT_PIN_PRIMARY
        val backupPin = BuildConfig.CERT_PIN_BACKUP

        if (hostname.isBlank() || primaryPin.isBlank()) {
            Log.d(TAG, "Certificate pinning disabled (debug build or pins not configured)")
            return
        }

        if (baseUrl.startsWith("http://")) {
            Log.w(TAG, "Certificate pinning skipped – baseUrl uses plain HTTP, not HTTPS. " +
                    "Pinning only works over TLS.")
            return
        }

        val pinner = CertificatePinner.Builder()
            .add(hostname, primaryPin)
            .apply { if (backupPin.isNotBlank()) add(hostname, backupPin) }
            .build()

        builder.certificatePinner(pinner)
        Log.i(TAG, "Certificate pinning enabled for host: $hostname")
    }


    // Token cache – used only during the initial login/testConnection flow
    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0
    private val AUTH_CACHE_DURATION = 5 * 60 * 1000L

    /**
     * Authenticate against POST /api/auth/login and return the full [AuthResponse].
     *
     * Accepts an optional [totpCode] for 2FA (#50).
     * On success the token is cached internally for [AUTH_CACHE_DURATION].
     * Returns null on any network or server error.
     */
    suspend fun login(totpCode: String? = null): AuthResponse? {
        Log.d(TAG, "Logging in with base URL:[$baseUrl] user:[$username]")
        return try {
            val loginRequest = AuthRequest(username, password, totp_code = totpCode)
            val jsonBody = json.encodeToString(AuthRequest.serializer(), loginRequest)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/auth/login")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val authResponse = json.decodeFromString<AuthResponse>(responseBody)
                    // Cache token for testConnection() fall-through callers
                    cachedToken = authResponse.token
                    tokenExpiryTime = System.currentTimeMillis() + AUTH_CACHE_DURATION
                    Log.d(TAG, "Login successful – token cached")
                    authResponse
                } else {
                    Log.e(TAG, "Login succeeded but response body was empty")
                    null
                }
            } else {
                Log.e(TAG, "Login failed: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                if (errorBody != null) {
                    try {
                        val error = json.decodeFromString<ApiError>(errorBody)
                        Log.e(TAG, "Login error: ${error.error} – ${error.message}")
                    } catch (_: Exception) {
                        Log.e(TAG, "Login error body: $errorBody")
                    }
                }
                null
            }
        } catch (e: SSLPeerUnverifiedException) {
            Log.e(TAG, "Certificate pinning failure during login – possible MITM attack", e)
            throw CertificatePinningException(cause = e)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during login", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during login", e)
            null
        }
    }

    // Keep private authenticate() delegating to login() to avoid breaking testConnection()
    private suspend fun authenticate(): String? {
        val currentTime = System.currentTimeMillis()
        if (cachedToken != null && currentTime < tokenExpiryTime) {
            Log.d(TAG, "Using cached authentication token")
            return cachedToken
        }
        return login()?.token
    }

    /**
     * Send SMS data to the server.
     * Token injection and 401 handling are done by [AuthInterceptor] when context is provided.
     */
    suspend fun sendSms(smsRequest: SmsApiRequest): Boolean {
        return try {
            val jsonBody = json.encodeToString(SmsApiRequest.serializer(), smsRequest)
            Log.d(TAG, "Sending SMS to server: $jsonBody")

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/sms/add")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "SMS sent successfully: ${response.code}")
                true
            } else {
                Log.e(TAG, "Failed to send SMS: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                if (errorBody != null) {
                    try {
                        val error = json.decodeFromString<ApiError>(errorBody)
                        Log.e(TAG, "SMS send error: ${error.error}")
                    } catch (_: Exception) {
                        Log.e(TAG, "SMS send error body: $errorBody")
                    }
                }
                // Note: 401 is handled centrally by AuthInterceptor when context is set
                if (response.code == 401 && cachedToken != null) {
                    Log.w(TAG, "Clearing cached token after 401")
                    cachedToken = null
                    tokenExpiryTime = 0
                }
                false
            }
        } catch (e: SSLPeerUnverifiedException) {
            Log.e(TAG, "Certificate pinning failure during SMS sync – possible MITM attack", e)
            throw CertificatePinningException(cause = e)
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending SMS", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending SMS", e)
            false
        }
    }

    /** Test connection to the server (used during login flow). */
    suspend fun testConnection(): Boolean = authenticate() != null

    /** Clear cached authentication (useful for credential changes). */
    fun clearAuthCache() {
        cachedToken = null
        tokenExpiryTime = 0
        Log.d(TAG, "Authentication cache cleared")
    }
}
