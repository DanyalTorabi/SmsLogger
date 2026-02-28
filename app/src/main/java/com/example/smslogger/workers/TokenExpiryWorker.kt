package com.example.smslogger.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.security.SessionManager

/**
 * WorkManager worker that periodically checks JWT token validity.
 *
 * Scheduled every 5 minutes after a successful login (see [SessionManager.scheduleTokenExpiryWorker]).
 * Cancelled on logout (see [SessionManager.cancelTokenExpiryWorker]).
 *
 * Behaviour:
 * - Token already expired → [SessionManager.invalidateSession] (SESSION_EXPIRED broadcast)
 * - Token expires within 5 minutes → [SessionManager.broadcastExpiryWarning]
 * - Token is healthy → no-op
 *
 * Related: #48 (Auto-Logout and Re-Authentication Flow)
 */
class TokenExpiryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val credentialManager by lazy {
        KeystoreCredentialManager.getInstance(applicationContext)
    }
    private val sessionManager by lazy {
        SessionManager.getInstance(applicationContext)
    }

    companion object {
        private const val TAG = "TokenExpiryWorker"

        /** Warn when token expires within this window (5 minutes in ms). */
        private const val WARNING_THRESHOLD_MS = 5 * 60 * 1000L
    }

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d(TAG, "Running token expiry check")

        return try {
            when {
                credentialManager.isTokenExpired() -> {
                    Log.w(TAG, "Token is expired – invalidating session")
                    sessionManager.invalidateSession(SessionManager.REASON_TOKEN_EXPIRED)
                }

                credentialManager.getTimeToTokenExpiry() <= WARNING_THRESHOLD_MS -> {
                    val remainingSec = credentialManager.getTimeToTokenExpiry() / 1000
                    Log.d(TAG, "Token expires in ${remainingSec}s – sending warning broadcast")
                    sessionManager.broadcastExpiryWarning()
                }

                else -> {
                    val remainingSec = credentialManager.getTimeToTokenExpiry() / 1000
                    Log.d(TAG, "Token is healthy – expires in ${remainingSec}s")
                }
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during token expiry check", e)
            ListenableWorker.Result.retry()
        }
    }
}
