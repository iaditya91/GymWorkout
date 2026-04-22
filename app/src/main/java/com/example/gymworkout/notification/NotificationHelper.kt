package com.example.gymworkout.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.gymworkout.MainActivity
import com.example.gymworkout.R

object NotificationHelper {

    const val CHANNEL_ID = "nutrition_reminders"
    private const val CHANNEL_NAME = "Nutrition Reminders"
    private const val CHANNEL_DESC = "Reminders for water, carbs, protein, vitamins, and sleep"

    private const val WORKOUT_CHANNEL_ID = "workout_reminders"
    private const val WORKOUT_CHANNEL_NAME = "Workout Reminders"
    private const val WORKOUT_CHANNEL_DESC = "Daily workout reminders"

    private const val QUOTE_CHANNEL_ID = "quote_reminders"
    private const val QUOTE_CHANNEL_NAME = "Motivational Quotes"
    private const val QUOTE_CHANNEL_DESC = "Daily motivational workout quotes"

    private const val AUTO_BACKUP_CHANNEL_ID = "auto_backup"
    private const val AUTO_BACKUP_CHANNEL_NAME = "Auto Backup"
    private const val AUTO_BACKUP_CHANNEL_DESC = "Daily automatic backup notifications"

    const val SOCIAL_CHANNEL_ID = "social_events"
    private const val SOCIAL_CHANNEL_NAME = "Social"
    private const val SOCIAL_CHANNEL_DESC = "Friend requests, battle/duel invites, and partnership requests"

    const val ACCOUNTABILITY_CHANNEL_ID = "accountability_check"
    private const val ACCOUNTABILITY_CHANNEL_NAME = "Accountability Partners"
    private const val ACCOUNTABILITY_CHANNEL_DESC = "Daily nudges when a partner hasn't logged their workout or habits"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createWorkoutNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WORKOUT_CHANNEL_ID,
                WORKOUT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = WORKOUT_CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createQuoteNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QUOTE_CHANNEL_ID,
                QUOTE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = QUOTE_CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createAiPlannerNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ai_planner_reminders",
                "AI Planner Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily AI planner tips at a random time"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createAutoBackupNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AUTO_BACKUP_CHANNEL_ID,
                AUTO_BACKUP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AUTO_BACKUP_CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createSocialNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SOCIAL_CHANNEL_ID,
                SOCIAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = SOCIAL_CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createAccountabilityNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ACCOUNTABILITY_CHANNEL_ID,
                ACCOUNTABILITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ACCOUNTABILITY_CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun createProgressNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "progress_notification",
                "Daily Progress Card",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing card showing today's workout, nutrition, sleep, and hydration progress"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        channelId: String = CHANNEL_ID,
        soundUri: Uri? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // On Android 8+, notification sound is controlled by the channel, not the builder.
        // Create a unique channel per sound URI so custom sounds actually play.
        val effectiveChannelId = if (soundUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val customChannelId = "${channelId}_sound_${soundUri.hashCode()}"
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(customChannelId) == null) {
                val baseChannel = manager.getNotificationChannel(channelId)
                val channelName = (baseChannel?.name ?: "Reminders").toString() + " (Custom Sound)"
                val channel = NotificationChannel(
                    customChannelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = baseChannel?.description ?: ""
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(channel)
            }
            customChannelId
        } else {
            channelId
        }

        val builder = NotificationCompat.Builder(context, effectiveChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (soundUri != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(soundUri)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, builder.build())
    }

    /**
     * Delete old custom sound channels for a base channel when sound changes.
     * Call this when the user picks a new sound, passing the new soundUri.
     */
    fun cleanupOldSoundChannels(context: Context, baseChannelId: String, newSoundUri: Uri?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val keepChannelId = if (newSoundUri != null) {
            "${baseChannelId}_sound_${newSoundUri.hashCode()}"
        } else null
        manager.notificationChannels
            .filter { it.id.startsWith("${baseChannelId}_sound_") && it.id != keepChannelId }
            .forEach { manager.deleteNotificationChannel(it.id) }
    }
}
