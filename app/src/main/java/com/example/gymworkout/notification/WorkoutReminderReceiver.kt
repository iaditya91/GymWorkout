package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        if (dayOfWeek < 0) return

        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayName = dayNames.getOrElse(dayOfWeek) { "today" }

        NotificationHelper.createWorkoutNotificationChannel(context)
        val notificationId = NOTIFICATION_ID_BASE + dayOfWeek
        NotificationHelper.showNotification(
            context, notificationId,
            "Workout Reminder",
            "Time to workout! It's $dayName - let's go!",
            channelId = "workout_reminders"
        )

        // Reschedule for next week
        CoroutineScope(Dispatchers.IO).launch {
            val dao = WorkoutDatabase.getDatabase(context).userDao()
            val reminders = dao.getEnabledWorkoutReminders()
            val reminder = reminders.find { it.dayOfWeek == dayOfWeek }
            if (reminder != null) {
                WorkoutReminderScheduler.scheduleForDay(context, reminder)
            }
        }
    }

    companion object {
        const val EXTRA_DAY_OF_WEEK = "extra_workout_day"
        const val NOTIFICATION_ID_BASE = 5000
    }
}
