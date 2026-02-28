package com.example.smslogger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smslogger.security.SessionManager

/**
 * Internal BroadcastReceiver that reacts to session expiry events.
 *
 * Register this receiver dynamically (not in the manifest) inside long-lived
 * components (Services, Activities) that need to react to forced logout:
 *
 * ```kotlin
 * private val sessionExpiredReceiver = SessionExpiredReceiver { reason ->
 *     stopSelf() // or navigateToLogin()
 * }
 *
 * // In onCreate / onStart:
 * ContextCompat.registerReceiver(
 *     this, sessionExpiredReceiver,
 *     IntentFilter(SessionManager.ACTION_SESSION_EXPIRED),
 *     ContextCompat.RECEIVER_NOT_EXPORTED
 * )
 *
 * // In onDestroy / onStop:
 * unregisterReceiver(sessionExpiredReceiver)
 * ```
 *
 * Related: #48 (Auto-Logout and Re-Authentication Flow)
 */
class SessionExpiredReceiver(
    private val onSessionExpired: (reason: String) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "SessionExpiredReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SessionManager.ACTION_SESSION_EXPIRED) {
            val reason = intent.getStringExtra(SessionManager.EXTRA_REASON)
                ?: SessionManager.REASON_TOKEN_EXPIRED
            Log.w(TAG, "Session expired – reason: $reason")
            onSessionExpired(reason)
        }
    }
}

