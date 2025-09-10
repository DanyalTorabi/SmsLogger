package com.example.smslogger

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SMS Logger application
 * Tests end-to-end workflows and component integration
 */
@RunWith(AndroidJUnit4::class)
class SmsLoggerIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `application context is available`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertNotNull(context)
        assertEquals("com.example.smslogger", context.packageName)
    }

    @Test
    fun `database can be created and accessed`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = com.example.smslogger.data.AppDatabase.getDatabase(context)

        assertNotNull(database)
        assertNotNull(database.smsDao())
    }

    @Test
    fun `configuration can be initialized`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = com.example.smslogger.config.SmsLoggerConfig.getInstance(context)

        assertNotNull(config)
        // Test default values
        assertFalse(config.serverUrl.isEmpty())
        assertTrue(config.syncEnabled)
    }

    @Test
    fun `error handler can categorize different exception types`() {
        val errorHandler = com.example.smslogger.util.ErrorHandler("IntegrationTest")

        val networkError = errorHandler.categorizeError(java.net.SocketTimeoutException("Timeout"))
        assertEquals(com.example.smslogger.util.ErrorHandler.ErrorType.NETWORK_TIMEOUT, networkError.type)

        val unknownError = errorHandler.categorizeError(RuntimeException("Unknown"))
        assertEquals(com.example.smslogger.util.ErrorHandler.ErrorType.UNKNOWN, unknownError.type)
    }
}
