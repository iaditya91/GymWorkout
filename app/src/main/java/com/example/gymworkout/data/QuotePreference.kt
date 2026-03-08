package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object QuotePreference {
    private const val PREF_NAME = "quote_prefs"
    private const val KEY_ENABLED = "quote_enabled"
    private const val KEY_SOURCE = "quote_source"       // "APP", "CUSTOM", "BOTH"
    private const val KEY_TIME = "quote_time"            // "HH:mm" format

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _source = MutableStateFlow("APP")
    val source: StateFlow<String> = _source

    private val _time = MutableStateFlow("08:00")
    val time: StateFlow<String> = _time

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _source.value = prefs.getString(KEY_SOURCE, "APP") ?: "APP"
        _time.value = prefs.getString(KEY_TIME, "08:00") ?: "08:00"
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    fun setSource(context: Context, source: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SOURCE, source).apply()
        _source.value = source
    }

    fun setTime(context: Context, time: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIME, time).apply()
        _time.value = time
    }

    fun getEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun getSource(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SOURCE, "APP") ?: "APP"
    }

    fun getTime(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TIME, "08:00") ?: "08:00"
    }
}
