package com.example.smslogger.api

import android.util.Base64
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * API client for communicating with SMS sync server
 * Handles authentication caching and SMS data synchronization
 */
class SmsApiClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val TAG = "SmsApiClient"

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // HTTP client with timeouts
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Authentication caching (5 minutes as requested)
    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0
    private val AUTH_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes in milliseconds

    /**
     * Authenticate with the server and cache the token for 5 minutes
     */
    private suspend fun authenticate(): String? {
        val currentTime = System.currentTimeMillis()

        // Return cached token if still valid
        if (cachedToken != null && currentTime < tokenExpiryTime) {
            Log.d(TAG, "Using cached authentication token")
            return cachedToken
        }

        Log.d(TAG, "Authenticating with server...")

        try {
            // Create Basic Auth header
            val credentials = "$username:$password"
            val basicAuth = "Basic " + Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url("$baseUrl/api/auth/login")
                .post("".toRequestBody()) // Empty body for login
                .header("Authorization", basicAuth)
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val authResponse = json.decodeFromString<AuthResponse>(responseBody)

                    // Cache the token
                    cachedToken = authResponse.token
                    tokenExpiryTime = currentTime + AUTH_CACHE_DURATION

                    Log.d(TAG, "Authentication successful, token cached for 5 minutes")
                    return authResponse.token
                }
            } else {
                Log.e(TAG, "Authentication failed: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                if (errorBody != null) {
                    try {
                        val error = json.decodeFromString<ApiError>(errorBody)
                        Log.e(TAG, "Auth error: ${error.error}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Auth error body: $errorBody")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during authentication", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during authentication", e)
        }

        return null
    }

    /**
     * Send SMS data to the server
     * Returns true if successful, false otherwise
     */
    suspend fun sendSms(smsRequest: SmsApiRequest): Boolean {
        try {
            // Get authentication token
            val token = authenticate()
            if (token == null) {
                Log.e(TAG, "Failed to authenticate, cannot send SMS")
                return false
            }

            // Prepare JSON payload
            val jsonBody = json.encodeToString(SmsApiRequest.serializer(), smsRequest)
            Log.d(TAG, "Sending SMS to server: $jsonBody")

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/sms/add")
                .post(requestBody)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "SMS sent successfully: ${response.code}")
                return true
            } else {
                Log.e(TAG, "Failed to send SMS: ${response.code} ${response.message}")

                val errorBody = response.body?.string()
                if (errorBody != null) {
                    try {
                        val error = json.decodeFromString<ApiError>(errorBody)
                        Log.e(TAG, "SMS send error: ${error.error}")
                    } catch (e: Exception) {
                        Log.e(TAG, "SMS send error body: $errorBody")
                    }
                }

                // Clear cached token if unauthorized (401)
                if (response.code == 401) {
                    Log.w(TAG, "Authentication token expired, clearing cache")
                    cachedToken = null
                    tokenExpiryTime = 0
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending SMS", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending SMS", e)
        }

        return false
    }

    /**
     * Test connection to the server
     */
    suspend fun testConnection(): Boolean {
        return authenticate() != null
    }

    /**
     * Clear cached authentication (useful for logout or credential changes)
     */
    fun clearAuthCache() {
        cachedToken = null
        tokenExpiryTime = 0
        Log.d(TAG, "Authentication cache cleared")
    }
}
