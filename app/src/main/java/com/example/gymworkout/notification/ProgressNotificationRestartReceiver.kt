package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.ProgressNotificationPreference

/**
 * Re-posts the persistent progress notification when the user swipes it away.
 *
 * Android 14+ allows the user to dismiss ongoing foreground-service notifications. The progress
 * card is a user-opt-in "always on" status, so when the system fires the delete intent we restart
 * the foreground service — which re-calls startForeground and puts the card back.
 *
 * If the user has turned the feature off in settings, we do nothing.
 */
class ProgressNotificationRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!ProgressNotificationPreference.getEnabled(context)) return
        ProgressNotificationService.start(context)
    }
}
