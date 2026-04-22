package com.example.sentinalx.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * Base Activity with animated cyber background
 * Provides consistent animated background across all activities
 *
 * Usage:
 * 1. Extend BaseActivity instead of AppCompatActivity
 * 2. Call setupWithBackground() in onCreate with your content view
 * 3. Override getBackgroundConfig() to customize animation settings
 */
abstract class BaseActivity : AppCompatActivity() {

    private var backgroundAnimation: LottieAnimationView? = null
    private var rootContainer: FrameLayout? = null

    /**
     * Configuration for background animation
     */
    data class BackgroundConfig(
        val animationName: String = "cyber_splash",
        val speed: Float = 0.6f,
        val alpha: Float = 0.3f,
        val scaleX: Float = 1.1f,
        val scaleY: Float = 1.1f,
        val enabled: Boolean = true
    )

    /**
     * Override this to customize background animation settings
     */
    open fun getBackgroundConfig(): BackgroundConfig = BackgroundConfig()

    /**
     * Setup activity with animated background
     * Call this instead of setContentView()
     */
    protected fun setupWithBackground(contentView: View) {
        val config = getBackgroundConfig()

        if (!config.enabled) {
            // No background animation, just set content directly
            super.setContentView(contentView)
            return
        }

        // Create root container
        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // Setup background animation
        backgroundAnimation = LottieAnimationView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            try {
                val rawResId = resources.getIdentifier(config.animationName, "raw", packageName)
                if (rawResId != 0) {
                    setAnimation(rawResId)
                    repeatCount = LottieDrawable.INFINITE
                    speed = config.speed
                    alpha = config.alpha
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    scaleX = config.scaleX
                    scaleY = config.scaleY
                    playAnimation()
                } else {
                    android.util.Log.w("BaseActivity", "Animation ${config.animationName} not found")
                }
            } catch (e: Exception) {
                android.util.Log.e("BaseActivity", "Failed to load animation", e)
            }
        }

        // Ensure content view has transparent background to see animation
        if (contentView.background == null) {
            contentView.setBackgroundColor(Color.TRANSPARENT)
        }

        // Build hierarchy: animation layer -> content layer
        rootContainer?.apply {
            addView(backgroundAnimation)
            addView(contentView)
        }

        super.setContentView(rootContainer)
    }

    /**
     * Get reference to background animation for custom control
     */
    protected fun getBackgroundAnimation(): LottieAnimationView? = backgroundAnimation

    /**
     * Manually control background animation playback
     */
    protected fun pauseBackground() {
        try {
            backgroundAnimation?.pauseAnimation()
        } catch (e: Exception) {
            android.util.Log.w("BaseActivity", "Failed to pause background", e)
        }
    }

    protected fun resumeBackground() {
        try {
            backgroundAnimation?.resumeAnimation()
        } catch (e: Exception) {
            android.util.Log.w("BaseActivity", "Failed to resume background", e)
        }
    }

    protected fun stopBackground() {
        try {
            backgroundAnimation?.cancelAnimation()
        } catch (e: Exception) {
            android.util.Log.w("BaseActivity", "Failed to stop background", e)
        }
    }

    /**
     * Setup dark status bar (consistent with cyber theme)
     */
    protected fun setupDarkStatusBar() {
        try {
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0 // Dark icons
            }
        } catch (e: Exception) {
            android.util.Log.w("BaseActivity", "Status bar setup failed", e)
        }
    }

    override fun onPause() {
        super.onPause()
        pauseBackground()
    }

    override fun onResume() {
        super.onResume()
        resumeBackground()
    }

    override fun onDestroy() {
        stopBackground()
        backgroundAnimation = null
        rootContainer = null
        super.onDestroy()
    }
}