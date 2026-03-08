package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_reminders")
data class WorkoutReminder(
    @PrimaryKey
    val dayOfWeek: Int = 0, // 0=Monday to 6=Sunday
    val time: String = "",  // "HH:mm" format
    val enabled: Boolean = false
)
