package com.example.smslogger.data // Replace with your actual package name

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // Optional: For reactive updates

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if SMS with same event ID (though unlikely with autogenerate)
    suspend fun insertSms(smsMessage: SmsMessage)

    @Query("SELECT * FROM sms_log ORDER BY eventTimestamp DESC")
    fun getAllSms(): Flow<List<SmsMessage>> // Observe changes with Flow

    @Query("SELECT * FROM sms_log ORDER BY eventTimestamp DESC")
    suspend fun getAllSmsList(): List<SmsMessage> // For one-time fetch

    /**
     * Fetches an SMS entry from the local database by its original Telephony.Sms._ID.
     * Returns null if no such SMS is found.
     */
    @Query("SELECT * FROM sms_log WHERE smsId = :originalSmsId LIMIT 1")
    suspend fun getSmsByOriginalId(originalSmsId: Long): SmsMessage? // Added this method

    /**
     * Checks if there is already an SMS with the same sender, body, and approximately the same timestamp.
     * This is used as a backup check when the provider SMS ID is not available.
     */
    @Query("SELECT * FROM sms_log WHERE phoneNumber = :phoneNumber AND body = :body AND ABS(smsTimestamp - :timestamp) < 5000 LIMIT 1")
    suspend fun findSimilarSms(phoneNumber: String, body: String, timestamp: Long): SmsMessage?
}