package com.example.smslogger.data.model

import kotlinx.serialization.Serializable

/**
 * Error response data class matching the server authentication API (#50).
 *
 * Received from POST /api/auth/login on failure (4xx / 5xx HTTP status).
 * [message] contains a human-readable explanation of the error.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null
)

