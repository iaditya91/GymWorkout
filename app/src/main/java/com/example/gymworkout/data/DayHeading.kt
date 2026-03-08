package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_headings")
data class DayHeading(
    @PrimaryKey
    val dayOfWeek: Int = 0, // 0=Monday, 6=Sunday
    val heading: String = ""
)
