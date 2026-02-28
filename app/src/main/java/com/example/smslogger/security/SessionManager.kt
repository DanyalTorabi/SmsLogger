package com.example.smslogger.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smslogger.R
import com.example.smslogger.workers.TokenExpiryWorker
import java.util.concurrent.TimeUnit

/**
 * Central session management for authentication lifecycle.
 *
 * Responsibilities:
 * - Trigger logout and broadcast session expiry to all active components
 * - Schedule/cancel WorkManager token expiry checks
 * - Provide a single entry point for session invalidation
 *
 * Related: #48 (Auto-Logout and Re-Authentication Flow)
 */
class SessionManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val credentialManager = KeystoreCredentialManager.getInstance(appContext)

    companion object {
        private const val TAG = "SessionManager"

        /** Broadcast action sent when the session expires or user is forcibly logged out. */
        const val ACTION_SESSION_EXPIRED = "com.example.smslogger.SESSION_EXPIRED"

        /** Broadcast action sent when the session will expire soon (5-minute warning). */
        const val ACTION_SESSION_EXPIRY_WARNING = "com.example.smslogger.SESSION_EXPIRY_WARNING"

        /** Extra key carrying the reason for session expiry. */
        const val EXTRA_REASON = "reason"

        const val REASON_TOKEN_EXPIRED = "TOKEN_EXPIRED"
        const val REASON_HTTP_401 = "HTTP_401_UNAUTHORIZED"
        const val REASON_MANUAL_LOGOUT = "MANUAL_LOGOUT"

        private const val WORK_TAG_TOKEN_EXPIRY = "token_expiry_check"

        /** Notification channel for session alerts. */
        const val NOTIFICATION_CHANNEL_SESSION = "session_alerts"
        private const val NOTIFICATION_ID_SESSION_EXPIRED = 1001
        private const val NOTIFICATION_ID_SESSION_WARNING = 1002

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    // ──────────────────────────────────────────────────
    // Session invalidation
    // ──────────────────────────────────────────────────

    /**
     * Perform a full session logout:
     * 1. Clear stored credentials
     * 2. Cancel token expiry worker
     * 3. Broadcast SESSION_EXPIRED so all active components can react
     * 4. Post a "Session expired" notification with tap-to-login action (#48)
     *
     * @param reason One of the REASON_* constants for diagnostics
     */
    fun invalidateSession(reason: String = REASON_MANUAL_LOGOUT) {
        Log.w(TAG, "Invalidating session – reason: $reason")
        credentialManager.clearCredentials()
        cancelTokenExpiryWorker()
        broadcastSessionExpired(reason)
        // Only post notification for automatic expiry (not manual logout – user already knows)
        if (reason != REASON_MANUAL_LOGOUT) {
            postSessionExpiredNotification()
        }
    }

    /**
     * Broadcast a 5-minute expiry warning without clearing credentials.
     * Activities may use this to show a dialog or refresh proactively.
     * Also posts a system notification so the user is informed even if the app is backgrounded.
     */
    fun broadcastExpiryWarning() {
        Log.d(TAG, "Broadcasting session expiry warning")
        val intent = Intent(ACTION_SESSION_EXPIRY_WARNING).apply {
            setPackage(appContext.packageName)
        }
        appContext.sendBroadcast(intent)
        postSessionExpiryWarningNotification()
    }

    // ──────────────────────────────────────────────────
    // Token expiry worker
    // ──────────────────────────────────────────────────

    /**
     * Schedule a periodic WorkManager job to check token validity every 5 minutes.
     * Call this after a successful login.
     */
    fun scheduleTokenExpiryWorker() {
        Log.d(TAG, "Scheduling token expiry worker")
        val workRequest = PeriodicWorkRequestBuilder<TokenExpiryWorker>(
            repeatInterval = 5,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).addTag(WORK_TAG_TOKEN_EXPIRY).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_TAG_TOKEN_EXPIRY,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancel the periodic token expiry worker.
     * Called on logout or when the user's session is fully cleaned up.
     */
    fun cancelTokenExpiryWorker() {
        Log.d(TAG, "Cancelling token expiry worker")
        WorkManager.getInstance(appContext).cancelAllWorkByTag(WORK_TAG_TOKEN_EXPIRY)
    }

    // ──────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────

    private fun broadcastSessionExpired(reason: String) {
        val intent = Intent(ACTION_SESSION_EXPIRED).apply {
            setPackage(appContext.packageName)
            putExtra(EXTRA_REASON, reason)
        }
        appContext.sendBroadcast(intent)
        Log.d(TAG, "SESSION_EXPIRED broadcast sent (reason=$reason)")
    }

    // ──────────────────────────────────────────────────
    // Notifications
    // ──────────────────────────────────────────────────

    private fun getNotificationManager(): NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_SESSION,
                appContext.getString(R.string.notification_channel_session_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            getNotificationManager().createNotificationChannel(channel)
        }
    }

    /**
     * Build a [PendingIntent] that opens [LoginActivity] when the notification is tapped.
     * Uses reflection-free class name lookup to avoid a direct dependency on the ui layer.
     */
    private fun buildLoginPendingIntent(): PendingIntent {
        val loginIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Route through MainActivity which redirects unauthenticated users to LoginActivity
            } ?: Intent().apply {
            setPackage(appContext.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(appContext, 0, loginIntent, flags)
    }

    private fun postSessionExpiredNotification() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_SESSION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appContext.getString(R.string.session_expired_notification_title))
            .setContentText(appContext.getString(R.string.session_expired_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildLoginPendingIntent())
            .build()
        getNotificationManager().notify(NOTIFICATION_ID_SESSION_EXPIRED, notification)
        Log.d(TAG, "Session expired notification posted")
    }

    private fun postSessionExpiryWarningNotification() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_SESSION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appContext.getString(R.string.session_expiry_warning_notification_title))
            .setContentText(appContext.getString(R.string.session_expiry_warning_notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(buildLoginPendingIntent())
            .build()
        getNotificationManager().notify(NOTIFICATION_ID_SESSION_WARNING, notification)
        Log.d(TAG, "Session expiry warning notification posted")
    }
}

