package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WorkoutSetLog): Long

    @Query("SELECT * FROM workout_set_logs WHERE loggedAt BETWEEN :from AND :to ORDER BY loggedAt ASC")
    fun getLogsInRange(from: Long, to: Long): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC LIMIT 1")
    suspend fun getMostRecentForExercise(exerciseId: Int): WorkoutSetLog?

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId AND loggedAt BETWEEN :from AND :to ORDER BY setIndex ASC")
    suspend fun getLogsForExerciseInRange(exerciseId: Int, from: Long, to: Long): List<WorkoutSetLog>

    @Query("DELETE FROM workout_set_logs WHERE id = (SELECT id FROM workout_set_logs WHERE exerciseId = :exerciseId AND loggedAt BETWEEN :from AND :to ORDER BY loggedAt DESC LIMIT 1)")
    suspend fun deleteLatestForExerciseInRange(exerciseId: Int, from: Long, to: Long)

    @Query("DELETE FROM workout_set_logs WHERE exerciseId = :exerciseId")
    suspend fun deleteAllForExercise(exerciseId: Int)

    // Backup / restore
    @Query("SELECT * FROM workout_set_logs")
    suspend fun getAllSync(): List<WorkoutSetLog>

    @Query("DELETE FROM workout_set_logs")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorkoutSetLog>)
}
