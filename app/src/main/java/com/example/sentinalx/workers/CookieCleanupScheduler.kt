package com.example.sentinalx.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object CookieCleanupScheduler {

    private const val TAG = "CleanupScheduler"
    private const val WORK_NAME = "weekly_cookie_cleanup"

    /**
     * Schedule weekly cookie cleanup
     */
    fun scheduleWeeklyCleanup(context: Context) {
        try {
            // Create constraints (run only when device is idle and charging - optional)
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Create periodic work request (every 7 days)
            val cleanupRequest = PeriodicWorkRequestBuilder<WeeklyCookieCleanupWorker>(
                7, TimeUnit.DAYS,  // Repeat every 7 days
                1, TimeUnit.HOURS   // Flex interval (can run within 1 hour window)
            )
                .setConstraints(constraints)
                .addTag("cookie_cleanup")
                .build()

            // Enqueue the work (replace existing if already scheduled)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already running
                cleanupRequest
            )

            Log.d(TAG, "✅ Weekly cookie cleanup scheduled")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule cleanup: ${e.message}")
        }
    }

    /**
     * Cancel scheduled cleanup
     */
    fun cancelScheduledCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "🛑 Weekly cleanup cancelled")
    }

    /**
     * Trigger immediate cleanup (for testing or manual trigger)
     */
    fun triggerImmediateCleanup(context: Context) {
        val cleanupRequest = OneTimeWorkRequestBuilder<WeeklyCookieCleanupWorker>()
            .addTag("manual_cleanup")
            .build()

        WorkManager.getInstance(context).enqueue(cleanupRequest)
        Log.d(TAG, "▶️ Immediate cleanup triggered")
    }
}