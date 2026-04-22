package com.example.sentinalx.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.sentinalx.ui.ScanningDialogActivity
import com.example.sentinalx.ui.BrowserActivity
import com.example.sentinalx.utils.LinkCacheManager
import java.util.regex.Pattern

class LinkInterceptionAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SENTINALX_SERVICE"

        private val URL_PATTERN = Pattern.compile(
            "(https?://[^\\s]+)|(www\\.[^\\s]+)|([a-zA-Z0-9-]+\\.(com|org|net|io|co|edu|gov|in)[^\\s]*)"
        )

        fun disableService() {
            serviceInstance?.disableSelf()
            Log.d(TAG, "⛔ Accessibility Service disabled")
        }

        var isServiceRunning = false
        private var serviceInstance: LinkInterceptionAccessibilityService? = null

        fun closeBrowser() {
            serviceInstance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        private val CHAT_PACKAGES = setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.instagram.android"
        )

        private val IN_APP_BROWSER_APPS = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.linkedin.android"
        )
    }

    private var lastScannedUrl = ""
    private var lastScanTime = 0L
    private val SCAN_COOLDOWN = 2000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        serviceInstance = this

        val cacheEnabled = LinkCacheManager.isCacheEnabled(this)
        val stats = LinkCacheManager.getCacheStats(this)

        Log.d(TAG, "═════════════════════════════════")
        Log.d(TAG, "✅ SentinalX Service Connected")
        Log.d(TAG, "Cache Status: ${if (cacheEnabled) "ENABLED" else "DISABLED"}")
        Log.d(TAG, "Cached Links: ${stats.activeEntries} active, ${stats.expiredEntries} expired")
        Log.d(TAG, "═════════════════════════════════")

        Toast.makeText(
            this,
            "✅ SentinalX Active! ${if (cacheEnabled) "(Smart Cache ON)" else ""}",
            Toast.LENGTH_SHORT
        ).show()

        // Clean expired cache on service start
        LinkCacheManager.cleanExpiredCache(this)
    }

    /**
     * Check if user is logged in
     */
    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPref.getBoolean("isLoggedIn", false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Security check: Only proceed if user is logged in
        if (!isUserLoggedIn()) {
            Log.d(TAG, "⛔ User not logged in - protection disabled")
            return
        }

        val pkg = event.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()

        // Cooldown check
        if (currentTime - lastScanTime < SCAN_COOLDOWN) {
            return
        }

        // Chat app link interception
        if (CHAT_PACKAGES.contains(pkg) && event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            Log.d(TAG, "📱 Click detected in $pkg")

            val clickedNode = event.source
            if (clickedNode == null) {
                Log.d(TAG, "❌ No source node available")
                return
            }

            // Check if clicked element contains URL
            val clickedText = clickedNode.text?.toString() ?: ""
            val clickedDesc = clickedNode.contentDescription?.toString() ?: ""
            val parentNode = clickedNode.parent

            val contextText = buildString {
                append(clickedText)
                append(" ")
                append(clickedDesc)
                if (parentNode != null) {
                    append(" ")
                    append(parentNode.text?.toString() ?: "")
                    append(" ")
                    append(parentNode.contentDescription?.toString() ?: "")
                }
            }

            clickedNode.recycle()
            parentNode?.recycle()

            // Only proceed if context contains URL
            val contextMatcher = URL_PATTERN.matcher(contextText)
            if (!contextMatcher.find()) {
                Log.d(TAG, "⏭️ No URL in clicked element - ignoring")
                return
            }

            Log.d(TAG, "🎯 URL detected in clicked area")

            // Extract URLs
            val urls = extractUrlsNearClick(event)

            if (urls.isNotEmpty()) {
                val primaryUrl = urls.first()
                Log.d(TAG, "✅ Found URL: $primaryUrl")
                handleUrl(primaryUrl, pkg)
            } else {
                Log.d(TAG, "❌ No valid URLs found")
            }
        }

        // In-app browser detection
        if (IN_APP_BROWSER_APPS.contains(pkg)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                handleInAppBrowserDetection(pkg)
            }
        }
    }

    /**
     * Handle URL - check cache first, then scan if needed
     */
    private fun handleUrl(url: String, sourceApp: String) {
        try {
            // Filter internal URLs
            if (url.contains("whatsapp.com") || url.contains("wa.me")) {
                Log.d(TAG, "⏭️ Ignoring internal URL")
                return
            }

            // Prevent duplicate processing
            if (url == lastScannedUrl && System.currentTimeMillis() - lastScanTime < SCAN_COOLDOWN) {
                Log.d(TAG, "⏭️ Same URL within cooldown period - ignoring")
                return
            }

            // Update last scan tracking
            lastScannedUrl = url
            lastScanTime = System.currentTimeMillis()

            // Check cache first
            val cachedResult = LinkCacheManager.getCachedResult(this, url)

            if (cachedResult != null) {
                // URL is cached and SAFE - open directly
                Log.d(TAG, "🚀 Opening cached SAFE link directly (scanned ${getDaysAgo(cachedResult.timestamp)} days ago)")
                Toast.makeText(
                    this,
                    "✓ Safe link (verified) - Opening directly",
                    Toast.LENGTH_LONG
                ).show()

                openUrlDirectly(url)

            } else {
                // Not cached or needs re-scan - show scanning dialog
                Log.d(TAG, "🔍 Showing scan dialog for new/expired URL")
                showUrlDialog(url, sourceApp)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling URL: ${e.message}")
        }
    }

    /**
     * Calculate days ago from timestamp
     */
    private fun getDaysAgo(timestamp: Long): Long {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp
        return diff / (24 * 60 * 60 * 1000)
    }

    /**
     * Open URL directly in secure browser (for cached safe links)
     */
    private fun openUrlDirectly(url: String) {
        try {
            val intent = Intent(this, BrowserActivity::class.java)
            intent.putExtra("url", url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "✅ Opened cached safe URL in secure browser")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: ${e.message}")
            // Fallback to external browser
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show scanning dialog for new or suspicious links
     */
    private fun showUrlDialog(url: String, sourceApp: String) {
        try {
            // Double-check login status
            if (!isUserLoggedIn()) {
                Log.d(TAG, "⛔ User logged out - dialog cancelled")
                return
            }

            val intent = Intent(this, ScanningDialogActivity::class.java)
            intent.putExtra("url", url)
            intent.putExtra("source_app", sourceApp)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

            startActivity(intent)
            Log.d(TAG, "✅ Scanning dialog launched")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error launching dialog: ${e.message}")
        }
    }

    /**
     * Extract URLs from clicked area
     */
    private fun extractUrlsNearClick(event: AccessibilityEvent): List<String> {
        val urls = mutableSetOf<String>()
        val clickedNode = event.source ?: return emptyList()

        try {
            extractUrlsFromNode(clickedNode, urls)

            val parent = clickedNode.parent
            if (parent != null) {
                extractUrlsFromNode(parent, urls)

                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i)
                    if (sibling != null) {
                        extractUrlsFromNode(sibling, urls)
                        sibling.recycle()
                    }
                }
                parent.recycle()
            }

            clickedNode.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting URLs: ${e.message}")
        }

        return urls.mapNotNull { url ->
            var cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http")) {
                cleanUrl = "https://$cleanUrl"
            }
            if (cleanUrl.length > 10 && !cleanUrl.contains("whatsapp.com")) {
                cleanUrl
            } else null
        }
    }

    /**
     * Extract URLs from node
     */
    private fun extractUrlsFromNode(node: AccessibilityNodeInfo, urls: MutableSet<String>) {
        try {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()

            listOf(text, desc).forEach { content ->
                if (!content.isNullOrEmpty()) {
                    val matcher = URL_PATTERN.matcher(content)
                    while (matcher.find()) {
                        urls.add(matcher.group())
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Detect in-app browser
     */
    private fun handleInAppBrowserDetection(packageName: String) {
        try {
            val rootNode = rootInActiveWindow ?: return
            val webViewUrl = findWebViewUrl(rootNode)
            rootNode.recycle()

            if (webViewUrl != null && webViewUrl != lastScannedUrl) {
                Log.d(TAG, "🌐 In-app browser detected in $packageName")
                Log.d(TAG, "WebView URL: $webViewUrl")
                handleUrl(webViewUrl, packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting in-app browser: ${e.message}")
        }
    }

    /**
     * Find WebView URL
     */
    private fun findWebViewUrl(node: AccessibilityNodeInfo?, depth: Int = 0): String? {
        if (node == null || depth > 5) return null

        try {
            val className = node.className?.toString() ?: ""

            if (className.contains("WebView")) {
                val url = extractUrlFromWebViewNode(node)
                if (url != null) return url
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val url = findWebViewUrl(child, depth + 1)
                child.recycle()
                if (url != null) return url
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    /**
     * Extract URL from WebView
     */
    private fun extractUrlFromWebViewNode(webViewNode: AccessibilityNodeInfo): String? {
        try {
            val desc = webViewNode.contentDescription?.toString()
            if (!desc.isNullOrEmpty()) {
                val matcher = URL_PATTERN.matcher(desc)
                if (matcher.find()) {
                    var url = matcher.group()
                    if (!url.startsWith("http")) {
                        url = "https://$url"
                    }
                    return url
                }
            }

            for (i in 0 until webViewNode.childCount) {
                val child = webViewNode.getChild(i) ?: continue
                val text = child.text?.toString()
                if (!text.isNullOrEmpty()) {
                    val matcher = URL_PATTERN.matcher(text)
                    if (matcher.find()) {
                        var url = matcher.group()
                        if (!url.startsWith("http")) {
                            url = "https://$url"
                        }
                        child.recycle()
                        return url
                    }
                }
                child.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting WebView URL: ${e.message}")
        }
        return null
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceInstance = null
        Log.d(TAG, "Service destroyed")
    }
}