package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        val ringtoneUriStr = intent.getStringExtra(EXTRA_RINGTONE_URI) ?: ""

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryEnum = try {
                    NutritionCategory.valueOf(category)
                } catch (_: Exception) {
                    null
                }

                val db = WorkoutDatabase.getDatabase(context)
                val label = if (categoryEnum != null) {
                    categoryEnum.label
                } else {
                    db.nutritionDao().getTargetSync(category)?.label ?: category
                }

                val title = "$label Reminder"
                val text = customText.ifBlank {
                    when (categoryEnum) {
                        NutritionCategory.WATER -> "Time to drink water!"
                        NutritionCategory.CALORIES -> "Don't forget your calorie intake!"
                        NutritionCategory.CARBS -> "Don't forget your carbs intake!"
                        NutritionCategory.PROTEIN -> "Time for your protein!"
                        NutritionCategory.SLEEP -> "Time to prepare for sleep!"
                        null -> "Don't forget your $label!"
                    }
                }

                NotificationHelper.createNotificationChannel(context)
                val notificationId = reminderId * 100 + timeIndex
                val soundUri = if (ringtoneUriStr.isNotBlank()) Uri.parse(ringtoneUriStr) else null
                NotificationHelper.showNotification(context, notificationId, title, text, soundUri = soundUri)

                // Reschedule for next day
                val reminder = db.reminderDao().getReminderById(reminderId)
                if (reminder != null && reminder.enabled) {
                    ReminderScheduler.scheduleReminder(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_CUSTOM_TEXT = "extra_custom_text"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TIME_INDEX = "extra_time_index"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
    }
}
