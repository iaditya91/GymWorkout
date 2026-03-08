package com.example.gymworkout.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.gymworkout.data.WorkoutReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

object WorkoutReminderScheduler {

    private const val REQUEST_CODE_BASE = 7000

    /**
     * Map app dayOfWeek (0=Monday..6=Sunday) to Calendar dayOfWeek
     */
    private fun appDayToCalendarDay(appDay: Int): Int = when (appDay) {
        0 -> Calendar.MONDAY
        1 -> Calendar.TUESDAY
        2 -> Calendar.WEDNESDAY
        3 -> Calendar.THURSDAY
        4 -> Calendar.FRIDAY
        5 -> Calendar.SATURDAY
        6 -> Calendar.SUNDAY
        else -> Calendar.MONDAY
    }

    fun scheduleForDay(context: Context, reminder: WorkoutReminder) {
        if (!reminder.enabled || reminder.time.isBlank()) {
            cancelForDay(context, reminder.dayOfWeek)
            return
        }

        val time = try {
            LocalTime.parse(reminder.time)
        } catch (_: Exception) {
            return
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            putExtra(WorkoutReminderReceiver.EXTRA_DAY_OF_WEEK, reminder.dayOfWeek)
        }

        val requestCode = REQUEST_CODE_BASE + reminder.dayOfWeek
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Find next occurrence of this day of week at the given time
        val calDay = appDayToCalendarDay(reminder.dayOfWeek)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the target time is in the past, move to next week
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val triggerTime = target.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
        }
    }

    fun cancelForDay(context: Context, dayOfWeek: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val requestCode = REQUEST_CODE_BASE + dayOfWeek
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(context: Context) {
        for (day in 0..6) {
            cancelForDay(context, day)
        }
    }
}
