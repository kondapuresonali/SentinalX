package com.example.sentinalx.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sentinalx.utils.LinkCacheManager
import java.text.SimpleDateFormat
import java.util.*

class CacheSettingsActivity : AppCompatActivity() {

    private lateinit var cacheToggle: Switch
    private lateinit var statsCard: LinearLayout
    private lateinit var cachedLinksContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    // Cyber Theme Colors (matching ScanningDialogActivity)
    private val COLOR_PRIMARY_CYAN = Color.parseColor("#00E5FF")
    private val COLOR_BRIGHT_CYAN = Color.parseColor("#64FFDA")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#34A853")
    private val COLOR_DANGER_RED = Color.parseColor("#EA4335")
    private val COLOR_WHITE = Color.parseColor("#FFFFFF")
    private val COLOR_DARK_BG = Color.parseColor("#0A0E27")
    private val COLOR_CARD_BG = Color.parseColor("#1A1F3A")
    private val COLOR_TEXT_GRAY = Color.parseColor("#B0BEC5")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = COLOR_DARK_BG

        setContentView(createUI())

        loadCacheData()
    }

    private fun createUI(): ScrollView {
        scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(COLOR_DARK_BG)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(24, 24, 24, 24)
        }

        // Header
        mainLayout.addView(createHeader())

        // Cache toggle card
        mainLayout.addView(createCacheToggleCard())

        // Statistics card
        statsCard = createStatsCard()
        mainLayout.addView(statsCard)

        // Action buttons card
        mainLayout.addView(createActionsCard())

        // Cached links section
        mainLayout.addView(createSectionHeader("Cached Safe Links"))
        cachedLinksContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainLayout.addView(cachedLinksContainer)

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }

            // Back button
            val backButton = TextView(this@CacheSettingsActivity).apply {
                text = "←"
                textSize = 28f
                setTextColor(COLOR_PRIMARY_CYAN)
                setPadding(0, 0, 24, 0)
                setOnClickListener { finish() }
            }
            addView(backButton)

            // Title
            val titleText = TextView(this@CacheSettingsActivity).apply {
                text = "Smart Cache Settings"
                textSize = 22f
                setTextColor(COLOR_WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(titleText)
        }
    }

    private fun createCacheToggleCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard()
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            // Toggle header
            val toggleHeader = LinearLayout(this@CacheSettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val icon = TextView(this@CacheSettingsActivity).apply {
                    text = "⚡"
                    textSize = 20f
                    setPadding(0, 0, 12, 0)
                }
                addView(icon)

                val label = TextView(this@CacheSettingsActivity).apply {
                    text = "Enable Smart Cache"
                    textSize = 16f
                    setTextColor(COLOR_WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                addView(label)

                cacheToggle = Switch(this@CacheSettingsActivity).apply {
                    isChecked = LinkCacheManager.isCacheEnabled(this@CacheSettingsActivity)
                    setOnCheckedChangeListener { _, isChecked ->
                        LinkCacheManager.setCacheEnabled(this@CacheSettingsActivity, isChecked)
                        Toast.makeText(
                            this@CacheSettingsActivity,
                            if (isChecked) "Smart Cache Enabled ✓" else "Smart Cache Disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCacheData()
                    }
                }
                addView(cacheToggle)
            }
            addView(toggleHeader)

            // Description
            val description = TextView(this@CacheSettingsActivity).apply {
                text = "Skip scanning for previously verified safe links (valid for 7 days)"
                textSize = 13f
                setTextColor(COLOR_TEXT_GRAY)
                setPadding(32, 12, 0, 0)
            }
            addView(description)
        }
    }

    private fun createStatsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard()
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }
    }

    private fun createActionsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard()
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }

            // Clean expired button
            val cleanExpiredButton = Button(this@CacheSettingsActivity).apply {
                text = "🧹 Clean Expired Entries"
                textSize = 14f
                setTextColor(COLOR_WHITE)
                background = createButton(COLOR_PRIMARY_CYAN)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }
                setOnClickListener {
                    val count = LinkCacheManager.cleanExpiredCache(this@CacheSettingsActivity)
                    Toast.makeText(
                        this@CacheSettingsActivity,
                        if (count > 0) "Removed $count expired entries" else "No expired entries found",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCacheData()
                }
            }
            addView(cleanExpiredButton)

            // Clear all button
            val clearAllButton = Button(this@CacheSettingsActivity).apply {
                text = "🗑️ Clear All Cache"
                textSize = 14f
                setTextColor(COLOR_WHITE)
                background = createButton(COLOR_DANGER_RED)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }
                setOnClickListener {
                    showClearAllConfirmation()
                }
            }
            addView(clearAllButton)

            // Export cache button
            val exportButton = Button(this@CacheSettingsActivity).apply {
                text = "📋 Export Cache Data"
                textSize = 14f
                setTextColor(COLOR_PRIMARY_CYAN)
                background = createOutlineButton(COLOR_PRIMARY_CYAN)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    showExportDialog()
                }
            }
            addView(exportButton)
        }
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }
    }

    private fun loadCacheData() {
        // Load statistics
        val stats = LinkCacheManager.getCacheStats(this)
        updateStatsCard(stats)

        // Load cached links
        val cachedLinks = LinkCacheManager.getAllCachedLinks(this)
        updateCachedLinksDisplay(cachedLinks)
    }

    private fun updateStatsCard(stats: LinkCacheManager.CacheStats) {
        statsCard.removeAllViews()

        // Title
        val title = TextView(this).apply {
            text = "📊 Cache Statistics"
            textSize = 16f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        statsCard.addView(title)

        // Stats grid
        val statsGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statsGrid.addView(createStatRow("Total Entries", stats.totalEntries.toString()))
        statsGrid.addView(createStatRow("Active", stats.activeEntries.toString(), COLOR_SUCCESS_GREEN))
        statsGrid.addView(createStatRow("Expired", stats.expiredEntries.toString(), COLOR_TEXT_GRAY))

        if (stats.oldestEntry != null) {
            val daysAgo = (System.currentTimeMillis() - stats.oldestEntry) / (24 * 60 * 60 * 1000)
            statsGrid.addView(createStatRow("Oldest Entry", "$daysAgo days ago"))
        }

        statsCard.addView(statsGrid)
    }

    private fun createStatRow(label: String, value: String, valueColor: Int = COLOR_PRIMARY_CYAN): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)

            val labelText = TextView(this@CacheSettingsActivity).apply {
                text = label
                textSize = 14f
                setTextColor(COLOR_TEXT_GRAY)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(labelText)

            val valueText = TextView(this@CacheSettingsActivity).apply {
                text = value
                textSize = 15f
                setTextColor(valueColor)
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(valueText)
        }
    }

    private fun updateCachedLinksDisplay(links: List<LinkCacheManager.CachedLink>) {
        cachedLinksContainer.removeAllViews()

        if (links.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No cached links yet"
                textSize = 14f
                setTextColor(COLOR_TEXT_GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
            }
            cachedLinksContainer.addView(emptyText)
            return
        }

        links.forEach { link ->
            cachedLinksContainer.addView(createCachedLinkCard(link))
        }
    }

    private fun createCachedLinkCard(link: LinkCacheManager.CachedLink): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard()
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }

            // URL
            val urlText = TextView(this@CacheSettingsActivity).apply {
                text = link.url
                textSize = 13f
                setTextColor(COLOR_WHITE)
                typeface = Typeface.create("monospace", Typeface.NORMAL)
                maxLines = 2
                setPadding(0, 0, 0, 12)
            }
            addView(urlText)

            // Info row
            val infoRow = LinearLayout(this@CacheSettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val statusBadge = TextView(this@CacheSettingsActivity).apply {
                    text = "✓ SAFE"
                    textSize = 11f
                    setTextColor(COLOR_WHITE)
                    background = GradientDrawable().apply {
                        setColor(COLOR_SUCCESS_GREEN)
                        cornerRadius = 8f
                    }
                    setPadding(12, 6, 12, 6)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(statusBadge)

                val spacer = View(this@CacheSettingsActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        0,
                        1f
                    )
                }
                addView(spacer)

                val daysAgo = (System.currentTimeMillis() - link.timestamp) / (24 * 60 * 60 * 1000)
                val timeText = TextView(this@CacheSettingsActivity).apply {
                    text = "${daysAgo}d ago"
                    textSize = 12f
                    setTextColor(COLOR_TEXT_GRAY)
                    setPadding(12, 0, 0, 0)
                }
                addView(timeText)
            }
            addView(infoRow)

            // Details row
            val detailsText = TextView(this@CacheSettingsActivity).apply {
                text = "Twin: ${link.twinName} • Score: ${link.threatScore}/100"
                textSize = 11f
                setTextColor(COLOR_TEXT_GRAY)
                setPadding(0, 8, 0, 8)
            }
            addView(detailsText)

            // Remove button
            val removeButton = TextView(this@CacheSettingsActivity).apply {
                text = "🗑️ Remove"
                textSize = 12f
                setTextColor(COLOR_DANGER_RED)
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    showRemoveConfirmation(link)
                }
            }
            addView(removeButton)
        }
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Cache?")
            .setMessage("This will remove all ${LinkCacheManager.getCacheStats(this).totalEntries} cached links. You'll need to re-scan links you click.")
            .setPositiveButton("Clear All") { _, _ ->
                val count = LinkCacheManager.clearAllCache(this)
                Toast.makeText(this, "Cleared $count cached links", Toast.LENGTH_SHORT).show()
                loadCacheData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveConfirmation(link: LinkCacheManager.CachedLink) {
        AlertDialog.Builder(this)
            .setTitle("Remove Cached Link?")
            .setMessage("URL: ${link.url}\n\nThis link will be re-scanned next time you click it.")
            .setPositiveButton("Remove") { _, _ ->
                LinkCacheManager.removeCachedUrl(this, link.url)
                Toast.makeText(this, "Removed from cache", Toast.LENGTH_SHORT).show()
                loadCacheData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportDialog() {
        val exportData = LinkCacheManager.exportCacheData(this)

        AlertDialog.Builder(this)
            .setTitle("Cache Export")
            .setMessage(exportData)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun createCard(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(COLOR_CARD_BG)
            cornerRadius = 16f
            setStroke(1, Color.parseColor("#3300E5FF"))
        }
    }

    private fun createButton(color: Int): GradientDrawable {
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
}