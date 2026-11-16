package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Display unit ("kg" or "lb") for lifting weights in the exercise screens.
 * Set logs are always stored in kg; this only controls display/input conversion.
 */
object WeightUnitPreference {
    const val KG = "kg"
    const val LB = "lb"

    private const val PREF_NAME = "weight_unit_prefs"
    private const val KEY_UNIT = "exercise_weight_unit"

    private val _unit = MutableStateFlow(KG)
    val unit: StateFlow<String> = _unit

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _unit.value = prefs.getString(KEY_UNIT, KG)?.takeIf { it == KG || it == LB } ?: KG
        initialized = true
    }

    fun setUnit(context: Context, unit: String) {
        val value = if (unit == LB) LB else KG
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_UNIT, value).apply()
        initialized = true
        _unit.value = value
    }
}
