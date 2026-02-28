package com.example.smslogger.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.smslogger.R
import com.example.smslogger.config.SmsLoggerConfig
import com.example.smslogger.data.repository.AuthRepository
import com.example.smslogger.security.KeystoreCredentialManager
import com.example.smslogger.service.SmsSyncService
import com.example.smslogger.ui.auth.LoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen for account and sync management (#54).
 *
 * Sections:
 * - Account: displays logged-in username, logout action
 * - Sync: last sync time, manual sync trigger
 * - About: app version, server URL, 2FA info (server-managed)
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ──────────────────────────────────────────────────────────────────────────

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var config: SmsLoggerConfig
        private lateinit var credentialManager: KeystoreCredentialManager
        private lateinit var authRepository: AuthRepository

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val ctx = requireContext()
            config = SmsLoggerConfig.getInstance(ctx)
            credentialManager = KeystoreCredentialManager.getInstance(ctx)
            authRepository = AuthRepository(credentialManager, config.serverUrl, ctx)

            populateDynamicPreferences()
            setupClickListeners()
        }

        override fun onResume() {
            super.onResume()
            // Refresh last sync time every time screen is shown
            updateLastSyncSummary()
        }

        // ── Population ────────────────────────────────────────────────────────

        private fun populateDynamicPreferences() {
            // Username
            findPreference<Preference>("pref_username")?.summary =
                credentialManager.getUsername() ?: getString(R.string.pref_username_summary_placeholder)

            // App version
            val versionName = try {
                requireContext().packageManager
                    .getPackageInfo(requireContext().packageName, 0).versionName
            } catch (_: Exception) { "–" }
            findPreference<Preference>("pref_app_version")?.summary = versionName

            // Server URL
            findPreference<Preference>("pref_server_url")?.summary = config.serverUrl

            // Last sync time
            updateLastSyncSummary()
        }

        private fun updateLastSyncSummary() {
            val lastSync = config.lastSyncTime
            val summary = if (lastSync == 0L) {
                getString(R.string.pref_last_sync_never)
            } else {
                val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                fmt.format(Date(lastSync))
            }
            findPreference<Preference>("pref_last_sync")?.summary = summary
        }

        // ── Click listeners ───────────────────────────────────────────────────

        private fun setupClickListeners() {
            // Logout
            findPreference<Preference>("pref_logout")?.setOnPreferenceClickListener {
                showLogoutConfirmationDialog()
                true
            }

            // Manual sync
            findPreference<Preference>("pref_sync_now")?.setOnPreferenceClickListener {
                SmsSyncService.startService(requireContext())
                findPreference<Preference>("pref_sync_now")?.summary =
                    getString(R.string.pref_sync_now_started)
                true
            }
        }

        // ── Logout flow ───────────────────────────────────────────────────────

        private fun showLogoutConfirmationDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_dialog_title))
                .setMessage(getString(R.string.logout_dialog_message))
                .setPositiveButton(getString(R.string.logout_dialog_confirm)) { _, _ ->
                    performLogout()
                }
                .setNegativeButton(getString(R.string.logout_dialog_cancel), null)
                .show()
        }

        private fun performLogout() {
            // Clear all data via repository (#54 — "Clear all data on logout")
            authRepository.logout()

            // Stop sync service
            SmsSyncService.stopService(requireContext())

            // Navigate back to login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}

