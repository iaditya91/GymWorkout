package com.example.gymworkout.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.gymworkout.data.AccountabilityCheckPreference
import java.time.LocalTime
import java.util.Calendar

object AccountabilityCheckScheduler {

    const val EXTRA_PARTNERSHIP_ID = "partnershipId"
    private const val REQUEST_CODE_BASE = 9200

    fun scheduleForPartner(context: Context, partnershipId: String) {
        if (partnershipId.isEmpty()) return
        val time = AccountabilityCheckPreference.ensureTimeForPartner(context, partnershipId)
        val parsed = try {
            LocalTime.parse(time)
        } catch (_: Exception) {
            LocalTime.of(20, 0)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildPendingIntent(context, partnershipId)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.hour)
            set(Calendar.MINUTE, parsed.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
            )
        }
    }

    fun cancelForPartner(context: Context, partnershipId: String) {
        if (partnershipId.isEmpty()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(buildPendingIntent(context, partnershipId))
    }

    fun rescheduleAll(context: Context) {
        val all = AccountabilityCheckPreference.getAllPartnerTimes(context)
        all.keys.forEach { scheduleForPartner(context, it) }
    }

    private fun buildPendingIntent(context: Context, partnershipId: String): PendingIntent {
        val intent = Intent(context, AccountabilityCheckReceiver::class.java).apply {
            putExtra(EXTRA_PARTNERSHIP_ID, partnershipId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + partnershipId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
