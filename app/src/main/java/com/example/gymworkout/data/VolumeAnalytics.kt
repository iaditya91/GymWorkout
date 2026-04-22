package com.example.gymworkout.data

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure functions over List<WorkoutSetLog> for volume analytics:
 *   - per-workout volume (a single day of lifts)
 *   - weekly hard-sets per muscle group (the metric used for MEV/MAV)
 *
 * Muscle attribution uses ExerciseRepository.findByName: primary muscles count as 1
 * hard set each, secondary muscles count as 0.5 (Schoenfeld fractional counting).
 */
object VolumeAnalytics {

    const val KG_TO_LB = 2.20462
    const val LB_TO_KG = 0.453592

    // Common hypertrophy heuristics, sets per muscle per week.
    const val MEV_SETS = 10  // Minimum Effective Volume
    const val MAV_SETS = 20  // Maximum Adaptive Volume

    data class PerExerciseVolume(
        val exerciseId: Int,
        val exerciseName: String,
        val sets: Int,
        val totalReps: Int,
        val totalVolumeKg: Double,
        val topWeightKg: Double
    )

    data class WorkoutVolumeSummary(
        val totalVolumeKg: Double,
        val totalSets: Int,
        val totalReps: Int,
        val perExercise: List<PerExerciseVolume>
    )

    data class MuscleGroupVolume(
        val muscle: String,
        val hardSets: Double,          // fractional (secondary = 0.5)
        val volumeKg: Double
    )

    /** One session's summary for an exercise, used to plot progression. */
    data class ExerciseSession(
        val dayStartMs: Long,           // local day start, for X-axis
        val sets: Int,
        val totalReps: Int,
        val totalVolumeKg: Double,
        val topWeightKg: Double,        // heaviest single set weight
        val topWeightReps: Int,         // reps @ topWeight (tiebreak: highest reps)
        val estimatedOneRepMaxKg: Double
    )

    data class ExerciseProgression(
        val sessions: List<ExerciseSession>, // chronological ascending
        val allTimeTopWeightKg: Double,
        val allTimeTopWeightReps: Int,
        val allTimeEstimatedOneRepMaxKg: Double,
        val totalSessions: Int,
        val totalSets: Int,
        val totalVolumeKg: Double
    )

    /** Epley formula: 1RM ≈ w * (1 + reps/30). For 1-rep sets, returns weight. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
        if (reps <= 0 || weightKg <= 0.0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    fun buildProgression(logs: List<WorkoutSetLog>): ExerciseProgression {
        if (logs.isEmpty()) {
            return ExerciseProgression(emptyList(), 0.0, 0, 0.0, 0, 0, 0.0)
        }
        val byDay = logs.groupBy { startOfDay(it.loggedAt) }
        val sessions = byDay.entries
            .sortedBy { it.key }
            .map { (dayStart, dayLogs) ->
                // Top set = heaviest weight; break ties by most reps at that weight.
                val top = dayLogs.maxWithOrNull(
                    compareBy<WorkoutSetLog> { it.weightKg }.thenBy { it.reps }
                )!!
                ExerciseSession(
                    dayStartMs = dayStart,
                    sets = dayLogs.size,
                    totalReps = dayLogs.sumOf { it.reps },
                    totalVolumeKg = dayLogs.sumOf { it.volumeKg },
                    topWeightKg = top.weightKg,
                    topWeightReps = top.reps,
                    estimatedOneRepMaxKg = dayLogs.maxOf {
                        estimatedOneRepMax(it.weightKg, it.reps)
                    }
                )
            }

        val topOverall = logs.maxWithOrNull(
            compareBy<WorkoutSetLog> { it.weightKg }.thenBy { it.reps }
        )!!
        val allTime1RM = logs.maxOf { estimatedOneRepMax(it.weightKg, it.reps) }

        return ExerciseProgression(
            sessions = sessions,
            allTimeTopWeightKg = topOverall.weightKg,
            allTimeTopWeightReps = topOverall.reps,
            allTimeEstimatedOneRepMaxKg = allTime1RM,
            totalSessions = sessions.size,
            totalSets = logs.size,
            totalVolumeKg = logs.sumOf { it.volumeKg }
        )
    }

    fun summarizeWorkout(logs: List<WorkoutSetLog>): WorkoutVolumeSummary {
        val byExercise = logs.groupBy { it.exerciseId }.map { (id, exLogs) ->
            PerExerciseVolume(
                exerciseId = id,
                exerciseName = exLogs.first().exerciseName,
                sets = exLogs.size,
                totalReps = exLogs.sumOf { it.reps },
                totalVolumeKg = exLogs.sumOf { it.volumeKg },
                topWeightKg = exLogs.maxOfOrNull { it.weightKg } ?: 0.0
            )
        }.sortedByDescending { it.totalVolumeKg }

        return WorkoutVolumeSummary(
            totalVolumeKg = logs.sumOf { it.volumeKg },
            totalSets = logs.size,
            totalReps = logs.sumOf { it.reps },
            perExercise = byExercise
        )
    }

    /**
     * Aggregate hard sets and volume per muscle group.
     * [muscleLookup] maps exerciseName -> Pair(primaryMuscles, secondaryMuscles) normalized lowercase.
     */
    fun aggregateByMuscleGroup(
        logs: List<WorkoutSetLog>,
        muscleLookup: (String) -> Pair<List<String>, List<String>>
    ): List<MuscleGroupVolume> {
        val sets = mutableMapOf<String, Double>()
        val volume = mutableMapOf<String, Double>()

        for (log in logs) {
            val (primary, secondary) = muscleLookup(log.exerciseName)
            for (m in primary) {
                val key = m.lowercase().trim()
                if (key.isBlank()) continue
                sets[key] = (sets[key] ?: 0.0) + 1.0
                volume[key] = (volume[key] ?: 0.0) + log.volumeKg
            }
            for (m in secondary) {
                val key = m.lowercase().trim()
                if (key.isBlank()) continue
                sets[key] = (sets[key] ?: 0.0) + 0.5
                volume[key] = (volume[key] ?: 0.0) + log.volumeKg * 0.5
            }
        }

        return sets.keys.map { key ->
            MuscleGroupVolume(
                muscle = key,
                hardSets = sets[key] ?: 0.0,
                volumeKg = volume[key] ?: 0.0
            )
        }.sortedByDescending { it.hardSets }
    }

