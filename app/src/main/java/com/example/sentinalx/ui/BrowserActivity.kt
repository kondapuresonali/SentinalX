package com.example.sentinalx.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sentinalx.ui.theme.SentinalXTheme
import com.example.sentinalx.utils.CookieDetector
import com.example.sentinalx.utils.CookieIsolationManager

class BrowserActivity : ComponentActivity() {

    private val TAG = "BrowserActivity"
    private lateinit var webView: WebView
    private lateinit var cookieDetector: CookieDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get URL from intent
        val url = intent.getStringExtra("url") ?: "https://www.google.com"

        // Initialize cookie isolation
        CookieIsolationManager.setupIsolatedCookies(this)

        // Setup cookie detector
        cookieDetector = CookieDetector { cookieInfo ->
            runOnUiThread {
                Toast.makeText(this, cookieInfo.message, Toast.LENGTH_LONG).show()
            }
        }

        // Handle back press (modern way - AndroidX compatible)
        onBackPressedDispatcher.addCallback(this) {
            if (::webView.isInitialized && webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }

        setContent {
            SentinalXTheme {
                BrowserScreen(
                    initialUrl = url,
                    onWebViewCreated = { webView = it },
                    cookieDetector = cookieDetector,
                    onOpenInChrome = { urlToOpen ->
                        openInChrome(urlToOpen)
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }

        Log.d(TAG, "✅ BrowserActivity started with URL: $url")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all cookies when activity closes (session-only)
        CookieIsolationManager.clearAllCookies {
            Log.d(TAG, "🧹 Session cookies cleared on activity close")
        }

        // Clear detector
        if (::cookieDetector.isInitialized) {
            cookieDetector.reset()
        }

        // Clean up WebView
        if (::webView.isInitialized) {
            webView.destroy()
        }
    }

    private fun openInChrome(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Try to open in Chrome
            intent.setPackage("com.android.chrome")

            startActivity(intent)

            Log.d(TAG, "✅ Opened in Chrome: $url")

            Toast.makeText(this, "Opening in Chrome (fresh session)", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // If Chrome not installed, show browser chooser
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "❌ Failed to open browser: ${e2.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    cookieDetector: CookieDetector,
    onOpenInChrome: (String) -> Unit,
    onClose: () -> Unit
) {
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showCookieIndicator by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    actions = {
                        // Cookie indicator
                        if (showCookieIndicator) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Cookies active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        // Open in Chrome button
                        IconButton(onClick = { onOpenInChrome(currentUrl) }) {
                            Icon(Icons.Default.ExitToApp, "Open in Chrome")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Loading progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ArrowBack, "Back") },
                    label = { Text("Back") },
                    selected = false,
                    onClick = {
                        webViewInstance?.let { webView ->
                            if (webView.canGoBack()) {
                                webView.goBack()
                            }
                        }
                    },
                    enabled = canGoBack
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ArrowForward, "Forward") },
                    label = { Text("Forward") },
                    selected = false,
                    onClick = {
                        webViewInstance?.let { webView ->
                            if (webView.canGoForward()) {
                                webView.goForward()
                            }
                        }
                    },
                    enabled = canGoForward
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Refresh, "Reload") },
                    label = { Text("Reload") },
                    selected = false,
                    onClick = {
                        webViewInstance?.reload()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Delete, "Clear") },
                    label = { Text("Clear") },
                    selected = false,
                    onClick = {
                        CookieIsolationManager.clearAllCookies {
                            Toast.makeText(context, "Cookies cleared", Toast.LENGTH_SHORT).show()
                            showCookieIndicator = false
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecureWebView(
                url = initialUrl,
                onWebViewCreated = { webView ->
                    onWebViewCreated(webView)
                    webViewInstance = webView
                },
                onUrlChanged = { currentUrl = it },
                onTitleChanged = { pageTitle = it },
                onLoadingChanged = { isLoading = it },
                onProgressChanged = { loadingProgress = it },
                onNavigationStateChanged = { back, forward ->
                    canGoBack = back
                    canGoForward = forward
                },
                cookieDetector = cookieDetector,
                onCookieDetected = { showCookieIndicator = true }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureWebView(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    cookieDetector: CookieDetector,
    onCookieDetected: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Basic settings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                // Setup WebView client
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onUrlChanged(it) }
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        onNavigationStateChanged(canGoBack(), canGoForward())
                        view?.title?.let { onTitleChanged(it) }

                        // Monitor cookies after page loads
                        url?.let { pageUrl ->
                            view?.let { webView ->
                                cookieDetector.monitorCookies(webView, pageUrl)

                                // Detect cookie banner
                                cookieDetector.detectCookieBanner(webView) { hasBanner ->
                                    if (hasBanner) {
                                        onCookieDetected()
                                    }
                                }
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false // Let WebView handle all URLs
                    }
                }

                // Setup Chrome client for progress
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }

                // Load URL
                loadUrl(url)

                // Return WebView reference
                onWebViewCreated(this)
            }
        },
        update = { }
    )
}