package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dayOfWeek: Int = 0, // 0 = Monday, 1 = Tuesday, ..., 6 = Sunday
    val name: String = "",
    val youtubeUrl: String = "",
    val sets: Int = 3,
    val reps: String = "10-12",
    val restTimeSeconds: Int = 0, // rest time in seconds, 0 = not set
    val completedSets: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val orderIndex: Int = 0,
    val supersetGroupId: String = "" // non-empty = part of a superset, matching ids are grouped
)
