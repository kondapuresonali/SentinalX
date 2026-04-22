package com.example.sentinalx.ui

import android.animation.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.sentinalx.utils.NetworkMonitor // Assuming this is available


/**
 * Premium Cybersecurity Splash Screen
 * With fallback animated background
 */
class SplashVideoActivity : AppCompatActivity() {

    private lateinit var rootLayout: FrameLayout
    private var lottieAnimation: LottieAnimationView? = null
    private var fallbackAnimation: View? = null
    private lateinit var overlayContainer: LinearLayout
    private lateinit var appName: TextView
    private lateinit var digitalTwin: TextView
    private lateinit var skipButton: TextView

    private var navigationHandler: Handler? = null
    private var animationHandler: Handler? = null

    private var isDestroyed = false
    private var isSkipped = false
    private var isNavigationScheduled = false
    private var hasLottieAnimation = false

    private val activeAnimators = mutableListOf<Animator>()

    companion object {
        // Timing constants
        private const val ANIMATION_START_DELAY = 0L
        private const val LOTTIE_FADE_IN_DURATION = 300L
        private const val TEXT_APPEAR_DELAY = 800L            // Delay to allow Lottie time to fully render
        private const val SKIP_BUTTON_DELAY = 1800L
        private const val TOTAL_SPLASH_DURATION = 6000L
        private const val EXIT_FADE_DURATION = 400L

        // Enhanced color palette
        private const val BG_BLACK = "#000000"
        private const val SEMI_WHITE = "#DDFFFFFF"
        private const val SKIP_BG = "#33FFFFFF"
        private const val PRIMARY_CYAN = "#00E5FF"
        private const val BRIGHT_GRAY_BLUE = "#64748B" // Used for Digital Twin Subtitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            // Block back button on splash
        }

