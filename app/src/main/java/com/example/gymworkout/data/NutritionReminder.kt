package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_reminders")
data class NutritionReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String = "",          // NutritionCategory.name
    val type: String = "SPECIFIC",      // "SPECIFIC" or "INTERVAL"
    val customText: String = "",        // Custom notification text
    val enabled: Boolean = true,
    // For SPECIFIC type: comma-separated times like "08:00,12:00,18:00"
    val specificTimes: String = "",
    // For INTERVAL type:
    val startTime: String = "",         // "08:00"
    val endTime: String = "",           // "22:00"
    val intervalMinutes: Int = 0,        // e.g., 60 for every hour
    val ringtoneUri: String = ""         // Custom notification ringtone URI (empty = default)
)
