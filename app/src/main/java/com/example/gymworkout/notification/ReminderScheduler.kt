package com.example.gymworkout.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.gymworkout.data.NutritionReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val MAX_TIMES_PER_REMINDER = 50

    fun scheduleReminder(context: Context, reminder: NutritionReminder) {
        if (!reminder.enabled) {
            cancelReminder(context, reminder)
            return
        }

        val times = getAlarmTimes(reminder)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        times.forEachIndexed { index, time ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_CATEGORY, reminder.category)
                putExtra(ReminderReceiver.EXTRA_CUSTOM_TEXT, reminder.customText)
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderReceiver.EXTRA_TIME_INDEX, index)
                putExtra(ReminderReceiver.EXTRA_RINGTONE_URI, reminder.ringtoneUri)
            }

            val requestCode = reminder.id * MAX_TIMES_PER_REMINDER + index
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate next trigger time
            val now = System.currentTimeMillis()
            var triggerTime = LocalDate.now()
                .atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // If time already passed today, schedule for tomorrow
            if (triggerTime <= now) {
                triggerTime = LocalDate.now().plusDays(1)
                    .atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }

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
                // Fallback to inexact alarm if exact alarm permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            }
        }
    }

    fun cancelReminder(context: Context, reminder: NutritionReminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val times = getAlarmTimes(reminder)

        times.forEachIndexed { index, _ ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val requestCode = reminder.id * MAX_TIMES_PER_REMINDER + index
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun getAlarmTimes(reminder: NutritionReminder): List<LocalTime> {
        return when (reminder.type) {
            "SPECIFIC" -> {
                reminder.specificTimes.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { timeStr ->
                        try {
                            LocalTime.parse(timeStr.trim())
                        } catch (_: Exception) {
                            null
                        }
                    }
            }
            "INTERVAL" -> {
                if (reminder.intervalMinutes <= 0) return emptyList()
                val start = try { LocalTime.parse(reminder.startTime) } catch (_: Exception) { return emptyList() }
                val end = try { LocalTime.parse(reminder.endTime) } catch (_: Exception) { return emptyList() }
                val times = mutableListOf<LocalTime>()
                var current = start
                while (!current.isAfter(end)) {
                    times.add(current)
                    current = current.plusMinutes(reminder.intervalMinutes.toLong())
                    if (current.isBefore(start)) break // Handle midnight wrap
                }
                times
            }
            else -> emptyList()
        }
    }
}
