package com.example.gymworkout.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object TimerSoundPreference {

    private const val PREFS_NAME = "timer_sound_prefs"
    private const val KEY_REST_TIMER_SOUND = "rest_timer_sound_uri"
    private const val KEY_HABIT_TIMER_SOUND = "habit_timer_sound_uri"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRestTimerSoundUri(context: Context): Uri {
        val uriStr = prefs(context).getString(KEY_REST_TIMER_SOUND, null)
        return if (uriStr != null) Uri.parse(uriStr)
        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun setRestTimerSoundUri(context: Context, uri: Uri) {
        prefs(context).edit().putString(KEY_REST_TIMER_SOUND, uri.toString()).apply()
    }

    fun getRestTimerSoundName(context: Context): String {
        val uri = getRestTimerSoundUri(context)
        return getRingtoneName(context, uri)
    }

    fun getHabitTimerSoundUri(context: Context): Uri {
        val uriStr = prefs(context).getString(KEY_HABIT_TIMER_SOUND, null)
        return if (uriStr != null) Uri.parse(uriStr)
        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun setHabitTimerSoundUri(context: Context, uri: Uri) {
        prefs(context).edit().putString(KEY_HABIT_TIMER_SOUND, uri.toString()).apply()
    }

    fun getHabitTimerSoundName(context: Context): String {
        val uri = getHabitTimerSoundUri(context)
        return getRingtoneName(context, uri)
    }

    fun getRingtoneName(context: Context, uri: Uri): String {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Default"
        } catch (_: Exception) {
            "Default"
        }
    }
}
