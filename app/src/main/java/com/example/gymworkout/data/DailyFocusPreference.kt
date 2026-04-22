package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DailyFocusPreference {
    private const val PREF_NAME = "daily_focus_prefs"
    private const val KEY_MODE = "focus_mode"
    private const val KEY_GOAL_TEXT = "focus_goal_text"
    private const val KEY_HABIT_CATEGORY = "focus_habit_category"

    const val MODE_NONE = "NONE"
    const val MODE_GOAL = "GOAL"
    const val MODE_HABIT = "HABIT"

    data class Focus(
        val mode: String = MODE_NONE,
        val goalText: String = "",
        val habitCategory: String = ""
    )

    private val _focus = MutableStateFlow(Focus())
    val focus: StateFlow<Focus> = _focus

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _focus.value = Focus(
            mode = prefs.getString(KEY_MODE, MODE_NONE) ?: MODE_NONE,
            goalText = prefs.getString(KEY_GOAL_TEXT, "") ?: "",
            habitCategory = prefs.getString(KEY_HABIT_CATEGORY, "") ?: ""
        )
    }

    fun setGoal(context: Context, text: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, MODE_GOAL)
            .putString(KEY_GOAL_TEXT, text)
            .apply()
        _focus.value = _focus.value.copy(mode = MODE_GOAL, goalText = text)
    }

    fun setHabit(context: Context, category: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, MODE_HABIT)
            .putString(KEY_HABIT_CATEGORY, category)
            .apply()
        _focus.value = _focus.value.copy(mode = MODE_HABIT, habitCategory = category)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, MODE_NONE)
            .apply()
        _focus.value = _focus.value.copy(mode = MODE_NONE)
    }

    fun get(context: Context): Focus {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Focus(
            mode = prefs.getString(KEY_MODE, MODE_NONE) ?: MODE_NONE,
            goalText = prefs.getString(KEY_GOAL_TEXT, "") ?: "",
            habitCategory = prefs.getString(KEY_HABIT_CATEGORY, "") ?: ""
        )
    }
}
