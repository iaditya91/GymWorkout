package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["category", "date"])]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String = "",   // matches NutritionTarget.category
    val date: String = "",       // yyyy-MM-dd
    val mood: String = "",       // emoji string, e.g. "😀"
    val text: String = "",       // free-form journal text
    val createdAt: Long = 0L     // epoch millis
)
