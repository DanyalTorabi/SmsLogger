package com.example.smslogger.api

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.smslogger.network.AuthInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * API client for communicating with SMS sync server.
 *
 * When [context] is provided, [AuthInterceptor] is added to the OkHttpClient so that:
 *  - The JWT Bearer token is attached automatically to every request
 *  - HTTP 401 responses trigger auto-logout via [SessionManager]
 *
 * The username/password constructor params are kept for the initial login/testConnection
 * flow in [AuthRepository] before credentials are persisted to [KeystoreCredentialManager].
 *
 * Related: #48 (Auto-Logout and Re-Authentication Flow)
 */
class SmsApiClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
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
        .build()

    // Token cache – used only during the initial login/testConnection flow
    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0
    private val AUTH_CACHE_DURATION = 5 * 60 * 1000L

    private suspend fun authenticate(): String? {
        val currentTime = System.currentTimeMillis()
        if (cachedToken != null && currentTime < tokenExpiryTime) {
            Log.d(TAG, "Using cached authentication token")
            return cachedToken
        }

        Log.d(TAG, "Authenticating with server...")
        return try {
            val loginRequest = AuthRequest(username, password)
            val jsonBody = json.encodeToString(AuthRequest.serializer(), loginRequest)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/auth/login")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            Log.d(TAG, "Authenticating with base URL:[$baseUrl] user:[$username]")
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val authResponse = json.decodeFromString<AuthResponse>(responseBody)
                    cachedToken = authResponse.token
                    tokenExpiryTime = currentTime + AUTH_CACHE_DURATION
                    Log.d(TAG, "Authentication successful, token cached for 5 minutes")
                    authResponse.token
                } else null
            } else {
                Log.e(TAG, "Authentication failed: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                if (errorBody != null) {
                    try {
                        val error = json.decodeFromString<ApiError>(errorBody)
                        Log.e(TAG, "Auth error: ${error.error}")
                    } catch (_: Exception) {
                        Log.e(TAG, "Auth error body: $errorBody")
                    }
                }
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during authentication", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during authentication", e)
            null
        }
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
