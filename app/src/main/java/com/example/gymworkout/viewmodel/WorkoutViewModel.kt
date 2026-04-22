package com.example.gymworkout.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.DayHeading
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.data.VolumeAnalytics
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.data.WorkoutSetLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class RestTimerState(
    val exerciseName: String,
    val totalSeconds: Int,
    val endElapsedRealtime: Long,       // SystemClock.elapsedRealtime() when timer ends
    val pausedRemainingMs: Long = 0L,   // remaining ms when paused
    val isRunning: Boolean = true,
    val isInline: Boolean = false,
    val dayIndex: Int = -1,
    val alertFired: Boolean = false     // true once the finish alert has been triggered
) {
    fun remainingSeconds(): Int {
        return if (isRunning) {
            ((endElapsedRealtime - SystemClock.elapsedRealtime()) / 1000).toInt().coerceAtLeast(0)
        } else {
            (pausedRemainingMs / 1000).toInt().coerceAtLeast(0)
        }
    }

    val isFinished: Boolean
        get() = if (isRunning) {
            SystemClock.elapsedRealtime() >= endElapsedRealtime
        } else {
            pausedRemainingMs <= 0
        }
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).exerciseDao()
    private val setLogDao = WorkoutDatabase.getDatabase(application).workoutSetLogDao()
    private val userDao = WorkoutDatabase.getDatabase(application).userDao()

    /** "kg" or "lb" — reflects the user's profile preference. */
    val weightUnit: Flow<String> = userDao.getProfile().map { it?.weightUnit ?: "kg" }

    suspend fun getExercisesByGroupId(groupId: String): List<Exercise> =
        dao.getExercisesByGroupId(groupId)

    // Rest timer state — survives navigation
    private val _restTimerState = MutableStateFlow<RestTimerState?>(null)
    val restTimerState: StateFlow<RestTimerState?> = _restTimerState.asStateFlow()

    fun startRestTimer(exerciseName: String, totalSeconds: Int, dayIndex: Int, inline: Boolean = false) {
        com.example.gymworkout.notification.TimerAlertService.clearAcknowledged()
        _restTimerState.value = RestTimerState(
            exerciseName = exerciseName,
            totalSeconds = totalSeconds,
            endElapsedRealtime = SystemClock.elapsedRealtime() + totalSeconds * 1000L,
            isRunning = true,
            isInline = inline,
            dayIndex = dayIndex
        )
    }

    fun pauseRestTimer() {
        _restTimerState.value?.let { state ->
            if (state.isRunning) {
                val remaining = (state.endElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                _restTimerState.value = state.copy(isRunning = false, pausedRemainingMs = remaining)
            }
        }
    }

    fun resumeRestTimer() {
        _restTimerState.value?.let { state ->
            if (!state.isRunning && state.pausedRemainingMs > 0) {
                _restTimerState.value = state.copy(
                    isRunning = true,
                    endElapsedRealtime = SystemClock.elapsedRealtime() + state.pausedRemainingMs,
                    pausedRemainingMs = 0
                )
            }
        }
    }

    fun resetRestTimer() {
        _restTimerState.value?.let { state ->
            _restTimerState.value = state.copy(
                isRunning = true,
                endElapsedRealtime = SystemClock.elapsedRealtime() + state.totalSeconds * 1000L,
                pausedRemainingMs = 0
            )
        }
    }

    fun setRestTimerInline(inline: Boolean) {
        _restTimerState.value?.let { state ->
            _restTimerState.value = state.copy(isInline = inline)
        }
    }

    fun markRestTimerAlertFired() {
        _restTimerState.value?.let { state ->
            _restTimerState.value = state.copy(alertFired = true)
        }
    }

    fun dismissRestTimer() {
        _restTimerState.value = null
    }

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
            // Shift exercises at and after the insertion point to make room
            dao.shiftOrderIndicesUp(existingExercise.dayOfWeek, existingExercise.orderIndex + 1)
            dao.update(existingExercise.copy(supersetGroupId = groupId))
            dao.insert(newExercise.copy(supersetGroupId = groupId, orderIndex = existingExercise.orderIndex + 1))
        }
    }

    fun toggleCompleted(id: Int, completed: Boolean) {
        viewModelScope.launch {
            dao.updateCompleted(id, completed)
        }
    }

    fun incrementSet(exercise: Exercise) {
        viewModelScope.launch {
            if (exercise.supersetGroupId.isNotBlank()) {
                // Increment all exercises in the superset group together
                val group = dao.getExercisesByGroupId(exercise.supersetGroupId)
                for (ex in group) {
                    val newCompleted = (ex.completedSets + 1).coerceAtMost(ex.sets)
                    val isDone = newCompleted >= ex.sets
                    dao.updateCompletedSets(ex.id, newCompleted, isDone)
                }
            } else {
                val newCompleted = (exercise.completedSets + 1).coerceAtMost(exercise.sets)
                val isDone = newCompleted >= exercise.sets
                dao.updateCompletedSets(exercise.id, newCompleted, isDone)
            }
        }
    }

    fun decrementSet(exercise: Exercise) {
        viewModelScope.launch {
            val dayStart = VolumeAnalytics.startOfDay()
            val dayEnd = VolumeAnalytics.endOfDay()
            if (exercise.supersetGroupId.isNotBlank()) {
                // Decrement all exercises in the superset group together
                val group = dao.getExercisesByGroupId(exercise.supersetGroupId)
                for (ex in group) {
                    val newCompleted = (ex.completedSets - 1).coerceAtLeast(0)
                    dao.updateCompletedSets(ex.id, newCompleted, false)
                    setLogDao.deleteLatestForExerciseInRange(ex.id, dayStart, dayEnd)
                }
            } else {
                val newCompleted = (exercise.completedSets - 1).coerceAtLeast(0)
                dao.updateCompletedSets(exercise.id, newCompleted, false)
                setLogDao.deleteLatestForExerciseInRange(exercise.id, dayStart, dayEnd)
            }
        }
    }

    /**
     * Log a completed set with actual reps + weight, then bump completedSets.
     * For supersets the caller provides one (reps, weightKg) pair per exercise in the group.
     */
    fun logAndIncrementSet(
        exercise: Exercise,
        reps: Int,
        weightKg: Double,
        groupEntries: List<Triple<Exercise, Int, Double>>? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (exercise.supersetGroupId.isNotBlank() && groupEntries != null) {
                for ((ex, r, w) in groupEntries) {
                    val setIndex = ex.completedSets + 1
                    val newCompleted = setIndex.coerceAtMost(ex.sets)
                    val isDone = newCompleted >= ex.sets
                    dao.updateCompletedSets(ex.id, newCompleted, isDone)
                    setLogDao.insert(
                        WorkoutSetLog(
                            exerciseId = ex.id,
                            exerciseName = ex.name,
                            dayOfWeek = ex.dayOfWeek,
                            setIndex = setIndex,
                            reps = r,
                            weightKg = w,
                            loggedAt = now
                        )
                    )
                }
            } else {
                val setIndex = exercise.completedSets + 1
                val newCompleted = setIndex.coerceAtMost(exercise.sets)
                val isDone = newCompleted >= exercise.sets
                dao.updateCompletedSets(exercise.id, newCompleted, isDone)
                setLogDao.insert(
                    WorkoutSetLog(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        dayOfWeek = exercise.dayOfWeek,
                        setIndex = setIndex,
                        reps = reps,
                        weightKg = weightKg,
                        loggedAt = now
                    )
                )
            }
        }
    }

    /** Most recent log row for this exercise, used to prefill the log dialog. */
    suspend fun getLastLogForExercise(exerciseId: Int): WorkoutSetLog? =
        setLogDao.getMostRecentForExercise(exerciseId)

    /** Flow of all logs in the current local week (Mon 00:00 → next Mon 00:00). */
    fun getLogsForCurrentWeek(): Flow<List<WorkoutSetLog>> =
        setLogDao.getLogsInRange(VolumeAnalytics.startOfWeek(), VolumeAnalytics.endOfWeek())

    /** Flow of all logs for today (local). */
    fun getLogsForToday(): Flow<List<WorkoutSetLog>> =
        setLogDao.getLogsInRange(VolumeAnalytics.startOfDay(), VolumeAnalytics.endOfDay())

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

    fun mergeIntoSuperset(draggedExercises: List<Exercise>, targetExercises: List<Exercise>) {
        viewModelScope.launch {
            // Use existing groupId if either side has one, otherwise create new
            val groupId = targetExercises.firstOrNull { it.supersetGroupId.isNotBlank() }?.supersetGroupId
                ?: draggedExercises.firstOrNull { it.supersetGroupId.isNotBlank() }?.supersetGroupId
                ?: java.util.UUID.randomUUID().toString()

            // Assign same groupId to all exercises and place dragged after target
            val allExercises = targetExercises + draggedExercises
            val baseOrder = targetExercises.first().orderIndex
            allExercises.forEachIndexed { index, exercise ->
                dao.updateSupersetGroupId(exercise.id, groupId)
                dao.updateOrderIndex(exercise.id, baseOrder + index)
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

    fun rolloverPlan(days: Int, forward: Boolean) {
        viewModelScope.launch {
            val shift = if (forward) days else -days
            // Shift exercises
            dao.shiftAllExercisesDayOfWeek(shift)
            // Shift day headings (PK is dayOfWeek, so fetch -> delete -> re-insert)
            val headings = dao.getAllDayHeadingsSync()
            dao.deleteAllDayHeadings()
            val shifted = headings.map { it.copy(dayOfWeek = ((it.dayOfWeek + shift) % 7 + 7) % 7) }
            dao.insertAllDayHeadings(shifted)
        }
    }
}
