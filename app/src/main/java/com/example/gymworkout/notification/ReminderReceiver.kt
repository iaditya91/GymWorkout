package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.NutritionCategory

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val customText = intent.getStringExtra(EXTRA_CUSTOM_TEXT) ?: ""
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val timeIndex = intent.getIntExtra(EXTRA_TIME_INDEX, 0)

        val categoryEnum = try {
            NutritionCategory.valueOf(category)
        } catch (_: Exception) {
            return
        }

        val title = "${categoryEnum.label} Reminder"
        val text = customText.ifBlank {
            when (categoryEnum) {
                NutritionCategory.WATER -> "Time to drink water!"
                NutritionCategory.CARBS -> "Don't forget your carbs intake!"
                NutritionCategory.PROTEIN -> "Time for your protein!"
                NutritionCategory.VITAMINS -> "Remember to take your vitamins!"
                NutritionCategory.SLEEP -> "Time to prepare for sleep!"
            }
        }

        NotificationHelper.createNotificationChannel(context)
        // Unique notification ID per reminder + time slot
        val notificationId = reminderId * 100 + timeIndex
        NotificationHelper.showNotification(context, notificationId, title, text)
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_CUSTOM_TEXT = "extra_custom_text"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TIME_INDEX = "extra_time_index"
    }
}
