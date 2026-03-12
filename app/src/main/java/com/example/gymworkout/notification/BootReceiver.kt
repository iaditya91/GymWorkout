package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.AiPlannerPreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = WorkoutDatabase.getDatabase(context)

                // Reschedule nutrition reminders
                val reminders = db.reminderDao().getAllEnabledReminders()
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(context, reminder)
                }

                // Reschedule workout reminders
                val workoutReminders = db.userDao().getEnabledWorkoutReminders()
                workoutReminders.forEach { wr ->
                    WorkoutReminderScheduler.scheduleForDay(context, wr)
                }

                // Reschedule quote reminders
                if (QuotePreference.getEnabled(context)) {
                    val time = QuotePreference.getTime(context)
                    QuoteReminderScheduler.schedule(context, time)
                }

                // Reschedule auto backup
                if (AutoBackupScheduler.isEnabled(context)) {
                    AutoBackupScheduler.schedule(context)
                }

                // Reschedule AI planner notifications
                if (AiPlannerPreference.getEnabled(context)) {
                    AiPlannerNotificationScheduler.schedule(context)
                }
            }
        }
    }
}
