package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WeightEntry
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
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
    val overallAverage: Float = 0f, // average across all elapsed days
    val idealDays: Int = 90,
    val estimatedDays: Int = 90,
    val daysElapsed: Int = 0,
    val requiredShape: String = "",
    val shapeProgress: Float = 0f // 0-1 progress: daysElapsed / estimatedDays
)

// Nutrition-related custom objective names (must match NutritionScreen)
private val nutritionRelatedNames = setOf(
    "fat", "fiber",
    "vitamin a", "vitamin b1", "vitamin b2", "vitamin b3",
    "vitamin b6", "vitamin b12", "vitamin c", "vitamin d",
    "vitamin e", "vitamin k",
    "folate", "iron", "calcium"
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WorkoutDatabase.getDatabase(application)
    private val checkInDao = db.dailyCheckInDao()
    private val nutritionDao = db.nutritionDao()
    private val exerciseDao = db.exerciseDao()
    private val userDao = db.userDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _journeyData = MutableStateFlow(JourneyData())
    val journeyData: StateFlow<JourneyData> = _journeyData

    init {
        // Check if weekly workout reset is needed on app launch
        checkWeeklyWorkoutReset()
    }

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

    // ===== AUTOMATIC CHECK-IN =====

    fun refreshTodayCheckIn() {
        viewModelScope.launch {
            val todayStr = todayString()
            val today = LocalDate.now()
            // dayOfWeek: Monday=1 ... Sunday=7, but Exercise uses 0=Mon ... 6=Sun
            val dayIndex = today.dayOfWeek.value - 1

            // 1. Workout: all exercises for today's day are completed
            val totalExercises = exerciseDao.getExerciseCountForDaySync(dayIndex)
            val completedExercises = exerciseDao.getCompletedCountForDaySync(dayIndex)
            val workoutDone = totalExercises > 0 && completedExercises >= totalExercises

            // 2. Nutrition: all nutrition targets met (CALORIES, PROTEIN, CARBS + nutrition-related custom)
            val allTargets = nutritionDao.getAllTargetsSync()
            val nutritionTargets = allTargets.filter { target ->
                // Built-in nutrition categories
                target.category in setOf(
                    NutritionCategory.CALORIES.name,
                    NutritionCategory.PROTEIN.name,
                    NutritionCategory.CARBS.name
                ) ||
                // Custom nutrition-related objectives
                (target.isCustom && target.label.lowercase() in nutritionRelatedNames)
            }
            val nutritionDone = nutritionTargets.isNotEmpty() && nutritionTargets.all { target ->
                val actual = nutritionDao.getTotalForDateAndCategorySync(todayStr, target.category)
                target.targetValue > 0f && actual >= target.targetValue
            }

            // 3. Sleep: sleep target met
            val sleepTarget = nutritionDao.getTargetSync(NutritionCategory.SLEEP.name)
            val actualSleep = nutritionDao.getTotalForDateAndCategorySync(todayStr, NutritionCategory.SLEEP.name)
            val sleepDone = sleepTarget != null && sleepTarget.targetValue > 0f && actualSleep >= sleepTarget.targetValue

            // 4. Habits: all habit targets met (WATER, SLEEP + custom non-nutrition objectives)
            val habitTargets = allTargets.filter { target ->
                target.category in setOf(
                    NutritionCategory.WATER.name,
                    NutritionCategory.SLEEP.name
                ) ||
                (target.isCustom && target.label.lowercase() !in nutritionRelatedNames)
            }
            val habitsDone = habitTargets.isNotEmpty() && habitTargets.all { target ->
                val actual = nutritionDao.getTotalForDateAndCategorySync(todayStr, target.category)
                target.targetValue > 0f && actual >= target.targetValue
            }

            checkInDao.upsert(
                DailyCheckIn(
                    date = todayStr,
                    workoutDone = workoutDone,
                    nutritionDone = nutritionDone,
                    sleepDone = sleepDone,
                    habitsDone = habitsDone
                )
            )
        }
    }

    // ===== WEEKLY WORKOUT RESET =====

    private fun checkWeeklyWorkoutReset() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("workout_prefs", 0)
            val lastResetStr = prefs.getString("last_weekly_reset", null)
            val now = LocalDateTime.now()

            // Find the most recent Sunday 23:59
            val lastSunday = if (now.dayOfWeek == DayOfWeek.SUNDAY && now.toLocalTime() >= LocalTime.of(23, 59)) {
                now.toLocalDate()
            } else {
                now.toLocalDate().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
            }
            val lastSundayStr = lastSunday.format(formatter)

            val needsReset = lastResetStr == null || lastResetStr < lastSundayStr

            if (needsReset) {
                exerciseDao.resetAllDays()
                prefs.edit().putString("last_weekly_reset", lastSundayStr).apply()
            }
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

    fun getHabitsDaysCount(yearMonth: YearMonth): Flow<Int> {
        val start = yearMonth.atDay(1).format(formatter)
        val end = yearMonth.atEndOfMonth().format(formatter)
        return checkInDao.getHabitsDaysCount(start, end)
    }

    fun todayString(): String = LocalDate.now().format(formatter)

    // Journey methods

    fun getProfile(): Flow<UserProfile?> = userDao.getProfile()

    // ===== WEIGHT TRACKING =====

    fun getWeightEntries(): Flow<List<WeightEntry>> = userDao.getAllWeightEntries()

    fun logWeight(date: String, weight: Float, unit: String) {
        viewModelScope.launch {
            userDao.upsertWeightEntry(WeightEntry(date = date, weight = weight, unit = unit))
            // Also sync profile's current weight with the most recent entry
            val latest = userDao.getAllWeightEntriesSync().maxByOrNull { it.date }
            if (latest != null) {
                val existing = userDao.getAllProfilesSync().firstOrNull() ?: UserProfile()
                userDao.upsertProfile(
                    existing.copy(weight = latest.weight, weightUnit = latest.unit)
                )
            }
        }
    }

    fun deleteWeightEntry(date: String) {
        viewModelScope.launch {
            userDao.deleteWeightEntryByDate(date)
            val latest = userDao.getAllWeightEntriesSync().maxByOrNull { it.date }
            val existing = userDao.getAllProfilesSync().firstOrNull() ?: UserProfile()
            userDao.upsertProfile(
                existing.copy(weight = latest?.weight ?: 0f, weightUnit = latest?.unit ?: existing.weightUnit)
            )
        }
    }

    fun saveJourneySetup(requiredShape: String, idealDays: Int) {
        viewModelScope.launch {
            val existing = userDao.getAllProfilesSync().firstOrNull() ?: UserProfile()
            userDao.upsertProfile(
                existing.copy(
                    requiredShape = requiredShape,
                    idealDays = idealDays,
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
            if (profile == null || profile.requiredShape.isEmpty()) {
                _journeyData.value = JourneyData(profile = profile, isSetup = false)
                return@launch
            }

            val today = LocalDate.now()
            val todayStr = today.format(formatter)

            // Compute today's DMGS
            val todayScore = computeDailyScore(todayStr)

            // Compute 7-day average (for display)
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

            // Compute overall average across ALL elapsed days since journey start
            val daysElapsedForAvg = if (profile.journeyStartDate.isNotEmpty()) {
                val startDate = LocalDate.parse(profile.journeyStartDate, formatter)
                ChronoUnit.DAYS.between(startDate, today).toInt().coerceAtLeast(0)
            } else 0

            var overallTotal = 0f
            var overallDays = 0
            for (i in 0 until daysElapsedForAvg) {
                val date = today.minusDays(i.toLong()).format(formatter)
                val score = computeDailyScore(date)
                if (score.dmgs > 0f) {
                    overallTotal += score.dmgs
                    overallDays++
                }
            }
            val overallAvg = if (overallDays > 0) overallTotal / overallDays else 0f

            val idealDays = profile.idealDays

            // Estimated Days = Ideal Days / Overall Average Score (uses all past data)
            val avgForEstimate = if (overallAvg > 0f) overallAvg else weeklyAvg
            val estimatedDays = if (avgForEstimate > 0f) (idealDays / avgForEstimate).toInt() else idealDays

            // Days elapsed since journey start
            val daysElapsed = if (profile.journeyStartDate.isNotEmpty()) {
                val startDate = LocalDate.parse(profile.journeyStartDate, formatter)
                ChronoUnit.DAYS.between(startDate, today).toInt().coerceAtLeast(0)
            } else 0

            // Shape progress: how far along vs estimated days
            val shapeProgress = if (estimatedDays > 0) {
                (daysElapsed.toFloat() / estimatedDays).coerceIn(0f, 1f)
            } else 0f

            _journeyData.value = JourneyData(
                profile = profile,
                isSetup = true,
                todayScore = todayScore,
                weeklyAverage = weeklyAvg,
                overallAverage = if (overallAvg > 0f) overallAvg else weeklyAvg,
                idealDays = idealDays,
                estimatedDays = estimatedDays,
                daysElapsed = daysElapsed,
                requiredShape = profile.requiredShape,
                shapeProgress = shapeProgress
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

        // Get workout status from check-in
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
