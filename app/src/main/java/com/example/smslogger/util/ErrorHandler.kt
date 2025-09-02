package com.example.smslogger.util

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Enhanced error handling and recovery strategies for SMS Logger
 */
@Suppress("unused") // Utility class for future integration
class ErrorHandler(private val tag: String) {

    enum class ErrorType {
        NETWORK_TIMEOUT,
        NETWORK_UNAVAILABLE,
        SERVER_ERROR,
        @Suppress("unused") AUTHENTICATION_FAILED,
        @Suppress("unused") DATABASE_ERROR,
        PERMISSION_DENIED,
        CONFIGURATION_ERROR,
        UNKNOWN
    }

    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val isRetryable: Boolean,
        val suggestedDelay: Long = 0L
    )

    @Suppress("unused")
    fun categorizeError(exception: Throwable): ErrorInfo {
        return when (exception) {
            is SocketTimeoutException -> ErrorInfo(
                ErrorType.NETWORK_TIMEOUT,
                "Network request timed out",
                isRetryable = true,
                suggestedDelay = 5000L
            )

            is UnknownHostException -> ErrorInfo(
                ErrorType.NETWORK_UNAVAILABLE,
                "Cannot resolve server host: ${exception.message}",
                isRetryable = true,
                suggestedDelay = 30000L // Longer delay for DNS issues
            )

            is SSLException -> ErrorInfo(
                ErrorType.SERVER_ERROR,
                "SSL/TLS connection failed: ${exception.message}",
                isRetryable = false // Usually a configuration issue
            )

            is IOException -> ErrorInfo(
                ErrorType.NETWORK_UNAVAILABLE,
                "Network I/O error: ${exception.message}",
                isRetryable = true,
                suggestedDelay = 10000L
            )

            is SecurityException -> ErrorInfo(
                ErrorType.PERMISSION_DENIED,
                "Permission denied: ${exception.message}",
                isRetryable = false
            )

            else -> ErrorInfo(
                ErrorType.UNKNOWN,
                "Unexpected error: ${exception.message}",
                isRetryable = true,
                suggestedDelay = 15000L
            )
        }
    }

    @Suppress("unused")
    fun logError(errorInfo: ErrorInfo, exception: Throwable? = null) {
        val logMessage = buildString {
            append("[${errorInfo.type}] ${errorInfo.message}")
            append(" | Retryable: ${errorInfo.isRetryable}")
            if (errorInfo.suggestedDelay > 0) {
                append(" | Suggested delay: ${errorInfo.suggestedDelay}ms")
            }
        }

        when (errorInfo.type) {
            ErrorType.PERMISSION_DENIED,
            ErrorType.CONFIGURATION_ERROR -> Log.e(tag, logMessage, exception)

            ErrorType.NETWORK_TIMEOUT,
            ErrorType.NETWORK_UNAVAILABLE -> Log.w(tag, logMessage, exception)

            else -> Log.e(tag, logMessage, exception)
        }
    }

    @Suppress("unused")
    fun shouldRetry(errorInfo: ErrorInfo, currentAttempt: Int, maxAttempts: Int): Boolean {
        return errorInfo.isRetryable && currentAttempt < maxAttempts
    }

    @Suppress("unused")
    fun calculateBackoffDelay(
        attempt: Int,
        baseDelay: Long = 1000L,
        maxDelay: Long = 60000L,
        errorInfo: ErrorInfo? = null
    ): Long {
        val exponentialDelay = (baseDelay * Math.pow(2.0, attempt.toDouble())).toLong()
        val cappedDelay = minOf(exponentialDelay, maxDelay)

        // Use suggested delay from error if available
        val suggestedDelay = errorInfo?.suggestedDelay ?: 0L

        return maxOf(cappedDelay, suggestedDelay)
    }
}
