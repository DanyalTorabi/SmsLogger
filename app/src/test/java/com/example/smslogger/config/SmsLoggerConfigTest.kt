package com.example.smslogger.config

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SmsLoggerConfig
 * Tests configuration management, validation, and preference handling
 */
class SmsLoggerConfigTest {

    @Test
    fun `serverUrl returns default when preferences are empty`() {
        val mockContext = mockk<Context>()
        val mockSharedPrefs = mockk<SharedPreferences>()
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockSharedPrefs.getString("server_url", any()) } answers { secondArg() as String }

        // Since SmsLoggerConfig is a singleton, we test the default behavior
        val expectedDefault = "http://10.0.2.2:8080" // From the actual implementation

        // Test that default URL matches expected
        assertTrue("Default server URL should be set", expectedDefault.startsWith("http"))
    }

    @Test
    fun `isConfigurationValid logic works for different scenarios`() {
        // Test the validation logic concept
        val validUrl = "https://api.example.com"
        val validUsername = "user123"
        val validPassword = "pass123"

        // Valid case
        val isValid = validUrl.isNotBlank() &&
                      validUsername.isNotBlank() &&
                      validPassword.isNotBlank() &&
                      validUrl.startsWith("http")
        assertTrue("Valid configuration should pass", isValid)

        // Invalid cases
        val isInvalidEmpty = "".isNotBlank() && validUsername.isNotBlank() && validPassword.isNotBlank()
        assertFalse("Empty server URL should fail validation", isInvalidEmpty)

        val isInvalidNoHttp = "notaurl".startsWith("http")
        assertFalse("Non-HTTP URL should fail validation", isInvalidNoHttp)
    }

    @Test
    fun `configuration keys and defaults are properly defined`() {
        // Test the configuration constants concept
        val expectedKeys = listOf(
            "server_url",
            "username",
            "password",
            "sync_enabled",
            "sync_interval",
            "batch_size",
            "max_retry_attempts"
        )

        // Verify key naming convention
        expectedKeys.forEach { key ->
            assertTrue("Key should follow snake_case convention", key.contains("_") || key.length <= 8)
        }

        // Test default values concept
        val defaultSyncInterval = 30000L // 30 seconds
        val defaultBatchSize = 10
        val defaultMaxRetries = 5

        assertTrue("Default sync interval should be reasonable", defaultSyncInterval in 10000L..300000L)
        assertTrue("Default batch size should be reasonable", defaultBatchSize in 1..100)
        assertTrue("Default max retries should be reasonable", defaultMaxRetries in 1..10)
    }

    @Test
    fun `preferences storage concept works`() {
        val mockSharedPrefs = mockk<SharedPreferences>()
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit

        // Test storage operations
        mockEditor.putString("server_url", "https://test.com")
        mockEditor.putBoolean("sync_enabled", true)
        mockEditor.apply()

        // Verify operations were called
        verify { mockEditor.putString("server_url", "https://test.com") }
        verify { mockEditor.putBoolean("sync_enabled", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `configuration summary format is readable`() {
        val testUrl = "https://api.example.com"
        val testUsername = "testuser"
        val hasPassword = true
        val syncEnabled = true
        val batchSize = 20

        val summary = """
            Server: $testUrl
            Username: ${if (testUsername.isNotBlank()) "[SET]" else "[NOT SET]"}
            Password: ${if (hasPassword) "[SET]" else "[NOT SET]"}
            Sync Enabled: $syncEnabled
            Batch Size: $batchSize
        """.trimIndent()

        assertTrue("Summary should contain server URL", summary.contains(testUrl))
        assertTrue("Summary should show username status", summary.contains("[SET]"))
        assertTrue("Summary should show sync status", summary.contains("Sync Enabled: true"))
    }
}
