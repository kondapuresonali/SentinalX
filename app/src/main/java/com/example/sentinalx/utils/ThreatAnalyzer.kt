package com.example.sentinalx.utils

import android.util.Log
import java.net.IDN

data class ScanData(
    val finalUrl: String,
    val redirectsCount: Int,
    val uniqueCookies: Int,
    val trackerDomains: Set<String>,
    val loginFormsDetected: Boolean
)

data class ThreatResult(
    val threatLevel: ThreatLevel,
    val score: Int,
    val reasons: List<String>
)

enum class ThreatLevel {
    SAFE,
    SUSPICIOUS,
    DANGEROUS
}

object ThreatAnalyzer {

    private const val TAG = "ThreatAnalyzer_Strict"
    private const val BASE_SCORE = 70 // Unknown sites start as SUSPICIOUS

    // Expanded dangerous TLDs
    private val DANGEROUS_TLDS = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".pw", ".cc", ".icu",
        ".xyz", ".top", ".bid", ".loan", ".work", ".click", ".link",
        ".download", ".stream", ".racing", ".party", ".trade"
    )

    // Expanded phishing keywords
    private val PHISHING_KEYWORDS = setOf(
        "verify", "account", "login", "password", "urgent", "suspended",
        "security", "update", "confirm", "banking", "paypal", "amazon",
        "prize", "winner", "free", "claim", "gift", "reward", "click",
        "authenticate", "validate", "restore", "locked", "expires",
        "wallet", "crypto", "bitcoin", "fund", "transfer"
    )

    // Trusted domains (only these get SAFE verdict by default)
    private val TRUSTED_DOMAINS = setOf(
        "google.com", "youtube.com", "facebook.com", "instagram.com",
        "linkedin.com", "github.com", "stackoverflow.com", "wikipedia.org",
        "reddit.com", "medium.com", "apple.com", "microsoft.com",
        "amazon.com", "netflix.com", "twitter.com", "whatsapp.com",
        "telegram.org", "zoom.us", "dropbox.com", "spotify.com"
    )

    private val MAJOR_TRACKERS = setOf(
        "googletagmanager.com", "google-analytics.com", "facebook.com",
        "doubleclick.net", "adservice.google.com"
    )

    // Test domains for demo purposes
    private val TEST_MALICIOUS_DOMAINS = setOf(
        "phishing.test", "malware.link", "test-login-page.com"
    )

    private var staticWarningFound = false

    /**
     * MAIN ANALYSIS: Combines static URL checks with dynamic scan data
     */
    fun analyzeScanData(initialUrl: String, scanData: ScanData): ThreatResult {
        Log.d(TAG, "=== Starting Analysis for: $initialUrl ===")

        // 1. Static URL Analysis
        val staticResult = analyzeUrlStatic(initialUrl)
        var score = staticResult.score
        val reasons = staticResult.reasons.toMutableList()

        staticWarningFound = staticResult.threatLevel != ThreatLevel.SAFE

        // 2. Dynamic Twin Scan Analysis
        reasons.add("--- Digital Twin Analysis ---")

        // Check A: Redirects (40 points - very suspicious)
        if (scanData.redirectsCount > 3) {
            score -= 40
            reasons.add("❌ CRITICAL: ${scanData.redirectsCount} redirects detected (cloaking)")
            Log.d(TAG, "Excessive redirects: -40 points")
        } else if (scanData.redirectsCount > 1) {
            score -= 20
            reasons.add("⚠️ Multiple redirects detected (${scanData.redirectsCount})")
        } else if (scanData.redirectsCount > 0) {
            reasons.add("⚠️ Single redirect detected")
        } else {
            reasons.add("✓ No redirects")
        }

        // Check B: Cookies & Trackers (30 points)
        val nonMajorTrackers = scanData.trackerDomains.count { domain ->
            MAJOR_TRACKERS.none { domain.contains(it) }
        }

        if (scanData.uniqueCookies > 15 || nonMajorTrackers > 3) {
            score -= 30
            reasons.add("❌ Excessive tracking (${scanData.uniqueCookies} cookies, $nonMajorTrackers unknown trackers)")
            Log.d(TAG, "High tracking: -30 points")
        } else if (scanData.uniqueCookies > 8) {
            score -= 15
            reasons.add("⚠️ Moderate tracking activity")
        } else {
            reasons.add("✓ Minimal tracking")
        }

        // Check C: Final URL mismatch
        if (scanData.finalUrl != initialUrl) {
            val finalResult = analyzeUrlStatic(scanData.finalUrl)
            if (finalResult.threatLevel == ThreatLevel.DANGEROUS) {
                score -= 35
                reasons.add("❌ CRITICAL: Redirect leads to dangerous domain")
            } else if (finalResult.threatLevel == ThreatLevel.SUSPICIOUS) {
                score -= 20
                reasons.add("⚠️ Redirect leads to suspicious domain")
            }
        }

        // Check D: Login forms on untrusted sites (45 points - CRITICAL)
        if (scanData.loginFormsDetected && !isTrustedDomain(scanData.finalUrl)) {
            score -= 45
            reasons.add("❌ CRITICAL: Credential harvesting detected!")
            Log.d(TAG, "Login form on untrusted site: -45 points")
        } else if (scanData.loginFormsDetected) {
            reasons.add("✓ Login form on trusted domain")
        }

        // 3. Contextual penalties
        if (staticWarningFound && (scanData.loginFormsDetected || scanData.redirectsCount > 1)) {
            score -= 15
            reasons.add("🔥 Combined threat indicators detected")
            Log.d(TAG, "Contextual penalty: -15 points")
        }

        // Final score
        score = score.coerceIn(0, 100)

        // Stricter thresholds
        val finalThreatLevel = when {
            score >= 80 -> ThreatLevel.SAFE
            score >= 50 -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.DANGEROUS
        }

        Log.d(TAG, "=== Final Score: $score -> $finalThreatLevel ===")
        return ThreatResult(finalThreatLevel, score, reasons)
    }

    /**
     * STATIC URL ANALYSIS: Examines URL structure and patterns
     */
    private fun analyzeUrlStatic(url: String): ThreatResult {
        var score = BASE_SCORE // Start at 70 (SUSPICIOUS) for unknown domains
        val reasons = mutableListOf<String>()
        val lowerUrl = url.lowercase()
        val domain = extractDomain(lowerUrl)

        Log.d(TAG, "Static analysis for: $domain")

        // Immediate DANGEROUS verdict for test malicious domains
        if (TEST_MALICIOUS_DOMAINS.any { domain.contains(it) }) {
            return ThreatResult(
                ThreatLevel.DANGEROUS,
                0,
                listOf("❌ Known malicious domain", "Site confirmed as threat")
            )
        }

        // Immediate SAFE verdict for trusted domains
        if (isTrustedDomain(lowerUrl)) {
            return ThreatResult(
                ThreatLevel.SAFE,
                100,
                listOf("✓ Verified trusted domain")
            )
        }

        reasons.add("--- Static URL Analysis ---")
        reasons.add("⚠️ Unknown domain (not in trusted list)")

        // 1. HTTPS Check (20 points)
        if (!lowerUrl.startsWith("https://")) {
            score -= 20
            reasons.add("❌ No HTTPS encryption")
        } else {
            score += 10 // Bonus for HTTPS on unknown domain
            reasons.add("✓ HTTPS enabled")
        }

        // 2. Dangerous TLDs (40 points - very high penalty)
        if (DANGEROUS_TLDS.any { domain.endsWith(it) }) {
            score -= 40
            reasons.add("❌ High-risk TLD (free/disposable)")
        }

        // 3. IP Address (35 points)
        if (lowerUrl.contains(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
            score -= 35
            reasons.add("❌ Raw IP address (bypassing DNS)")
        }

        // 4. Punycode/IDN (35 points)
        if (domain.startsWith("xn--")) {
            score -= 35
            reasons.add("❌ Punycode encoding (homograph attack)")
        }

        // 5. Domain spoofing check (30 points)
        val rootDomain = domain.substringBeforeLast('.')
        if (rootDomain.length > 3) {
            for (trusted in TRUSTED_DOMAINS) {
                val trustedRoot = trusted.substringBeforeLast('.')
                if (calculateLevenshteinDistance(rootDomain, trustedRoot) <= 2) {
                    score -= 30
                    reasons.add("❌ Domain spoofing detected (looks like $trustedRoot)")
                    break
                }
            }
        }

        // 6. Phishing keywords (8 points each, max 40)
        var keywordCount = 0
        PHISHING_KEYWORDS.forEach { keyword ->
            if (lowerUrl.contains(keyword)) {
                keywordCount++
                if (keywordCount <= 3) {
                    reasons.add("⚠️ Suspicious keyword: '$keyword'")
                }
            }
        }
        val keywordPenalty = minOf(keywordCount * 8, 40)
        score -= keywordPenalty

        // 7. URL obfuscation (25 points)
        if (lowerUrl.contains("@")) {
            score -= 25
            reasons.add("❌ URL obfuscation (@ symbol)")
        }

        // 8. Excessive path complexity (15 points)
        if (lowerUrl.count { it == '/' } > 6) {
            score -= 15
            reasons.add("⚠️ Complex URL structure")
        }

        // 9. Short domain length (often malicious) (10 points)
        if (rootDomain.length < 4) {
            score -= 10
            reasons.add("⚠️ Very short domain name")
        }

        // Final static score
        score = score.coerceIn(0, 100)

        val threatLevel = when {
            score >= 80 -> ThreatLevel.SAFE
            score >= 50 -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.DANGEROUS
        }

        return ThreatResult(threatLevel, score, reasons)
    }

    // Helper functions
    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) {
            for (j in 0..n) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> dp[i][j] = minOf(
                        dp[i - 1][j - 1] + if (s1[i - 1] != s2[j - 1]) 1 else 0,
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1
                    )
                }
            }
        }
        return dp[m][n]
    }

    private fun extractDomain(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            if (domain.startsWith("xn--")) {
                IDN.toUnicode(domain)
            } else {
                domain.substringBefore("?").substringBefore("#")
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun isTrustedDomain(url: String): Boolean {
        val domain = extractDomain(url)
        return TRUSTED_DOMAINS.any { trusted ->
            domain == trusted || domain.endsWith(".$trusted")
        }
    }
}