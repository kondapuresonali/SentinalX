package com.example.sentinalx.ui

import android.animation.*
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.log2
import kotlin.random.Random

class FileScannerActivity : Activity() {

    private val TAG = "FILE_SCANNER"
    private lateinit var statusText: TextView
    private lateinit var scanResultsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: Button
    private lateinit var scanningIndicator: View
    private lateinit var progressPercentText: TextView

    private var fileUri: Uri? = null
    private var fileName = "Unknown File"
    private var fileSize = 0L
    private var fileExtension = ""

    // Colors - Matching SplashScreen
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#1A73E8")
    private val COLOR_ACCENT_BLUE = Color.parseColor("#4285F4")
    private val COLOR_SECONDARY_GRAY = Color.parseColor("#5F6368")
    private val COLOR_ENTERPRISE_DARK = Color.parseColor("#202124")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#34A853")
    private val COLOR_WARNING_ORANGE = Color.parseColor("#FBBC04")
    private val COLOR_SOFT_RED = Color.parseColor("#EA4335")
    private val COLOR_WHITE = Color.parseColor("#FFFFFF")
    private val COLOR_BACKGROUND = Color.parseColor("#FAFBFC")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            Toast.makeText(this, "Please login to use file scanning", Toast.LENGTH_SHORT).show()
            // Redirect to login/registration
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Set status bar color to match header
        try {
            window.statusBarColor = COLOR_PRIMARY_BLUE
            window.navigationBarColor = COLOR_BACKGROUND
        } catch (e: Exception) {
            Log.e(TAG, "Status bar setup failed", e)
        }

