// TwinManagerActivity.kt (COMPLETE REPLACEMENT)

package com.example.sentinalx.ui
import android.graphics.PorterDuff
import android.animation.*
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class TwinManagerActivity : AppCompatActivity() {

    private lateinit var rootLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var mainLayout: LinearLayout
    private lateinit var twinCardsContainer: LinearLayout

    // The list of twins is now mutable and loaded from preferences
    private var digitalTwins = mutableListOf<TwinData>()

    companion object {
        // Professional Dark/Red Theme (Unified with Dashboard aesthetic where possible)
        private const val BACKGROUND_DARK = "#0A0F1E" // Deep Navy/Black
        private const val PRIMARY_RED = "#DC143C"     // Primary Accent (Red)
        private const val ACCENT_RED = "#FF4444"      // Secondary Glow/Action (Lighter Red)
        private const val DARK_CARD = "#1E253A"       // Card Surface
        private const val TEXT_WHITE = "#FFFFFF"
        private const val TEXT_GRAY = "#B0B0B0"
        private const val SUCCESS_GREEN = "#00FF99"   // Neon Green
        private const val WARNING_YELLOW = "#FFD700"
        private const val BORDER_RED = "#8B0000"
        private const val MAX_TWINS = 3 // Enforced limit
    }

    data class TwinData(
        var name: String,
        var status: String,
        var activationDate: String,
        var threatsBlocked: Int,
        var cookiesObserved: Int,
        var trackersAbsorbed: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚡ FIX: Load twins from storage ⚡
        loadTwinsFromStorage()

        setupDarkStatusBar()
        createTwinManagerUI()
    }

    // --- Data Persistence and Synchronization ---

    private fun loadTwinsFromStorage() {
        val sharedPrefs = getSharedPreferences("TwinDataPrefs", MODE_PRIVATE)
        val twinSet = sharedPrefs.getStringSet("digitalTwins", null)

        if (twinSet != null && twinSet.isNotEmpty()) {
            digitalTwins.clear()
            twinSet.forEach { twinString ->
                // Format: name|status|date|threats|cookies|trackers
                val parts = twinString.split("|")
                if (parts.size == 6) {
                    digitalTwins.add(TwinData(
                        parts[0], parts[1], parts[2],
                        parts[3].toIntOrNull() ?: 0,
                        parts[4].toIntOrNull() ?: 0,
                        parts[5].toIntOrNull() ?: 0
                    ))
                }
            }
        } else {
            // Initial generation if storage is empty (ensures 3 twins exist)
            digitalTwins.add(TwinData("Sentinel-Alpha", "Active", "2024-01-01", 120, 500, 300))
            digitalTwins.add(TwinData("Guardian-Beta", "Standby", "2024-01-01", 80, 400, 250))
            digitalTwins.add(TwinData("Defender-Gamma", "Standby", "2024-01-01", 60, 300, 200))
            saveTwinsToStorage() // Save the initial set
        }
    }

    private fun saveTwinsToStorage() {
        val sharedPrefs = getSharedPreferences("TwinDataPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        val twinSet = digitalTwins.map { twin ->
            // Format: name|status|date|threats|cookies|trackers
            "${twin.name}|${twin.status}|${twin.activationDate}|${twin.threatsBlocked}|${twin.cookiesObserved}|${twin.trackersAbsorbed}"
        }.toSet()

        editor.putStringSet("digitalTwins", twinSet)
        editor.apply()

        // Also update the Dashboard's list of twin names for synchronization
        editor.putStringSet("digitalTwinNames", digitalTwins.map { it.name }.toSet())
        editor.apply()
    }

    // --- UI Setup ---

    private fun setupDarkStatusBar() {
        try {
            window.statusBarColor = Color.parseColor(BACKGROUND_DARK)
            window.navigationBarColor = Color.parseColor(BACKGROUND_DARK)
        } catch (e: Exception) {
            android.util.Log.w("TwinManager", "Status bar setup failed", e)
        }
    }

    private fun createTwinManagerUI() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(BACKGROUND_DARK))
        }

        createHeader()

        scrollView = ScrollView(this)
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        createOverviewSection()
        createTwinCardsSection()
        createActionButtons()

        scrollView.addView(mainLayout)
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(rootLayout)
    }

    private fun createHeader() {
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor(DARK_CARD))
            setPadding(20, 40, 20, 20)
            elevation = 8f
        }

        val backButton = TextView(this).apply {
            text = "←"
            textSize = 28f
            setTextColor(Color.parseColor(PRIMARY_RED))
            setPadding(0, 0, 20, 0)
            setOnClickListener { finish() }
        }

        val titleText = TextView(this).apply {
            text = "Digital Twin Manager"
            textSize = 20f
            setTextColor(Color.parseColor(TEXT_WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val liveIndicator = TextView(this).apply {
            text = "● LIVE"
            textSize = 12f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.DEFAULT_BOLD
        }

        headerLayout.addView(backButton)
        headerLayout.addView(titleText)
        headerLayout.addView(liveIndicator)

        rootLayout.addView(headerLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    // --- UI Sections ---

    private fun createOverviewSection() {
        val overviewCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(createDarkCard())
            setPadding(24, 24, 24, 24)
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
        }

        val titleText = TextView(this).apply {
            text = "Total Protection Statistics"
            textSize = 16f
            setTextColor(Color.parseColor(PRIMARY_RED))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }

        val totalThreats = digitalTwins.sumOf { it.threatsBlocked }
        val totalCookies = digitalTwins.sumOf { it.cookiesObserved }
        val totalTrackers = digitalTwins.sumOf { it.trackersAbsorbed }

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        // ⚡ Professional Stat Labels ⚡
        statsRow.addView(createStatItem("🛡️", totalThreats.toString(), "Threats Blocked", ACCENT_RED))
        statsRow.addView(createStatItem("🍪", totalCookies.toString(), "Cookies Observed", WARNING_YELLOW))
        statsRow.addView(createStatItem("📡", totalTrackers.toString(), "Trackers Absorbed", SUCCESS_GREEN))

        val activeTwinsText = TextView(this).apply {
            text = "Total Twins: ${digitalTwins.size}/${MAX_TWINS} (Active: ${digitalTwins.count { it.status == "Active" }})"
            textSize = 14f
            setTextColor(Color.parseColor(TEXT_GRAY))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        overviewCard.addView(titleText)
        overviewCard.addView(statsRow)
        overviewCard.addView(activeTwinsText)

        // Find the existing overview section and replace it if it exists (for refresh)
        val existingOverview = mainLayout.getChildAt(0)
        if (existingOverview != null && existingOverview is LinearLayout && existingOverview.childCount > 0 && (existingOverview.getChildAt(0) as? TextView)?.text == "Total Protection Statistics") {
            mainLayout.removeViewAt(0)
            mainLayout.addView(overviewCard, 0)
        } else {
            mainLayout.addView(overviewCard, 0)
        }
    }

    private fun createStatItem(emoji: String, value: String, label: String, color: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 0, 8, 0)

            val emojiText = TextView(this@TwinManagerActivity).apply {
                text = emoji
                textSize = 24f
                gravity = Gravity.CENTER
            }

            val valueText = TextView(this@TwinManagerActivity).apply {
                text = value
                textSize = 20f
                setTextColor(Color.parseColor(color))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 4)
            }

            val labelText = TextView(this@TwinManagerActivity).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(TEXT_GRAY))
                gravity = Gravity.CENTER
            }

            addView(emojiText)
            addView(valueText)
            addView(labelText)
        }
    }

    private fun createTwinCardsSection() {
        val sectionTitle = TextView(this).apply {
            text = "Digital Twins (${digitalTwins.size}/${MAX_TWINS})"
            textSize = 16f
            setTextColor(Color.parseColor(TEXT_WHITE))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 8, 8, 16)
        }

        twinCardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        mainLayout.addView(sectionTitle)
        mainLayout.addView(twinCardsContainer)

        refreshTwinCards()
    }

    private fun refreshTwinCards() {
        twinCardsContainer.removeAllViews()

        // 1. Recreate the overview section to update total stats
        createOverviewSection()

        // 2. Refresh Twin Cards
        digitalTwins.forEachIndexed { index, twin ->
            twinCardsContainer.addView(createTwinCard(twin, index))
        }

        // 3. Save to storage for synchronization
        saveTwinsToStorage()
    }

    private fun createTwinCard(twin: TwinData, index: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(if (twin.status == "Active") createActiveTwinCard() else createDarkCard())
            setPadding(20, 20, 20, 20)
            elevation = if (twin.status == "Active") 6f else 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            val headerRow = LinearLayout(this@TwinManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 12)
            }

            val twinName = TextView(this@TwinManagerActivity).apply {
                text = twin.name
                textSize = 16f
                setTextColor(if (twin.status == "Active") Color.parseColor(ACCENT_RED) else Color.parseColor(TEXT_WHITE))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val statusBadge = TextView(this@TwinManagerActivity).apply {
                text = twin.status.uppercase()
                textSize = 10f
                setTextColor(if (twin.status == "Active") Color.parseColor(SUCCESS_GREEN) else Color.parseColor(TEXT_GRAY))
                typeface = Typeface.DEFAULT_BOLD
                setBackground(createPillBackground(
                    if (twin.status == "Active") SUCCESS_GREEN else TEXT_GRAY
                ))
                setPadding(10, 5, 10, 5)
            }

            headerRow.addView(twinName)
            headerRow.addView(statusBadge)

            val activationRow = LinearLayout(this@TwinManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, 12)
            }

            val calendarIcon = TextView(this@TwinManagerActivity).apply {
                text = "📅"
                textSize = 14f
                setTextColor(Color.parseColor(TEXT_GRAY))
                setPadding(0, 0, 8, 0)
            }

            val activationText = TextView(this@TwinManagerActivity).apply {
                text = "Activated: ${twin.activationDate}"
                textSize = 12f
                setTextColor(Color.parseColor(TEXT_GRAY))
            }

            activationRow.addView(calendarIcon)
            activationRow.addView(activationText)

            val statsGrid = LinearLayout(this@TwinManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 16)
            }

            statsGrid.addView(createMiniStat("🛡️", twin.threatsBlocked.toString(), "Threats"))
            statsGrid.addView(createMiniStat("🍪", twin.cookiesObserved.toString(), "Cookies"))
            statsGrid.addView(createMiniStat("📡", twin.trackersAbsorbed.toString(), "Trackers"))

            val actionsRow = LinearLayout(this@TwinManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val activateBtn = createActionButton(
                if (twin.status == "Active") "Deactivate" else "Activate",
                PRIMARY_RED
            ) {
                toggleTwinStatus(index)
            }

            val editBtn = createActionButton("Edit", TEXT_GRAY) {
                showEditDialog(index)
            }

            val deleteBtn = createActionButton("Delete", ACCENT_RED) {
                deleteTwin(index)
            }

            actionsRow.addView(activateBtn)
            actionsRow.addView(editBtn)
            actionsRow.addView(deleteBtn)

            addView(headerRow)
            addView(activationRow)
            addView(statsGrid)
            addView(actionsRow)
        }
    }

    private fun createMiniStat(icon: String, value: String, label: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val iconText = TextView(this@TwinManagerActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
            }

            val valueText = TextView(this@TwinManagerActivity).apply {
                text = value
                textSize = 16f
                setTextColor(Color.parseColor(TEXT_WHITE))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 2)
            }

            val labelText = TextView(this@TwinManagerActivity).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(TEXT_GRAY))
                gravity = Gravity.CENTER
            }

            addView(iconText)
            addView(valueText)
            addView(labelText)
        }
    }

    private fun createActionButton(text: String, color: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor(color))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackground(createButtonBackground(color))
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createActionButtons() {
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 40)
        }

        val createNewBtn = TextView(this).apply {
            text = "+ Create New Digital Twin"
            textSize = 16f
            setTextColor(Color.parseColor(TEXT_WHITE))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackground(createPrimaryButton())
            setPadding(24, 16, 24, 16)
            elevation = 4f

            // Disable button if twin limit is reached
            if (digitalTwins.size >= MAX_TWINS) {
                setBackground(createDisabledButton())
                setOnClickListener {
                    Toast.makeText(this@TwinManagerActivity, "Twin limit (${MAX_TWINS}) reached.", Toast.LENGTH_SHORT).show()
                }
            } else {
                setOnClickListener { showCreateDialog() }
            }
        }

        buttonContainer.addView(createNewBtn)
        mainLayout.addView(buttonContainer)
    }

    private fun toggleTwinStatus(index: Int) {
        if (digitalTwins[index].status == "Active") {
            Toast.makeText(this, "Cannot deactivate the only active twin.", Toast.LENGTH_SHORT).show()
            return
        }

        digitalTwins.forEach { it.status = "Standby" }
        digitalTwins[index].status = "Active"

        refreshTwinCards()
        Toast.makeText(this, "Twin activated: ${digitalTwins[index].name}", Toast.LENGTH_SHORT).show()

        animateStatusChange()
    }

    private fun showEditDialog(index: Int) {
        val twin = digitalTwins[index]

        // Use dark dialog theme for readability
        val context = android.view.ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val nameInput = EditText(context).apply {
            setText(twin.name)
            hint = "Twin Name"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            // Use a resource ID for the background tint in a professional app
            // For MVP, we use this direct color fix
            background.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)
        }

        dialogLayout.addView(nameInput)

        android.app.AlertDialog.Builder(context)
            .setTitle("Edit Digital Twin")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    digitalTwins[index].name = newName
                    refreshTwinCards()
                    Toast.makeText(this, "Twin updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTwin(index: Int) {
        if (digitalTwins.size <= 1) {
            Toast.makeText(this, "Cannot delete the last remaining twin.", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Digital Twin")
            .setMessage("Are you sure you want to delete '${digitalTwins[index].name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val twinName = digitalTwins[index].name
                val wasActive = digitalTwins[index].status == "Active"

                digitalTwins.removeAt(index)

                // If the active twin was deleted, activate the first remaining twin
                if (wasActive && digitalTwins.isNotEmpty()) {
                    digitalTwins[0].status = "Active"
                }

                refreshTwinCards()
                Toast.makeText(this, "Twin deleted: $twinName", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateDialog() {
        if (digitalTwins.size >= MAX_TWINS) {
            Toast.makeText(this, "Maximum of ${MAX_TWINS} twins reached.", Toast.LENGTH_SHORT).show()
            return
        }

        val context = android.view.ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val nameInput = EditText(context).apply {
            hint = "Twin Name (e.g., New-Defender)"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            background.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)
        }

        dialogLayout.addView(nameInput)

        android.app.AlertDialog.Builder(context)
            .setTitle("Create New Digital Twin")
            .setView(dialogLayout)
            .setPositiveButton("Create") { _, _ ->
                val newName = nameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val newTwin = TwinData(
                        name = newName,
                        status = "Standby", // New twins start in standby
                        activationDate = currentDate,
                        threatsBlocked = Random.nextInt(0, 50), // Mock data
                        cookiesObserved = Random.nextInt(0, 300),
                        trackersAbsorbed = Random.nextInt(0, 150)
                    )
                    digitalTwins.add(newTwin)
                    refreshTwinCards()
                    Toast.makeText(this, "Twin created: $newName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun animateStatusChange() {
        val scaleAnim = ObjectAnimator.ofFloat(twinCardsContainer, "scaleY", 1f, 0.98f, 1f)
        scaleAnim.duration = 200
        scaleAnim.start()
    }

    // --- Styling functions ---
    private fun createDarkCard(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(DARK_CARD))
            cornerRadius = 12f
            setStroke(1, Color.parseColor(BORDER_RED))
        }
    }

    private fun createActiveTwinCard(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                Color.parseColor("#1A0000"), // Dark background for subtle glow
                Color.parseColor(DARK_CARD)
            )
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
            cornerRadius = 12f
            setStroke(2, Color.parseColor(PRIMARY_RED))
        }
    }

    private fun createDisabledButton(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(TEXT_GRAY))
            cornerRadius = 12f
//alpha = 5f
        }
    }

    private fun createPillBackground(color: String): GradientDrawable {
        return GradientDrawable().apply {
            val colorInt = Color.parseColor(color)
            val alphaColor = Color.argb(51, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
            setColor(alphaColor)
            cornerRadius = 16f
        }
    }

    private fun createButtonBackground(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = 8f
            setStroke(2, Color.parseColor(color))
        }
    }

    private fun createPrimaryButton(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                Color.parseColor(PRIMARY_RED),
                Color.parseColor(ACCENT_RED)
            )
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
            cornerRadius = 12f
        }
    }
}