package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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

        if (!notifyEnabled) return

        // Both habit and rest timers: continuous ring/vibrate until dismissed in-app
        TimerAlertService.start(context, label, timerType)
    }
}
