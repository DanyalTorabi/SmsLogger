package com.example.smslogger.network

import android.content.Context
import android.util.Log
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp Interceptor for authentication token injection and 401 detection.
 *
 * Responsibilities:
 * 1. Proactively check token validity before each request; abort early if expired
 * 2. Attach the Bearer token to every outgoing request
 * 3. On a 401 Unauthorized response, trigger [SessionManager.invalidateSession]
 *    so the app broadcasts SESSION_EXPIRED and all components navigate to login
 *
 * Related: #48 (Auto-Logout and Re-Authentication Flow)
 */
class AuthInterceptor(context: Context) : Interceptor {

    private val appContext = context.applicationContext
    private val credentialManager = KeystoreCredentialManager.getInstance(appContext)
    private val sessionManager = SessionManager.getInstance(appContext)

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        // ── Proactive token expiry check ──────────────────────────────────────
        if (credentialManager.isTokenExpired()) {
            Log.w(TAG, "Token expired before request – triggering session invalidation")
            sessionManager.invalidateSession(SessionManager.REASON_TOKEN_EXPIRED)
            // Proceed unauthenticated; components will redirect to login via the broadcast
            return chain.proceed(originalRequest)
        }

        // ── Attach Bearer token ───────────────────────────────────────────────
        val token: String? = credentialManager.getToken()
        val authenticatedRequest: Request = if (token != null) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        } else {
            Log.w(TAG, "No token available – proceeding without Authorization header")
            originalRequest
        }

        // ── Execute and detect 401 ────────────────────────────────────────────
        val response: Response = chain.proceed(authenticatedRequest)

        if (response.code == 401) {
            Log.w(TAG, "HTTP 401 received – triggering session invalidation")
            sessionManager.invalidateSession(SessionManager.REASON_HTTP_401)
        }

        return response
    }
}

