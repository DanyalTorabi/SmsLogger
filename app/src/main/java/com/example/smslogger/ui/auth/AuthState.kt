package com.example.smslogger.ui.auth

import com.example.smslogger.api.UserInfo

/**
 * Sealed class representing authentication state
 * Used by AuthViewModel to communicate UI state to LoginActivity
 */
sealed class AuthState {
    /**
     * Initial state - not yet attempted login
     */
    object Initial : AuthState()

    /**
     * Loading state - authentication in progress
     */
    object Loading : AuthState()

    /**
     * Success state - user authenticated successfully
     * @param user User information from server
     */
    data class Success(val user: UserInfo) : AuthState()

    /**
     * Error state - authentication failed
     * @param message User-friendly error message
     * @param code Error code from server (optional)
     */
    data class Error(val message: String, val code: String? = null) : AuthState()
}

