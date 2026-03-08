package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return
        val customText = intent.getStringExtra(EXTRA_CUSTOM_TEXT) ?: ""
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val timeIndex = intent.getIntExtra(EXTRA_TIME_INDEX, 0)

        val categoryEnum = try {
            NutritionCategory.valueOf(category)
        } catch (_: Exception) {
            null
        }

        val title = if (categoryEnum != null) {
            "${categoryEnum.label} Reminder"
        } else {
            "$category Reminder"
        }
        val text = customText.ifBlank {
            when (categoryEnum) {
                NutritionCategory.WATER -> "Time to drink water!"
                NutritionCategory.CARBS -> "Don't forget your carbs intake!"
                NutritionCategory.PROTEIN -> "Time for your protein!"
                NutritionCategory.VITAMINS -> "Remember to take your vitamins!"
                NutritionCategory.SLEEP -> "Time to prepare for sleep!"
                null -> "Don't forget your $category!"
            }
        }

        NotificationHelper.createNotificationChannel(context)
        val notificationId = reminderId * 100 + timeIndex
        NotificationHelper.showNotification(context, notificationId, title, text)

        // Reschedule for next day
        CoroutineScope(Dispatchers.IO).launch {
            val dao = WorkoutDatabase.getDatabase(context).reminderDao()
            val reminder = dao.getReminderById(reminderId)
            if (reminder != null && reminder.enabled) {
                ReminderScheduler.scheduleReminder(context, reminder)
            }
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_CUSTOM_TEXT = "extra_custom_text"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TIME_INDEX = "extra_time_index"
    }
}
