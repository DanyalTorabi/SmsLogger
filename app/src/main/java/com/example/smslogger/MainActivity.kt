package com.example.smslogger // Replace with your actual package name

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smslogger.service.SmsLoggingService

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allGranted = false
                    Log.w(TAG, "Permission denied: ${it.key}")
                }
            }
            if (allGranted) {
                Log.d(TAG, "All permissions granted.")
                startSmsService()
            } else {
                Toast.makeText(this, "SMS permissions are required to log messages.", Toast.LENGTH_LONG).show()
                // You might want to explain to the user why the permissions are needed
                // or direct them to settings.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // You'll need a simple layout with a button

        val startServiceButton: Button = findViewById(R.id.startServiceButton) // Add this button to your layout
        startServiceButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        // Optional: Check if service is already running and update UI accordingly
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECEIVE_SMS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        if (requiredPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${requiredPermissions.joinToString()}")
            requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All necessary permissions already granted.")
            startSmsService()
        }
    }

    private fun startSmsService() {
        Log.d(TAG, "Attempting to start SmsLoggingService.")
        val serviceIntent = Intent(this, SmsLoggingService::class.java)
        // For Android O and above, you need to start foreground services differently
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "SMS Logging Service starting...", Toast.LENGTH_SHORT).show()
    }
}