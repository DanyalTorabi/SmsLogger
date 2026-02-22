package com.example.smslogger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.service.SmsLoggingService
import com.example.smslogger.service.SmsSyncService
import com.example.smslogger.ui.auth.LoginActivity

/**
 * Main Activity - Entry point of the app
 * Checks authentication status and redirects to login if needed
 *
 * Updated for Epic #44 - Authentication System
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var credentialManager: KeystoreCredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity starting...")

        // Initialize credential manager
        credentialManager = KeystoreCredentialManager.getInstance(this)

        // Check authentication status
        if (!credentialManager.isAuthenticated()) {
            Log.d(TAG, "User not authenticated, redirecting to login...")
            navigateToLogin()
            return
        }

        Log.d(TAG, "User authenticated, proceeding with service startup")

        // Check permissions and start services automatically
        if (arePermissionsGranted()) {
            Log.d(TAG, "Permissions already granted, starting services...")
            startServices()
        } else {
            Log.w(TAG, "SMS permissions not granted. Please grant permissions in Android Settings.")
            Log.w(TAG, "Go to: Settings > Apps > SMS Logger > Permissions > Enable SMS")
        }

        // Finish activity immediately - no UI needed
        finish()
    }

    /**
     * Navigate to login screen
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startServices() {
        try {
            // Start SMS logging service
            val loggingServiceIntent = Intent(this, SmsLoggingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(loggingServiceIntent)
            } else {
                startService(loggingServiceIntent)
            }
            Log.d(TAG, "SMS Logging Service started")

            // Start SMS sync service
            SmsSyncService.startService(this)
            Log.d(TAG, "SMS Sync Service started")

            Log.d(TAG, "All services started successfully - app running in background")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting services", e)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
    }
}