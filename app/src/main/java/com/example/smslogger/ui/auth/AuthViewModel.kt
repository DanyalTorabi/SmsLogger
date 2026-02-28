package com.example.smslogger.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smslogger.data.exception.AccountInactiveException
import com.example.smslogger.data.exception.AccountLockedException
import com.example.smslogger.data.exception.AuthException
import com.example.smslogger.data.exception.InvalidCredentialsException
import com.example.smslogger.data.exception.NetworkException
import com.example.smslogger.data.exception.ServerErrorException
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
    private val _isLocked = MutableStateFlow(false)

    // Public immutable state for UI observation
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** True when the server has indicated the account is locked (#55). */
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Local failed-attempt counter for security event logging (#55)
    private var failedAttempts = 0

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

            result.fold(
                onSuccess = { loginResponse ->
                    Log.d(TAG, "Login successful")
                    failedAttempts = 0
                    _isLocked.value = false
                    _authState.value = AuthState.Success(
                        loginResponse.user ?: com.example.smslogger.api.UserInfo(
                            id = username, username = username, email = null, createdAt = null
                        )
                    )
                    _errorMessage.value = null
                },
                onFailure = { throwable ->
                    val userMessage = mapAuthExceptionToMessage(throwable)
                    failedAttempts++
                    Log.w(TAG, "Login failed (attempt $failedAttempts): ${throwable.javaClass.simpleName}")
                    // Track account-locked state for UI (#55)
                    _isLocked.value = throwable is AccountLockedException
                    _authState.value = AuthState.Error(message = userMessage)
                    _errorMessage.value = userMessage
                }
            )
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
        _isLocked.value = false
        _errorMessage.value = null
    }

    /**
     * Logout - clear all stored credentials
     */
    fun logout() {
        authRepository.logout()
        failedAttempts = 0
        _isLocked.value = false
        _authState.value = AuthState.Initial
        _errorMessage.value = null
        Log.d(TAG, "User logged out")
    }

    /**
     * Map a typed [AuthException] (or any [Throwable]) to a user-friendly message.
     * Uses the exception's own message when it carries a server-provided description,
     * falling back to sensible defaults.
     */
    private fun mapAuthExceptionToMessage(throwable: Throwable): String = when (throwable) {
        is InvalidCredentialsException -> throwable.message ?: "Invalid username or password"
        is AccountLockedException     -> throwable.message ?: "Account locked. Try again in 30 minutes"
        is AccountInactiveException   -> throwable.message ?: "Your account has been deactivated"
        is ServerErrorException       -> throwable.message ?: "Server temporarily unavailable"
        is NetworkException           -> throwable.message ?: "Network error. Please try again"
        is AuthException              -> throwable.message ?: "An error occurred. Please try again"
        else                          -> "An error occurred. Please try again"
    }
}

