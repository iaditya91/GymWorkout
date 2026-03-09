package com.example.gymworkout.data

data class ExerciseInfo(
    val name: String = "",
    val force: String? = null,
    val level: String = "",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "",
    val images: List<String> = emptyList(),
    val id: String = ""
)
