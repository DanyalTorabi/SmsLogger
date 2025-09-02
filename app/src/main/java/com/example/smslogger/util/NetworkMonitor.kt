package com.example.smslogger.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Network state monitor for SMS Logger
 * Tracks network availability and type for intelligent sync scheduling
 */
@Suppress("unused") // Utility class for future integration
class NetworkMonitor(context: Context) {

    private val TAG = "NetworkMonitor"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(false)
    @Suppress("unused")
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isWifiConnected = MutableStateFlow(false)
    @Suppress("unused")
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected

    private val _isMetered = MutableStateFlow(true)
    @Suppress("unused")
    val isMetered: StateFlow<Boolean> = _isMetered

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            updateNetworkState()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            updateNetworkState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.d(TAG, "Network capabilities changed: $network")
            updateNetworkState()
        }
    }

    init {
        registerNetworkCallback()
        updateNetworkState()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun updateNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val wifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val metered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true

        _isConnected.value = connected
        _isWifiConnected.value = wifi
        _isMetered.value = metered

        Log.d(TAG, "Network state updated - Connected: $connected, WiFi: $wifi, Metered: $metered")
    }

    @Suppress("unused")
    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    @Suppress("unused")
    fun shouldAllowSync(wifiOnlyMode: Boolean): Boolean {
        return when {
            !_isConnected.value -> {
                Log.d(TAG, "Sync blocked: No network connection")
                false
            }
            wifiOnlyMode && !_isWifiConnected.value -> {
                Log.d(TAG, "Sync blocked: WiFi-only mode but not on WiFi")
                false
            }
            else -> {
                Log.d(TAG, "Sync allowed: Network conditions satisfied")
                true
            }
        }
    }
}
