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
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import com.example.smslogger.data.AppDatabase // Import AppDatabase
import com.example.smslogger.data.SmsMessage // Import SmsMessage
import com.example.smslogger.service.SmsLoggingService
import com.example.smslogger.service.SmsSyncService
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var startServiceButton: Button
    private lateinit var buttonLogAllSms: Button
    private lateinit var startSyncServiceButton: Button

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Log.d(TAG, "All required permissions granted.")
                Toast.makeText(this, "SMS Permissions Granted.", Toast.LENGTH_SHORT).show()
                startSmsLoggerService()
            } else {
                Log.w(TAG, "Not all required permissions were granted.")
                Toast.makeText(this, "SMS Permissions Denied. App functionality may be limited.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // You'll need a simple layout with a button

        startServiceButton = findViewById(R.id.startServiceButton)
        buttonLogAllSms = findViewById(R.id.buttonLogAllSms) // Initialize the new button

        startServiceButton.setOnClickListener {
            checkAndRequestPermissions()
        }
        buttonLogAllSms.setOnClickListener {
            logAllSmsFromDbToLogcat()
        }

        if (arePermissionsGranted()) {
            // Permissions are already granted, you could potentially auto-start service
            // or update UI to reflect this state.
            Log.d(TAG, "Permissions were already granted on create.")
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Permissions are already granted
            Log.d(TAG, "Permissions already granted when button clicked.")
            Toast.makeText(this, "Permissions already granted.", Toast.LENGTH_SHORT).show()
            startSmsLoggerService()
        }
    }

    private fun startSmsLoggerService() {
        Log.d(TAG, "Attempting to start SmsLoggingService.")
        val serviceIntent = Intent(this, SmsLoggingService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "SmsLoggingService start command sent.")

            // Also start the SMS sync service
            Log.d(TAG, "Starting SMS Sync Service...")
            SmsSyncService.startService(this)
            Log.d(TAG, "SMS Sync Service start command sent.")

            Toast.makeText(this, "SMS Logging and Sync Services starting...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting services", e)
            Toast.makeText(this, "Error starting services: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // New function to log all SMS messages from the database to Logcat
    private fun logAllSmsFromDbToLogcat() {
        Toast.makeText(this, "Fetching all SMS from database...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allSms = AppDatabase.getDatabase(applicationContext).smsDao().getAllSmsList()
                Log.d(TAG, "Found ${allSms.size} SMS messages in database")

                allSms.forEach { sms ->
                    val syncStatus = if (sms.syncedAt != null) "Synced" else "Pending"
                    Log.d(TAG, "SMS[ID: ${sms.id}] From: ${sms.phoneNumber}, Type: ${sms.eventType}, " +
                            "Date: ${sms.smsTimestamp}, DateSent: ${sms.dateSent ?: "N/A"}, " +
                            "ThreadID: ${sms.threadId ?: "N/A"}, Person: ${sms.person ?: "N/A"}, " +
                            "Sync: $syncStatus, " +
                            "Body: ${sms.body.take(30)}${if (sms.body.length > 30) "..." else ""}")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Logged ${allSms.size} SMS messages to Logcat",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading SMS from database", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Error reading SMS: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        // Define required permissions. READ_SMS is crucial for the service to read existing SMS.
        // RECEIVE_SMS for the BroadcastReceiver.
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
            // Consider adding Manifest.permission.POST_NOTIFICATIONS if targetSDK is 33+
            // and your service uses notifications.
        )
    }
}