package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val weight: Float = 0f,
    val weightUnit: String = "kg", // "kg" or "lb"
    val height: Float = 0f,
    val heightUnit: String = "cm", // "cm" or "ft"
    val photoUris: String = "", // comma-separated URIs
    val targetWeight: Float = 0f,
    val startingWeight: Float = 0f,
    val fitnessLevel: String = "beginner", // "beginner", "intermediate", "advanced"
    val journeyStartDate: String = "" // yyyy-MM-dd
)

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String = "DO", // "DO" or "DONT"
    val text: String = "",
    val isChecked: Boolean = false
)
