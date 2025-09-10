package com.example.smslogger.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Unit tests for ErrorHandler
 * Tests error categorization, recovery strategies, and retry logic
 */
class ErrorHandlerTest {

    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setUp() {
        errorHandler = ErrorHandler("TestTag")
    }

    @Test
    fun `categorizeError handles SocketTimeoutException`() {
        val exception = SocketTimeoutException("Connection timed out")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.NETWORK_TIMEOUT, errorInfo.type)
        assertEquals("Network request timed out", errorInfo.message)
        assertTrue(errorInfo.isRetryable)
        assertEquals(5000L, errorInfo.suggestedDelay)
    }

    @Test
    fun `categorizeError handles UnknownHostException`() {
        val exception = UnknownHostException("api.example.com")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.NETWORK_UNAVAILABLE, errorInfo.type)
        assertTrue(errorInfo.message.contains("Cannot resolve server host"))
        assertTrue(errorInfo.isRetryable)
        assertEquals(30000L, errorInfo.suggestedDelay)
    }

    @Test
    fun `categorizeError handles SSLException`() {
        val exception = SSLException("SSL handshake failed")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.SERVER_ERROR, errorInfo.type)
        assertTrue(errorInfo.message.contains("SSL/TLS connection failed"))
        assertFalse(errorInfo.isRetryable)
    }

    @Test
    fun `categorizeError handles SecurityException`() {
        val exception = SecurityException("Permission denied")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.PERMISSION_DENIED, errorInfo.type)
        assertTrue(errorInfo.message.contains("Permission denied"))
        assertFalse(errorInfo.isRetryable)
    }

    @Test
    fun `categorizeError handles generic IOException`() {
        val exception = IOException("Generic IO error")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.NETWORK_UNAVAILABLE, errorInfo.type)
        assertTrue(errorInfo.message.contains("Network I/O error"))
        assertTrue(errorInfo.isRetryable)
        assertEquals(10000L, errorInfo.suggestedDelay)
    }

    @Test
    fun `categorizeError handles unknown exceptions`() {
        val exception = RuntimeException("Unknown error")

        val errorInfo = errorHandler.categorizeError(exception)

        assertEquals(ErrorHandler.ErrorType.UNKNOWN, errorInfo.type)
        assertTrue(errorInfo.message.contains("Unexpected error"))
        assertTrue(errorInfo.isRetryable) // Changed from false to true
        assertEquals(15000L, errorInfo.suggestedDelay)
    }

    @Test
    fun `shouldRetry returns true for retryable errors within limit`() {
        val retryableError = ErrorHandler.ErrorInfo(
            ErrorHandler.ErrorType.NETWORK_TIMEOUT,
            "Timeout",
            isRetryable = true,
            suggestedDelay = 1000L
        )

        assertTrue(errorHandler.shouldRetry(retryableError, currentAttempt = 1, maxAttempts = 3))
        assertTrue(errorHandler.shouldRetry(retryableError, currentAttempt = 2, maxAttempts = 3))
        assertFalse(errorHandler.shouldRetry(retryableError, currentAttempt = 3, maxAttempts = 3))
    }

    @Test
    fun `shouldRetry returns false for non-retryable errors`() {
        val nonRetryableError = ErrorHandler.ErrorInfo(
            ErrorHandler.ErrorType.PERMISSION_DENIED,
            "No permission",
            isRetryable = false
        )

        assertFalse(errorHandler.shouldRetry(nonRetryableError, currentAttempt = 1, maxAttempts = 3))
    }

    @Test
    fun `calculateBackoffDelay increases exponentially`() {
        val baseDelay = 1000L

        assertEquals(baseDelay, errorHandler.calculateBackoffDelay(0, baseDelay))
        assertEquals(baseDelay * 2, errorHandler.calculateBackoffDelay(1, baseDelay))
        assertEquals(baseDelay * 4, errorHandler.calculateBackoffDelay(2, baseDelay))
        assertEquals(baseDelay * 8, errorHandler.calculateBackoffDelay(3, baseDelay))
    }

    @Test
    fun `calculateBackoffDelay respects maximum delay`() {
        val baseDelay = 1000L
        val maxDelay = 5000L

        val largeAttemptDelay = errorHandler.calculateBackoffDelay(10, baseDelay, maxDelay)
        assertTrue(largeAttemptDelay <= maxDelay)
    }

    @Test
    fun `error type enum has all expected values`() {
        val expectedTypes = listOf(
            ErrorHandler.ErrorType.NETWORK_TIMEOUT,
            ErrorHandler.ErrorType.NETWORK_UNAVAILABLE,
            ErrorHandler.ErrorType.SERVER_ERROR,
            ErrorHandler.ErrorType.AUTHENTICATION_FAILED,
            ErrorHandler.ErrorType.DATABASE_ERROR,
            ErrorHandler.ErrorType.PERMISSION_DENIED,
            ErrorHandler.ErrorType.CONFIGURATION_ERROR,
            ErrorHandler.ErrorType.UNKNOWN
        )

        assertEquals(8, ErrorHandler.ErrorType.values().size)
        expectedTypes.forEach { expectedType ->
            assertTrue(ErrorHandler.ErrorType.values().contains(expectedType))
        }
    }
}
