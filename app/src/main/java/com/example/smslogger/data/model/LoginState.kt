package com.example.smslogger.data.model

import com.example.smslogger.api.UserInfo

/**
 * Sealed class representing login state during authentication flow
 * Used to model the business logic state separate from UI state
 *
 * States:
 * - Idle: No login attempt has been made
 * - Loading: Login request is in progress
 * - Success: User successfully authenticated
 * - Error: Authentication failed with specific error details
 */
sealed class LoginState {
    /**
     * Idle state - initial state before any login attempt
     */
    object Idle : LoginState()

    /**
     * Loading state - authentication request in progress
     */
    object Loading : LoginState()

    /**
     * Success state - user authenticated successfully
     * @param user User information returned from server
     */
    data class Success(val user: UserInfo) : LoginState()

    /**
     * Error state - authentication failed
     * @param message User-friendly error message to display to user
     * @param code Error code from server for logging/analytics (e.g., "INVALID_CREDENTIALS", "ACCOUNT_LOCKED")
     */
    data class Error(val message: String, val code: String? = null) : LoginState()
}

