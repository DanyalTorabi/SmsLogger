package com.example.smslogger.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smslogger.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication logic
 * Handles login, credential validation, and secure storage via AuthRepository
 *
 * State Management:
 * - authState: StateFlow<AuthState> - Primary authentication state (Initial, Loading, Success, Error)
 * - errorMessage: StateFlow<String?> - Separate error message for UI display
 *
 * Dependencies: #46 (KeystoreCredentialManager), #50 (API Models), AuthRepository
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val TAG = "AuthViewModel"

    // Mutable state for internal use
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Public immutable state for UI observation
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Login with username and password
     * Optionally supports TOTP code for 2FA authentication
     *
     * Input Validation:
     * - Username must not be blank
     * - Password must not be blank and at least 6 characters
     * - TOTP code (if provided) must be 6 digits
     *
     * @param username User's username or email
     * @param password User's password
     * @param totpCode Optional 6-digit TOTP code for 2FA authentication
     * @param rememberMe Whether to save password for future logins
     */
    fun login(
        username: String,
        password: String,
        totpCode: String? = null,
        rememberMe: Boolean = false
    ) {
        // Input validation
        val validationError = validateLoginInput(username, password, totpCode)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            _errorMessage.value = validationError
            return
        }

        // Set loading state
        _authState.value = AuthState.Loading
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.login(
                username = username,
                password = password,
                totpCode = totpCode,
                rememberMe = rememberMe
            )

            when (result) {
                is AuthRepository.LoginResult.Success -> {
                    Log.d(TAG, "Login successful for user: $username")
                    _authState.value = AuthState.Success(result.user)
                    _errorMessage.value = null
                }

                is AuthRepository.LoginResult.Error -> {
                    Log.w(TAG, "Login failed for user: $username - Code: ${result.code}")
                    val userMessage = mapErrorCodeToMessage(result.code, result.httpCode)
                    _authState.value = AuthState.Error(
                        message = userMessage,
                        code = result.code
                    )
                    _errorMessage.value = userMessage
                }
            }
        }
    }

    /**
     * Validate login input before attempting authentication
     * @return Error message if validation fails, null if valid
     */
    private fun validateLoginInput(
        username: String,
        password: String,
        totpCode: String?
    ): String? {
        return when {
            username.isBlank() -> "Username is required"
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            totpCode != null && !isTotpCodeValid(totpCode) -> "TOTP code must be 6 digits"
            else -> null
        }
    }

    /**
     * Validate TOTP code format
     * Must be exactly 6 digits
     */
    private fun isTotpCodeValid(totpCode: String): Boolean {
        return totpCode.matches(Regex("^\\d{6}$"))
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isAuthenticated()
    }

    /**
     * Check if user is already logged in (alias for isLoggedIn for backward compatibility)
     */
    fun checkAuthStatus(): Boolean {
        return isLoggedIn()
    }

    /**
     * Get stored username for pre-filling login form
     */
    fun getSavedUsername(): String? {
        return authRepository.getCurrentUsername()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
        _errorMessage.value = null
    }

    /**
     * Logout - clear all stored credentials
     */
    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Initial
        _errorMessage.value = null
        Log.d(TAG, "User logged out")
    }

    /**
     * Map server error code to user-friendly message
     * Handles all error scenarios from issue #47:
     * - Invalid credentials
     * - Account locked (30-minute retry window)
     * - Invalid TOTP/2FA code
     * - Network errors
     * - Server errors
     *
     * @param errorCode Error code from AuthRepository
     * @param httpCode HTTP status code (if applicable)
     * @return User-friendly error message for display
     */
    private fun mapErrorCodeToMessage(errorCode: String, httpCode: Int? = null): String {
        return when {
            // Handle HTTP status codes first
            httpCode == 401 -> "Invalid username or password"
            httpCode == 403 -> {
                // Could be account locked or other forbidden reason
                if (errorCode == "ACCOUNT_LOCKED") {
                    "Account locked. Try again in 30 minutes"
                } else {
                    "Access forbidden. Please contact support"
                }
            }
            httpCode == 500 -> "Server error. Please try again later"

            // Handle specific error codes
            errorCode == "INVALID_CREDENTIALS" -> "Invalid username or password"
            errorCode == "ACCOUNT_LOCKED" -> "Account locked. Try again in 30 minutes"
            errorCode == "INVALID_TOTP" -> "Invalid 2FA code"
            errorCode == "TOTP_REQUIRED" -> "Two-factor authentication code required"
            errorCode == "ACCOUNT_INACTIVE" -> "Your account has been deactivated"
            errorCode == "NETWORK_ERROR" -> "Network error. Please try again"
            errorCode == "SERVER_ERROR" -> "Server temporarily unavailable"

            // Default
            else -> "An error occurred. Please try again"
        }
    }
}

