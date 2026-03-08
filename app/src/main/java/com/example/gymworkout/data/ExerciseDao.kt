package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE dayOfWeek = :day ORDER BY orderIndex ASC")
    fun getExercisesForDay(day: Int): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises WHERE dayOfWeek = :day")
    fun getExerciseCountForDay(day: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM exercises WHERE dayOfWeek = :day AND isCompleted = 1")
    fun getCompletedCountForDay(day: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise)

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("UPDATE exercises SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompleted(id: Int, completed: Boolean)

    @Query("UPDATE exercises SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Int, notes: String)

    @Query("UPDATE exercises SET isCompleted = 0 WHERE dayOfWeek = :day")
    suspend fun resetDay(day: Int)

    @Query("UPDATE exercises SET isCompleted = 0")
    suspend fun resetAllDays()

    @Query("SELECT * FROM exercises WHERE supersetGroupId = :groupId")
    suspend fun getExercisesByGroupId(groupId: String): List<Exercise>

    @Query("UPDATE exercises SET supersetGroupId = '' WHERE id = :id")
    suspend fun clearSupersetGroupId(id: Int)

    // Day headings
    @Query("SELECT * FROM day_headings WHERE dayOfWeek = :day")
    fun getDayHeading(day: Int): Flow<DayHeading?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayHeading(heading: DayHeading)

    // Bulk operations for backup/restore
    @Query("SELECT * FROM exercises")
    suspend fun getAllSync(): List<Exercise>

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Exercise>)

    @Query("SELECT * FROM day_headings")
    suspend fun getAllDayHeadingsSync(): List<DayHeading>

    @Query("DELETE FROM day_headings")
    suspend fun deleteAllDayHeadings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDayHeadings(items: List<DayHeading>)
}
