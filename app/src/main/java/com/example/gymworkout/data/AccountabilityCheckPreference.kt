package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccountabilityCheckPreference {
    private const val PREF_NAME = "accountability_check_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TIME = "time"
    const val DEFAULT_TIME = "21:00"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _time = MutableStateFlow(DEFAULT_TIME)
    val time: StateFlow<String> = _time

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _time.value = prefs.getString(KEY_TIME, DEFAULT_TIME) ?: DEFAULT_TIME
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    fun getEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setTime(context: Context, time: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIME, time).apply()
        _time.value = time
    }

    fun getTime(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TIME, DEFAULT_TIME) ?: DEFAULT_TIME
}
