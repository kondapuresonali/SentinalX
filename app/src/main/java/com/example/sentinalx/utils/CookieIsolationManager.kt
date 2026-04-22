package com.example.sentinalx.utils

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage

object CookieIsolationManager {

    private const val TAG = "CookieIsolation"

    /**
     * Initialize WebView with complete cookie isolation
     */
    fun setupIsolatedCookies(context: Context) {
        try {
            val cookieManager = CookieManager.getInstance()

            // Enable cookies for WebView
            cookieManager.setAcceptCookie(true)

            // Enable third-party cookies (for sites that need them)
            cookieManager.setAcceptThirdPartyCookies(
                android.webkit.WebView(context),
                true
            )

            Log.d(TAG, "✅ Cookie isolation initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to setup cookies: ${e.message}")
        }
    }

    /**
     * Clear all cookies and web storage (called on app close)
     */
    fun clearAllCookies(onComplete: (() -> Unit)? = null) {
        try {
            val cookieManager = CookieManager.getInstance()

            // Remove all cookies
            cookieManager.removeAllCookies { success ->
                if (success) {
                    Log.d(TAG, "✅ All cookies cleared")
                } else {
                    Log.e(TAG, "⚠️ Cookie clearing failed")
                }
                onComplete?.invoke()
            }

            // Clear WebStorage (localStorage, sessionStorage, etc.)
            WebStorage.getInstance().deleteAllData()

            Log.d(TAG, "✅ WebStorage cleared")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing cookies: ${e.message}")
            onComplete?.invoke()
        }
    }

    /**
     * Get cookie count for current session
     */
    fun getCookieCount(): Int {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://example.com") ?: ""
            cookies.split(";").filter { it.isNotBlank() }.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Check if cookies are enabled
     */
    fun areCookiesEnabled(): Boolean {
        return try {
            CookieManager.getInstance().acceptCookie()
        } catch (e: Exception) {
            false
        }
    }
}