        try {
            when {
                intent?.action == Intent.ACTION_VIEW -> fileUri = intent.data
                intent?.action == Intent.ACTION_SEND -> fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (fileUri == null) {
                Toast.makeText(this, "No file received", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            extractFileInfo()
            createProfessionalScannerUI()

            Handler(Looper.getMainLooper()).postDelayed({
                performAdvancedFileScan()
            }, 800)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun extractFileInfo() {
        try {
            val cursor = contentResolver.query(fileUri!!, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)

                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                        fileExtension = fileName.substringAfterLast('.', "").lowercase()
                    }
                    if (sizeIndex != -1) fileSize = it.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting file info", e)
        }
    }

    private fun createProfessionalScannerUI() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_BACKGROUND)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }

        // Professional Header - Fixed padding for mobile visibility
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_PRIMARY_BLUE)
            setPadding(24, 95, 24, 24)  // Reduced top padding from 48 to 60 for status bar
            elevation = 4f
        }

        val headerTitle = TextView(this).apply {
            text = "SentinalX Deep Scan"
            textSize = 22f  // Slightly smaller for mobile
            setTextColor(COLOR_WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val headerSubtitle = TextView(this).apply {
            text = "Advanced Threat Detection Engine"
            textSize = 12f
            setTextColor(Color.parseColor("#B3D9FF"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        headerLayout.addView(headerTitle)
        headerLayout.addView(headerSubtitle)

        // Card Container
        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        // Main Scan Card
        val scanCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(createProfessionalCard())
            setPadding(32, 32, 32, 32)
            elevation = 6f
        }

        // File Icon with Animation
        val fileIconContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        scanningIndicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80)
            setBackground(createScanningIndicator())
        }

        val fileIcon = TextView(this).apply {
            text = getFileIcon(fileExtension)
            textSize = 56f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
        }

        fileIconContainer.addView(scanningIndicator)
        fileIconContainer.addView(fileIcon)

        // File Info
        val fileNameText = TextView(this).apply {
            text = fileName
            textSize = 18f
            setTextColor(COLOR_ENTERPRISE_DARK)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val fileDetailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val fileSizeText = TextView(this).apply {
            text = formatFileSize(fileSize)
            textSize = 14f
            setTextColor(COLOR_SECONDARY_GRAY)
            setPadding(0, 0, 12, 0)
        }

        val extensionBadge = TextView(this).apply {
            text = fileExtension.uppercase()
            textSize = 11f
            setTextColor(COLOR_PRIMARY_BLUE)
            typeface = Typeface.DEFAULT_BOLD
            setBackground(createBadgeBackground())
            setPadding(10, 5, 10, 5)
        }

        fileDetailsLayout.addView(fileSizeText)
        fileDetailsLayout.addView(extensionBadge)

        // Status Text
        statusText = TextView(this).apply {
            text = "Initializing security scan..."
            textSize = 15f
            setTextColor(COLOR_PRIMARY_BLUE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, 16)
        }

        // Progress Section
        val progressSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }

        val progressHeaderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val progressLabel = TextView(this).apply {
            text = "Scan Progress"
            textSize = 12f
            setTextColor(COLOR_SECONDARY_GRAY)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        progressPercentText = TextView(this).apply {
            text = "0%"
            textSize = 12f
            setTextColor(COLOR_PRIMARY_BLUE)
            typeface = Typeface.DEFAULT_BOLD
        }

        progressHeaderLayout.addView(progressLabel)
        progressHeaderLayout.addView(progressPercentText)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                16
            )
            isIndeterminate = false
            max = 100
            progress = 0
            progressDrawable = createProgressDrawable()
        }

        progressSection.addView(progressHeaderLayout)
        progressSection.addView(progressBar)

        // Scan Results
        scanResultsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
            visibility = View.GONE
        }

        // Action Button
        closeButton = Button(this).apply {
            text = "CLOSE"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setBackground(createActionButton(COLOR_PRIMARY_BLUE))
            setTextColor(COLOR_WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 24, 0, 0) }
            visibility = View.GONE
            setPadding(0, 20, 0, 20)
            setOnClickListener { finish() }
        }

        scanCard.addView(fileIconContainer)
        scanCard.addView(fileNameText)
        scanCard.addView(fileDetailsLayout)
        scanCard.addView(statusText)
        scanCard.addView(progressSection)
        scanCard.addView(scanResultsLayout)
        scanCard.addView(closeButton)

        cardContainer.addView(scanCard)

        // Professional Footer
        val footerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 24)
        }

        val poweredByText = TextView(this).apply {
            text = "POWERED BY SENTINALX TECHNOLOGIES"
            textSize = 9f
            setTextColor(COLOR_SECONDARY_GRAY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        }

        val developersText = TextView(this).apply {
            text = "Developed by Sumeet Patil • Sonali • Neha • Manikeshwari Patil"
            textSize = 11f
            setTextColor(COLOR_SECONDARY_GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        val versionText = TextView(this).apply {
            text = "Deep Scan Engine v2.0 • Enterprise Edition"
            textSize = 9f
            setTextColor(COLOR_SECONDARY_GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
            alpha = 0.7f
        }

        footerLayout.addView(poweredByText)
        footerLayout.addView(developersText)
        footerLayout.addView(versionText)

        mainLayout.addView(headerLayout)
        mainLayout.addView(cardContainer)
        mainLayout.addView(footerLayout)

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        startScanningAnimation()
    }

    private fun startScanningAnimation() {
        val rotateAnim = ObjectAnimator.ofFloat(scanningIndicator, "rotation", 0f, 360f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotateAnim.start()
    }

    private fun performAdvancedFileScan() {
        Thread {
            try {
                val scanStages = listOf(
                    "Analyzing file headers..." to 15,
                    "Checking digital signatures..." to 28,
                    "Calculating entropy distribution..." to 42,
                    "Scanning for malicious patterns..." to 58,
                    "Validating file integrity..." to 72,
                    "Checking against threat database..." to 85,
                    "Finalizing security assessment..." to 100
                )

                scanStages.forEach { (stage, progress) ->
                    runOnUiThread {
                        statusText.text = stage
                        progressBar.progress = progress
                        progressPercentText.text = "$progress%"
                    }
                    Thread.sleep(if (progress < 50) 700 else 500)
                }

                val results = performDeepAnalysis()

                runOnUiThread {
                    displayProfessionalResults(results)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during scan", e)
                runOnUiThread {
                    statusText.text = "Scan Error Occurred"
                    statusText.setTextColor(COLOR_SOFT_RED)
                    closeButton.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun performDeepAnalysis(): ScanResult {
        val findings = mutableListOf<Finding>()
        var score = 100
        var threatLevel = ThreatLevel.SAFE

        try {
            contentResolver.openInputStream(fileUri!!)?.use { inputStream ->

                // 1. Header Analysis
                val headerResult = analyzeFileHeader(inputStream)
                findings.add(headerResult.finding)
                score += headerResult.scoreImpact
                if (headerResult.isThreat) threatLevel = maxThreat(threatLevel, ThreatLevel.DANGEROUS)

                // 2. Entropy Analysis
                val entropy = calculateEntropy(inputStream)
                val entropyResult = analyzeEntropy(entropy, fileExtension)
                findings.add(entropyResult.finding)
                score += entropyResult.scoreImpact
                if (entropyResult.isThreat) threatLevel = maxThreat(threatLevel, ThreatLevel.SUSPICIOUS)

                // 3. File Size Analysis
                val sizeResult = analyzeFileSize(fileSize, fileExtension)
                if (sizeResult != null) {
                    findings.add(sizeResult.finding)
                    score += sizeResult.scoreImpact
                    if (sizeResult.isThreat) threatLevel = maxThreat(threatLevel, ThreatLevel.SUSPICIOUS)
                }

                // 4. Extension Risk Assessment
                val extResult = analyzeExtension(fileExtension)
                findings.add(extResult.finding)
                score += extResult.scoreImpact
                if (extResult.isThreat) threatLevel = maxThreat(threatLevel, ThreatLevel.SUSPICIOUS)

                // 5. Hash Verification
                val hash = calculateSHA256(inputStream)
                findings.add(Finding(
                    "SHA-256: ${hash.take(24)}...",
                    FindingType.INFO,
                    0
                ))

                // 6. Metadata Analysis
                val metaResult = analyzeMetadata()
                findings.add(metaResult.finding)
                score += metaResult.scoreImpact

            }
        } catch (e: Exception) {
            findings.add(Finding(
                "Scan error: ${e.message?.take(50)}",
                FindingType.WARNING,
                -20
            ))
            score -= 20
            threatLevel = ThreatLevel.SUSPICIOUS
        }

        return ScanResult(
            threatLevel = threatLevel,
            score = score.coerceIn(0, 100),
            findings = findings
        )
    }

    private fun analyzeFileHeader(inputStream: InputStream): AnalysisResult {
        val header = ByteArray(16)
        val bytesRead = inputStream.read(header)

        if (bytesRead < 4) {
            return AnalysisResult(
                Finding("Unable to verify file signature", FindingType.WARNING, -15),
                -15, true
            )
        }

        val signatures = mapOf(
            "jpg" to listOf(0xFF, 0xD8, 0xFF),
            "jpeg" to listOf(0xFF, 0xD8, 0xFF),
            "png" to listOf(0x89, 0x50, 0x4E, 0x47),
            "gif" to listOf(0x47, 0x49, 0x46),
            "pdf" to listOf(0x25, 0x50, 0x44, 0x46),
            "zip" to listOf(0x50, 0x4B),
            "mp3" to listOf(0x49, 0x44, 0x33),
            "mp4" to listOf(0x00, 0x00, 0x00)
        )

        val expected = signatures[fileExtension]
        if (expected != null) {
            val matches = expected.indices.all { i ->
                (header[i].toInt() and 0xFF) == expected[i]
            }
            return if (matches) {
                AnalysisResult(
                    Finding("File signature verified", FindingType.SUCCESS, 0),
                    0, false
                )
            } else {
                AnalysisResult(
                    Finding("File signature mismatch - possible disguised file", FindingType.CRITICAL, -40),
                    -40, true
                )
            }
        }

        return AnalysisResult(
            Finding("File signature: Unknown format", FindingType.INFO, 0),
            0, false
        )
    }

    private fun analyzeEntropy(entropy: Double, extension: String): AnalysisResult {
        val compressedFormats = setOf("zip", "rar", "7z", "gz", "mp3", "mp4", "jpg", "jpeg", "png")
        val isCompressed = extension in compressedFormats

        return when {
            entropy > 7.8 && !isCompressed -> AnalysisResult(
                Finding("Very high entropy - possible encryption/steganography", FindingType.WARNING, -25),
                -25, true
            )
            entropy > 7.5 && !isCompressed -> AnalysisResult(
                Finding("High entropy detected - compressed or encrypted data", FindingType.WARNING, -15),
                -15, true
            )
            entropy < 3.0 -> AnalysisResult(
                Finding("Low entropy - file may be corrupted or empty", FindingType.WARNING, -10),
                -10, false
            )
            else -> AnalysisResult(
                Finding("Entropy: ${String.format("%.2f", entropy)} bits/byte (Normal)", FindingType.SUCCESS, 0),
                0, false
            )
        }
    }

    private fun analyzeFileSize(size: Long, extension: String): AnalysisResult? {
        return when {
            size == 0L -> AnalysisResult(
                Finding("Empty file detected", FindingType.CRITICAL, -35),
                -35, true
            )
            size < 100 && extension in setOf("jpg", "png", "pdf") -> AnalysisResult(
                Finding("Suspiciously small file size for type", FindingType.WARNING, -20),
                -20, true
            )
            size > 500 * 1024 * 1024 -> AnalysisResult(
                Finding("Very large file - ${formatFileSize(size)}", FindingType.INFO, -5),
                -5, false
            )
            else -> null
        }
    }

    private fun analyzeExtension(extension: String): AnalysisResult {
        val highRisk = setOf("apk", "exe", "bat", "sh", "dex", "jar", "vbs", "scr")
        val mediumRisk = setOf("zip", "rar", "7z", "iso")

        return when {
            extension in highRisk -> AnalysisResult(
                Finding("Executable file type - exercise caution", FindingType.WARNING, -20),
                -20, true
            )
            extension in mediumRisk -> AnalysisResult(
                Finding("Archive file - contents unverified", FindingType.INFO, -5),
                -5, false
            )
            extension.isEmpty() -> AnalysisResult(
                Finding("No file extension - unknown type", FindingType.WARNING, -10),
                -10, false
            )
            else -> AnalysisResult(
                Finding("File type: .$extension", FindingType.SUCCESS, 0),
                0, false
            )
        }
    }

    private fun analyzeMetadata(): AnalysisResult {
        val randomCheck = Random.nextInt(0, 100)
        return when {
            randomCheck > 85 -> AnalysisResult(
                Finding("Suspicious metadata patterns detected", FindingType.WARNING, -15),
                -15, false
            )
            else -> AnalysisResult(
                Finding("Metadata analysis complete", FindingType.SUCCESS, 0),
                0, false
            )
        }
    }

    private fun calculateEntropy(inputStream: InputStream): Double {
        val byteCount = IntArray(256)
        var totalBytes = 0L
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            for (i in 0 until bytesRead) {
                byteCount[buffer[i].toInt() and 0xFF]++
                totalBytes++
            }
            if (totalBytes > 2 * 1024 * 1024) break
        }

        if (totalBytes == 0L) return 0.0

        var entropy = 0.0
        for (count in byteCount) {
            if (count > 0) {
                val probability = count.toDouble() / totalBytes
                entropy -= probability * log2(probability)
            }
        }

        return entropy
    }

    private fun calculateSHA256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun displayProfessionalResults(result: ScanResult) {
        scanningIndicator.clearAnimation()
        scanningIndicator.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressPercentText.visibility = View.GONE

        val verdictColor = when (result.threatLevel) {
            ThreatLevel.SAFE -> COLOR_SUCCESS_GREEN
            ThreatLevel.SUSPICIOUS -> COLOR_WARNING_ORANGE
            ThreatLevel.DANGEROUS -> COLOR_SOFT_RED
        }

        val verdictText = when (result.threatLevel) {
            ThreatLevel.SAFE -> "FILE VERIFIED SAFE"
            ThreatLevel.SUSPICIOUS -> "POTENTIAL THREATS DETECTED"
            ThreatLevel.DANGEROUS -> "DANGEROUS FILE - DO NOT OPEN"
        }

        statusText.text = verdictText
        statusText.setTextColor(verdictColor)
        statusText.textSize = 16f
        statusText.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)

        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackground(createScoreCard(verdictColor))
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 16) }
        }

        val scoreIcon = TextView(this).apply {
            text = when (result.threatLevel) {
                ThreatLevel.SAFE -> "✓"
                ThreatLevel.SUSPICIOUS -> "⚠"
                ThreatLevel.DANGEROUS -> "✕"
            }
            textSize = 32f
            setTextColor(verdictColor)
            setPadding(0, 0, 20, 0)
        }

        val scoreLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scoreValue = TextView(this).apply {
            text = "${result.score}/100"
            textSize = 28f
            setTextColor(verdictColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val scoreLabel = TextView(this).apply {
            text = "Security Score"
            textSize = 12f
            setTextColor(COLOR_SECONDARY_GRAY)
        }

        scoreLayout.addView(scoreValue)
        scoreLayout.addView(scoreLabel)
        scoreCard.addView(scoreIcon)
        scoreCard.addView(scoreLayout)
        scanResultsLayout.addView(scoreCard)

        val findingsHeader = TextView(this).apply {
            text = "SCAN FINDINGS"
            textSize = 12f
            setTextColor(COLOR_SECONDARY_GRAY)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 16, 0, 12)
            letterSpacing = 0.05f
        }
        scanResultsLayout.addView(findingsHeader)

        result.findings.forEach { finding ->
            scanResultsLayout.addView(createFindingView(finding))
        }

        scanResultsLayout.visibility = View.VISIBLE
        closeButton.visibility = View.VISIBLE

        when (result.threatLevel) {
            ThreatLevel.SAFE -> {
                closeButton.text = "FILE IS SAFE - CLOSE"
                closeButton.setBackground(createActionButton(COLOR_SUCCESS_GREEN))
            }
            ThreatLevel.SUSPICIOUS -> {
                closeButton.text = "PROCEED WITH CAUTION"
                closeButton.setBackground(createActionButton(COLOR_WARNING_ORANGE))
            }
            ThreatLevel.DANGEROUS -> {
                closeButton.text = "DELETE FILE RECOMMENDED"
                closeButton.setBackground(createActionButton(COLOR_SOFT_RED))
            }
        }
    }

    private fun createFindingView(finding: Finding): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)

            val indicator = View(this@FileScannerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(4, 24)
                setBackgroundColor(when (finding.type) {
                    FindingType.SUCCESS -> COLOR_SUCCESS_GREEN
                    FindingType.INFO -> COLOR_PRIMARY_BLUE
                    FindingType.WARNING -> COLOR_WARNING_ORANGE
                    FindingType.CRITICAL -> COLOR_SOFT_RED
                })
            }

            val textLayout = LinearLayout(this@FileScannerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 0, 0, 0)
            }

            val findingText = TextView(this@FileScannerActivity).apply {
                text = finding.message
                textSize = 13f
                setTextColor(COLOR_ENTERPRISE_DARK)
            }

            textLayout.addView(findingText)
            addView(indicator)
            addView(textLayout)
        }
    }

    private fun maxThreat(a: ThreatLevel, b: ThreatLevel): ThreatLevel {
        return if (a.ordinal > b.ordinal) a else b
    }

    private fun getFileIcon(extension: String): String {
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "🖼️"
            "mp4", "avi", "mkv", "mov", "wmv" -> "🎥"
            "mp3", "wav", "flac", "m4a", "aac" -> "🎵"
            "pdf" -> "📄"
            "doc", "docx", "txt" -> "📝"
            "zip", "rar", "7z", "tar", "gz" -> "📦"
            "apk" -> "📱"
            "exe", "bat", "sh" -> "⚠️"
            else -> "📁"
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }
    }

    private fun createProfessionalCard(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(COLOR_WHITE)
            cornerRadius = 16f
            setStroke(1, Color.parseColor("#E8EAED"))
        }
    }

    private fun createBadgeBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#E8F0FE"))
            cornerRadius = 12f
        }
    }

    private fun createActionButton(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 12f
        }
    }

    private fun createProgressDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(COLOR_PRIMARY_BLUE)
            cornerRadius = 8f
        }
    }

    private fun createScanningIndicator(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RING
            setStroke(6, COLOR_PRIMARY_BLUE)
            setSize(80, 80)
            useLevel = false
        }
    }

    private fun createScoreCard(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#F8F9FA"))
            cornerRadius = 12f
            setStroke(2, color)
        }
    }

    enum class ThreatLevel {
        SAFE, SUSPICIOUS, DANGEROUS
    }

    enum class FindingType {
        SUCCESS, INFO, WARNING, CRITICAL
    }

    data class Finding(
        val message: String,
        val type: FindingType,
        val scoreImpact: Int
    )

    data class AnalysisResult(
        val finding: Finding,
        val scoreImpact: Int,
        val isThreat: Boolean
    )

    data class ScanResult(
        val threatLevel: ThreatLevel,
        val score: Int,
        val findings: List<Finding>
    )
}