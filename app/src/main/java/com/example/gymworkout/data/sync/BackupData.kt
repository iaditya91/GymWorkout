package com.example.gymworkout.data.sync

import com.example.gymworkout.data.ChecklistItem
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.UserProfile

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
    val themePreference: Boolean? = null
)
