package com.example.smslogger.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smslogger.R
import com.example.smslogger.api.SmsApiClient
import com.example.smslogger.api.SmsApiRequest
import com.example.smslogger.data.AppDatabase
import com.example.smslogger.data.SmsMessage
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Independent background service for syncing SMS data to server
 * Features:
 * - Exponential retry (1-15 seconds)
 * - Authentication caching (5 minutes)
 * - Independent operation from other app components
 * - Persistent sync tracking
 */
class SmsSyncService : Service() {

    private val TAG = "SmsSyncService"
    private val NOTIFICATION_CHANNEL_ID = "SmsSyncChannel"
    private val NOTIFICATION_ID = 2

    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var db: AppDatabase
    private lateinit var apiClient: SmsApiClient

    // Sync configuration
    private var isSyncing = false
    private var syncJob: Job? = null

    // Exponential backoff configuration
    private val MIN_RETRY_DELAY = 1000L // 1 second
    private val MAX_RETRY_DELAY = 15000L // 15 seconds
    private val BACKOFF_MULTIPLIER = 2.0
    private val JITTER_RANGE = 0.1 // 10% jitter to avoid thundering herd

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)

        // Initialize API client with configuration
        // TODO: These should be configurable via settings or build config
        apiClient = SmsApiClient(
            baseUrl = "http://localhost:8080", // Configure this
            username = "testuser", // Configure this
            password = "testpass"  // Configure this
        )

        Log.d(TAG, "SMS Sync Service Created")
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SMS Sync Service Started")
        startForeground(NOTIFICATION_ID, createNotification())

        // Start sync process
        startSyncProcess()

        return START_STICKY // Restart if killed
    }

    /**
     * Start the continuous sync process with retry logic
     */
    private fun startSyncProcess(connectionRetryAttempt: Int = 0) {
        if (isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping")
            return
        }

        syncJob = serviceScope.launch {
            isSyncing = true
            Log.d(TAG, "Starting SMS sync process... (connection attempt ${connectionRetryAttempt + 1})")

            try {
                // Test connection first
                if (!apiClient.testConnection()) {
                    Log.e(TAG, "Cannot connect to server, will retry later")
                    val nextAttempt = connectionRetryAttempt + 1
                    scheduleConnectionRetry(nextAttempt)
                    return@launch
                }

                Log.d(TAG, "Successfully connected to server, starting sync")

                // Start continuous sync loop
                var syncRetryAttempt = 0

                while (isActive) {
                    try {
                        val syncedCount = performSyncBatch()

                        if (syncedCount > 0) {
                            Log.d(TAG, "Synced $syncedCount messages successfully")
                            updateNotification("Synced $syncedCount messages")
                            syncRetryAttempt = 0 // Reset retry counter on success
                        }

                        // Check if there are more messages to sync
                        val remainingCount = db.smsDao().getUnsyncedCount()
                        if (remainingCount == 0) {
                            Log.d(TAG, "All messages synced, waiting for new messages...")
                            updateNotification("All messages synced")
                            delay(30000) // Wait 30 seconds before checking again
                        } else {
                            // Continue syncing with a short delay
                            delay(2000) // 2 second delay between batches
                        }

                    } catch (e: Exception) {
                        syncRetryAttempt++
                        Log.e(TAG, "Sync batch failed (attempt $syncRetryAttempt)", e)

                        val delay = calculateRetryDelay(syncRetryAttempt)
                        Log.d(TAG, "Retrying sync in ${delay}ms...")
                        updateNotification("Sync failed, retrying in ${delay/1000}s...")

                        delay(delay)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Sync process failed", e)
                val nextAttempt = connectionRetryAttempt + 1
                scheduleConnectionRetry(nextAttempt)
            } finally {
                isSyncing = false
            }
        }
    }

    /**
     * Perform a batch sync of unsynced messages
     * Returns number of successfully synced messages
     */
    private suspend fun performSyncBatch(): Int {
        val unsyncedMessages = db.smsDao().getUnsyncedSms()

        if (unsyncedMessages.isEmpty()) {
            return 0
        }

        Log.d(TAG, "Found ${unsyncedMessages.size} unsynced messages")
        var syncedCount = 0

        // Process messages one by one to avoid overwhelming the server
        for (smsMessage in unsyncedMessages.take(10)) { // Batch size of 10
            try {
                val apiRequest = smsMessage.toApiRequest()

                if (apiClient.sendSms(apiRequest)) {
                    // Mark as synced
                    val currentTime = System.currentTimeMillis()
                    db.smsDao().markAsSynced(smsMessage.id, currentTime)
                    syncedCount++

                    Log.d(TAG, "Successfully synced SMS ID: ${smsMessage.id}")
                } else {
                    Log.w(TAG, "Failed to sync SMS ID: ${smsMessage.id}")
                    break // Stop batch on failure to avoid rate limiting
                }

                // Small delay between requests to be respectful to the server
                delay(100)

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing SMS ID: ${smsMessage.id}", e)
                break // Stop batch on error
            }
        }

        return syncedCount
    }

    /**
     * Calculate exponential backoff delay with jitter
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = (MIN_RETRY_DELAY * BACKOFF_MULTIPLIER.pow(attempt - 1)).toLong()
        val cappedDelay = min(baseDelay, MAX_RETRY_DELAY)

        // Add jitter to avoid thundering herd problem
        val jitter = (cappedDelay * JITTER_RANGE * Random.nextDouble()).toLong()
        val finalDelay = cappedDelay + jitter

        return finalDelay
    }

    /**
     * Schedule a retry after failure
     */
    private fun scheduleRetry(attempt: Int) {
        syncJob = serviceScope.launch {
            val delay = calculateRetryDelay(attempt)
            Log.d(TAG, "Scheduling retry in ${delay}ms (attempt $attempt)")
            updateNotification("Connection failed, retrying in ${delay/1000}s...")

            delay(delay)
            startSyncProcess()
        }
    }

    /**
     * Schedule a connection retry after failure
     */
    private fun scheduleConnectionRetry(attempt: Int) {
        syncJob = serviceScope.launch {
            val delay = calculateRetryDelay(attempt)
            Log.d(TAG, "Scheduling connection retry in ${delay}ms (attempt $attempt)")
            updateNotification("Connection failed, retrying in ${delay/1000}s...")

            delay(delay)
            startSyncProcess(attempt)
        }
    }

    /**
     * Get count of remaining unsynced messages
     */
    private suspend fun getRemainingCount(): Int {
        return try {
            db.smsDao().getUnsyncedCount()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Create notification channel for the service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SMS Sync Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(contentText: String = "Syncing SMS messages to server..."): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SMS Sync Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Update notification with current status
     */
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // This is a started service, not bound
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isSyncing = false
        Log.d(TAG, "SMS Sync Service Destroyed")
    }

    companion object {
        /**
         * Start the SMS sync service
         */
        fun startService(context: Context) {
            val intent = Intent(context, SmsSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the SMS sync service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, SmsSyncService::class.java)
            context.stopService(intent)
        }
    }
}

/**
 * Extension function to convert SmsMessage to API request
 */
private fun SmsMessage.toApiRequest(): SmsApiRequest {
    return SmsApiRequest(
        smsId = this.smsId,
        smsTimestamp = this.smsTimestamp,
        eventTimestamp = this.eventTimestamp,
        phoneNumber = this.phoneNumber,
        body = this.body,
        eventType = this.eventType,
        threadId = this.threadId,
        dateSent = this.dateSent,
        person = this.person
    )
}
