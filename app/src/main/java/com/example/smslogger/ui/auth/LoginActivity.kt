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

        // Initialize ViewModel
        val credentialManager = KeystoreCredentialManager.getInstance(this)
        val factory = AuthViewModelFactory(credentialManager, config.serverUrl)
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

        // Handle IME actions
        binding.editTextUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.editTextPassword.requestFocus()
                true
            } else {
                false
            }
        }

        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
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
        val rememberMe = binding.checkBoxRememberMe.isChecked

        // Clear previous errors
        binding.textInputLayoutUsername.error = null
        binding.textInputLayoutPassword.error = null
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

        if (hasError) {
            return
        }

        // Hide keyboard
        currentFocus?.clearFocus()

        // Attempt login via ViewModel
        viewModel.login(username, password, rememberMe)
    }

    /**
     * Observe authentication state changes
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        private val credentialManager: KeystoreCredentialManager,
        private val serverUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(credentialManager, serverUrl) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

