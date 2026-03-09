package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.DayHeading
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).exerciseDao()

    fun getExercisesForDay(day: Int): Flow<List<Exercise>> = dao.getExercisesForDay(day)

    fun getExerciseCountForDay(day: Int): Flow<Int> = dao.getExerciseCountForDay(day)

    fun getCompletedCountForDay(day: Int): Flow<Int> = dao.getCompletedCountForDay(day)

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.insert(exercise)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.update(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            val groupId = exercise.supersetGroupId
            dao.delete(exercise)
            // If deleted exercise was in a superset, check if only 1 remains
            if (groupId.isNotBlank()) {
                val remaining = dao.getExercisesByGroupId(groupId)
                if (remaining.size == 1) {
                    dao.clearSupersetGroupId(remaining[0].id)
                }
            }
        }
    }

    fun convertToSuperset(existingExercise: Exercise, newExercise: Exercise) {
        viewModelScope.launch {
            val groupId = java.util.UUID.randomUUID().toString()
            dao.update(existingExercise.copy(supersetGroupId = groupId))
            dao.insert(newExercise.copy(supersetGroupId = groupId))
        }
    }

    fun toggleCompleted(id: Int, completed: Boolean) {
        viewModelScope.launch {
            dao.updateCompleted(id, completed)
        }
    }

    fun updateNotes(id: Int, notes: String) {
        viewModelScope.launch {
            dao.updateNotes(id, notes)
        }
    }

    fun reorderExercises(orderedExercises: List<Exercise>) {
        viewModelScope.launch {
            orderedExercises.forEachIndexed { index, exercise ->
                dao.updateOrderIndex(exercise.id, index)
            }
        }
    }

    fun resetDay(day: Int) {
        viewModelScope.launch {
            dao.resetDay(day)
        }
    }

    fun resetAllDays() {
        viewModelScope.launch {
            dao.resetAllDays()
        }
    }

    fun getDayHeading(day: Int): Flow<DayHeading?> = dao.getDayHeading(day)

    fun saveDayHeading(day: Int, heading: String) {
        viewModelScope.launch {
            dao.upsertDayHeading(DayHeading(dayOfWeek = day, heading = heading.trim()))
        }
    }
}
