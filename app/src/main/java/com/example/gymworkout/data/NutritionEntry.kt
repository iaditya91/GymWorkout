package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NutritionCategory(val label: String, val unit: String) {
    WATER("Water", "L"),
    CARBS("Carbs", "cal"),
    PROTEIN("Protein", "g"),
    VITAMINS("Vitamins", "count"),
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
    val category: String = "", // NutritionCategory name
    val targetValue: Float = 0f
)
