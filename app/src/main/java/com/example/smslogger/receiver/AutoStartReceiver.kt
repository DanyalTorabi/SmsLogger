package com.example.smslogger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smslogger.service.SmsLoggingService
import com.example.smslogger.service.SmsSyncService

/**
 * Auto-start receiver for SMS Logger services
 * Starts services automatically when needed
 */
class AutoStartReceiver : BroadcastReceiver() {

    private val TAG = "AutoStartReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "App updated/replaced, starting services...")
                startServices(context)
            }

            "com.example.smslogger.START_SERVICES" -> {
                Log.d(TAG, "Manual service start requested...")
                startServices(context)
            }
        }
    }

    private fun startServices(context: Context) {
        try {
            // Start SMS logging service
            SmsLoggingService.startService(context)
            Log.d(TAG, "SMS Logging Service start requested")

            // Start SMS sync service
            SmsSyncService.startService(context)
            Log.d(TAG, "SMS Sync Service start requested")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting services", e)
        }
    }
}
