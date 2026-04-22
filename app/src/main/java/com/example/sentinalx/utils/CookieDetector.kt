package com.example.sentinalx.utils

import android.util.Log
import android.webkit.WebView

class CookieDetector(
    private val onCookieDetected: (CookieInfo) -> Unit
) {

    private val TAG = "CookieDetector"
    private val detectedDomains = mutableSetOf<String>()

    data class CookieInfo(
        val domain: String,
        val cookieCount: Int,
        val hasThirdParty: Boolean,
        val message: String
    )

    /**
     * Monitor cookies for a specific URL
     */
    fun monitorCookies(webView: WebView, url: String) {
        try {
            val domain = extractDomain(url)

            // Check if we already alerted for this domain
            if (detectedDomains.contains(domain)) {
                return
            }

            // Get cookies for this domain
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie(url)

            if (!cookies.isNullOrEmpty()) {
                val cookieList = cookies.split(";").map { it.trim() }
                val cookieCount = cookieList.size

                // Check for third-party cookies (simplified detection)
                val hasThirdParty = cookieList.any {
                    it.contains("_ga") || // Google Analytics
                            it.contains("_fbp") || // Facebook Pixel
                            it.contains("__utm") || // UTM tracking
                            it.contains("_pk_") // Piwik/Matomo
                }

                val info = CookieInfo(
                    domain = domain,
                    cookieCount = cookieCount,
                    hasThirdParty = hasThirdParty,
                    message = buildCookieMessage(domain, cookieCount, hasThirdParty)
                )

                // Mark as detected
                detectedDomains.add(domain)

                // Notify listener
                onCookieDetected(info)

                Log.d(TAG, "🍪 Cookies detected on $domain: $cookieCount cookies, Third-party: $hasThirdParty")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error monitoring cookies: ${e.message}")
        }
    }

    /**
     * Check if page has cookie consent banner (basic detection)
     */
    fun detectCookieBanner(webView: WebView, callback: (Boolean) -> Unit) {
        val script = """
            (function() {
                var keywords = ['cookie', 'consent', 'privacy', 'gdpr', 'accept', 'agree'];
                var elements = document.querySelectorAll('div, aside, section');
                
                for (var i = 0; i < elements.length; i++) {
                    var text = elements[i].innerText.toLowerCase();
                    var hasKeyword = keywords.some(function(keyword) {
                        return text.includes(keyword);
                    });
                    
                    // Check if element is visible and positioned like a banner
                    var style = window.getComputedStyle(elements[i]);
                    var isVisible = style.display !== 'none' && style.visibility !== 'hidden';
                    var isFixed = style.position === 'fixed' || style.position === 'absolute';
                    
                    if (hasKeyword && isVisible && isFixed) {
                        return true;
                    }
                }
                return false;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            val hasBanner = result == "true"
            callback(hasBanner)
            if (hasBanner) {
                Log.d(TAG, "🍪 Cookie consent banner detected")
            }
        }
    }

    /**
     * Clear detected domains (reset detection)
     */
    fun reset() {
        detectedDomains.clear()
        Log.d(TAG, "🔄 Cookie detector reset")
    }

    // Helper functions

    private fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun buildCookieMessage(domain: String, count: Int, hasThirdParty: Boolean): String {
        return when {
            hasThirdParty && count > 5 ->
                "⚠️ $domain is storing $count cookies including tracking cookies"
            hasThirdParty ->
                "🍪 $domain is storing cookies with third-party tracking"
            count > 10 ->
                "🍪 $domain is storing $count cookies"
            else ->
                "🍪 $domain is storing $count cookies"
        }
    }
}