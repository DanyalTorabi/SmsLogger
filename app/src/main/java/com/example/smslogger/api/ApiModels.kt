package com.example.smslogger.api

import kotlinx.serialization.Serializable

/**
 * Data model for SMS API request payload
 * Matches the API documentation requirements
 */
@Serializable
data class SmsApiRequest(
    val smsId: Long? = null,
    val smsTimestamp: Long,
    val eventTimestamp: Long? = null,
    val phoneNumber: String,
    val body: String,
    val eventType: String,
    val threadId: Long? = null,
    val dateSent: Long? = null,
    val person: String? = null
)

/**
 * Authentication request for login
 */
@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

/**
 * Authentication response containing JWT token
 */
@Serializable
data class AuthResponse(
    val token: String
)

/**
 * API error response
 */
@Serializable
data class ApiError(
    val error: String
)
