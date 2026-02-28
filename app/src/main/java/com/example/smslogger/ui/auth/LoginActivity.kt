package com.example.smslogger.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smslogger.MainActivity
import com.example.smslogger.R
import com.example.smslogger.config.SmsLoggerConfig
import com.example.smslogger.data.repository.AuthRepository
import com.example.smslogger.databinding.ActivityLoginBinding
import com.example.smslogger.security.KeystoreCredentialManager
import kotlinx.coroutines.launch

/**
 * Login Activity for user authentication
 * Implements Material Design 3 UI with secure credential storage
 *
 * Features:
 * - Username/password authentication
 * - Remember me functionality
 * - Input validation
 * - Error handling with user-friendly messages
 * - Loading states
 * - Keyboard management
 *
 * Dependencies: #46 (KeystoreCredentialManager), #50 (API Models)
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var config: SmsLoggerConfig
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize config
        config = SmsLoggerConfig.getInstance(this)

        // Initialize dependencies
        val credentialManager = KeystoreCredentialManager.getInstance(this)
        val authRepository = AuthRepository(credentialManager, config.serverUrl, this)

        // Initialize ViewModel
        val factory = AuthViewModelFactory(authRepository)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // Setup UI
        setupInputFields()
        setupLoginButton()
        observeAuthState()

        // Pre-fill username if saved
        val savedUsername = viewModel.getSavedUsername()
        if (!savedUsername.isNullOrBlank()) {
            binding.editTextUsername.setText(savedUsername)
            binding.checkBoxRememberMe.isChecked = true
            // Focus on password field if username exists
            binding.editTextPassword.requestFocus()
        }
    }

    /**
     * Setup input fields with validation and keyboard handling
     */
    private fun setupInputFields() {
        // Clear error on text change
        binding.editTextUsername.doAfterTextChanged {
            binding.textInputLayoutUsername.error = null
            hideError()
        }

        binding.editTextPassword.doAfterTextChanged {
            binding.textInputLayoutPassword.error = null
            hideError()
        }

        // TOTP field: clear error on change; auto-advance to login button after 6 digits
        binding.editTextTotp.doAfterTextChanged { text ->
            binding.textInputLayoutTotp.error = null
            hideError()
            if ((text?.length ?: 0) == 6) {
                // Move focus to login button so user can confirm or just tap it
                binding.buttonLogin.requestFocus()
            }
        }

        // Handle IME actions
        binding.editTextUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.editTextPassword.requestFocus()
                true
            } else {
                false
            }
        }

        // Password moves to TOTP field on Next
        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.editTextTotp.requestFocus()
                true
            } else {
                false
            }
        }

        // TOTP triggers login on Done
        binding.editTextTotp.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    /**
     * Setup login button click handler
     */
    private fun setupLoginButton() {
        binding.buttonLogin.setOnClickListener {
            attemptLogin()
        }
    }

    /**
     * Attempt login with input validation
     */
    private fun attemptLogin() {
        // Get input values
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString()
        val totpRaw = binding.editTextTotp.text.toString().trim()
        val rememberMe = binding.checkBoxRememberMe.isChecked

        // Clear previous errors
        binding.textInputLayoutUsername.error = null
        binding.textInputLayoutPassword.error = null
        binding.textInputLayoutTotp.error = null
        hideError()

        // Basic validation
        var hasError = false

        if (username.isEmpty()) {
            binding.textInputLayoutUsername.error = getString(R.string.error_username_required)
            hasError = true
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = getString(R.string.error_password_required)
            hasError = true
        } else if (password.length < 6) {
            binding.textInputLayoutPassword.error = getString(R.string.error_password_too_short)
            hasError = true
        }

        // TOTP validation: must be empty OR exactly 6 digits
        if (totpRaw.isNotEmpty() && (totpRaw.length != 6 || !totpRaw.all { it.isDigit() })) {
            binding.textInputLayoutTotp.error = getString(R.string.error_totp_invalid)
            hasError = true
        }

        if (hasError) {
            return
        }

        // Hide keyboard
        currentFocus?.clearFocus()

        // Attempt login via ViewModel
        viewModel.login(
            username = username,
            password = password,
            totpCode = totpRaw.ifEmpty { null },
            rememberMe = rememberMe
        )
    }

    /**
     * Observe authentication state changes
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe primary auth state
                launch {
                    viewModel.authState.collect { state ->
                        when (state) {
                            is AuthState.Initial -> {
                                showLoading(false)
                                hideError()
                            }
                            is AuthState.Loading -> {
                                showLoading(true)
                                hideError()
                            }
                            is AuthState.Success -> {
                                showLoading(false)
                                hideError()
                                onLoginSuccess()
                            }
                            is AuthState.Error -> {
                                showLoading(false)
                                showError(state.message)
                                // Clear TOTP on error so user enters a fresh code (#52, #55)
                                binding.editTextTotp.text?.clear()
                                // Highlight TOTP field for 2FA-specific errors (#55)
                                val msg = state.message.lowercase()
                                if (msg.contains("2fa") || msg.contains("totp") ||
                                    msg.contains("authenticator") || msg.contains("two-factor")) {
                                    binding.textInputLayoutTotp.error = state.message
                                }
                            }
                        }
                    }
                }
                // Observe lockout state: disable login controls when account is locked (#55)
                launch {
                    viewModel.isLocked.collect { locked ->
                        val notLoading = viewModel.authState.value !is AuthState.Loading
                        binding.buttonLogin.isEnabled = !locked
                        if (locked) {
                            binding.editTextUsername.isEnabled = false
                            binding.editTextPassword.isEnabled = false
                            binding.editTextTotp.isEnabled = false
                        } else if (notLoading) {
                            binding.editTextUsername.isEnabled = true
                            binding.editTextPassword.isEnabled = true
                            binding.editTextTotp.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Show/hide loading state
     */
    private fun showLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
        binding.editTextUsername.isEnabled = !loading
        binding.editTextPassword.isEnabled = !loading
        binding.editTextTotp.isEnabled = !loading
        binding.checkBoxRememberMe.isEnabled = !loading

        if (loading) {
            binding.buttonLogin.text = ""
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.buttonLogin.text = getString(R.string.login_button)
            binding.progressBar.visibility = View.GONE
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        binding.textViewError.text = message
        binding.textViewError.visibility = View.VISIBLE
    }

    /**
     * Hide error message
     */
    private fun hideError() {
        binding.textViewError.visibility = View.GONE
    }

    /**
     * Handle successful login
     */
    private fun onLoginSuccess() {
        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * ViewModel Factory for dependency injection
     */
    class AuthViewModelFactory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

