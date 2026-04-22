package com.example.sentinalx.ui

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.sentinalx.utils.NetworkMonitor
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class RegistrationActivity : BaseActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var mainLayout: LinearLayout

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var phoneInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout

    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var termsCheckBox: CheckBox
    private lateinit var registerButton: Button

    companion object {
        // Cyber theme colors matching splash
        private const val PRIMARY_CYAN = "#00E5FF"
        private const val BRIGHT_CYAN = "#64FFDA"
        private const val SUCCESS_GREEN = "#34A853"
        private const val SOFT_RED = "#EA4335"
        private const val WHITE = "#FFFFFF"
        private const val SEMI_TRANSPARENT_WHITE = "#DDFFFFFF"
        private const val TRANSPARENT_CARD = "#33FFFFFF"
        private const val BORDER_CYAN = "#4400E5FF"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkLoginAndNavigate()) {
            return
        }

        setupDarkStatusBar()
        val contentView = createRegistrationUI()
        setupWithBackground(contentView)
    }

    override fun getBackgroundConfig() = BackgroundConfig(
        speed = 0.6f,
        alpha = 0.5f,
        scaleX = 1.1f,
        scaleY = 1.1f
    )

    private fun checkLoginAndNavigate(): Boolean {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            val userName = sharedPref.getString("user_name", "User")
            val intent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("user_name", userName)
            }
            startActivity(intent)
            finish()
            return true
        }
        return false
    }

    private fun createRegistrationUI(): View {
        // Scrollable content
        scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 60, 24, 40)
        }

        // Build registration sections
        createRegistrationHeader()
        createTransparentRegistrationForm()
        createActionButtons()

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun createRegistrationHeader() {
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 50)
        }

        val brandContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val appTitle = TextView(this).apply {
            text = "SentinalX"
            textSize = 32f
            setTextColor(Color.parseColor(PRIMARY_CYAN))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val proLabel = TextView(this).apply {
            text = "ENTERPRISE"
            textSize = 10f
            setTextColor(Color.parseColor(SUCCESS_GREEN))
            typeface = Typeface.DEFAULT_BOLD
            setBackground(createPillBackground(SUCCESS_GREEN, 0.15f))
            setPadding(10, 5, 10, 5)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(12, 8, 0, 0) }
        }

        brandContainer.addView(appTitle)
        brandContainer.addView(proLabel)

        val welcomeText = TextView(this).apply {
            text = "Create Your Digital Identity"
            textSize = 18f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        val subtitleText = TextView(this).apply {
            text = "Join the next generation of digital privacy protection"
            textSize = 14f
            setTextColor(Color.parseColor(BRIGHT_CYAN))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
            alpha = 0.8f
        }

        headerContainer.addView(brandContainer)
        headerContainer.addView(welcomeText)
        headerContainer.addView(subtitleText)
        mainLayout.addView(headerContainer)
    }

    private fun createTransparentRegistrationForm() {
        val formCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createTransparentCard()
            setPadding(28, 32, 28, 32)
            elevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }

        val formTitle = TextView(this).apply {
            text = "Account Information"
            textSize = 16f
            setTextColor(Color.parseColor(WHITE))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }

        nameInputLayout = createTransparentTextInputLayout("Full Name")
        nameEditText = createTransparentTextInputEditText()
        nameInputLayout.addView(nameEditText)

        phoneInputLayout = createTransparentTextInputLayout("Phone Number")
        phoneEditText = createTransparentTextInputEditText().apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        phoneInputLayout.addView(phoneEditText)

        emailInputLayout = createTransparentTextInputLayout("Email Address")
        emailEditText = createTransparentTextInputEditText().apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        emailInputLayout.addView(emailEditText)

        passwordInputLayout = createTransparentTextInputLayout("Password")
        passwordEditText = createTransparentTextInputEditText().apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passwordInputLayout.addView(passwordEditText)

        val termsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, 20, 0, 0)
        }

        termsCheckBox = CheckBox(this).apply {
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(PRIMARY_CYAN))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 16, 0) }
        }

        val termsText = TextView(this).apply {
            text = "I agree to the Terms of Service and Privacy Policy for SentinalX Enterprise"
            textSize = 12f
            setTextColor(Color.parseColor(BRIGHT_CYAN))
            alpha = 0.9f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        termsContainer.addView(termsCheckBox)
        termsContainer.addView(termsText)

        formCard.addView(formTitle)
        formCard.addView(nameInputLayout)
        formCard.addView(phoneInputLayout)
        formCard.addView(emailInputLayout)
        formCard.addView(passwordInputLayout)
        formCard.addView(termsContainer)

        mainLayout.addView(formCard)
    }

    private fun createActionButtons() {
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }

        registerButton = Button(this).apply {
            text = "Create Account"
            textSize = 16f
            setTextColor(Color.parseColor("#000000"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = createCyanButton()
            setPadding(24, 18, 24, 18)
            elevation = 6f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
            setOnClickListener { handleRegistration() }
        }

        val loginPrompt = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        val promptText = TextView(this).apply {
            text = "Already have an account? "
            textSize = 14f
            setTextColor(Color.parseColor(BRIGHT_CYAN))
            alpha = 0.7f
        }

        val loginLink = TextView(this).apply {
            text = "Sign In"
            textSize = 14f
            setTextColor(Color.parseColor(PRIMARY_CYAN))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(8, 12, 8, 12)
            setOnClickListener {
                Toast.makeText(this@RegistrationActivity, "Sign In - Enterprise Feature", Toast.LENGTH_SHORT).show()
            }
        }

        loginPrompt.addView(promptText)
        loginPrompt.addView(loginLink)

        buttonContainer.addView(registerButton)
        buttonContainer.addView(loginPrompt)
        mainLayout.addView(buttonContainer)
    }

    private fun createTransparentTextInputLayout(hint: String): TextInputLayout {
        return TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(10f, 10f, 10f, 10f)
            setHint(hint)
            setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor(BRIGHT_CYAN)))
            setBoxStrokeColorStateList(createCyanStrokeColor())
            hintTextColor = android.content.res.ColorStateList.valueOf(Color.parseColor(BRIGHT_CYAN))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
        }
    }

    private fun createCyanStrokeColor(): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf()
            ),
            intArrayOf(
                Color.parseColor(PRIMARY_CYAN),
                Color.parseColor(BRIGHT_CYAN)
            )
        )
    }

    private fun createTransparentTextInputEditText(): TextInputEditText {
        return TextInputEditText(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor(WHITE))
            setHintTextColor(Color.parseColor(BRIGHT_CYAN))
            setPadding(18, 20, 18, 20)
            background = createInputBackground()
        }
    }

    private fun createInputBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#22FFFFFF"))
            cornerRadius = 10f
        }
    }

    private fun handleRegistration() {
        if (validateInputs()) {
            performRegistration()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        nameInputLayout.error = null
        phoneInputLayout.error = null
        emailInputLayout.error = null
        passwordInputLayout.error = null

        val name = nameEditText.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            nameInputLayout.error = "Name must be at least 2 characters"
            isValid = false
        }

        val phone = phoneEditText.text.toString().trim()
        if (TextUtils.isEmpty(phone)) {
            phoneInputLayout.error = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            phoneInputLayout.error = "Please enter a valid phone number"
            isValid = false
        }

        val email = emailEditText.text.toString().trim()
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email address"
            isValid = false
        }

        val password = passwordEditText.text.toString().trim()
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (!termsCheckBox.isChecked) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun performRegistration() {
        if (!NetworkMonitor.isOnline(this)) {
            Toast.makeText(this, "Network required for initial registration and security key exchange.", Toast.LENGTH_LONG).show()
            return
        }

        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        registerButton.isEnabled = false
        registerButton.text = "Creating Account..."
        registerButton.background = createDisabledButton()

        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Welcome to SentinalX Enterprise!", Toast.LENGTH_SHORT).show()

            generateAndSaveInitialTwins(name)

            val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            sharedPref.edit().apply {
                putBoolean("isLoggedIn", true)
                putString("user_name", name)
                putString("user_email", email)
                putString("user_phone", phone)
                commit() // Use commit() for critical data before navigation
            }

            val dashboardIntent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("user_name", name)
            }
            startActivity(dashboardIntent)
            finish()
        }, 2000)
    }

    private fun generateAndSaveInitialTwins(fullName: String) {
        val nameParts = fullName.trim().lowercase().split(" ")
        val firstName = nameParts.firstOrNull() ?: "user"
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val initialTwins = mutableListOf(
            "${firstName.uppercase()}-Twin-A" to "Active",
            "${firstName.first().uppercase()}Patil-D${Random.nextInt(10, 99)}" to "Standby",
            "${firstName.uppercase()}_SEC_${Random.nextInt(100, 999)}" to "Standby"
        )

        val twinDataStrings = initialTwins.map { (name, status) ->
            "${name}|${status}|${currentDate}|${Random.nextInt(5, 50)}|${Random.nextInt(50, 300)}|${Random.nextInt(30, 150)}"
        }.toSet()

        val twinNames = initialTwins.map { it.first }.toSet()

        val sharedPrefs = getSharedPreferences("TwinDataPrefs", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putStringSet("digitalTwins", twinDataStrings)
            putStringSet("digitalTwinNames", twinNames)
            apply()
        }
    }

    // Styling helper methods
    private fun createTransparentCard(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(TRANSPARENT_CARD))
            cornerRadius = 16f
            setStroke(2, Color.parseColor(BORDER_CYAN))
        }
    }

    private fun createCyanButton(): GradientDrawable {
        return GradientDrawable().apply {
            colors = intArrayOf(
                Color.parseColor(PRIMARY_CYAN),
                Color.parseColor(BRIGHT_CYAN)
            )
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
            cornerRadius = 10f
        }
    }

    private fun createDisabledButton(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#555555"))
            cornerRadius = 10f
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
            cornerRadius = 16f
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}