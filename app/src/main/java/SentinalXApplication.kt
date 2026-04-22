package com.example.sentinalx

import android.app.Application
import android.util.Log
import com.example.sentinalx.workers.CookieCleanupScheduler

class SentinalXApplication : Application() {

    private val TAG = "SentinalXApp"

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 SentinalX Application started")

        // Schedule weekly cookie cleanup
        CookieCleanupScheduler.scheduleWeeklyCleanup(this)

        Log.d(TAG, "✅ Weekly cookie cleanup scheduled")
    }
}