package com.example.smslogger.receiver // Replace with your actual package name

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.smslogger.data.AppDatabase
import com.example.smslogger.data.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    init {
        Log.d(TAG, "SmsReceiver instance CREATED (constructor-like initialization)")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive CALLED! Action: ${intent.action}")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult: PendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
//            val smsDao = AppDatabase.getDatabase(context.applicationContext).smsDao()

            messages?.forEach { sms ->
                val sender = sms.displayOriginatingAddress
                val messageBody = sms.messageBody
                val timestamp = sms.timestampMillis // Timestamp of the SMS itself

                Log.d(TAG, "Received SMS from: $sender, Body: $messageBody")

                val smsEntry = SmsMessage(
                    smsId = null, // In this case, we don't have the Telephony.Sms._ID directly
                    // You might need to query ContentProvider after receiving if you need this ID
                    smsTimestamp = timestamp,
                    eventTimestamp = System.currentTimeMillis(), // Current time for the logging event
                    phoneNumber = sender,
                    body = messageBody,
                    eventType = "RECEIVED"
                )

                // Insert into database using a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val smsDao = AppDatabase.getDatabase(context.applicationContext).smsDao()
                        smsDao.insertSms(smsEntry)
                        Log.d(TAG, "SMS saved to database.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving SMS to database", e)
                    } finally {
                        pendingResult.finish() // Always call finish when done
                    }
                }
            }
            // If messages is null or empty, still need to finish if goAsync was called
            if (messages == null || messages.isEmpty()) {
                pendingResult.finish()
            }
        }
    }
}