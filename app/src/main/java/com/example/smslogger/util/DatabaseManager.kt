package com.example.smslogger.util

import android.util.Log
import com.example.smslogger.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/**
 * Database health monitor and maintenance utilities
 */
@Suppress("unused") // Utility class for future integration
class DatabaseManager(private val database: AppDatabase) {

    private val TAG = "DatabaseManager"
    private val lastMaintenanceTime = AtomicLong(0)
    private val MAINTENANCE_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    private val CUTOFF_INTERVAL = 30 * 24 * 60 * 60 * 1000L // 30 days

    @Suppress("unused")
    suspend fun performMaintenance(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMaintenanceTime.get() < MAINTENANCE_INTERVAL) {
                Log.d(TAG, "Maintenance not needed yet")
                return@withContext true
            }

            Log.d(TAG, "Starting database maintenance...")

            // Clean up old sync records (older than 30 days)
            val cutoffTime = currentTime - CUTOFF_INTERVAL
            val deletedCount = database.smsDao().deleteOldSyncedMessages(cutoffTime)
            Log.d(TAG, "Cleaned up $deletedCount old synced messages")

            // Update statistics
            val totalMessages = database.smsDao().getTotalMessageCount()
            val unsyncedCount = database.smsDao().getUnsyncedCount()
            val syncedCount = totalMessages - unsyncedCount

            Log.d(TAG, "Database stats - Total: $totalMessages, Synced: $syncedCount, Unsynced: $unsyncedCount")

            lastMaintenanceTime.set(currentTime)
            true

        } catch (e: Exception) {
            Log.e(TAG, "Database maintenance failed", e)
            false
        }
    }

    @Suppress("unused")
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val dao = database.smsDao()
            DatabaseStats(
                totalMessages = dao.getTotalMessageCount(),
                unsyncedMessages = dao.getUnsyncedCount(),
                syncedMessages = dao.getSyncedCount(),
                oldestMessage = dao.getOldestMessageTimestamp() ?: 0L,
                newestMessage = dao.getNewestMessageTimestamp() ?: 0L,
                databaseSizeBytes = getDatabaseSize()
            )
        } catch (_: Exception) {
            Log.e(TAG, "Failed to get database stats")
            DatabaseStats()
        }
    }

    private fun getDatabaseSize(): Long {
        return try {
            // Approximate database size calculation
            // In a real implementation, you'd check the actual DB file size
            0L
        } catch (_: Exception) {
            0L
        }
    }

    data class DatabaseStats(
        val totalMessages: Int = 0,
        val unsyncedMessages: Int = 0,
        val syncedMessages: Int = 0,
        val oldestMessage: Long = 0L,
        val newestMessage: Long = 0L,
        val databaseSizeBytes: Long = 0L
    ) {
        @Suppress("unused")
        val syncPercentage: Float
            get() = if (totalMessages > 0) (syncedMessages.toFloat() / totalMessages * 100) else 0f
    }
}