        try {
            setupSystemUI()
            createLayout()

            // Start animation sequence
            startAnimationSequence()
            scheduleNavigation()
        } catch (e: Exception) {
            android.util.Log.e("SplashVideo", "Error in onCreate", e)
            performNavigationSafely()
        }
    }

    private fun setupSystemUI() {
        try {
            window.statusBarColor = Color.parseColor(BG_BLACK)
            window.navigationBarColor = Color.parseColor(BG_BLACK)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0
            }
        } catch (e: Exception) {
            android.util.Log.w("SplashVideo", "System UI setup failed", e)
        }
    }

    private fun createLayout() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(BG_BLACK))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Try to load Lottie Animation first
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val size = maxOf(screenWidth, screenHeight)

        try {
            try {
                val rawResId = resources.getIdentifier("cyber_splash", "raw", packageName)
                android.util.Log.d("SplashVideo", "Resource ID found: $rawResId")

                if (rawResId != 0) {
                    lottieAnimation = LottieAnimationView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(size, size).apply {
                            gravity = Gravity.CENTER
                            leftMargin = -40
                            topMargin = 100
                        }

                        try {
                            setAnimation(rawResId)
                        } catch (animError: Exception) {
                            android.util.Log.e("SplashVideo", "setAnimation() failed", animError)
                            throw animError
                        }

                        repeatCount = LottieDrawable.INFINITE
                        speed = 1.0f
                        alpha = 0f
                        scaleX = 1.05f
                        scaleY = 1.05f

                        setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE)
                    }
                    hasLottieAnimation = true
                    android.util.Log.d("SplashVideo", "✅ Lottie animation loaded successfully!")
                } else {
                    android.util.Log.w("SplashVideo", "⚠️ cyber_splash resource ID is 0, using fallback")
                    createFallbackAnimation()
                }
            } catch (innerError: Exception) {
                android.util.Log.e("SplashVideo", "❌ Inner exception loading Lottie", innerError)
                createFallbackAnimation()
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashVideo", "❌ Outer exception, using fallback", e)
            createFallbackAnimation()
        }

        // Text Overlay Container
        overlayContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 700
                leftMargin = -40
            }
        }

        // App Name
        appName = TextView(this).apply {
            text = "SENTINALX"
            textSize = 38f
            setTextColor(Color.parseColor("#FFFFFF"))
            typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            gravity = Gravity.CENTER
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            setPadding(0, 0, 0, 8)
            letterSpacing = 0.25f
        }

        // Digital Twin subtitle
        digitalTwin = TextView(this).apply {
            text = "━━━  DIGITAL TWIN  ━━━"
            textSize = 11f
            setTextColor(Color.parseColor(BRIGHT_GRAY_BLUE))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            alpha = 0f
            setPadding(0, 12, 0, 0)
            letterSpacing = 0.15f
        }

        // Skip Button
        skipButton = TextView(this).apply {
            text = "SKIP"
            textSize = 13f
            setTextColor(Color.parseColor(SEMI_WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            alpha = 0f
            letterSpacing = 0.1f
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor(SKIP_BG))
            elevation = 8f

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 50f)
                    }
                }
                clipToOutline = true
            }

            setOnClickListener { performSkip() }
        }

        val skipParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 70, 50, 0)
        }

        // Assemble hierarchy
        overlayContainer.addView(appName)
        overlayContainer.addView(digitalTwin)

        // Add animation (Lottie or fallback)
        if (hasLottieAnimation && lottieAnimation != null) {
            rootLayout.addView(lottieAnimation)
        } else if (fallbackAnimation != null) {
            rootLayout.addView(fallbackAnimation)
        }

        rootLayout.addView(overlayContainer)
        rootLayout.addView(skipButton, skipParams)

        setContentView(rootLayout)
    }

    private fun createFallbackAnimation() {
        // Create animated gradient background as fallback
        fallbackAnimation = object : View(this) {
            private val paint = Paint().apply {
                isAntiAlias = true
            }
            private var animationProgress = 0f
            private val gradientAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 8000
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener {
                    animationProgress = it.animatedValue as Float
                    invalidate()
                }
            }

            init {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                alpha = 0f
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                val centerX = width / 2f
                val centerY = height / 2f
                val radius = maxOf(width, height) * 0.8f

                val gradient = RadialGradient(
                    centerX,
                    centerY,
                    radius,
                    intArrayOf(
                        Color.parseColor("#2200E5FF"),
                        Color.parseColor("#1164FFDA"),
                        Color.parseColor("#00000000")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )

                paint.shader = gradient
                canvas.save()
                canvas.rotate(animationProgress, centerX, centerY)
                canvas.drawCircle(centerX, centerY, radius, paint)
                canvas.restore()

                // Draw animated circles
                for (i in 0..2) {
                    val offset = (animationProgress + i * 120) % 360
                    val x = centerX + kotlin.math.cos(Math.toRadians(offset.toDouble())).toFloat() * 200
                    val y = centerY + kotlin.math.sin(Math.toRadians(offset.toDouble())).toFloat() * 200

                    paint.shader = RadialGradient(
                        x, y, 150f,
                        intArrayOf(
                            Color.parseColor("#4400E5FF"),
                            Color.parseColor("#00000000")
                        ),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(x, y, 150f, paint)
                }
            }

            fun startAnimation() {
                gradientAnimator.start()
            }

            fun stopAnimation() {
                gradientAnimator.cancel()
            }
        }

        hasLottieAnimation = false
    }

    private fun startAnimationSequence() {
        animationHandler = Handler(Looper.getMainLooper())

        // 1. Start background animation and fade in (300ms)
        startBackgroundAnimation()

        // 2. Start text animation (delayed until the background fade-in is complete)
        animationHandler?.postDelayed({
            if (!isDestroyed && !isSkipped) {
                animateText()
            }
        }, LOTTIE_FADE_IN_DURATION + 100) // 100ms buffer after Lottie fade-in

        // 3. Skip button appears later
        animationHandler?.postDelayed({
            if (!isDestroyed && !isSkipped) {
                animateSkipButton()
            }
        }, SKIP_BUTTON_DELAY)
    }

    private fun startBackgroundAnimation() {
        val backgroundView = if (hasLottieAnimation) lottieAnimation else fallbackAnimation

        if (backgroundView != null) {
            // Start the actual animation playback
            if (hasLottieAnimation) {
                lottieAnimation?.playAnimation()
            } else {
                (fallbackAnimation as? View)?.let { view ->
                    try {
                        view.javaClass.getMethod("startAnimation").invoke(view)
                    } catch (e: Exception) {
                        android.util.Log.e("SplashVideo", "Fallback animation start failed via reflection", e)
                    }
                }
            }

            // Fade in the background layer
            val fadeInAnimator = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, if (hasLottieAnimation) 1f else 0.7f).apply {
                duration = LOTTIE_FADE_IN_DURATION
                interpolator = android.view.animation.LinearInterpolator()
            }

            fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // ⚡ TEXT AND SKIP BUTTON WILL BE LAUNCHED BY THE postDelayed IN startAnimationSequence
                }
            })

            fadeInAnimator.start()
            activeAnimators.add(fadeInAnimator)
        }
    }

    private fun animateText() {
        val nameAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(appName, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(appName, "scaleY", 0.9f, 1f),
                ObjectAnimator.ofFloat(appName, "translationY", 30f, 0f)
            )
            duration = 1200
            interpolator = android.view.animation.DecelerateInterpolator(2f)
        }

        nameAnim.start()
        activeAnimators.add(nameAnim)

        startBreathingEffect()

        animationHandler?.postDelayed({
            if (!isDestroyed && !isSkipped) {
                val digitalAnim = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(digitalTwin, "alpha", 0f, 0.7f),
                        ObjectAnimator.ofFloat(digitalTwin, "translationY", 20f, 0f)
                    )
                    duration = 800
                    interpolator = android.view.animation.DecelerateInterpolator()
                }
                digitalAnim.start()
                activeAnimators.add(digitalAnim)
            }
        }, 600) // Small delay between name and subtitle fade-in
    }

    private fun startBreathingEffect() {
        val breathe = ObjectAnimator.ofFloat(appName, "scaleX", 1f, 1.01f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val breatheY = ObjectAnimator.ofFloat(appName, "scaleY", 1f, 1.01f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        breathe.start()
        breatheY.start()
        activeAnimators.add(breathe)
        activeAnimators.add(breatheY)
    }

    private fun animateSkipButton() {
        val fadeIn = ObjectAnimator.ofFloat(skipButton, "alpha", 0f, 0.8f).apply {
            duration = 500
            interpolator = android.view.animation.DecelerateInterpolator()
        }

        fadeIn.start()
        activeAnimators.add(fadeIn)
    }

    private fun performSkip() {
        if (isSkipped || isDestroyed) return
        isSkipped = true

        cancelAllAnimations()

        val quickFade = ObjectAnimator.ofFloat(rootLayout, "alpha", 1f, 0f).apply {
            duration = 300
            interpolator = android.view.animation.AccelerateInterpolator()
        }

        quickFade.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                performNavigationSafely()
            }
        })

        quickFade.start()
    }

    private fun scheduleNavigation() {
        navigationHandler = Handler(Looper.getMainLooper())
        navigationHandler?.postDelayed({
            if (!isDestroyed && !isSkipped) {
                android.util.Log.d("SplashVideo", "⏱️ Starting exit at ${TOTAL_SPLASH_DURATION}ms")
                startExitAnimation()
            }
        }, TOTAL_SPLASH_DURATION)

        android.util.Log.d("SplashVideo", "⏰ Navigation scheduled for ${TOTAL_SPLASH_DURATION}ms")
    }

    private fun startExitAnimation() {
        val exitFade = ObjectAnimator.ofFloat(rootLayout, "alpha", 1f, 0f).apply {
            duration = EXIT_FADE_DURATION
            interpolator = android.view.animation.AccelerateInterpolator()
        }

        exitFade.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                performNavigationSafely()
            }
        })

        exitFade.start()
        activeAnimators.add(exitFade)
    }

    private fun performNavigationSafely() {
        if (isDestroyed || isNavigationScheduled) return
        isNavigationScheduled = true

        try {
            val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

            val intent = if (isLoggedIn) {
                val userName = sharedPref.getString("user_name", "User") ?: "User"
                Intent(this, DashboardActivity::class.java).apply {
                    putExtra("user_name", userName)
                }
            } else {
                Intent(this, RegistrationActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()

        } catch (e: Exception) {
            android.util.Log.e("SplashVideo", "Navigation failed", e)
            finish()
        }
    }

    private fun cancelAllAnimations() {
        activeAnimators.forEach { animator ->
            try {
                animator.cancel()
            } catch (e: Exception) {
                android.util.Log.w("SplashVideo", "Failed to cancel animator", e)
            }
        }
        activeAnimators.clear()

        try {
            lottieAnimation?.cancelAnimation()
        } catch (e: Exception) {
            android.util.Log.w("SplashVideo", "Failed to cancel Lottie", e)
        }

        try {
            // Stop the custom fallback animation using reflection
            fallbackAnimation?.let { view ->
                if (view.javaClass.simpleName.contains("$1")) {
                    view.javaClass.getMethod("stopAnimation").invoke(view)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SplashVideo", "Failed to stop fallback", e)
        }

        navigationHandler?.removeCallbacksAndMessages(null)
        navigationHandler = null
        animationHandler?.removeCallbacksAndMessages(null)
        animationHandler = null
    }

    override fun onDestroy() {
        isDestroyed = true
        cancelAllAnimations()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try {
            lottieAnimation?.pauseAnimation()
        } catch (e: Exception) {
            android.util.Log.w("SplashVideo", "Pause failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isSkipped && !isDestroyed) {
            try {
                lottieAnimation?.resumeAnimation()
            } catch (e: Exception) {
                android.util.Log.w("SplashVideo", "Resume failed", e)
            }
        }
    }
}