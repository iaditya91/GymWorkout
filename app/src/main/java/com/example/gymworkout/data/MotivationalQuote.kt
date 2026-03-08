package com.example.gymworkout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motivational_quotes")
data class MotivationalQuote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String = ""
)
