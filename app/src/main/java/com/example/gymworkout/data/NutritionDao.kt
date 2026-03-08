package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    // Entries
    @Query("SELECT * FROM nutrition_entries WHERE date = :date ORDER BY id DESC")
    fun getEntriesForDate(date: String): Flow<List<NutritionEntry>>

    @Query("SELECT COALESCE(SUM(value), 0) FROM nutrition_entries WHERE date = :date AND category = :category")
    fun getTotalForDateAndCategory(date: String, category: String): Flow<Float>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NutritionEntry)

    @Delete
    suspend fun deleteEntry(entry: NutritionEntry)

    @Query("DELETE FROM nutrition_entries WHERE date = :date AND category = :category")
    suspend fun clearEntriesForDateAndCategory(date: String, category: String)

    // Targets
    @Query("SELECT * FROM nutrition_targets")
    fun getAllTargets(): Flow<List<NutritionTarget>>

    @Query("SELECT * FROM nutrition_targets WHERE category = :category")
    fun getTarget(category: String): Flow<NutritionTarget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: NutritionTarget)

    // For stats - check if date has any entries
    @Query("SELECT DISTINCT date FROM nutrition_entries ORDER BY date DESC")
    fun getAllEntryDates(): Flow<List<String>>

    @Query("SELECT COALESCE(SUM(value), 0) FROM nutrition_entries WHERE date = :date AND category = 'SLEEP'")
    fun getSleepForDate(date: String): Flow<Float>
}
