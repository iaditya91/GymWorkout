package com.example.gymworkout.data.sync

import com.example.gymworkout.data.ChecklistItem
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.DayHeading
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.data.FoodLogEntry
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WorkoutReminder
import com.example.gymworkout.data.CustomFoodItem
import com.example.gymworkout.data.MotivationalQuote

data class BackupPhoto(
    val fileName: String = "",
    val base64Data: String = ""
)

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val exercises: List<Exercise> = emptyList(),
    val nutritionEntries: List<NutritionEntry> = emptyList(),
    val nutritionTargets: List<NutritionTarget> = emptyList(),
    val dailyCheckIns: List<DailyCheckIn> = emptyList(),
    val userProfiles: List<UserProfile> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val nutritionReminders: List<NutritionReminder> = emptyList(),
    val dayHeadings: List<DayHeading> = emptyList(),
    val workoutReminders: List<WorkoutReminder> = emptyList(),
    val foodLogEntries: List<FoodLogEntry> = emptyList(),
    val themePreference: Boolean? = null,
    val customQuotes: List<MotivationalQuote> = emptyList(),
    val quoteEnabled: Boolean = false,
    val quoteSource: String = "APP",
    val quoteTime: String = "08:00",
    val progressPhotos: List<BackupPhoto> = emptyList(),
    val customFoods: List<CustomFoodItem> = emptyList()
)
