package com.example.smslogger

/**
 * Test utilities and shared test data for SMS Logger tests
 * Provides common test fixtures and helper methods
 */
object TestUtils {

    /**
     * Creates a sample SmsMessage for testing
     */
    fun createSampleSmsMessage(
        id: Long = 1L,
        smsId: Long? = 12345L,
        phoneNumber: String = "+1234567890",
        body: String = "Test SMS message",
        eventType: String = "RECEIVED",
        timestamp: Long = System.currentTimeMillis()
    ) = com.example.smslogger.data.SmsMessage(
        id = id,
        smsId = smsId,
        smsTimestamp = timestamp,
        eventTimestamp = timestamp + 1000,
        phoneNumber = phoneNumber,
        body = body,
        eventType = eventType,
        threadId = 1L,
        dateSent = timestamp - 1000,
        person = "Test Contact",
        syncedAt = null
    )

    /**
     * Creates a sample SmsApiRequest for testing
     */
    fun createSampleApiRequest(
        phoneNumber: String = "+1234567890",
        body: String = "Test message",
        eventType: String = "RECEIVED"
    ) = com.example.smslogger.api.SmsApiRequest(
        smsId = 12345L,
        smsTimestamp = System.currentTimeMillis(),
        eventTimestamp = System.currentTimeMillis(),
        phoneNumber = phoneNumber,
        body = body,
        eventType = eventType,
        threadId = 1L,
        dateSent = System.currentTimeMillis() - 1000,
        person = "Test Contact"
    )

    /**
     * Phone numbers for testing various formats
     */
    val TEST_PHONE_NUMBERS = listOf(
        "+1234567890",
        "1234567890",
        "(123) 456-7890",
        "123-456-7890",
        "123.456.7890",
        "+1 (123) 456-7890"
    )

    /**
     * Test SMS bodies with various content types
     */
    val TEST_SMS_BODIES = listOf(
        "Simple message",
        "Message with emoji 🎉📱",
        "Message with special chars: café, naïve, résumé",
        "Message with Chinese characters: 你好世界",
        "Message with quotes \"Hello World\"",
        "Message with newlines\nLine 2\nLine 3",
        "Very long message: " + "x".repeat(1000),
        ""  // Empty message
    )

    /**
     * Valid SMS event types
     */
    val VALID_EVENT_TYPES = listOf("RECEIVED", "SENT", "DELIVERED", "FAILED")

    /**
     * Creates a mock Android Context for testing
     */
    fun createMockContext(): android.content.Context {
        return io.mockk.mockk(relaxed = true)
    }
}
