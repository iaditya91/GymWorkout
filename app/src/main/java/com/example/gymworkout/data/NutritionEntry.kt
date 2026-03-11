package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NutritionCategory(val label: String, val unit: String) {
    WATER("Water", "L"),
    CARBS("Carbs", "g"),
    CALORIES("Calories", "cal"),
    PROTEIN("Protein", "g"),
    SLEEP("Sleep", "hrs")
}

@Entity(tableName = "nutrition_entries")
data class NutritionEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String = "", // yyyy-MM-dd
    val category: String = "", // NutritionCategory name
    val value: Float = 0f
)

@Entity(tableName = "nutrition_targets")
data class NutritionTarget(
    @PrimaryKey
    val category: String = "", // NutritionCategory name or custom key
    val targetValue: Float = 0f,
    val label: String = "",    // Display name (e.g. "Water", "Creatine")
    val unit: String = "",     // Unit (e.g. "L", "g", "mg")
    val isCustom: Boolean = false, // true for user-added objectives
    val notes: String = "",    // User notes for this objective
    val timerSeconds: Int = 0, // Timer duration in seconds (0 = no timer)
    val timerNotifyEnabled: Boolean = true   // Show notification when timer finishes (respects device sound profile)
)
