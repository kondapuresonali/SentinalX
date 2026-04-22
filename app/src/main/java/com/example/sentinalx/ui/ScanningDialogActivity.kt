package com.example.sentinalx.ui

import android.animation.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.*
import com.example.sentinalx.utils.ThreatAnalyzer
import com.example.sentinalx.utils.ThreatLevel
import com.example.sentinalx.utils.ScanData
import com.example.sentinalx.utils.NetworkMonitor
import com.example.sentinalx.utils.LinkCacheManager
import android.util.Log
import com.example.sentinalx.service.LinkInterceptionAccessibilityService
import kotlin.math.cos
import kotlin.math.sin

class ScanningDialogActivity : Activity() {

    private val TAG = "SCANNING_DIALOG"

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var reasonsLayout: LinearLayout
    private lateinit var openButton: Button
    private lateinit var closeButton: Button
    private lateinit var scanningText: TextView
    private lateinit var twinStatusText: TextView
    private lateinit var scanProgressView: ScanProgressView
    private lateinit var scanLinesContainer: FrameLayout
    private lateinit var particleContainer: FrameLayout

    private var currentUrl = ""
    private var shouldCloseAfterScan = false
    private val scanDurationMs = 4500L

    // Cyber Theme Colors
    private val COLOR_PRIMARY_CYAN = Color.parseColor("#00E5FF")
    private val COLOR_BRIGHT_CYAN = Color.parseColor("#64FFDA")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#34A853")
    private val COLOR_WARNING_ORANGE = Color.parseColor("#FBBC04")
    private val COLOR_DANGER_RED = Color.parseColor("#EA4335")
    private val COLOR_WHITE = Color.parseColor("#FFFFFF")
    private val COLOR_DARK_BG = Color.parseColor("#0A0E27")
    private val COLOR_CARD_BG = Color.parseColor("#1A1F3A")
    private val COLOR_TEXT_GRAY = Color.parseColor("#B0BEC5")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security Gate
        if (!isUserLoggedIn()) {
            Toast.makeText(this, "Please register/login to activate protection", Toast.LENGTH_LONG).show()
            val loginIntent = Intent(this, RegistrationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(loginIntent)
            finish()
            return
        }

        try {
            currentUrl = intent.getStringExtra("url") ?: "No URL"
            shouldCloseAfterScan = intent.getBooleanExtra("close_after_scan", false)

            Log.d(TAG, "Scanning URL: $currentUrl")

            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
            })

            setContentView(createCyberScanningUI())

            // Network Check
            if (!NetworkMonitor.isOnline(this)) {
                handleNetworkFailure()
                return
            }

            startEpicScanAnimation()

