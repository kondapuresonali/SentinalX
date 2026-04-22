package com.example.sentinalx.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sentinalx.R
import android.widget.Button

class BrowserIntegrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_integration)

        val btnInstallExtension = findViewById<Button>(R.id.btn_install_extension)
        btnInstallExtension.setOnClickListener {
            // Future: Logic to guide user to install browser extension
        }
    }
}
