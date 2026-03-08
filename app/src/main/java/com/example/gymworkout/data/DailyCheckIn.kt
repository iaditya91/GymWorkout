package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckIn(
    @PrimaryKey
    val date: String = "", // yyyy-MM-dd
    val workoutDone: Boolean = false,
    val nutritionDone: Boolean = false,
    val sleepDone: Boolean = false
)
