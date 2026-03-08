package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).nutritionDao()
    private val reminderDao = WorkoutDatabase.getDatabase(application).reminderDao()
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
            dao.insertTarget(
                NutritionTarget(
                    category = category.name,
                    targetValue = value,
                    label = category.label,
                    unit = category.unit,
                    isCustom = false
                )
            )
        }
    }

    fun setTargetByKey(category: String, value: Float) {
        viewModelScope.launch {
            val existing = dao.getTargetSync(category)
            if (existing != null) {
                dao.insertTarget(existing.copy(targetValue = value))
            }
        }
    }

    fun addCustomObjective(name: String, unit: String, target: Float) {
        viewModelScope.launch {
            val key = "CUSTOM_${name.uppercase().replace(" ", "_")}_${System.currentTimeMillis()}"
            dao.insertTarget(
                NutritionTarget(
                    category = key,
                    targetValue = target,
                    label = name,
                    unit = unit,
                    isCustom = true
                )
            )
        }
    }

    fun deleteObjective(category: String) {
        viewModelScope.launch {
            dao.deleteTarget(category)
            dao.deleteEntriesForCategory(category)
        }
    }

    fun addEntryByKey(date: String, category: String, value: Float) {
        viewModelScope.launch {
            dao.insertEntry(NutritionEntry(date = date, category = category, value = value))
        }
    }

    fun initDefaultTargets() {
        viewModelScope.launch {
            val defaults = mapOf(
                NutritionCategory.WATER to 3f,
                NutritionCategory.CARBS to 2000f,
                NutritionCategory.PROTEIN to 120f,
                NutritionCategory.VITAMINS to 3f,
                NutritionCategory.SLEEP to 8f
            )
            defaults.forEach { (cat, value) ->
                dao.insertTarget(
                    NutritionTarget(
                        category = cat.name,
                        targetValue = value,
                        label = cat.label,
                        unit = cat.unit,
                        isCustom = false
                    )
                )
            }
        }
    }

    fun todayString(): String = LocalDate.now().format(formatter)

    // --- Reminder methods ---

    fun getRemindersForCategory(category: String): Flow<List<NutritionReminder>> =
        reminderDao.getRemindersForCategory(category)

    fun saveReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            val id = reminderDao.insertReminder(reminder)
            val saved = reminder.copy(id = id.toInt())
            ReminderScheduler.scheduleReminder(getApplication(), saved)
        }
    }

    fun updateReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder)
            if (reminder.enabled) {
                ReminderScheduler.scheduleReminder(getApplication(), reminder)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), reminder)
            }
        }
    }

    fun deleteReminder(reminder: NutritionReminder) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication(), reminder)
            reminderDao.deleteReminder(reminder)
        }
    }

    fun toggleReminderEnabled(reminder: NutritionReminder) {
        val updated = reminder.copy(enabled = !reminder.enabled)
        updateReminder(updated)
    }
}
