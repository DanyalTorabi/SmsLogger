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
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var startServiceButton: Button
    private lateinit var buttonLogAllSms: Button

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
            Toast.makeText(this, "SMS Logging Service starting...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SmsLoggingService", e)
            Toast.makeText(this, "Error starting logging service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // New function to log all SMS messages from the database to Logcat
    private fun logAllSmsFromDbToLogcat() {
        lifecycleScope.launch { // Use lifecycleScope for coroutines tied to activity lifecycle
            Log.i(TAG, "Fetching all SMS from database to log...")
            val smsList: List<SmsMessage> = withContext(Dispatchers.IO) {
                // Access DAO and fetch data on IO dispatcher
                val smsDao = AppDatabase.getDatabase(applicationContext).smsDao()
                smsDao.getAllSmsList()
            }

            if (smsList.isEmpty()) {
                Log.i(TAG, "No SMS messages found in the local database.")
                Toast.makeText(this@MainActivity, "No SMS messages in DB.", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "--- Dumping ${smsList.size} SMS messages from DB ---")
                smsList.forEachIndexed { index, sms ->
                    // Log relevant details of each SMS. Adjust as needed.
                    Log.d(TAG, "DB SMS ${index + 1}: ID=${sms.id}, OriginalSmsID=${sms.smsId}, Phone='${sms.phoneNumber}', Type='${sms.eventType}', EventTS=${sms.eventTimestamp}, SmsTS=${sms.smsTimestamp}, Body='${sms.body.take(60).replace("\n", " ")}...'")
                }
                Log.i(TAG, "--- End of DB SMS Dump ---")
                Toast.makeText(this@MainActivity, "Logged ${smsList.size} SMS to Logcat.", Toast.LENGTH_SHORT).show()
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