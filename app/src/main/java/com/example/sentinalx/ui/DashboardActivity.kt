package com.example.sentinalx.ui

import android.animation.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.setPadding
import com.example.sentinalx.service.LinkInterceptionAccessibilityService
import com.example.sentinalx.utils.CookieIsolationManager
import java.util.*
import kotlin.random.Random
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

class DashboardActivity : BaseActivity() {

    // --- UI Component Declarations ---
    private lateinit var rootLayout: FrameLayout
    private lateinit var mainLayout: LinearLayout
    private lateinit var hamburgerDropdown: LinearLayout
    private lateinit var dimOverlay: View
    private lateinit var currentTwinText: TextView
    private lateinit var protectionStatusDot: TextView
    private lateinit var animatedGraph: LinearLayout
    private lateinit var cyberHygieneCard: LinearLayout
    private lateinit var cyberHygieneText: TextView
    private lateinit var hygieneIcon: TextView
    private lateinit var cookieManagementCard: LinearLayout
    private lateinit var cookieCountText: TextView

    // --- Bottom Navigation Buttons ---
    private lateinit var btnDashboard: LinearLayout
    private lateinit var btnThreatIntel: LinearLayout
    private lateinit var btnTwinManager: LinearLayout
    private lateinit var btnSettings: LinearLayout
    private lateinit var btnLogout: LinearLayout

    // --- Dynamic State Variables ---
    private var isDropdownOpen = false
    private var isDestroyed = false
    private var currentTipIndex = 0
    private var activeTwinIndex = 0
    private lateinit var digitalTwins: List<String>

    // Dynamic Metrics State
    private var threatsBlocked = 24
    private var trackersAbsorbed = 156
    private var cookiesIntercepted = 89
    private var linksScannedData = mutableListOf(12, 18, 15, 28, 24, 35, 31, 42, 38, 48, 45, 52)
    private var sessionCookieCount = 0

    // Handlers
    private var animationHandler: Handler? = null
    private var dataUpdateHandler: Handler? = null
    private var cookieUpdateHandler: Handler? = null

    // Cyber Hygiene Tips
    private val cyberHygieneTips = listOf(
        "🛡️" to "Enable two-factor authentication on all your accounts for enhanced security",
        "🔒" to "Use unique, strong passwords with a mix of letters, numbers, and symbols",
        "⚡" to "Keep your software and operating system updated to patch security vulnerabilities",
        "🕵️" to "Be cautious of suspicious emails and never click on unknown attachments",
        "🌐" to "Always verify website URLs before entering sensitive information",
        "📱" to "Regularly review and audit app permissions on your mobile devices",
        "🔄" to "Back up your important data regularly to multiple secure locations",
        "🚫" to "Avoid using public Wi-Fi networks for sensitive transactions",
        "👥" to "Never share personal information through unsolicited phone calls or emails",
        "🧹" to "Clean your browser history and cookies periodically to maintain privacy"
    )

    companion object {
        // Cyber Theme Colors
        private const val PRIMARY_CYAN = "#00E5FF"
        private const val BRIGHT_CYAN = "#64FFDA"
        private const val SUCCESS_GREEN = "#34A853"
        private const val WARM_ORANGE = "#FBBC04"
        private const val SOFT_RED = "#EA4335"
        private const val PURPLE_ACCENT = "#BB86FC"
        private const val WHITE = "#FFFFFF"
        private const val TRANSPARENT_CARD = "#33FFFFFF"
        private const val BORDER_CYAN = "#4400E5FF"
        private const val TEXT_GRAY = "#B0BEC5"
        private val FALLBACK_TWINS = listOf("Sentinel-Alpha", "Guardian-Beta", "Defender-Gamma")
        private const val MAX_TWINS = 3
    }

    // --- Activity Lifecycle ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generatePersonalizedTwins()
        checkOverlayPermission()
        setupDarkStatusBar()
        initializeCookieManager()

