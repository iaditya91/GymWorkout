package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.MotivationalQuotes
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuoteReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val source = QuotePreference.getSource(context)
            val db = WorkoutDatabase.getDatabase(context)
            val customQuotes = db.userDao().getAllCustomQuotesSync()

            val quote = pickQuote(source, customQuotes.map { it.text })

            if (quote != null) {
                NotificationHelper.createQuoteNotificationChannel(context)
                NotificationHelper.showNotification(
                    context, NOTIFICATION_ID,
                    "Daily Motivation",
                    quote,
                    channelId = CHANNEL_ID
                )
            }

            // Reschedule for tomorrow
            val enabled = QuotePreference.getEnabled(context)
            if (enabled) {
                val time = QuotePreference.getTime(context)
                QuoteReminderScheduler.schedule(context, time)
            }
        }
    }

    private fun pickQuote(source: String, customQuotes: List<String>): String? {
        val appQuotes = MotivationalQuotes.quotes
        return when (source) {
            "APP" -> appQuotes.randomOrNull()
            "CUSTOM" -> {
                if (customQuotes.isNotEmpty()) customQuotes.random()
                else appQuotes.randomOrNull() // fallback to app quotes if no custom ones
            }
            "BOTH" -> {
                val combined = appQuotes + customQuotes
                combined.randomOrNull()
            }
            else -> appQuotes.randomOrNull()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val CHANNEL_ID = "quote_reminders"
    }
}
