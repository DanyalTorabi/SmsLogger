package com.example.smslogger.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smslogger.api.SmsApiClient
import com.example.smslogger.security.KeystoreCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for authentication logic
 * Handles login, credential validation, and secure storage
 *
 * Dependencies: #46 (KeystoreCredentialManager), #50 (API Models)
 */
class AuthViewModel(
    private val credentialManager: KeystoreCredentialManager,
    private val serverUrl: String
) : ViewModel() {

    private val TAG = "AuthViewModel"

    // Mutable state for internal use
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)

    // Public immutable state for UI observation
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Login with username and password
     * @param username User's username or email
     * @param password User's password
     * @param rememberMe Whether to save password for future logins
     */
    fun login(username: String, password: String, rememberMe: Boolean = false) {
        // Input validation
        if (username.isBlank()) {
            _authState.value = AuthState.Error("Username is required")
            return
        }

        if (password.isBlank()) {
            _authState.value = AuthState.Error("Password is required")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        // Set loading state
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                // Create API client for authentication
                // Note: This will be refactored in #49 to use JWT interceptor
                val apiClient = SmsApiClient(serverUrl, username, password)

                // Attempt authentication
                val success = withContext(Dispatchers.IO) {
                    apiClient.testConnection()
                }

                if (success) {
                    // Authentication successful
                    // For now, we create a mock token since old API doesn't return full response
                    // This will be properly handled when #49/#50 updates the API client

                    Log.d(TAG, "Login successful for user: $username")

                    // Save credentials to secure storage
                    credentialManager.saveCredentials(
                        username = username,
                        password = if (rememberMe) password else null,
                        jwtToken = "legacy_token_${System.currentTimeMillis()}", // Placeholder
                        refreshToken = null,
                        expiresInSeconds = 3600, // 1 hour
                        userId = username, // Use username as ID for now
                        email = null
                    )

                    // Create mock user info
                    val userInfo = com.example.smslogger.api.UserInfo(
                        id = username,
                        username = username,
                        email = null,
                        createdAt = null
                    )

                    _authState.value = AuthState.Success(userInfo)

                } else {
                    Log.w(TAG, "Login failed for user: $username")
                    _authState.value = AuthState.Error(
                        message = "Invalid username or password",
                        code = "INVALID_CREDENTIALS"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                _authState.value = AuthState.Error(
                    message = getErrorMessage(e),
                    code = "NETWORK_ERROR"
                )
            }
        }
    }

    /**
     * Check if user is already logged in
     */
    fun checkAuthStatus(): Boolean {
        return credentialManager.isAuthenticated()
    }

    /**
     * Get stored username for pre-filling login form
     */
    fun getSavedUsername(): String? {
        return credentialManager.getUsername()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
    }

    /**
     * Logout - clear all stored credentials
     */
    fun logout() {
        credentialManager.clearCredentials()
        _authState.value = AuthState.Initial
        Log.d(TAG, "User logged out")
    }

    /**
     * Convert exception to user-friendly error message
     */
    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("Unable to resolve host") == true ->
                "Cannot connect to server. Check your network connection."
            exception.message?.contains("timeout") == true ->
                "Connection timeout. Please try again."
            exception.message?.contains("Failed to connect") == true ->
                "Cannot reach server. Check server URL in settings."
            else ->
                "An error occurred. Please try again."
        }
    }

    /**
     * Map server error code to user-friendly message
     * This will be expanded when #55 is implemented
     */
    private fun mapErrorCodeToMessage(errorCode: String): String {
        return when (errorCode) {
            "INVALID_CREDENTIALS" -> "Username or password is incorrect"
            "TOTP_REQUIRED" -> "Two-factor authentication required"
            "INVALID_TOTP" -> "Invalid or expired 2FA code"
            "ACCOUNT_LOCKED" -> "Account locked due to multiple failed attempts"
            "ACCOUNT_INACTIVE" -> "Your account has been deactivated"
            "NETWORK_ERROR" -> "Network connection failed"
            "SERVER_ERROR" -> "Server temporarily unavailable"
            else -> "An error occurred. Please try again."
        }
    }
}

