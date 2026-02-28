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
 * Supports optional TOTP code for 2FA authentication
 */
@Serializable
data class AuthRequest(
    val username: String,
    val password: String,
    val totp_code: String? = null  // Optional TOTP code for 2FA
)

/**
 * Authentication response containing JWT token and user info.
 *
 * Supports two expiry representations (#50):
 * - [expires_at]: ISO-8601 timestamp returned by the server ("expires_at" field)
 * - [expiresIn]: duration in seconds (legacy / alternative servers)
 *
 * Both are optional so older server responses that only return [token] are still accepted.
 */
@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String? = null,
    val expires_at: String? = null,   // ISO-8601 expiry timestamp from server (#50)
    val expiresIn: Long? = null,      // Token validity in seconds (alternative format)
    val user: UserInfo? = null
)

/**
 * User information returned on successful authentication
 */
@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val email: String? = null,
    val createdAt: String? = null
)

/**
 * API error response
 * Enhanced with optional message and details for better error handling
 */
@Serializable
data class ApiError(
    val error: String,
    val message: String? = null,
    val details: Map<String, String>? = null
)
