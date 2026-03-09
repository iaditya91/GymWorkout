package com.example.gymworkout.data

data class MuscleTarget(
    val target: String = "",
    val subTargets: List<String> = emptyList()
)

data class ReplacementExercise(
    val name: String = "",
    val id: String = ""
)

data class ExerciseInfo(
    val name: String = "",
    val force: String? = null,
    val level: String = "",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<MuscleTarget> = emptyList(),
    val secondaryMuscles: List<MuscleTarget> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "",
    val images: List<String> = emptyList(),
    val id: String = "",
    val replacementExercises: List<ReplacementExercise> = emptyList()
)
