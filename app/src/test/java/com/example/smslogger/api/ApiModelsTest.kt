package com.example.smslogger.api

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for API Models
 * Tests serialization, deserialization, and data validation
 */
class ApiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun `SmsApiRequest serializes correctly with all fields`() {
        val request = SmsApiRequest(
            smsId = 12345L,
            smsTimestamp = 1640995200000L,
            eventTimestamp = 1640995201000L,
            phoneNumber = "+1234567890",
            body = "Test message",
            eventType = "RECEIVED",
            threadId = 1L,
            dateSent = 1640995199000L,
            person = "John Doe"
        )

        val jsonString = json.encodeToString(SmsApiRequest.serializer(), request)

        assertTrue(jsonString.contains("\"smsId\":12345"))
        assertTrue(jsonString.contains("\"phoneNumber\":\"+1234567890\""))
        assertTrue(jsonString.contains("\"body\":\"Test message\""))
        assertTrue(jsonString.contains("\"eventType\":\"RECEIVED\""))
    }

    @Test
    fun `SmsApiRequest serializes correctly with minimal fields`() {
        val request = SmsApiRequest(
            smsId = null,
            smsTimestamp = 1640995200000L,
            eventTimestamp = null,
            phoneNumber = "+1234567890",
            body = "Test message",
            eventType = "RECEIVED",
            threadId = null,
            dateSent = null,
            person = null
        )

        val jsonString = json.encodeToString(SmsApiRequest.serializer(), request)

        assertTrue(jsonString.contains("\"smsTimestamp\":1640995200000"))
        assertTrue(jsonString.contains("\"phoneNumber\":\"+1234567890\""))
        assertTrue(jsonString.contains("\"body\":\"Test message\""))
        assertTrue(jsonString.contains("\"eventType\":\"RECEIVED\""))
        assertFalse(jsonString.contains("\"smsId\"")) // Should not include null fields
    }

    @Test
    fun `SmsApiRequest deserializes correctly`() {
        val jsonString = """
            {
                "smsId": 12345,
                "smsTimestamp": 1640995200000,
                "eventTimestamp": 1640995201000,
                "phoneNumber": "+1234567890",
                "body": "Test message",
                "eventType": "RECEIVED",
                "threadId": 1,
                "dateSent": 1640995199000,
                "person": "John Doe"
            }
        """.trimIndent()

        val request = json.decodeFromString(SmsApiRequest.serializer(), jsonString)

        assertEquals(12345L, request.smsId)
        assertEquals(1640995200000L, request.smsTimestamp)
        assertEquals(1640995201000L, request.eventTimestamp)
        assertEquals("+1234567890", request.phoneNumber)
        assertEquals("Test message", request.body)
        assertEquals("RECEIVED", request.eventType)
        assertEquals(1L, request.threadId)
        assertEquals(1640995199000L, request.dateSent)
        assertEquals("John Doe", request.person)
    }

    @Test
    fun `SmsApiRequest handles special characters in body`() {
        val request = SmsApiRequest(
            smsId = 1L,
            smsTimestamp = System.currentTimeMillis(),
            phoneNumber = "+1234567890",
            body = "Special chars: 🎉 emoji, café, naïve, résumé, 中文, \"quotes\", \\backslash",
            eventType = "RECEIVED"
        )

        val jsonString = json.encodeToString(SmsApiRequest.serializer(), request)
        val deserialized = json.decodeFromString(SmsApiRequest.serializer(), jsonString)

        assertEquals(request.body, deserialized.body)
    }

    @Test
    fun `AuthRequest serializes correctly`() {
        val authRequest = AuthRequest(
            username = "testuser",
            password = "testpass123"
        )

        val jsonString = json.encodeToString(AuthRequest.serializer(), authRequest)

        assertTrue(jsonString.contains("\"username\":\"testuser\""))
        assertTrue(jsonString.contains("\"password\":\"testpass123\""))
    }

    @Test
    fun `AuthRequest deserializes correctly`() {
        val jsonString = """
            {
                "username": "testuser",
                "password": "testpass123"
            }
        """.trimIndent()

        val authRequest = json.decodeFromString(AuthRequest.serializer(), jsonString)

        assertEquals("testuser", authRequest.username)
        assertEquals("testpass123", authRequest.password)
    }

    @Test
    fun `AuthResponse serializes and deserializes correctly`() {
        val authResponse = AuthResponse(token = "jwt.token.here")

        val jsonString = json.encodeToString(AuthResponse.serializer(), authResponse)
        val deserialized = json.decodeFromString(AuthResponse.serializer(), jsonString)

        assertEquals("jwt.token.here", deserialized.token)
        assertTrue(jsonString.contains("\"token\":\"jwt.token.here\""))
    }

    @Test
    fun `ApiError serializes and deserializes correctly`() {
        val apiError = ApiError(error = "Authentication failed")

        val jsonString = json.encodeToString(ApiError.serializer(), apiError)
        val deserialized = json.decodeFromString(ApiError.serializer(), jsonString)

        assertEquals("Authentication failed", deserialized.error)
        assertTrue(jsonString.contains("\"error\":\"Authentication failed\""))
    }

    @Test
    fun `SmsApiRequest handles empty strings`() {
        val request = SmsApiRequest(
            smsId = 1L,
            smsTimestamp = System.currentTimeMillis(),
            phoneNumber = "",
            body = "",
            eventType = "RECEIVED"
        )

        val jsonString = json.encodeToString(SmsApiRequest.serializer(), request)
        val deserialized = json.decodeFromString(SmsApiRequest.serializer(), jsonString)

        assertEquals("", deserialized.phoneNumber)
        assertEquals("", deserialized.body)
    }

    @Test
    fun `SmsApiRequest ignores unknown fields during deserialization`() {
        val jsonWithExtraFields = """
            {
                "smsId": 12345,
                "smsTimestamp": 1640995200000,
                "phoneNumber": "+1234567890",
                "body": "Test message",
                "eventType": "RECEIVED",
                "unknownField": "should be ignored",
                "anotherUnknownField": 999
            }
        """.trimIndent()

        val request = json.decodeFromString(SmsApiRequest.serializer(), jsonWithExtraFields)

        assertEquals(12345L, request.smsId)
        assertEquals("+1234567890", request.phoneNumber)
        assertEquals("Test message", request.body)
        assertEquals("RECEIVED", request.eventType)
    }

    @Test
    fun `SmsApiRequest validates event types`() {
        val validEventTypes = listOf("RECEIVED", "SENT", "DELIVERED", "FAILED")

        validEventTypes.forEach { eventType ->
            val request = SmsApiRequest(
                smsId = 1L,
                smsTimestamp = System.currentTimeMillis(),
                phoneNumber = "+1234567890",
                body = "Test",
                eventType = eventType
            )

            assertEquals(eventType, request.eventType)

            val jsonString = json.encodeToString(SmsApiRequest.serializer(), request)
            val deserialized = json.decodeFromString(SmsApiRequest.serializer(), jsonString)

            assertEquals(eventType, deserialized.eventType)
        }
    }

    @Test
    fun `all models have proper data class functionality`() {
        // Test SmsApiRequest
        val smsRequest1 = SmsApiRequest(
            smsId = 1L,
            smsTimestamp = 1640995200000L,
            phoneNumber = "+1234567890",
            body = "Test",
            eventType = "RECEIVED"
        )
        val smsRequest2 = smsRequest1.copy(body = "Updated test")

        assertEquals(smsRequest1.phoneNumber, smsRequest2.phoneNumber)
        assertNotEquals(smsRequest1.body, smsRequest2.body)

        // Test AuthRequest
        val authRequest1 = AuthRequest("user1", "pass1")
        val authRequest2 = authRequest1.copy(password = "pass2")

        assertEquals(authRequest1.username, authRequest2.username)
        assertNotEquals(authRequest1.password, authRequest2.password)

        // Test AuthResponse
        val authResponse1 = AuthResponse("token1")
        val authResponse2 = authResponse1.copy(token = "token2")

        assertNotEquals(authResponse1.token, authResponse2.token)

        // Test ApiError
        val apiError1 = ApiError("error1")
        val apiError2 = apiError1.copy(error = "error2")

        assertNotEquals(apiError1.error, apiError2.error)
    }
}