        try {
            val contentView = createCyberDashboard()
            setupWithBackground(contentView)
            startAnimations()
            startLiveMonitoring()
            startCyberHygieneTips()
            startDataSimulation()
            startCookieMonitoring()
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "Error in onCreate", e)
        }
    }

    override fun getBackgroundConfig() = BackgroundConfig(
        speed = 0.5f,
        alpha = 0.4f,
        scaleX = 1.15f,
        scaleY = 1.15f
    )

    override fun onResume() {
        super.onResume()

        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            val intent = Intent(this, RegistrationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        updateProtectionIndicatorStatus()
        startProtectionStatusMonitoring()
        updateCookieCount()
    }

    override fun onDestroy() {
        isDestroyed = true
        animationHandler?.removeCallbacksAndMessages(null)
        animationHandler = null
        dataUpdateHandler?.removeCallbacksAndMessages(null)
        dataUpdateHandler = null
        cookieUpdateHandler?.removeCallbacksAndMessages(null)
        cookieUpdateHandler = null
        super.onDestroy()
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (isDropdownOpen) {
            toggleProfessionalDropdown()
        } else {
            super.onBackPressed()
        }
    }

    // --- UI Creation ---

    private fun createCyberDashboard(): View {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 50, 20, 0)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        createCyberHeader()
        createWelcomeMessage()
        createDigitalTwinCard()
        createLiveProtectionIndicator()
        createCookieManagementCard()
        createAnimatedGraphSection()
        createCyberHygieneTipsCard()

        mainLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        })

        createBottomNavigation()
        createProfessionalDropdownMenu()

        rootLayout.addView(mainLayout)
        return rootLayout
    }

    private fun createCyberHeader() {
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 24)
        }

        val brandContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val appTitle = TextView(this).apply {
            text = "SentinalX"
            textSize = 24f
            setTextColor(Color.parseColor(PRIMARY_CYAN))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.02f
        }

        val proLabel = TextView(this).apply {
            text = "PRO"
            textSize = 9f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.DEFAULT_BOLD
            setBackground(createPillBackground(SUCCESS_GREEN, 0.15f))
            setPadding(10, 5, 10, 5)
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(12, 4, 0, 0) }
        }

        brandContainer.addView(appTitle)
        brandContainer.addView(proLabel)

        val hamburgerBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
            setOnClickListener { toggleProfessionalDropdown() }
        }

        repeat(3) { index ->
            val line = View(this@DashboardActivity).apply {
                layoutParams = LinearLayout.LayoutParams(28, 3).apply {
                    setMargins(0, if (index == 0) 0 else 6, 0, 0)
                }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(WHITE))
                    cornerRadius = 2f
                }
            }
            hamburgerBtn.addView(line)
        }

        headerLayout.addView(brandContainer)
        headerLayout.addView(hamburgerBtn)
        mainLayout.addView(headerLayout)
    }

    private fun createWelcomeMessage() {
        val userName = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            .getString("user_name", "User")?.split(" ")?.firstOrNull() ?: "User"

        val welcomeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCyberCard()
            setPadding(20, 16, 20, 16)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val welcomeIcon = TextView(this).apply {
            text = "👋"
            textSize = 16f
            setPadding(0, 0, 12, 0)
        }

        val welcomeText = TextView(this).apply {
            text = "Welcome, $userName!"
            textSize = 15f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val securityStatus = TextView(this).apply {
            text = "🔒 SECURED"
            textSize = 10f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setBackground(createPillBackground(SUCCESS_GREEN, 0.15f))
            setPadding(10, 5, 10, 5)
        }

        welcomeContainer.addView(welcomeIcon)
        welcomeContainer.addView(welcomeText)
        welcomeContainer.addView(securityStatus)

        mainLayout.addView(welcomeContainer)
    }

    private fun createDigitalTwinCard() {
        val twinCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCyberCard()
            setPadding(24, 24, 24, 24)
            elevation = 6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val cardHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val headerTitle = TextView(this).apply {
            text = "Active Digital Twin"
            textSize = 14f
            setTextColor(Color.parseColor(TEXT_GRAY))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statusIndicator = TextView(this).apply {
            text = "● LIVE"
            textSize = 10f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setBackground(createPillBackground(SUCCESS_GREEN, 0.15f))
            setPadding(12, 6, 12, 6)
        }

        cardHeader.addView(headerTitle)
        cardHeader.addView(statusIndicator)

        currentTwinText = TextView(this).apply {
            text = digitalTwins[activeTwinIndex]
            textSize = 20f
            setTextColor(Color.parseColor(PRIMARY_CYAN))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            background = createSubtleGlow()
            setPadding(18, 14, 18, 14)
            gravity = Gravity.CENTER
        }

        val metricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }

        metricsRow.addView(createMetricItem(threatsBlocked.toString(), "Threats", SOFT_RED))
        metricsRow.addView(createMetricItem(trackersAbsorbed.toString(), "Trackers", WARM_ORANGE))
        metricsRow.addView(createMetricItem(cookiesIntercepted.toString(), "Cookies", SUCCESS_GREEN))

        twinCard.addView(cardHeader)
        twinCard.addView(currentTwinText)
        twinCard.addView(metricsRow)

        mainLayout.addView(twinCard)
    }

    private fun createLiveProtectionIndicator(): LinearLayout {
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createCyberCard()
            setPadding(24, 20, 24, 20)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            setOnClickListener {
                if (!isAccessibilityServiceEnabled()) {
                    openAccessibilitySettings()
                } else {
                    Toast.makeText(this@DashboardActivity, "Protection is active", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val isEnabled = isAccessibilityServiceEnabled()

        protectionStatusDot = TextView(this).apply {
            text = "●"
            textSize = 14f
            setTextColor(Color.parseColor(if (isEnabled) SUCCESS_GREEN else SOFT_RED))
            setPadding(0, 0, 12, 0)
        }

        val statusText = TextView(this).apply {
            text = if (isEnabled) "Protection Active" else "Tap to Enable"
            textSize = 14f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val monitoringText = TextView(this).apply {
            text = if (isEnabled) "8 Apps Protected" else "Setup Required"
            textSize = 12f
            setTextColor(Color.parseColor(if (isEnabled) SUCCESS_GREEN else WARM_ORANGE))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        statusCard.addView(protectionStatusDot)
        statusCard.addView(statusText)
        statusCard.addView(monitoringText)

        mainLayout.addView(statusCard)
        return statusCard
    }

    private fun createCookieManagementCard() {
        cookieManagementCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCookieGradientCard()
            setPadding(24, 22, 24, 22)
            elevation = 6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            setOnClickListener {
                showCookieManagementDialog()
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 14)
        }

        val cookieIcon = TextView(this).apply {
            text = "🍪"
            textSize = 26f
            setPadding(0, 0, 14, 0)
        }

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val cookieTitle = TextView(this).apply {
            text = "Cookie Management"
            textSize = 15f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val cookieSubtitle = TextView(this).apply {
            text = "Session Isolation Active"
            textSize = 11f
            setTextColor(Color.parseColor(PURPLE_ACCENT))
            alpha = 0.8f
            setPadding(0, 2, 0, 0)
        }

        titleColumn.addView(cookieTitle)
        titleColumn.addView(cookieSubtitle)

        val actionBtn = TextView(this).apply {
            text = "MANAGE"
            textSize = 11f
            setTextColor(Color.parseColor(PURPLE_ACCENT))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setBackground(createPillBackground(PURPLE_ACCENT, 0.2f))
            setPadding(14, 7, 14, 7)
        }

        headerRow.addView(cookieIcon)
        headerRow.addView(titleColumn)
        headerRow.addView(actionBtn)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }

        cookieCountText = TextView(this).apply {
            text = "0"
            textSize = 28f
            setTextColor(Color.parseColor(PURPLE_ACCENT))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        val statsLabel = TextView(this).apply {
            text = " cookies intercepted this session"
            textSize = 13f
            setTextColor(Color.parseColor(TEXT_GRAY))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 0, 0, 0)
        }

        statsRow.addView(cookieCountText)
        statsRow.addView(statsLabel)

        val featuresList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        featuresList.addView(createFeatureItem("✓", "Auto-clear on app close"))
        featuresList.addView(createFeatureItem("✓", "Third-party blocking"))
        featuresList.addView(createFeatureItem("✓", "Weekly deep cleanup"))

        cookieManagementCard.addView(headerRow)
        cookieManagementCard.addView(statsRow)
        cookieManagementCard.addView(featuresList)

        mainLayout.addView(cookieManagementCard)
    }

    private fun createFeatureItem(icon: String, text: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 6)

            var iconText = TextView(this@DashboardActivity).apply {
               // text = icon
                textSize = 12f
                setTextColor(Color.parseColor(SUCCESS_GREEN))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 10, 0)
            }

            val featureText = TextView(this@DashboardActivity).apply {
                this.text = text
                textSize = 12f
                setTextColor(Color.parseColor(TEXT_GRAY))
            }

            addView(iconText)
            addView(featureText)
        }
    }

    private fun createAnimatedGraphSection() {
        val graphCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCyberCard()
            setPadding(24, 24, 24, 24)
            elevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                320
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val graphHeader = TextView(this).apply {
            text = "Links Scanned (Real-time)"
            textSize = 13f
            setTextColor(Color.parseColor(TEXT_GRAY))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }

        val graphView = createAnimatedLineGraph()

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 0)
        }

        val currentValue = TextView(this).apply {
            text = "${linksScannedData.last()}"
            textSize = 24f
            setTextColor(Color.parseColor(PRIMARY_CYAN))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        val valueLabel = TextView(this).apply {
            text = " links/hour"
            textSize = 13f
            setTextColor(Color.parseColor(TEXT_GRAY))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val trend = if (linksScannedData.size >= 2) linksScannedData.last() - linksScannedData[linksScannedData.size - 2] else 0
        val trendIndicator = TextView(this).apply {
            text = if (trend > 0) "↗ +$trend" else if (trend < 0) "↘ $trend" else "— 0"
            textSize = 12f
            setTextColor(Color.parseColor(if (trend > 0) SUCCESS_GREEN else if (trend < 0) SOFT_RED else TEXT_GRAY))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        statsRow.addView(currentValue)
        statsRow.addView(valueLabel)
        statsRow.addView(trendIndicator)

        graphCard.addView(graphHeader)
        graphCard.addView(graphView)
        graphCard.addView(statsRow)

        animatedGraph = graphCard
        mainLayout.addView(graphCard)
    }

    private fun createAnimatedLineGraph(): View {
        return object : View(this@DashboardActivity) {
            private var animationProgress = 0f
            private val graphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2000
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animationProgress = it.animatedValue as Float
                    invalidate()
                }
            }

            init {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                graphAnimator.start()
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                if (linksScannedData.isEmpty()) return

                val paint = Paint().apply {
                    color = Color.parseColor(PRIMARY_CYAN)
                    strokeWidth = 4f
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                val pointPaint = Paint().apply {
                    color = Color.parseColor(PRIMARY_CYAN)
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }

                val glowPaint = Paint().apply {
                    color = Color.parseColor(PRIMARY_CYAN)
                    alpha = 60
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }

                val shadowPaint = Paint().apply {
                    color = Color.parseColor(PRIMARY_CYAN)
                    alpha = 30
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }

                val maxValue = linksScannedData.maxOrNull()?.toFloat() ?: 1f
                val minValue = linksScannedData.minOrNull()?.toFloat() ?: 0f
                val range = maxOf(maxValue - minValue, 1f)

                val graphWidth = width - 40f
                val graphHeight = height - 40f
                val stepX = graphWidth / maxOf(linksScannedData.size - 1, 1).toFloat()

                val path = Path()
                val startX = 20f
                val startY = 20f + graphHeight - ((linksScannedData[0] - minValue) / range) * graphHeight
                path.moveTo(startX, startY)

                val visiblePoints = (linksScannedData.size * animationProgress).toInt().coerceAtLeast(1)

                for (i in 1 until visiblePoints) {
                    val x = 20f + i * stepX
                    val y = 20f + graphHeight - ((linksScannedData[i] - minValue) / range) * graphHeight
                    path.lineTo(x, y)
                }

                path.lineTo(20f + (visiblePoints - 1) * stepX, 20f + graphHeight)
                path.lineTo(20f, 20f + graphHeight)
                path.close()
                canvas.drawPath(path, shadowPaint)

                for (i in 0 until visiblePoints - 1) {
                    val x1 = 20f + i * stepX
                    val y1 = 20f + graphHeight - ((linksScannedData[i] - minValue) / range) * graphHeight
                    val x2 = 20f + (i + 1) * stepX
                    val y2 = 20f + graphHeight - ((linksScannedData[i + 1] - minValue) / range) * graphHeight

                    canvas.drawLine(x1, y1, x2, y2, paint)
                }

                for (i in 0 until visiblePoints) {
                    val x = 20f + i * stepX
                    val y = 20f + graphHeight - ((linksScannedData[i] - minValue) / range) * graphHeight

                    canvas.drawCircle(x, y, 12f, glowPaint)
                    canvas.drawCircle(x, y, 6f, pointPaint)

                    val whiteDot = Paint().apply {
                        color = Color.WHITE
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(x, y, 2.5f, whiteDot)
                }
            }
        }
    }

    private fun createCyberHygieneTipsCard() {
        cyberHygieneCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createHygieneGradientCard()
            setPadding(24, 22, 24, 22)
            elevation = 5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 14)
        }

        hygieneIcon = TextView(this).apply {
            text = cyberHygieneTips[currentTipIndex].first
            textSize = 24f
            setPadding(0, 0, 14, 0)
        }

        val tipHeader = TextView(this).apply {
            text = "Security Tip"
            textSize = 12f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tipCounter = TextView(this).apply {
            text = "${currentTipIndex + 1}/${cyberHygieneTips.size}"
            textSize = 11f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            alpha = 0.7f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        headerRow.addView(hygieneIcon)
        headerRow.addView(tipHeader)
        headerRow.addView(tipCounter)

        cyberHygieneText = TextView(this).apply {
            text = cyberHygieneTips[currentTipIndex].second
            textSize = 13f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(5f, 1.15f)
        }

        cyberHygieneCard.addView(headerRow)
        cyberHygieneCard.addView(cyberHygieneText)

        mainLayout.addView(cyberHygieneCard)
    }

    private fun createBottomNavigation() {
        val navContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createBottomNavBackground()
            setPadding(8, 16, 8, 16)
            elevation = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        btnDashboard = createNavButton("📊", "Dashboard")
        btnThreatIntel = createNavButton("🔍", "Threats")
        btnTwinManager = createNavButton("👥", "Twins")
        btnSettings = createNavButton("⚙️", "Settings")
        btnLogout = createNavButton("🚪", "Logout")

        btnDashboard.setOnClickListener {
            showToast("Already on Dashboard")
        }

        btnThreatIntel.setOnClickListener {
            showToast("Threat Intelligence - Coming Soon")
        }

        btnTwinManager.setOnClickListener {
            val intent = Intent(this@DashboardActivity, TwinManagerActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            showToast("Settings - Coming Soon")
        }

        btnLogout.setOnClickListener {
            handleLogout()
        }

        navContainer.addView(btnDashboard)
        navContainer.addView(btnThreatIntel)
        navContainer.addView(btnTwinManager)
        navContainer.addView(btnSettings)
        navContainer.addView(btnLogout)

        mainLayout.addView(navContainer)
    }

    private fun createNavButton(icon: String, label: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 12, 8, 12)

            val iconText = TextView(this@DashboardActivity).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
            }

            val labelText = TextView(this@DashboardActivity).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(TEXT_GRAY))
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }

            addView(iconText)
            addView(labelText)
        }
    }

    private fun createMetricItem(value: String, label: String, color: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 0, 8, 0)

            val valueText = TextView(this@DashboardActivity).apply {
                text = value
                textSize = 18f
                setTextColor(Color.parseColor(color))
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val labelText = TextView(this@DashboardActivity).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(TEXT_GRAY))
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }

            addView(valueText)
            addView(labelText)
        }
    }

    // --- Cookie Management Dialog ---

    private fun showCookieManagementDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE121212"))
                cornerRadius = 20f
                setStroke(2, Color.parseColor(PURPLE_ACCENT))
            }
        }

        val title = TextView(this).apply {
            text = "🍪 Cookie Management"
            textSize = 20f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val subtitle = TextView(this).apply {
            text = "Session-based Isolation Protection"
            textSize = 13f
            setTextColor(Color.parseColor(PURPLE_ACCENT))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        val statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFFFFF"))
                cornerRadius = 12f
            }
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
        }

        val cookieStatusText = TextView(this).apply {
            text = "Session Cookies: $sessionCookieCount"
            textSize = 15f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val statusDescription = TextView(this).apply {
            text = "All cookies will be cleared when you close the app"
            textSize = 12f
            setTextColor(Color.parseColor(TEXT_GRAY))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        statsContainer.addView(cookieStatusText)
        statsContainer.addView(statusDescription)

        val clearButton = TextView(this).apply {
            text = "Clear All Cookies Now"
            textSize = 14f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor(SOFT_RED))
                cornerRadius = 12f
            }
            setPadding(0, 18, 0, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }

            setOnClickListener {
                clearAllCookiesWithAnimation()
            }
        }

        val closeButton = TextView(this).apply {
            text = "Close"
            textSize = 14f
            setTextColor(Color.parseColor(TEXT_GRAY))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = 12f
            }
            setPadding(0, 18, 0, 18)
        }

        dialogView.addView(title)
        dialogView.addView(subtitle)
        dialogView.addView(statsContainer)
        dialogView.addView(clearButton)
        dialogView.addView(closeButton)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearAllCookiesWithAnimation() {
        CookieIsolationManager.clearAllCookies {
            runOnUiThread {
                sessionCookieCount = 0
                cookiesIntercepted = 0
                updateCookieCount()

                Toast.makeText(this, "✅ All cookies cleared successfully!", Toast.LENGTH_SHORT).show()

                // Animate the cookie count
                val animator = ValueAnimator.ofInt(cookieCountText.text.toString().toIntOrNull() ?: 0, 0)
                animator.duration = 800
                animator.addUpdateListener {
                    cookieCountText.text = (it.animatedValue as Int).toString()
                }
                animator.start()
            }
        }
    }

    // --- Dropdown Menu ---

    private fun createProfessionalDropdownMenu() {
        dimOverlay = View(this).apply {
            setBackgroundColor(Color.parseColor("#70000000"))
            alpha = 0f
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (isDropdownOpen) {
                    toggleProfessionalDropdown()
                }
            }
        }

        hamburgerDropdown = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createDropdownBackground()
            setPadding(0, 12, 0, 12)
            elevation = 16f
            translationY = -400f
            visibility = View.GONE
            isClickable = true
            isFocusable = true
        }

        val menuItems = listOf(
            "Activity Dashboard" to "Real-time monitoring",
            "Cookie Manager" to "Manage cookie isolation",
            "Threat Intelligence" to "Security insights",
            "Digital Twin Manager" to "Manage identities",
            "Security Settings" to "Configure protection",
            "Browser Integration" to "Browser extensions",
            "Share App" to "Share with others",
            "Logout" to "Sign out securely"
        )

        menuItems.forEach { (title, description) ->
            hamburgerDropdown.addView(createDropdownMenuItem(title, description))
        }

        val dropdownParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(20, 120, 20, 0)
        }

        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        rootLayout.addView(dimOverlay, overlayParams)
        rootLayout.addView(hamburgerDropdown, dropdownParams)
    }

    private fun createDropdownMenuItem(title: String, description: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
            setOnClickListener {
                when (title) {
                    "Cookie Manager" -> {
                        toggleProfessionalDropdown()
                        showCookieManagementDialog()
                    }
                    "Digital Twin Manager" -> {
                        toggleProfessionalDropdown()
                        val intent = Intent(this@DashboardActivity, TwinManagerActivity::class.java)
                        startActivity(intent)
                    }
                    "Share App" -> {
                        toggleProfessionalDropdown()
                        shareApp()
                    }
                    "Logout" -> {
                        handleLogout()
                    }
                    else -> {
                        showToast("$title - Coming Soon")
                        toggleProfessionalDropdown()
                    }
                }
            }

            val titleText = TextView(this@DashboardActivity).apply {
                text = title
                textSize = 14f
                setTextColor(Color.parseColor(WHITE))
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val descText = TextView(this@DashboardActivity).apply {
                text = description
                textSize = 12f
                setTextColor(Color.parseColor(TEXT_GRAY))
                setPadding(0, 4, 0, 0)
            }

            addView(titleText)
            addView(descText)
        }
    }

    private fun toggleProfessionalDropdown() {
        if (isDropdownOpen) {
            val slideUp = ObjectAnimator.ofFloat(hamburgerDropdown, "translationY", 0f, -400f)
            val fadeOut = ObjectAnimator.ofFloat(dimOverlay, "alpha", 0.7f, 0f)

            AnimatorSet().apply {
                playTogether(slideUp, fadeOut)
                duration = 220
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        hamburgerDropdown.visibility = View.GONE
                        dimOverlay.visibility = View.GONE
                    }
                })
            }.start()
        } else {
            hamburgerDropdown.visibility = View.VISIBLE
            dimOverlay.visibility = View.VISIBLE

            val slideDown = ObjectAnimator.ofFloat(hamburgerDropdown, "translationY", -400f, 0f)
            val fadeIn = ObjectAnimator.ofFloat(dimOverlay, "alpha", 0f, 0.7f)

            AnimatorSet().apply {
                playTogether(slideDown, fadeIn)
                duration = 280
                interpolator = DecelerateInterpolator()
            }.start()
        }
        isDropdownOpen = !isDropdownOpen
    }

    // --- Animations and Updates ---

    private fun startAnimations() {
        animationHandler = Handler(Looper.getMainLooper())

        val pulseAnim = ObjectAnimator.ofFloat(protectionStatusDot, "alpha", 1f, 0.4f, 1f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        pulseAnim.start()

        val cookieGlow = ObjectAnimator.ofFloat(cookieManagementCard, "alpha", 1f, 0.92f, 1f).apply {
            duration = 3000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        cookieGlow.start()
    }

    private fun startLiveMonitoring() {
        // Monitoring started in startAnimations
    }

    private fun startCyberHygieneTips() {
        animationHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (!isDestroyed) {
                    currentTipIndex = (currentTipIndex + 1) % cyberHygieneTips.size

                    val slideOut = AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(cyberHygieneCard, "alpha", 1f, 0.3f),
                            ObjectAnimator.ofFloat(cyberHygieneCard, "translationX", 0f, -50f),
                            ObjectAnimator.ofFloat(cyberHygieneCard, "scaleX", 1f, 0.95f),
                            ObjectAnimator.ofFloat(cyberHygieneCard, "scaleY", 1f, 0.95f)
                        )
                        duration = 400
                        interpolator = AccelerateInterpolator()
                    }

                    slideOut.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            cyberHygieneText.text = cyberHygieneTips[currentTipIndex].second
                            hygieneIcon.text = cyberHygieneTips[currentTipIndex].first

                            val headerRow = cyberHygieneCard.getChildAt(0) as? LinearLayout
                            val counter = headerRow?.getChildAt(2) as? TextView
                            counter?.text = "${currentTipIndex + 1}/${cyberHygieneTips.size}"

                            val slideIn = AnimatorSet().apply {
                                playTogether(
                                    ObjectAnimator.ofFloat(cyberHygieneCard, "alpha", 0.3f, 1f),
                                    ObjectAnimator.ofFloat(cyberHygieneCard, "translationX", 50f, 0f),
                                    ObjectAnimator.ofFloat(cyberHygieneCard, "scaleX", 0.95f, 1f),
                                    ObjectAnimator.ofFloat(cyberHygieneCard, "scaleY", 0.95f, 1f)
                                )
                                duration = 500
                                interpolator = DecelerateInterpolator()
                            }
                            slideIn.start()
                        }
                    })

                    slideOut.start()
                    animationHandler?.postDelayed(this, 8000)
                }
            }
        }, 8000)
    }

    private fun startDataSimulation() {
        dataUpdateHandler = Handler(Looper.getMainLooper())

        dataUpdateHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (!isDestroyed) {
                    threatsBlocked += Random.nextInt(0, 2)
                    trackersAbsorbed += Random.nextInt(3, 12)
                    cookiesIntercepted += Random.nextInt(5, 20)
                    sessionCookieCount += Random.nextInt(2, 8)

                    val lastValue = linksScannedData.last()
                    val newValue = maxOf(lastValue + Random.nextInt(-2, 6), 1)

                    linksScannedData.removeAt(0)
                    linksScannedData.add(newValue)

                    updateMetrics()
                    updateCookieCount()
                    animatedGraph.findViewById<View>(animatedGraph.childCount - 1)?.invalidate()

                    dataUpdateHandler?.postDelayed(this, 5000)
                }
            }
        }, 5000)
    }

    private fun startCookieMonitoring() {
        cookieUpdateHandler = Handler(Looper.getMainLooper())

        cookieUpdateHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (!isDestroyed) {
                    updateCookieCount()
                    cookieUpdateHandler?.postDelayed(this, 3000)
                }
            }
        }, 3000)
    }

    private fun updateCookieCount() {
        if (::cookieCountText.isInitialized) {
            cookieCountText.text = sessionCookieCount.toString()
        }
    }

    private fun updateMetrics() {
        val twinCard = mainLayout.getChildAt(2) as? LinearLayout
        val metricsRow = twinCard?.getChildAt(2) as? LinearLayout

        metricsRow?.let { row ->
            (row.getChildAt(0) as? LinearLayout)?.let {
                (it.getChildAt(0) as? TextView)?.text = threatsBlocked.toString()
            }
            (row.getChildAt(1) as? LinearLayout)?.let {
                (it.getChildAt(0) as? TextView)?.text = trackersAbsorbed.toString()
            }
            (row.getChildAt(2) as? LinearLayout)?.let {
                (it.getChildAt(0) as? TextView)?.text = cookiesIntercepted.toString()
            }
        }

        val graphCard = animatedGraph
        val statsRow = graphCard.getChildAt(2) as? LinearLayout
        statsRow?.let { row ->
            (row.getChildAt(0) as? TextView)?.text = linksScannedData.last().toString()

            val trend = if (linksScannedData.size >= 2) linksScannedData.last() - linksScannedData[linksScannedData.size - 2] else 0
            (row.getChildAt(2) as? TextView)?.apply {
                text = if (trend > 0) "↗ +$trend" else if (trend < 0) "↘ $trend" else "— 0"
                setTextColor(Color.parseColor(if (trend > 0) SUCCESS_GREEN else if (trend < 0) SOFT_RED else TEXT_GRAY))
            }
        }
    }

    // --- System Methods ---

    private fun initializeCookieManager() {
        CookieIsolationManager.setupIsolatedCookies(this)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "com.example.sentinalx/.service.LinkInterceptionAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable SentinalX in Accessibility", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProtectionIndicatorStatus() {
        if (!::protectionStatusDot.isInitialized) return

        val isEnabled = isAccessibilityServiceEnabled()
        protectionStatusDot.setTextColor(Color.parseColor(if (isEnabled) SUCCESS_GREEN else SOFT_RED))

        val statusCard = mainLayout.getChildAt(3) as? LinearLayout
        statusCard?.let { card ->
            (card.getChildAt(1) as? TextView)?.text = if (isEnabled) "Protection Active" else "Tap to Enable"
            (card.getChildAt(2) as? TextView)?.apply {
                text = if (isEnabled) "8 Apps Protected" else "Setup Required"
                setTextColor(Color.parseColor(if (isEnabled) SUCCESS_GREEN else WARM_ORANGE))
            }
        }
    }

    private fun startProtectionStatusMonitoring() {
        animationHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (!isDestroyed) {
                    updateProtectionIndicatorStatus()
                    animationHandler?.postDelayed(this, 3000)
                }
            }
        }, 3000)
    }

    private fun generatePersonalizedTwins() {
        val sharedPrefs = getSharedPreferences("TwinDataPrefs", MODE_PRIVATE)
        val twinNameSet = sharedPrefs.getStringSet("digitalTwinNames", null)

        val loadedTwins = if (twinNameSet != null && twinNameSet.isNotEmpty()) {
            twinNameSet.toList().sorted()
        } else {
            FALLBACK_TWINS
        }

        digitalTwins = when {
            loadedTwins.size > MAX_TWINS -> loadedTwins.subList(0, MAX_TWINS)
            loadedTwins.size < MAX_TWINS -> {
                val paddedList = loadedTwins.toMutableList()
                while (paddedList.size < MAX_TWINS) {
                    paddedList.add(FALLBACK_TWINS[paddedList.size % FALLBACK_TWINS.size])
                }
                paddedList
            }
            else -> loadedTwins
        }

        if (activeTwinIndex >= digitalTwins.size) {
            activeTwinIndex = 0
        }
    }

    private fun handleLogout() {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().commit()

        if (isDropdownOpen) {
            toggleProfessionalDropdown()
        }

        Toast.makeText(this, "Clearing cookies and logging out...", Toast.LENGTH_SHORT).show()

        CookieIsolationManager.clearAllCookies {
            try {
                LinkInterceptionAccessibilityService.disableService()
            } catch (e: Exception) {
                android.util.Log.e("Dashboard", "Error disabling service: ${e.message}")
            }

            val registrationIntent = Intent(this, RegistrationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(registrationIntent)
            finish()
        }
    }

    private fun shareApp() {
        val appPackageName = packageName
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out SentinalX")
            putExtra(
                Intent.EXTRA_TEXT,
                "🛡️ Protect yourself with SentinalX!\n\n" +
                        "Download: https://play.google.com/store/apps/details?id=$appPackageName"
            )
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Share SentinalX"))
        } catch (e: Exception) {
            showToast("Unable to share")
        }
    }

    // --- Style Helpers ---

    private fun createCyberCard(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(TRANSPARENT_CARD))
            cornerRadius = 14f
            setStroke(2, Color.parseColor(BORDER_CYAN))
        }
    }

    private fun createCookieGradientCard(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                Color.parseColor("#1ABB86FC"),
                Color.parseColor("#2A9C27B0")
            )
            orientation = GradientDrawable.Orientation.TL_BR
            cornerRadius = 14f
            setStroke(2, Color.parseColor("#44BB86FC"))
        }
    }

    private fun createHygieneGradientCard(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                Color.parseColor("#1A4CAF50"),
                Color.parseColor("#2A34A853")
            )
            orientation = GradientDrawable.Orientation.TL_BR
            cornerRadius = 14f
            setStroke(2, Color.parseColor("#4434A853"))
        }
    }

    private fun createSubtleGlow(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#2200E5FF"))
            cornerRadius = 10f
            setStroke(1, Color.parseColor(PRIMARY_CYAN))
        }
    }

    private fun createBottomNavBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#DD000000"))
            cornerRadii = floatArrayOf(16f, 16f, 16f, 16f, 0f, 0f, 0f, 0f)
            setStroke(2, Color.parseColor(BORDER_CYAN))
        }
    }

    private fun createDropdownBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#EE000000"))
            cornerRadius = 16f
            setStroke(2, Color.parseColor(BORDER_CYAN))
        }
    }

    private fun createPillBackground(color: String, alpha: Float): GradientDrawable {
        return GradientDrawable().apply {
            val colorInt = Color.parseColor(color)
            val alphaColor = Color.argb(
                (255 * alpha).toInt(),
                Color.red(colorInt),
                Color.green(colorInt),
                Color.blue(colorInt)
            )
            setColor(alphaColor)
            cornerRadius = 20f
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}