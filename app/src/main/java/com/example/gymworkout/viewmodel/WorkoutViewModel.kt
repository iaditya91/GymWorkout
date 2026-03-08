package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
            dao.delete(exercise)
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

    fun resetDay(day: Int) {
        viewModelScope.launch {
            dao.resetDay(day)
        }
    }
}
