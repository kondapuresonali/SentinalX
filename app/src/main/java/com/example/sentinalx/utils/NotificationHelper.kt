package com.example.sentinalx.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = "sentinalx_protection"
    private const val CHANNEL_NAME = "SentinalX Protection"
    private var notificationId = 1000

    fun sendThreatNotification(
        context: Context,
        threatLevel: ThreatLevel,
        url: String,
        sourceApp: String
    ) {
        // Create notification channel (Android 8.0+)
        createNotificationChannel(context)

        // Get shortened URL for notification
        val shortUrl = url.take(50) + if (url.length > 50) "..." else ""

        // Build notification based on threat level
        val (title, message, priority) = when (threatLevel) {
            ThreatLevel.SAFE -> Triple(
                "✅ Safe Link Detected",
                "Link verified safe in $sourceApp",
                NotificationCompat.PRIORITY_LOW
            )
            ThreatLevel.SUSPICIOUS -> Triple(
                "⚠️ Suspicious Link Detected",
                "Link from $sourceApp shows warning signs",
                NotificationCompat.PRIORITY_HIGH
            )
            ThreatLevel.DANGEROUS -> Triple(
                "❌ Dangerous Link Blocked",
                "High-risk link intercepted in $sourceApp",
                NotificationCompat.PRIORITY_MAX
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\n\nURL: $shortUrl"))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId++, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted (Android 13+)
            android.util.Log.w("NotificationHelper", "Notification permission not granted")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for link scanning and threat detection"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}