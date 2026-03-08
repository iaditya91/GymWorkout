package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val checkInDao = WorkoutDatabase.getDatabase(application).dailyCheckInDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    fun setMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun getCheckInsForMonth(yearMonth: YearMonth): Flow<List<DailyCheckIn>> {
        val start = yearMonth.atDay(1).format(formatter)
        val end = yearMonth.atEndOfMonth().format(formatter)
        return checkInDao.getCheckInsForRange(start, end)
    }

    fun getCheckIn(date: String): Flow<DailyCheckIn?> = checkInDao.getCheckIn(date)

    fun toggleWorkout(date: String, current: DailyCheckIn?) {
        viewModelScope.launch {
            val existing = current ?: DailyCheckIn(date = date)
            checkInDao.upsert(existing.copy(workoutDone = !existing.workoutDone))
        }
    }

    fun toggleNutrition(date: String, current: DailyCheckIn?) {
        viewModelScope.launch {
            val existing = current ?: DailyCheckIn(date = date)
            checkInDao.upsert(existing.copy(nutritionDone = !existing.nutritionDone))
        }
    }

    fun toggleSleep(date: String, current: DailyCheckIn?) {
        viewModelScope.launch {
            val existing = current ?: DailyCheckIn(date = date)
            checkInDao.upsert(existing.copy(sleepDone = !existing.sleepDone))
        }
    }

    fun getWorkoutDaysCount(yearMonth: YearMonth): Flow<Int> {
        val start = yearMonth.atDay(1).format(formatter)
        val end = yearMonth.atEndOfMonth().format(formatter)
        return checkInDao.getWorkoutDaysCount(start, end)
    }

    fun getNutritionDaysCount(yearMonth: YearMonth): Flow<Int> {
        val start = yearMonth.atDay(1).format(formatter)
        val end = yearMonth.atEndOfMonth().format(formatter)
        return checkInDao.getNutritionDaysCount(start, end)
    }

    fun getSleepDaysCount(yearMonth: YearMonth): Flow<Int> {
        val start = yearMonth.atDay(1).format(formatter)
        val end = yearMonth.atEndOfMonth().format(formatter)
        return checkInDao.getSleepDaysCount(start, end)
    }

    fun todayString(): String = LocalDate.now().format(formatter)
}
