package com.example.smslogger.service // Replace with your actual package name

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smslogger.R
import com.example.smslogger.data.AppDatabase
import com.example.smslogger.data.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SmsLoggingService : Service() {

    private val TAG = "SmsLoggingService"
    private val NOTIFICATION_CHANNEL_ID = "SmsLoggerChannel"
    private val NOTIFICATION_ID = 1

    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
        Log.d(TAG, "Service Created")
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service Started")
        startForeground(NOTIFICATION_ID, createNotification())

        // Sync SMS messages in the background
        serviceScope.launch {
            try {
                val syncStart = System.currentTimeMillis()
                val syncCount = syncNewSmsMessages(applicationContext, db)
                val syncTime = System.currentTimeMillis() - syncStart

                // Update notification with sync results
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(
                    NOTIFICATION_ID,
                    createNotification("Synced $syncCount new SMS messages in ${syncTime}ms")
                )

                Log.d(TAG, "Initial SMS sync complete: found $syncCount new messages in ${syncTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error during initial SMS sync", e)
            }
        }
        return START_STICKY
    }

    /**
     * Synchronized function to fetch and process new SMS messages
     * This function is designed to be used in both startup routine and onReceive
     *
     * @param context Application context to access ContentResolver
     * @param database Database instance to use for operations
     * @return Number of new SMS messages added to the database
     */
    companion object {
        suspend fun syncNewSmsMessages(context: Context, database: AppDatabase): Int {
            val TAG = "SmsLoggingService"
            Log.d(TAG, "Starting to sync new SMS messages...")
            val contentResolver: ContentResolver = context.contentResolver
            val smsUri: Uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            // Sort by date in descending order to process newest messages first
            val sortOrder = "${Telephony.Sms.DATE} DESC"

            val cursor: Cursor? = contentResolver.query(smsUri, projection, null, null, sortOrder)
            var newSmsCount = 0

            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyColumn = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateColumn = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeColumn = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                    val smsDao = database.smsDao()

                    // Reset cursor to beginning
                    it.moveToFirst()

                    // Second pass: Process all SMS messages up to the boundary (or all if no boundary found)
                    // We need to process them in chronological order (oldest first)
                    val smsToProcess = mutableListOf<SmsMessage>()

                    do {
                        val smsIdFromProvider = it.getLong(idColumn)
                        val address = it.getString(addressColumn)
                        val body = it.getString(bodyColumn)
                        val date = it.getLong(dateColumn)
                        val typeInt = it.getInt(typeColumn)

                        // Skip if address or body is null
                        if (address == null || body == null) {
                            continue
                        }

                        // Check if this SMS already exists in our database
                        val existingSms = smsDao.getSmsByOriginalId(smsIdFromProvider)
                        if (existingSms != null) {
                            // Reached where the all messages before this one are already logged to DB.
                            break
                        }

                        // If not found, create a new SmsMessage object to be inserted later
                        val eventType = when (typeInt) {
                            Telephony.Sms.MESSAGE_TYPE_INBOX -> "RECEIVED"
                            Telephony.Sms.MESSAGE_TYPE_SENT -> "SENT"
                            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "OUTBOX"
                            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "DRAFT"
                            Telephony.Sms.MESSAGE_TYPE_FAILED -> "FAILED"
                            Telephony.Sms.MESSAGE_TYPE_QUEUED -> "QUEUED"
                            else -> "UNKNOWN ($typeInt)"
                        }

                        val smsEntry = SmsMessage(
                            id = 0, // Let Room generate a unique ID
                            smsId = smsIdFromProvider,
                            smsTimestamp = date,
                            eventTimestamp = System.currentTimeMillis(),
                            phoneNumber = address,
                            body = body,
                            eventType = eventType
                        )

                        // Add to our list (in reverse chronological order)
                        smsToProcess.add(smsEntry)

                    } while (it.moveToNext())

                    // Now insert SMS entries in chronological order (oldest first)
                    for (smsEntry in smsToProcess.reversed()) {
                        try {
                            smsDao.insertSms(smsEntry)
                            newSmsCount++
                            Log.d(TAG, "Added new SMS: ProviderID ${smsEntry.smsId}, Type: ${smsEntry.eventType}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving SMS (ProviderID: ${smsEntry.smsId}) to database", e)
                        }
                    }

                    Log.d(TAG, "Synced $newSmsCount new SMS messages")
                } else {
                    Log.d(TAG, "No SMS messages found to sync")
                }
            }

            return newSmsCount
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Sms Logger Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String = "Logging SMS messages in the background."): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sms Logger Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service Destroyed")
    }
}
