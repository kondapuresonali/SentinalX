package com.example.sentinalx.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe singleton manager for caching safe link scan results
 * Only SAFE links are cached for 7 days to skip redundant scans
 */
object LinkCacheManager {

    private const val TAG = "LINK_CACHE_MANAGER"
    private const val PREFS_NAME = "sentinalx_link_cache"
    private const val KEY_CACHE_ENABLED = "cache_enabled"
    private const val KEY_CACHE_DATA = "cache_data"
    private const val CACHE_VALIDITY_DAYS = 7
    private const val CACHE_VALIDITY_MS = CACHE_VALIDITY_DAYS * 24 * 60 * 60 * 1000L

    private val lock = ReentrantReadWriteLock()

    /**
     * Cached link data structure
     */
    data class CachedLink(
        val url: String,
        val urlHash: String,
        val threatLevel: ThreatLevel,
        val threatScore: Int,
        val timestamp: Long,
        val twinName: String,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("url", url)
                put("urlHash", urlHash)
                put("threatLevel", threatLevel.name)
                put("threatScore", threatScore)
                put("timestamp", timestamp)
                put("twinName", twinName)
                put("expiresAt", expiresAt)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): CachedLink {
                return CachedLink(
                    url = json.getString("url"),
                    urlHash = json.getString("urlHash"),
                    threatLevel = ThreatLevel.valueOf(json.getString("threatLevel")),
                    threatScore = json.getInt("threatScore"),
                    timestamp = json.getLong("timestamp"),
                    twinName = json.getString("twinName"),
                    expiresAt = json.getLong("expiresAt")
                )
            }
        }
    }

    /**
     * Cache statistics
     */
    data class CacheStats(
        val totalEntries: Int,
        val activeEntries: Int,
        val expiredEntries: Int,
        val oldestEntry: Long?,
        val newestEntry: Long?,
        val cacheEnabled: Boolean
    )

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Generate SHA-256 hash of URL for storage key
     */
    private fun hashUrl(url: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(url.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing URL", e)
            url.hashCode().toString()
        }
    }

    /**
     * Normalize URL for consistent caching
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim().lowercase()

        // Remove trailing slashes
        normalized = normalized.trimEnd('/')

        // Remove common tracking parameters
        val trackingParams = listOf("utm_", "fbclid=", "gclid=", "ref=")
        trackingParams.forEach { param ->
            val index = normalized.indexOf(param)
            if (index > 0) {
                normalized = normalized.substring(0, index - 1)
            }
        }

        return normalized
    }

    /**
     * Check if caching is enabled
     */
    fun isCacheEnabled(context: Context): Boolean {
        return lock.read {
            getPrefs(context).getBoolean(KEY_CACHE_ENABLED, true) // Default: enabled
        }
    }

    /**
     * Enable or disable caching
     */
    fun setCacheEnabled(context: Context, enabled: Boolean) {
        lock.write {
            getPrefs(context).edit().putBoolean(KEY_CACHE_ENABLED, enabled).apply()
            Log.d(TAG, "Cache ${if (enabled) "ENABLED" else "DISABLED"}")
        }
    }

    /**
     * Save a scan result to cache (ONLY for SAFE links)
     */
    fun saveScanResult(
        context: Context,
        url: String,
        threatLevel: ThreatLevel,
        score: Int,
        twinName: String
    ) {
        // Only cache SAFE results
        if (threatLevel != ThreatLevel.SAFE) {
            Log.d(TAG, "Skipping cache - Only SAFE links are cached (got: $threatLevel)")
            return
        }

        if (!isCacheEnabled(context)) {
            Log.d(TAG, "Cache disabled - not saving")
            return
        }

        lock.write {
            try {
                val normalizedUrl = normalizeUrl(url)
                val urlHash = hashUrl(normalizedUrl)
                val currentTime = System.currentTimeMillis()
                val expiresAt = currentTime + CACHE_VALIDITY_MS

                val cachedLink = CachedLink(
                    url = normalizedUrl,
                    urlHash = urlHash,
                    threatLevel = threatLevel,
                    threatScore = score,
                    timestamp = currentTime,
                    twinName = twinName,
                    expiresAt = expiresAt
                )

                // Load existing cache
                val cacheMap = loadCacheMap(context).toMutableMap()

                // Add/update entry
                cacheMap[urlHash] = cachedLink

                // Save back to storage
                saveCacheMap(context, cacheMap)

                Log.d(TAG, "✅ Cached SAFE link: $normalizedUrl (expires in $CACHE_VALIDITY_DAYS days)")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving to cache", e)
            }
        }
    }

    /**
     * Get cached result for a URL
     * Returns null if not cached, expired, or suspicious/dangerous
     */
    fun getCachedResult(context: Context, url: String): CachedLink? {
        if (!isCacheEnabled(context)) {
            return null
        }

        return lock.read {
            try {
                val normalizedUrl = normalizeUrl(url)
                val urlHash = hashUrl(normalizedUrl)

                val cacheMap = loadCacheMap(context)
                val cached = cacheMap[urlHash]

                if (cached == null) {
                    Log.d(TAG, "URL not in cache: $normalizedUrl")
                    return@read null
                }

                if (cached.isExpired()) {
                    Log.d(TAG, "Cache entry expired for: $normalizedUrl")
                    // Clean up expired entry in background
                    Thread {
                        lock.write {
                            val map = loadCacheMap(context).toMutableMap()
                            map.remove(urlHash)
                            saveCacheMap(context, map)
                        }
                    }.start()
                    return@read null
                }

                // Only return SAFE cached results
                if (cached.threatLevel != ThreatLevel.SAFE) {
                    Log.d(TAG, "Cache entry not SAFE (${cached.threatLevel}) - removing")
                    // Remove non-SAFE entries
                    Thread {
                        lock.write {
                            val map = loadCacheMap(context).toMutableMap()
                            map.remove(urlHash)
                            saveCacheMap(context, map)
                        }
                    }.start()
                    return@read null
                }

                Log.d(TAG, "✅ Cache HIT: $normalizedUrl (scanned ${getDaysAgo(cached.timestamp)} days ago)")
                cached

            } catch (e: Exception) {
                Log.e(TAG, "Error reading from cache", e)
                null
            }
        }
    }

    /**
     * Remove a specific URL from cache
     */
    fun removeCachedUrl(context: Context, url: String): Boolean {
        return lock.write {
            try {
                val normalizedUrl = normalizeUrl(url)
                val urlHash = hashUrl(normalizedUrl)

                val cacheMap = loadCacheMap(context).toMutableMap()
                val removed = cacheMap.remove(urlHash) != null

                if (removed) {
                    saveCacheMap(context, cacheMap)
                    Log.d(TAG, "Removed from cache: $normalizedUrl")
                }

                removed
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from cache", e)
                false
            }
        }
    }

    /**
     * Get all cached links (sorted by timestamp, newest first)
     */
    fun getAllCachedLinks(context: Context): List<CachedLink> {
        return lock.read {
            try {
                loadCacheMap(context)
                    .values
                    .sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all cached links", e)
                emptyList()
            }
        }
    }

    /**
     * Clean expired cache entries
     */
    fun cleanExpiredCache(context: Context): Int {
        return lock.write {
            try {
                val cacheMap = loadCacheMap(context).toMutableMap()
                val initialSize = cacheMap.size

                // Remove expired entries
                val iterator = cacheMap.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.isExpired()) {
                        iterator.remove()
                    }
                }

                val removedCount = initialSize - cacheMap.size

                if (removedCount > 0) {
                    saveCacheMap(context, cacheMap)
                    Log.d(TAG, "Cleaned $removedCount expired cache entries")
                }

                removedCount
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning expired cache", e)
                0
            }
        }
    }

    /**
     * Clear entire cache
     */
    fun clearAllCache(context: Context): Int {
        return lock.write {
            try {
                val cacheMap = loadCacheMap(context)
                val count = cacheMap.size

                getPrefs(context).edit().remove(KEY_CACHE_DATA).apply()

                Log.d(TAG, "Cleared all cache ($count entries)")
                count
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                0
            }
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(context: Context): CacheStats {
        return lock.read {
            try {
                val cacheMap = loadCacheMap(context)
                val currentTime = System.currentTimeMillis()

                val activeEntries = cacheMap.values.count { !it.isExpired() }
                val expiredEntries = cacheMap.values.count { it.isExpired() }

                val timestamps = cacheMap.values.map { it.timestamp }
                val oldestEntry = timestamps.minOrNull()
                val newestEntry = timestamps.maxOrNull()

                CacheStats(
                    totalEntries = cacheMap.size,
                    activeEntries = activeEntries,
                    expiredEntries = expiredEntries,
                    oldestEntry = oldestEntry,
                    newestEntry = newestEntry,
                    cacheEnabled = isCacheEnabled(context)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cache stats", e)
                CacheStats(0, 0, 0, null, null, isCacheEnabled(context))
            }
        }
    }

    /**
     * Load cache map from SharedPreferences
     */
    private fun loadCacheMap(context: Context): Map<String, CachedLink> {
        return try {
            val json = getPrefs(context).getString(KEY_CACHE_DATA, null) ?: return emptyMap()
            val jsonArray = JSONArray(json)

            val map = mutableMapOf<String, CachedLink>()
            for (i in 0 until jsonArray.length()) {
                val linkJson = jsonArray.getJSONObject(i)
                val link = CachedLink.fromJson(linkJson)
                map[link.urlHash] = link
            }

            map
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cache map", e)
            emptyMap()
        }
    }

    /**
     * Save cache map to SharedPreferences
     */
    private fun saveCacheMap(context: Context, cacheMap: Map<String, CachedLink>) {
        try {
            val jsonArray = JSONArray()
            cacheMap.values.forEach { link ->
                jsonArray.put(link.toJson())
            }

            getPrefs(context).edit()
                .putString(KEY_CACHE_DATA, jsonArray.toString())
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache map", e)
        }
    }

    /**
     * Calculate days ago from timestamp
     */
    private fun getDaysAgo(timestamp: Long): Long {
        val diff = System.currentTimeMillis() - timestamp
        return diff / (24 * 60 * 60 * 1000)
    }

    /**
     * Export cache data for debugging/backup
     */
    fun exportCacheData(context: Context): String {
        return lock.read {
            try {
                val stats = getCacheStats(context)
                val links = getAllCachedLinks(context)

                buildString {
                    appendLine("=== SentinalX Cache Export ===")
                    appendLine("Cache Enabled: ${stats.cacheEnabled}")
                    appendLine("Total Entries: ${stats.totalEntries}")
                    appendLine("Active Entries: ${stats.activeEntries}")
                    appendLine("Expired Entries: ${stats.expiredEntries}")
                    appendLine()

                    links.forEach { link ->
                        appendLine("---")
                        appendLine("URL: ${link.url}")
                        appendLine("Threat Level: ${link.threatLevel}")
                        appendLine("Score: ${link.threatScore}")
                        appendLine("Twin: ${link.twinName}")
                        appendLine("Scanned: ${getDaysAgo(link.timestamp)} days ago")
                        appendLine("Expires: ${if (link.isExpired()) "EXPIRED" else "${getDaysAgo(link.expiresAt)} days"}")
                    }
                }
            } catch (e: Exception) {
                "Error exporting cache: ${e.message}"
            }
        }
    }
}