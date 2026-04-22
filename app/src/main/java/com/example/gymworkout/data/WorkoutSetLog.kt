package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row = one completed set. Weight is always stored in kg (canonical) —
 * the UI converts to/from the user's preferred unit using UserProfile.weightUnit.
 */
@Entity(
    tableName = "workout_set_logs",
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["loggedAt"])
    ]
)
data class WorkoutSetLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val exerciseId: Int = 0,
    val exerciseName: String = "",
    val dayOfWeek: Int = 0,
    val setIndex: Int = 0,
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val loggedAt: Long = 0L
) {
    val volumeKg: Double get() = reps * weightKg
}
