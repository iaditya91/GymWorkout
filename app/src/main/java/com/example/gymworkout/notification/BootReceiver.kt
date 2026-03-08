package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = WorkoutDatabase.getDatabase(context).reminderDao()
                val reminders = dao.getAllEnabledReminders()
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(context, reminder)
                }
            }
        }
    }
}
