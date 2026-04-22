package com.example.sentinalx.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

object NetworkMonitor {
    /**
     * Checks if the device has an active network connection (Wi-Fi or Mobile Data).
     * * This function is used to gate the app launch (Splash), Registration,
     * and Link Scanning processes.
     */
    fun isOnline(context: Context): Boolean {
        // Retrieve the ConnectivityManager service
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            Log.e("NetworkMonitor", "ConnectivityManager is null. Cannot check network status.")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use NetworkCapabilities for modern Android versions (API 23+)
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            // Use deprecated method for older Android versions (legacy support)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}