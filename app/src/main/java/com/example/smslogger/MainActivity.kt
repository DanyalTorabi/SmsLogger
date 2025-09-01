package com.example.smslogger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smslogger.service.SmsLoggingService
import com.example.smslogger.service.SmsSyncService

/**
 * Headless SMS Logger - No UI required
 * This activity automatically starts services and finishes immediately
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "SMS Logger starting in headless mode...")

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