package com.example.smslogger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.smslogger.data.AppDatabase
import com.example.smslogger.service.SmsLoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    init {
        Log.d(TAG, "SmsReceiver instance CREATED (constructor-like initialization)")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive CALLED! Action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS received - scheduling sync in 5 seconds")

            // Schedule the SMS sync to happen 5 seconds later without blocking onReceive
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    // Wait 5 seconds for the SMS to be fully processed and available in content provider
                    delay(5000)

                    Log.d(TAG, "Starting delayed SMS sync...")

                    // Get the database instance
                    val db = AppDatabase.getDatabase(context.applicationContext)

                    // Use the static function from SmsLoggingService to sync new messages
                    val newMessages = SmsLoggingService.syncNewSmsMessages(context.applicationContext, db)

                    Log.d(TAG, "Successfully processed $newMessages new SMS messages from delayed sync")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing delayed SMS sync", e)
                }
            }

            // onReceive returns immediately here
            Log.d(TAG, "onReceive returning immediately, sync scheduled")
        }
    }
}
