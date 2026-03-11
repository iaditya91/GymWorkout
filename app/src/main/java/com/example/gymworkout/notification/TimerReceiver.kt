package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.TimerSoundPreference

class TimerReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_LABEL = "timer_label"
        const val EXTRA_NOTIFY = "timer_notify"
        const val EXTRA_NOTIFICATION_ID = "timer_notification_id"
        const val EXTRA_TIMER_TYPE = "timer_type" // "rest" or "habit"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
        val notifyEnabled = intent.getBooleanExtra(EXTRA_NOTIFY, true)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, label.hashCode())
        val timerType = intent.getStringExtra(EXTRA_TIMER_TYPE) ?: "habit"

        if (notifyEnabled) {
            val soundUri = when (timerType) {
                "rest" -> TimerSoundPreference.getRestTimerSoundUri(context)
                else -> TimerSoundPreference.getHabitTimerSoundUri(context)
            }
            NotificationHelper.showNotification(
                context = context,
                notificationId = notificationId,
                title = "$label Timer Done",
                text = "Your $label timer has finished!",
                soundUri = soundUri
            )
        }
    }
}