    /** Start of current local day in epoch millis. */
    fun startOfDay(timeMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Exclusive end of day = start of next day. */
    fun endOfDay(timeMs: Long = System.currentTimeMillis()): Long {
        return startOfDay(timeMs) + 24L * 60 * 60 * 1000
    }

    /**
     * Start of current ISO-style week (Monday 00:00 local time).
     */
    fun startOfWeek(timeMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMs
        cal.firstDayOfWeek = Calendar.MONDAY
        // Shift to Monday of current week
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.MONDAY = 2, SUNDAY = 1. Days since Monday (Mon=0 ... Sun=6):
        val daysSinceMonday = (dow + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfWeek(timeMs: Long = System.currentTimeMillis()): Long {
        return startOfWeek(timeMs) + 7L * 24 * 60 * 60 * 1000
    }

    fun kgToDisplay(kg: Double, unit: String): Double =
        if (unit.equals("lb", ignoreCase = true)) kg * KG_TO_LB else kg

    fun displayToKg(value: Double, unit: String): Double =
        if (unit.equals("lb", ignoreCase = true)) value * LB_TO_KG else value

    fun formatVolume(kg: Double, unit: String): String {
        val v = kgToDisplay(kg, unit)
        return when {
            v >= 10_000 -> "%.1fk %s".format(v / 1000.0, unit)
            v >= 100 -> "%.0f %s".format(v, unit)
            else -> "%.1f %s".format(v, unit)
        }
    }

    fun formatWeight(kg: Double, unit: String): String {
        val v = kgToDisplay(kg, unit)
        // strip trailing .0 for readability
        val s = "%.1f".format(v)
        val clean = if (s.endsWith(".0")) s.dropLast(2) else s
        return "$clean $unit"
    }

    // Canonical display order for muscle groups (so charts don't reorder chaotically).
    val CANONICAL_MUSCLE_ORDER: List<String> = listOf(
        "chest", "back", "lats", "middle back", "lower back",
        "shoulders", "biceps", "triceps", "forearms",
        "abdominals", "core",
        "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors",
        "traps", "neck"
    )

    fun canonicalIndex(muscle: String): Int {
        val i = CANONICAL_MUSCLE_ORDER.indexOfFirst { it == muscle.lowercase().trim() }
        return if (i < 0) Int.MAX_VALUE else i
    }
}
