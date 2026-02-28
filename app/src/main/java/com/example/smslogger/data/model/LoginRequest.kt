package com.example.smslogger.data.model

import kotlinx.serialization.Serializable

/**
 * Login request data class matching the server authentication API (#50).
 *
 * Sent as JSON to POST /api/auth/login.
 * [totp_code] is optional and only included when the user provides a 2FA code.
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val totp_code: String? = null
)

