package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckInDao {

    @Query("SELECT * FROM daily_checkins WHERE date = :date")
    fun getCheckIn(date: String): Flow<DailyCheckIn?>

    @Query("SELECT * FROM daily_checkins WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getCheckInsForRange(startDate: String, endDate: String): Flow<List<DailyCheckIn>>

    @Query("SELECT * FROM daily_checkins ORDER BY date DESC")
    fun getAllCheckIns(): Flow<List<DailyCheckIn>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: DailyCheckIn)

    @Query("SELECT COUNT(*) FROM daily_checkins WHERE workoutDone = 1 AND date BETWEEN :startDate AND :endDate")
    fun getWorkoutDaysCount(startDate: String, endDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_checkins WHERE nutritionDone = 1 AND date BETWEEN :startDate AND :endDate")
    fun getNutritionDaysCount(startDate: String, endDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_checkins WHERE sleepDone = 1 AND date BETWEEN :startDate AND :endDate")
    fun getSleepDaysCount(startDate: String, endDate: String): Flow<Int>

    @Query("SELECT * FROM daily_checkins WHERE date = :date")
    suspend fun getCheckInSync(date: String): DailyCheckIn?

    // Bulk operations for backup/restore
    @Query("SELECT * FROM daily_checkins")
    suspend fun getAllSync(): List<DailyCheckIn>

    @Query("DELETE FROM daily_checkins")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyCheckIn>)
}
