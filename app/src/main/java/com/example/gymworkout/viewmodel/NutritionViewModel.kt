package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).nutritionDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(formatter))
    val selectedDate: StateFlow<String> = _selectedDate

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun getEntriesForDate(date: String): Flow<List<NutritionEntry>> =
        dao.getEntriesForDate(date)

    fun getTotalForCategory(date: String, category: String): Flow<Float> =
        dao.getTotalForDateAndCategory(date, category)

    fun getAllTargets(): Flow<List<NutritionTarget>> = dao.getAllTargets()

    fun getTarget(category: String): Flow<NutritionTarget?> = dao.getTarget(category)

    fun addEntry(date: String, category: NutritionCategory, value: Float) {
        viewModelScope.launch {
            dao.insertEntry(
                NutritionEntry(
                    date = date,
                    category = category.name,
                    value = value
                )
            )
        }
    }

    fun deleteEntry(entry: NutritionEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    fun setTarget(category: NutritionCategory, value: Float) {
        viewModelScope.launch {
            dao.insertTarget(NutritionTarget(category = category.name, targetValue = value))
        }
    }

    fun initDefaultTargets() {
        viewModelScope.launch {
            // Only set if no targets exist
            val defaults = mapOf(
                NutritionCategory.WATER to 3f,
                NutritionCategory.CARBS to 2000f,
                NutritionCategory.PROTEIN to 120f,
                NutritionCategory.VITAMINS to 3f,
                NutritionCategory.SLEEP to 8f
            )
            defaults.forEach { (cat, value) ->
                dao.insertTarget(NutritionTarget(category = cat.name, targetValue = value))
            }
        }
    }

    fun todayString(): String = LocalDate.now().format(formatter)
}
