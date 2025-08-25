package com.example.smslogger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.smslogger.service.SmsLoggingService
import com.example.smslogger.service.SmsSyncService

class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting SMS services")

            try {
                // Start SMS logging service
                val loggingServiceIntent = Intent(context, SmsLoggingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(loggingServiceIntent)
                } else {
                    context.startService(loggingServiceIntent)
                }
                Log.d(TAG, "SMS Logger service start requested after boot")

                // Start SMS sync service
                SmsSyncService.startService(context)
                Log.d(TAG, "SMS Sync service start requested after boot")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start services after boot", e)
            }
        }
    }
}
