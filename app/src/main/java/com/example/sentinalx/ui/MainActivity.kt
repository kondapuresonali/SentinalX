package com.example.sentinalx.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.sentinalx.ui.theme.SentinalXTheme

class MainActivity : ComponentActivity() {

    private val TAG = "SentinelX_Interceptor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for incoming URL immediately
        handleIntent(intent)

        // Set a blank screen (no text)
        setContent {
            SentinalXTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Empty - just shows background color
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val url = uri.toString()

                Log.e(TAG, "═════════════════════════════════")
                Log.e(TAG, "✅ LINK INTERCEPTED: $url")
                Log.e(TAG, "═════════════════════════════════")

                // Launch scanning dialog immediately
                showScanningDialog(url)
            } else {
                Log.e(TAG, "URI is null")
            }
        } else {
            Log.d(TAG, "App launched normally (not from link)")
            // If opened normally (not from a link), just finish so user doesn't see blank screen
            finish()
        }
    }

    private fun showScanningDialog(url: String) {
        try {
            val scanIntent = Intent(this, ScanningDialogActivity::class.java)
            scanIntent.putExtra("url", url)
            scanIntent.putExtra("source_app", "link_intent")
            scanIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            scanIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(scanIntent)

            Log.e(TAG, "✅ Scanning dialog launched successfully")

            // Close MainActivity immediately after launching dialog
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error launching scanning dialog: ${e.message}")
            e.printStackTrace()
            finish()
        }
    }
}