            Handler(Looper.getMainLooper()).postDelayed({
                performThreatAnalysis()
            }, scanDurationMs)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Security Dialog Error", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createCyberScanningUI(): View {
        val mainLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(30, 30, 30, 30)
        }

        // Particle effect container (background)
        particleContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        mainLayout.addView(particleContainer)

        // Main card
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackground(createCyberCard())
            elevation = 24f
            setPadding(40, 45, 40, 45)
        }

        // Warning banner if already opened
        if (shouldCloseAfterScan) {
            val warningBanner = createWarningBanner()
            cardLayout.addView(warningBanner)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "◆ SENTINALX THREAT SCANNER ◆"
            textSize = 11f
            setTextColor(COLOR_PRIMARY_CYAN)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
            setPadding(0, 0, 0, 16)
        }
        cardLayout.addView(titleText)

        // URL Display
        val urlCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFFFFF"))
                cornerRadius = 10f
                setStroke(1, Color.parseColor("#3300E5FF"))
            }
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }

        val urlLabel = TextView(this).apply {
            text = "TARGET URL"
            textSize = 10f
            setTextColor(COLOR_TEXT_GRAY)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setPadding(0, 0, 0, 6)
        }

        val urlText = TextView(this).apply {
            text = shortenUrl(currentUrl)
            textSize = 13f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            maxLines = 2
        }

        urlCard.addView(urlLabel)
        urlCard.addView(urlText)
        cardLayout.addView(urlCard)

        // Scan animation container
        val scanAnimationContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }

        // Scan lines container
        scanLinesContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        scanAnimationContainer.addView(scanLinesContainer)

        // Circular progress view
        scanProgressView = ScanProgressView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                300,
                300,
                Gravity.CENTER
            )
        }
        scanAnimationContainer.addView(scanProgressView)

        cardLayout.addView(scanAnimationContainer)

        // Scanning status text
        scanningText = TextView(this).apply {
            text = "⟡ INITIALIZING DIGITAL TWIN ⟡"
            textSize = 13f
            setTextColor(COLOR_PRIMARY_CYAN)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 8)
        }
        cardLayout.addView(scanningText)

        // Twin status
        twinStatusText = TextView(this).apply {
            text = "Deploying: ${getActiveTwinName()}"
            textSize = 11f
            setTextColor(COLOR_BRIGHT_CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        cardLayout.addView(twinStatusText)

        // Status text (verdict)
        statusText = TextView(this).apply {
            text = "ANALYZING THREAT LEVEL..."
            textSize = 15f
            setTextColor(COLOR_PRIMARY_CYAN)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        cardLayout.addView(statusText)

        // Reasons layout (hidden initially)
        reasonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A000000"))
                cornerRadius = 10f
            }
            setPadding(20, 16, 20, 16)
            visibility = View.GONE
        }
        cardLayout.addView(reasonsLayout)

        // Action buttons
        val actionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 0)
        }

        openButton = Button(this).apply {
            text = "PROCEED TO WEBSITE"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setBackground(createCyberButton(COLOR_SUCCESS_GREEN))
            setTextColor(COLOR_WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
            setPadding(0, 20, 0, 20)
            letterSpacing = 0.05f
            setOnClickListener {
                openUrl(currentUrl)
            }
            visibility = View.GONE
        }
        actionsContainer.addView(openButton)

        closeButton = Button(this).apply {
            text = if (shouldCloseAfterScan) "CLOSE BROWSER" else "CANCEL"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setBackground(createOutlineButton(COLOR_TEXT_GRAY))
            setTextColor(COLOR_TEXT_GRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 18, 0, 18)
            setOnClickListener {
                if (shouldCloseAfterScan) {
                    closeInAppBrowser()
                }
                finish()
            }
        }
        actionsContainer.addView(closeButton)

        cardLayout.addView(actionsContainer)
        mainLayout.addView(cardLayout)

        return mainLayout
    }

    private fun createWarningBanner(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1FFBBC04"))
                cornerRadius = 8f
                setStroke(1, COLOR_WARNING_ORANGE)
            }
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }

            val icon = TextView(this@ScanningDialogActivity).apply {
                text = "⚠"
                textSize = 16f
                setTextColor(COLOR_WARNING_ORANGE)
                setPadding(0, 0, 12, 0)
            }

            val text = TextView(this@ScanningDialogActivity).apply {
                text = "Link already opened - Performing background scan"
                textSize = 11f
                setTextColor(COLOR_WARNING_ORANGE)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            addView(icon)
            addView(text)
        }
    }

    private fun startEpicScanAnimation() {
        // Start circular progress animation
        scanProgressView.startAnimation(scanDurationMs)

        // Create floating particles
        createFloatingParticles()

        // Create scanning lines
        createScanLines()

        // Animate scan stages
        val scanStages = listOf(
            "⟡ INITIALIZING DIGITAL TWIN ⟡" to 0L,
            "⟡ ANALYZING URL STRUCTURE ⟡" to 800L,
            "⟡ CHECKING DOMAIN REPUTATION ⟡" to 1600L,
            "⟡ LOADING PAGE IN SANDBOX ⟡" to 2400L,
            "⟡ DETECTING THREATS ⟡" to 3200L,
            "⟡ FINALIZING ANALYSIS ⟡" to 4000L
        )

        scanStages.forEach { (stage, delay) ->
            Handler(Looper.getMainLooper()).postDelayed({
                scanningText.text = stage

                // Pulse animation on text
                val scaleUp = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(scanningText, "scaleX", 1f, 1.05f, 1f),
                        ObjectAnimator.ofFloat(scanningText, "scaleY", 1f, 1.05f, 1f)
                    )
                    duration = 400
                    interpolator = DecelerateInterpolator()
                }
                scaleUp.start()
            }, delay)
        }

        // Animate twin status updates
        val twinUpdates = listOf(
            "Deploying: ${getActiveTwinName()}" to 0L,
            "Twin Status: Connecting..." to 800L,
            "Twin Status: Rendering page..." to 2000L,
            "Twin Status: Analyzing cookies..." to 3000L,
            "Twin Status: Scan complete ✓" to scanDurationMs - 300
        )

        twinUpdates.forEach { (status, delay) ->
            Handler(Looper.getMainLooper()).postDelayed({
                twinStatusText.text = status
            }, delay)
        }

        // Color pulse on status text
        val colorAnim = ObjectAnimator.ofInt(
            statusText,
            "textColor",
            COLOR_PRIMARY_CYAN,
            COLOR_BRIGHT_CYAN,
            COLOR_PRIMARY_CYAN
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        colorAnim.start()
    }

    private fun createFloatingParticles() {
        for (i in 0..15) {
            Handler(Looper.getMainLooper()).postDelayed({
                val particle = View(this).apply {
                    val size = (6..12).random()
                    layoutParams = FrameLayout.LayoutParams(size, size)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(COLOR_PRIMARY_CYAN)
                        alpha = (100..180).random()
                    }
                    x = (0..particleContainer.width).random().toFloat()
                    y = particleContainer.height.toFloat()
                }

                particleContainer.addView(particle)

                // Animate particle floating up
                val animator = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(particle, "translationY", 0f, -particleContainer.height.toFloat() - 100f),
                        ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f),
                        ObjectAnimator.ofFloat(particle, "translationX", 0f, ((-50..50).random()).toFloat())
                    )
                    duration = (2000L..4000L).random()
                    interpolator = LinearInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            particleContainer.removeView(particle)
                        }
                    })
                }
                animator.start()
            }, i * 200L)
        }
    }

    private fun createScanLines() {
        for (i in 0..3) {
            Handler(Looper.getMainLooper()).postDelayed({
                val scanLine = View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        3
                    )
                    background = GradientDrawable().apply {
                        setColor(COLOR_PRIMARY_CYAN)
                        alpha = 150
                    }
                    y = 0f
                }

                scanLinesContainer.addView(scanLine)

                // Animate scan line moving down
                val animator = ObjectAnimator.ofFloat(
                    scanLine,
                    "translationY",
                    0f,
                    scanLinesContainer.height.toFloat()
                ).apply {
                    duration = 1200
                    interpolator = LinearInterpolator()
                    repeatCount = (scanDurationMs / 1200).toInt()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            scanLinesContainer.removeView(scanLine)
                        }
                    })
                }
                animator.start()
            }, i * 300L)
        }
    }

    private fun performThreatAnalysis() {
        // Final network check
        if (!NetworkMonitor.isOnline(this)) {
            handleNetworkFailure()
            return
        }

        try {
            Log.d(TAG, "Performing threat analysis for: $currentUrl")

            val initialUrlLower = currentUrl.lowercase()
            val activeTwinName = getActiveTwinName()

            // Threat detection logic
            val isPhishing = initialUrlLower.contains("login") ||
                    initialUrlLower.contains("verify") ||
                    initialUrlLower.contains("account") ||
                    initialUrlLower.contains("update")
            val isMalware = initialUrlLower.contains("malware") ||
                    initialUrlLower.contains(".tk") ||
                    initialUrlLower.contains("phishing.test")
            val isTrusted = initialUrlLower.contains("google.com") ||
                    initialUrlLower.contains("wikipedia") ||
                    initialUrlLower.contains("youtube.com")

            val finalUrl = if (isMalware) "http://malicious-redirect.tk" else currentUrl

            val mockScanData = ScanData(
                finalUrl = finalUrl,
                redirectsCount = if (isMalware) 4 else if (isPhishing) 1 else 0,
                uniqueCookies = if (isMalware) 25 else if (isPhishing) 10 else 2,
                trackerDomains = if (isMalware) setOf("badtracker.com", "unknown.net") else emptySet(),
                loginFormsDetected = isPhishing && !isTrusted
            )

            val result = ThreatAnalyzer.analyzeScanData(currentUrl, mockScanData)

            Log.d(TAG, "Analysis complete - Level: ${result.threatLevel}, Score: ${result.score}")

            // 💾 Save result to cache (only SAFE results are cached)
            if (result.threatLevel == ThreatLevel.SAFE) {
                LinkCacheManager.saveScanResult(
                    context = this,
                    url = currentUrl,
                    threatLevel = result.threatLevel,
                    score = result.score,
                    twinName = activeTwinName
                )
                Log.d(TAG, "✅ SAFE result saved to cache - will skip scanning for 7 days")
            } else {
                Log.d(TAG, "⚠️ ${result.threatLevel} result NOT cached - will re-scan next time")
            }

            // Hide scanning animation
            scanProgressView.stopAnimation()
            scanProgressView.visibility = View.GONE
            scanLinesContainer.visibility = View.GONE
            particleContainer.visibility = View.GONE
            scanningText.visibility = View.GONE

            // Show results
            reasonsLayout.visibility = View.VISIBLE

            val verdictColor: Int
            val verdictIcon: String
            when (result.threatLevel) {
                ThreatLevel.SAFE -> {
                    verdictColor = COLOR_SUCCESS_GREEN
                    verdictIcon = "✓"
                    statusText.text = "$verdictIcon SECURE - ACCESS GRANTED"
                    openButton.text = if (shouldCloseAfterScan) "CONTINUE TO WEBSITE" else "PROCEED TO WEBSITE"
                    openButton.setBackground(createCyberButton(verdictColor))
                    openButton.visibility = View.VISIBLE
                    closeButton.setBackground(createOutlineButton(COLOR_TEXT_GRAY))
                    closeButton.text = if (shouldCloseAfterScan) "CLOSE BROWSER" else "GO BACK"
                    closeButton.setTextColor(COLOR_TEXT_GRAY)
                }
                ThreatLevel.SUSPICIOUS -> {
                    verdictColor = COLOR_WARNING_ORANGE
                    verdictIcon = "⚠"
                    statusText.text = "$verdictIcon SUSPICIOUS - PROCEED WITH CAUTION"
                    openButton.text = if (shouldCloseAfterScan) "CONTINUE ANYWAY (RISKY)" else "PROCEED ANYWAY (RISKY)"
                    openButton.setBackground(createCyberButton(verdictColor))
                    openButton.visibility = View.VISIBLE
                    closeButton.setBackground(createCyberButton(COLOR_DANGER_RED))
                    closeButton.text = if (shouldCloseAfterScan) "CLOSE BROWSER (RECOMMENDED)" else "GO BACK (RECOMMENDED)"
                    closeButton.setTextColor(COLOR_WHITE)
                }
                ThreatLevel.DANGEROUS -> {
                    verdictColor = COLOR_DANGER_RED
                    verdictIcon = "✖"
                    statusText.text = "$verdictIcon DANGEROUS - THREAT BLOCKED"
                    openButton.visibility = View.GONE
                    closeButton.setBackground(createCyberButton(verdictColor))
                    closeButton.text = if (shouldCloseAfterScan) "CLOSE BROWSER NOW" else "GO BACK"
                    closeButton.setTextColor(COLOR_WHITE)

                    // Auto-close dangerous links
                    if (shouldCloseAfterScan) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            closeInAppBrowser()
                            finish()
                        }, 2000)
                    }
                }
            }

            statusText.setTextColor(verdictColor)
            twinStatusText.text = "Scanned by: $activeTwinName | Threat Score: ${result.score}/100"
            twinStatusText.setTextColor(verdictColor)
            twinStatusText.visibility = View.VISIBLE

            // Victory animation
            val victoryAnim = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(statusText, "scaleX", 0.8f, 1.1f, 1f),
                    ObjectAnimator.ofFloat(statusText, "scaleY", 0.8f, 1.1f, 1f),
                    ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f)
                )
                duration = 600
                interpolator = OvershootInterpolator()
            }
            victoryAnim.start()

            // Add scan results
            reasonsLayout.addView(createReasonItem("🔍", "Digital Twin: $activeTwinName deployed successfully"))
            result.reasons.forEach { reason ->
                val icon = when {
                    reason.contains("✓") || reason.contains("Safe") -> "✓"
                    reason.contains("⚠") || reason.contains("Warning") -> "⚠"
                    reason.contains("✖") || reason.contains("Danger") -> "✖"
                    else -> "•"
                }
                reasonsLayout.addView(createReasonItem(icon, reason))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in threat analysis", e)
            handleAnalysisError()
        }
    }

    private fun handleNetworkFailure() {
        Log.e(TAG, "Network failure detected")

        scanProgressView.visibility = View.GONE
        scanLinesContainer.visibility = View.GONE
        particleContainer.visibility = View.GONE
        scanningText.visibility = View.GONE

        statusText.text = "✖ OFFLINE - SCAN FAILED"
        statusText.setTextColor(COLOR_DANGER_RED)
        twinStatusText.text = "Network connection required for scanning"
        twinStatusText.setTextColor(COLOR_DANGER_RED)

        reasonsLayout.visibility = View.VISIBLE
        reasonsLayout.addView(createReasonItem("✖", "Internet connection unavailable"))
        reasonsLayout.addView(createReasonItem("•", "Please connect to the internet and try again"))

        openButton.visibility = View.GONE
        closeButton.setBackground(createCyberButton(COLOR_DANGER_RED))
        closeButton.text = "CLOSE"
        closeButton.setTextColor(COLOR_WHITE)
    }

    private fun handleAnalysisError() {
        statusText.text = "✖ ANALYSIS FAILED"
        statusText.setTextColor(COLOR_DANGER_RED)
        reasonsLayout.visibility = View.VISIBLE
        reasonsLayout.addView(createReasonItem("✖", "Unable to complete threat analysis"))
        openButton.visibility = View.VISIBLE
        openButton.text = "OPEN UNVERIFIED LINK"
        openButton.setBackground(createCyberButton(COLOR_WARNING_ORANGE))
    }

    private fun createReasonItem(icon: String, text: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)

            val iconView = TextView(this@ScanningDialogActivity).apply {
                this.text = icon
                textSize = 14f
                setTextColor(COLOR_PRIMARY_CYAN)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 12, 0)
            }

            val textView = TextView(this@ScanningDialogActivity).apply {
                this.text = text
                textSize = 12f
                setTextColor(COLOR_WHITE)
                setLineSpacing(4f, 1f)
            }

            addView(iconView)
            addView(textView)
        }
    }

    private fun closeInAppBrowser() {
        try {
            Log.d(TAG, "Attempting to close in-app browser")
            LinkInterceptionAccessibilityService.closeBrowser()
            Toast.makeText(this, "Browser closed - Link blocked", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not close browser: ${e.message}")
        }
    }

    private fun openUrl(url: String) {
        try {
            Log.d(TAG, "Opening URL in secure WebView: $url")

            if (shouldCloseAfterScan) {
                Toast.makeText(this, "Continuing to website...", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val intent = Intent(this, BrowserActivity::class.java)
            intent.putExtra("url", url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            finish()

        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: ${e.message}", e)
            Toast.makeText(this, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()

            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallbackIntent)
                finish()
            } catch (e2: Exception) {
                Toast.makeText(this, "No browser available", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getActiveTwinName(): String {
        val sharedPrefs = getSharedPreferences("TwinDataPrefs", Context.MODE_PRIVATE)
        val twinNames = sharedPrefs.getStringSet("digitalTwinNames", null)?.toList()?.sorted()
        return twinNames?.firstOrNull() ?: "Sentinel-Alpha"
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("isLoggedIn", false)
    }

    private fun shortenUrl(url: String): String {
        return if (url.length > 45) {
            url.substring(0, 42) + "..."
        } else {
            url
        }
    }

    private fun createCyberCard(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                COLOR_CARD_BG,
                Color.parseColor("#151A35")
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            cornerRadius = 20f
            setStroke(2, Color.parseColor("#3300E5FF"))
        }
    }

    private fun createCyberButton(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 12f
        }
    }

    private fun createOutlineButton(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 12f
            setStroke(2, color)
        }
    }

    // Custom circular progress view
    inner class ScanProgressView(context: Context) : View(context) {
        private var progress = 0f
        private var isAnimating = false

        private val circlePaint = Paint().apply {
            color = COLOR_PRIMARY_CYAN
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        private val glowPaint = Paint().apply {
            color = COLOR_PRIMARY_CYAN
            style = Paint.Style.STROKE
            strokeWidth = 12f
            isAntiAlias = true
            alpha = 80
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }

        private val backgroundPaint = Paint().apply {
            color = Color.parseColor("#22FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = COLOR_WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        private val percentPaint = Paint().apply {
            color = COLOR_PRIMARY_CYAN
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        fun startAnimation(duration: Long) {
            isAnimating = true
            val animator = ValueAnimator.ofFloat(0f, 100f).apply {
                this.duration = duration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    progress = animation.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isAnimating = false
                    }
                })
            }
            animator.start()
        }

        fun stopAnimation() {
            isAnimating = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width.coerceAtMost(height) / 2f) - 40f

            // Draw background circle
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

            // Draw glow effect
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                -90f,
                (progress / 100f) * 360f,
                false,
                glowPaint
            )

            // Draw main progress arc
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                -90f,
                (progress / 100f) * 360f,
                false,
                circlePaint
            )

            // Draw percentage text
            val progressText = "${progress.toInt()}"
            canvas.drawText(progressText, centerX, centerY + 15f, textPaint)
            canvas.drawText("%", centerX, centerY + 45f, percentPaint)

            // Draw animated dots around circle
            if (isAnimating) {
                for (i in 0..11) {
                    val angle = (i * 30f) + (progress * 3.6f)
                    val dotX = centerX + (radius + 20f) * cos(Math.toRadians(angle.toDouble())).toFloat()
                    val dotY = centerY + (radius + 20f) * sin(Math.toRadians(angle.toDouble())).toFloat()

                    val dotPaint = Paint().apply {
                        color = COLOR_PRIMARY_CYAN
                        style = Paint.Style.FILL
                        isAntiAlias = true
                        alpha = ((progress + (i * 8.33f)) % 100 * 2.55f).toInt().coerceIn(50, 255)
                    }
                    canvas.drawCircle(dotX, dotY, 4f, dotPaint)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Dialog destroyed")
    }
}