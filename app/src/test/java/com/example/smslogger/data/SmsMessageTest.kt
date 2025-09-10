package com.example.smslogger.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SmsMessage data class
 * Tests data validation, mapping, and business logic
 */
class SmsMessageTest {

    @Test
    fun `smsMessage creation with all fields`() {
        val message = SmsMessage(
            id = 1L,
            smsId = 12345L,
            smsTimestamp = 1640995200000L, // Jan 1, 2022
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Test message",
            eventType = "RECEIVED",
            threadId = 1L,
            dateSent = 1640995199000L,
            person = "John Doe",
            syncedAt = null
        )

        assertEquals(1L, message.id)
        assertEquals(12345L, message.smsId)
        assertEquals(1640995200000L, message.smsTimestamp)
        assertEquals(1640995201000L, message.eventTimestamp)
        assertEquals("+1234567890", message.phoneNumber)
        assertEquals("Test message", message.body)
        assertEquals("RECEIVED", message.eventType)
        assertEquals(1L, message.threadId)
        assertEquals(1640995199000L, message.dateSent)
        assertEquals("John Doe", message.person)
        assertNull(message.syncedAt)
    }

    @Test
    fun `smsMessage creation with minimal required fields`() {
        val message = SmsMessage(
            smsId = null,
            smsTimestamp = 1640995200000L,
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Test message",
            eventType = "RECEIVED",
            threadId = null,
            dateSent = null,
            person = null
        )

        assertEquals(0L, message.id) // Default auto-generated value
        assertNull(message.smsId)
        assertEquals("+1234567890", message.phoneNumber)
        assertEquals("Test message", message.body)
        assertEquals("RECEIVED", message.eventType)
        assertNull(message.threadId)
        assertNull(message.dateSent)
        assertNull(message.person)
        assertNull(message.syncedAt)
    }

    @Test
    fun `smsMessage with empty body`() {
        val message = SmsMessage(
            smsId = 123L,
            smsTimestamp = System.currentTimeMillis(),
            eventTimestamp = System.currentTimeMillis(),
            phoneNumber = "+1234567890",
            body = "",
            eventType = "RECEIVED",
            threadId = null,
            dateSent = null,
            person = null
        )

        assertTrue(message.body.isEmpty())
    }

    @Test
    fun `smsMessage event types validation`() {
        val validEventTypes = listOf("RECEIVED", "SENT", "DELIVERED", "FAILED")

        validEventTypes.forEach { eventType ->
            val message = SmsMessage(
                smsId = 123L,
                smsTimestamp = System.currentTimeMillis(),
                eventTimestamp = System.currentTimeMillis(),
                phoneNumber = "+1234567890",
                body = "Test",
                eventType = eventType,
                threadId = null,
                dateSent = null,
                person = null
            )

            assertEquals(eventType, message.eventType)
        }
    }

    @Test
    fun `smsMessage timestamp validation`() {
        val currentTime = System.currentTimeMillis()
        val message = SmsMessage(
            smsId = 123L,
            smsTimestamp = currentTime - 1000,
            eventTimestamp = currentTime,
            phoneNumber = "+1234567890",
            body = "Test",
            eventType = "RECEIVED",
            threadId = null,
            dateSent = null,
            person = null
        )

        assertTrue(message.eventTimestamp >= message.smsTimestamp)
    }

    @Test
    fun `smsMessage equality and hashcode`() {
        val message1 = SmsMessage(
            id = 1L,
            smsId = 123L,
            smsTimestamp = 1640995200000L,
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Test",
            eventType = "RECEIVED",
            threadId = 1L,
            dateSent = null,
            person = null
        )

        val message2 = SmsMessage(
            id = 1L,
            smsId = 123L,
            smsTimestamp = 1640995200000L,
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Test",
            eventType = "RECEIVED",
            threadId = 1L,
            dateSent = null,
            person = null
        )

        assertEquals(message1, message2)
        assertEquals(message1.hashCode(), message2.hashCode())
    }

    @Test
    fun `smsMessage copy functionality`() {
        val original = SmsMessage(
            id = 1L,
            smsId = 123L,
            smsTimestamp = 1640995200000L,
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Original message",
            eventType = "RECEIVED",
            threadId = 1L,
            dateSent = null,
            person = null
        )

        val copy = original.copy(
            body = "Updated message",
            syncedAt = System.currentTimeMillis()
        )

        assertEquals(original.id, copy.id)
        assertEquals(original.phoneNumber, copy.phoneNumber)
        assertEquals("Updated message", copy.body)
        assertNotNull(copy.syncedAt)
        assertNull(original.syncedAt)
    }

    @Test
    fun `phone number formats`() {
        val phoneFormats = listOf(
            "+1234567890",
            "1234567890",
            "(123) 456-7890",
            "123-456-7890",
            "123.456.7890"
        )

        phoneFormats.forEach { phoneNumber ->
            val message = SmsMessage(
                smsId = 123L,
                smsTimestamp = System.currentTimeMillis(),
                eventTimestamp = System.currentTimeMillis(),
                phoneNumber = phoneNumber,
                body = "Test",
                eventType = "RECEIVED",
                threadId = null,
                dateSent = null,
                person = null
            )

            assertEquals(phoneNumber, message.phoneNumber)
        }
    }
}
