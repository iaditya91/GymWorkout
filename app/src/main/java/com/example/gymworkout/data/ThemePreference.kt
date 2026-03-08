package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemePreference {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = follow system

    val isDarkMode: StateFlow<Boolean?> = _isDarkMode

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else {
            null // follow system
        }
    }

    fun setDarkMode(context: Context, darkMode: Boolean?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (darkMode == null) {
            prefs.edit().remove(KEY_DARK_MODE).apply()
        } else {
            prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply()
        }
        _isDarkMode.value = darkMode
    }
}
