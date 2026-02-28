package com.example.smslogger.data.model

import com.example.smslogger.api.UserInfo
import kotlinx.serialization.Serializable

/**
 * Login response data class matching the server authentication API (#50).
 *
 * Received from POST /api/auth/login on success.
 * [expires_at] is an ISO-8601 timestamp (e.g. "2026-03-01T12:00:00Z").
 * [user] contains the authenticated user's profile.
 */
@Serializable
data class LoginResponse(
    val token: String,
    val expires_at: String? = null,
    val user: UserInfo? = null
)

