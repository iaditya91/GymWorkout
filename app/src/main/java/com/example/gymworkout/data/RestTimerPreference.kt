package com.example.gymworkout.data

import android.content.Context

/**
 * Whether a manually started rest timer opens as the on-screen (inline) bar
 * instead of the popup dialog.
 */
object RestTimerPreference {

    private const val PREFS_NAME = "rest_timer_prefs"
    private const val KEY_SHOW_ON_SCREEN_DEFAULT = "show_on_screen_default"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isShowOnScreenDefault(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_ON_SCREEN_DEFAULT, false)

    fun setShowOnScreenDefault(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_ON_SCREEN_DEFAULT, enabled).apply()
    }
}
