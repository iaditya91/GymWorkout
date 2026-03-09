package com.example.gymworkout.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ExerciseRepository {

    private val exercises: MutableList<ExerciseInfo> = mutableListOf()

    fun load(context: Context) {
        if (exercises.isNotEmpty()) return
        val json = context.resources.openRawResource(
            context.resources.getIdentifier("exercises", "raw", context.packageName)
        ).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ExerciseInfo>>() {}.type
        exercises.addAll(Gson().fromJson<List<ExerciseInfo>>(json, type))
    }

    fun search(query: String): List<ExerciseInfo> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return exercises.filter { it.name.lowercase().contains(q) }.take(8)
    }

    fun findByName(name: String): ExerciseInfo? {
        return exercises.find { it.name.equals(name, ignoreCase = true) }
    }

    fun findById(id: String): ExerciseInfo? {
        return exercises.find { it.id == id }
    }

    fun save(exercise: ExerciseInfo) {
        val index = exercises.indexOfFirst { it.name.equals(exercise.name, ignoreCase = true) }
        if (index >= 0) {
            exercises[index] = exercise
        } else {
            exercises.add(exercise)
        }
    }

    fun generateId(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9 ]"), "").replace(" ", "_")
    }
}
