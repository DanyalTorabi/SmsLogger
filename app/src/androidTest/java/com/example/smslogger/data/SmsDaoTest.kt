package com.example.smslogger.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SmsDao database operations
 * Tests CRUD operations, queries, and data integrity
 */
@RunWith(AndroidJUnit4::class)
class SmsDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var smsDao: SmsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        smsDao = database.smsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertSms_and_getAllSms() = runTest {
        val testMessage = createTestSmsMessage()

        smsDao.insertSms(testMessage)

        val messages = smsDao.getAllSmsList()
        assertEquals(1, messages.size)

        val retrievedMessage = messages[0]
        assertEquals(testMessage.phoneNumber, retrievedMessage.phoneNumber)
        assertEquals(testMessage.body, retrievedMessage.body)
        assertEquals(testMessage.eventType, retrievedMessage.eventType)
    }

    @Test
    fun insertMultipleSms_orderedByEventTimestamp() = runTest {
        val currentTime = System.currentTimeMillis()
        val message1 = createTestSmsMessage(eventTimestamp = currentTime - 2000)
        val message2 = createTestSmsMessage(eventTimestamp = currentTime - 1000)
        val message3 = createTestSmsMessage(eventTimestamp = currentTime)

        smsDao.insertSms(message1)
        smsDao.insertSms(message2)
        smsDao.insertSms(message3)

        val messages = smsDao.getAllSmsList()
        assertEquals(3, messages.size)

        // Should be ordered by eventTimestamp DESC (newest first)
        assertTrue(messages[0].eventTimestamp >= messages[1].eventTimestamp)
        assertTrue(messages[1].eventTimestamp >= messages[2].eventTimestamp)
    }

    @Test
    fun getAllSmsFlow_observesChanges() = runTest {
        smsDao.getAllSms().test {
            // Initial state should be empty
            val initialMessages = awaitItem()
            assertTrue(initialMessages.isEmpty())

            // Insert a message
            val testMessage = createTestSmsMessage()
            smsDao.insertSms(testMessage)

            // Should receive update
            val updatedMessages = awaitItem()
            assertEquals(1, updatedMessages.size)
            assertEquals(testMessage.phoneNumber, updatedMessages[0].phoneNumber)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getSmsByOriginalId_existingMessage() = runTest {
        val testMessage = createTestSmsMessage(smsId = 12345L)
        smsDao.insertSms(testMessage)

        val retrievedMessage = smsDao.getSmsByOriginalId(12345L)

        assertNotNull(retrievedMessage)
        assertEquals(testMessage.smsId, retrievedMessage!!.smsId)
        assertEquals(testMessage.phoneNumber, retrievedMessage.phoneNumber)
    }

    @Test
    fun getSmsByOriginalId_nonExistingMessage() = runTest {
        val retrievedMessage = smsDao.getSmsByOriginalId(99999L)
        assertNull(retrievedMessage)
    }

    @Test
    fun findSimilarSms_exactMatch() = runTest {
        val phoneNumber = "+1234567890"
        val body = "Test message"
        val timestamp = System.currentTimeMillis()

        val testMessage = createTestSmsMessage(
            phoneNumber = phoneNumber,
            body = body,
            smsTimestamp = timestamp
        )
        smsDao.insertSms(testMessage)

        val similarMessage = smsDao.findSimilarSms(phoneNumber, body, timestamp)

        assertNotNull(similarMessage)
        assertEquals(phoneNumber, similarMessage!!.phoneNumber)
        assertEquals(body, similarMessage.body)
    }

    @Test
    fun findSimilarSms_withinTimeThreshold() = runTest {
        val phoneNumber = "+1234567890"
        val body = "Test message"
        val timestamp = System.currentTimeMillis()

        val testMessage = createTestSmsMessage(
            phoneNumber = phoneNumber,
            body = body,
            smsTimestamp = timestamp
        )
        smsDao.insertSms(testMessage)

        // Search with timestamp 3 seconds different (within 5 second threshold)
        val similarMessage = smsDao.findSimilarSms(phoneNumber, body, timestamp + 3000)

        assertNotNull(similarMessage)
        assertEquals(phoneNumber, similarMessage!!.phoneNumber)
    }

    @Test
    fun findSimilarSms_outsideTimeThreshold() = runTest {
        val phoneNumber = "+1234567890"
        val body = "Test message"
        val timestamp = System.currentTimeMillis()

        val testMessage = createTestSmsMessage(
            phoneNumber = phoneNumber,
            body = body,
            smsTimestamp = timestamp
        )
        smsDao.insertSms(testMessage)

        // Search with timestamp 6 seconds different (outside 5 second threshold)
        val similarMessage = smsDao.findSimilarSms(phoneNumber, body, timestamp + 6000)

        assertNull(similarMessage)
    }

    @Test
    fun getUnsyncedSms_returnsUnsyncedOnly() = runTest {
        val syncedMessage = createTestSmsMessage(syncedAt = System.currentTimeMillis())
        val unsyncedMessage1 = createTestSmsMessage(eventTimestamp = System.currentTimeMillis() - 1000)
        val unsyncedMessage2 = createTestSmsMessage(eventTimestamp = System.currentTimeMillis())

        smsDao.insertSms(syncedMessage)
        smsDao.insertSms(unsyncedMessage1)
        smsDao.insertSms(unsyncedMessage2)

        val unsyncedMessages = smsDao.getUnsyncedSms()

        assertEquals(2, unsyncedMessages.size)
        assertTrue(unsyncedMessages.all { it.syncedAt == null })
        // Should be ordered by eventTimestamp ASC (oldest first)
        assertTrue(unsyncedMessages[0].eventTimestamp <= unsyncedMessages[1].eventTimestamp)
    }

    @Test
    fun markAsSynced_updatesSyncedAt() = runTest {
        val testMessage = createTestSmsMessage()
        smsDao.insertSms(testMessage)

        val insertedMessage = smsDao.getAllSmsList()[0]
        assertNull(insertedMessage.syncedAt)

        val syncTimestamp = System.currentTimeMillis()
        smsDao.markAsSynced(insertedMessage.id, syncTimestamp)

        val updatedMessage = smsDao.getAllSmsList()[0]
        assertEquals(syncTimestamp, updatedMessage.syncedAt)
    }

    @Test
    fun getUnsyncedCount_returnsCorrectCount() = runTest {
        val syncedMessage = createTestSmsMessage(syncedAt = System.currentTimeMillis())
        val unsyncedMessage1 = createTestSmsMessage()
        val unsyncedMessage2 = createTestSmsMessage()

        smsDao.insertSms(syncedMessage)
        smsDao.insertSms(unsyncedMessage1)
        smsDao.insertSms(unsyncedMessage2)

        val unsyncedCount = smsDao.getUnsyncedCount()
        assertEquals(2, unsyncedCount)
    }

    @Test
    fun insertSms_onConflictIgnore() = runTest {
        val testMessage1 = createTestSmsMessage(smsId = 123L)
        val testMessage2 = createTestSmsMessage(smsId = 123L, body = "Different body")

        smsDao.insertSms(testMessage1)
        smsDao.insertSms(testMessage2) // Should be ignored due to conflict strategy

        val messages = smsDao.getAllSmsList()
        assertEquals(1, messages.size)
        assertEquals(testMessage1.body, messages[0].body)
    }

    private fun createTestSmsMessage(
        smsId: Long? = 12345L,
        phoneNumber: String = "+1234567890",
        body: String = "Test message",
        eventType: String = "RECEIVED",
        smsTimestamp: Long = System.currentTimeMillis(),
        eventTimestamp: Long = System.currentTimeMillis(),
        syncedAt: Long? = null
    ): SmsMessage {
        return SmsMessage(
            smsId = smsId,
            smsTimestamp = smsTimestamp,
            eventTimestamp = eventTimestamp,
            phoneNumber = phoneNumber,
            body = body,
            eventType = eventType,
            threadId = 1L,
            dateSent = smsTimestamp - 1000,
            person = null,
            syncedAt = syncedAt
        )
    }
}
