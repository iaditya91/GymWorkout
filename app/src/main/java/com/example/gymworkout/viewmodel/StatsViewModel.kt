package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

data class DailyScoreBreakdown(
    val proteinScore: Float = 0f,
    val caloriesScore: Float = 0f,
    val workoutScore: Float = 0f,
    val sleepScore: Float = 0f,
    val hydrationScore: Float = 0f,
    val dmgs: Float = 0f
)

data class JourneyData(
    val profile: UserProfile? = null,
    val isSetup: Boolean = false,
    val todayScore: DailyScoreBreakdown = DailyScoreBreakdown(),
    val weeklyAverage: Float = 0f,
    val estimatedDays: Int = 90,
    val daysElapsed: Int = 0,
    val weightProgress: Float = 0f // 0-1 progress from starting to target
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WorkoutDatabase.getDatabase(application)
    private val checkInDao = db.dailyCheckInDao()
    private val nutritionDao = db.nutritionDao()
    private val userDao = db.userDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _journeyData = MutableStateFlow(JourneyData())
    val journeyData: StateFlow<JourneyData> = _journeyData

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

    // Journey methods

    fun getProfile(): Flow<UserProfile?> = userDao.getProfile()

    fun saveJourneySetup(targetWeight: Float, startingWeight: Float, fitnessLevel: String) {
        viewModelScope.launch {
            val existing = userDao.getAllProfilesSync().firstOrNull() ?: UserProfile()
            val startWeight = if (startingWeight > 0f) startingWeight else existing.weight
            userDao.upsertProfile(
                existing.copy(
                    targetWeight = targetWeight,
                    startingWeight = startWeight,
                    fitnessLevel = fitnessLevel,
                    journeyStartDate = if (existing.journeyStartDate.isEmpty())
                        LocalDate.now().format(formatter) else existing.journeyStartDate
                )
            )
            loadJourneyData()
        }
    }

    fun loadJourneyData() {
        viewModelScope.launch {
            val profile = userDao.getAllProfilesSync().firstOrNull()
            if (profile == null || profile.targetWeight <= 0f) {
                _journeyData.value = JourneyData(profile = profile, isSetup = false)
                return@launch
            }

            val today = LocalDate.now()
            val todayStr = today.format(formatter)

            // Compute today's DMGS
            val todayScore = computeDailyScore(todayStr)

            // Compute 7-day average
            var weekTotal = 0f
            var weekDays = 0
            for (i in 0 until 7) {
                val date = today.minusDays(i.toLong()).format(formatter)
                val score = computeDailyScore(date)
                if (score.dmgs > 0f) {
                    weekTotal += score.dmgs
                    weekDays++
                }
            }
            val weeklyAvg = if (weekDays > 0) weekTotal / weekDays else 0f

            // Base transformation days based on fitness level
            val baseDays = when (profile.fitnessLevel) {
                "beginner" -> 90
                "intermediate" -> 120
                "advanced" -> 180
                else -> 90
            }

            // Adjusted days
            val adjustedDays = if (weeklyAvg > 0f) (baseDays / weeklyAvg).toInt() else baseDays

            // Days elapsed since journey start
            val daysElapsed = if (profile.journeyStartDate.isNotEmpty()) {
                val startDate = LocalDate.parse(profile.journeyStartDate, formatter)
                ChronoUnit.DAYS.between(startDate, today).toInt().coerceAtLeast(0)
            } else 0

            // Weight progress (0-1)
            val weightDiff = profile.targetWeight - profile.startingWeight
            val currentDiff = profile.weight - profile.startingWeight
            val weightProgress = if (weightDiff != 0f) {
                (currentDiff / weightDiff).coerceIn(0f, 1f)
            } else 0f

            _journeyData.value = JourneyData(
                profile = profile,
                isSetup = true,
                todayScore = todayScore,
                weeklyAverage = weeklyAvg,
                estimatedDays = adjustedDays,
                daysElapsed = daysElapsed,
                weightProgress = weightProgress
            )
        }
    }

    private suspend fun computeDailyScore(date: String): DailyScoreBreakdown {
        // Get nutrition totals
        val actualProtein = nutritionDao.getTotalForDateAndCategorySync(date, "PROTEIN")
        val actualCalories = nutritionDao.getTotalForDateAndCategorySync(date, "CALORIES")
        val actualSleep = nutritionDao.getTotalForDateAndCategorySync(date, "SLEEP")
        val actualWater = nutritionDao.getTotalForDateAndCategorySync(date, "WATER")

        // Get targets
        val proteinTarget = nutritionDao.getTargetSync("PROTEIN")?.targetValue ?: 150f
        val caloriesTarget = nutritionDao.getTargetSync("CALORIES")?.targetValue ?: 2600f
        val sleepTarget = nutritionDao.getTargetSync("SLEEP")?.targetValue ?: 7f
        val waterTarget = nutritionDao.getTargetSync("WATER")?.targetValue ?: 3f

        // Get workout status
        val checkIn = checkInDao.getCheckInSync(date)
        val workoutDone = checkIn?.workoutDone ?: false

        // Compute individual scores
        val proteinScore = if (proteinTarget > 0f) min(actualProtein / proteinTarget, 1f) else 0f
        val caloriesScore = if (caloriesTarget > 0f) min(actualCalories / caloriesTarget, 1f) else 0f
        val workoutScore = if (workoutDone) 1f else 0f
        val sleepScore = if (sleepTarget > 0f) min(actualSleep / sleepTarget, 1f) else 0f
        val hydrationScore = if (waterTarget > 0f) min(actualWater / waterTarget, 1f) else 0f

        // DMGS = weighted sum
        val dmgs = (proteinScore * 0.35f) +
                (caloriesScore * 0.20f) +
                (workoutScore * 0.20f) +
                (sleepScore * 0.15f) +
                (hydrationScore * 0.10f)

        return DailyScoreBreakdown(
            proteinScore = proteinScore,
            caloriesScore = caloriesScore,
            workoutScore = workoutScore,
            sleepScore = sleepScore,
            hydrationScore = hydrationScore,
            dmgs = dmgs
        )
    }
}
