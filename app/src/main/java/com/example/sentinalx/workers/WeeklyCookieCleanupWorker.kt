package com.example.sentinalx.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sentinalx.utils.CookieIsolationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeeklyCookieCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "CookieCleanup"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 Starting weekly cookie cleanup...")

            // Clear all cookies
            CookieIsolationManager.clearAllCookies {
                Log.d(TAG, "✅ Weekly cleanup completed successfully")
            }

            // Return success
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Weekly cleanup failed: ${e.message}")
            // Retry on failure
            Result.retry()
        }
    }
}