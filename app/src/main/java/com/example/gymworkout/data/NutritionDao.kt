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

    @Query("SELECT * FROM nutrition_targets WHERE category = :category")
    suspend fun getTargetSync(category: String): NutritionTarget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: NutritionTarget)

    @Query("DELETE FROM nutrition_targets WHERE category = :category")
    suspend fun deleteTarget(category: String)

    @Query("DELETE FROM nutrition_entries WHERE category = :category")
    suspend fun deleteEntriesForCategory(category: String)

    @Query("UPDATE nutrition_targets SET notes = :notes WHERE category = :category")
    suspend fun updateTargetNotes(category: String, notes: String)

    // For stats - check if date has any entries
    @Query("SELECT DISTINCT date FROM nutrition_entries ORDER BY date DESC")
    fun getAllEntryDates(): Flow<List<String>>

    @Query("SELECT COALESCE(SUM(value), 0) FROM nutrition_entries WHERE date = :date AND category = 'SLEEP'")
    fun getSleepForDate(date: String): Flow<Float>

    @Query("SELECT COALESCE(SUM(value), 0) FROM nutrition_entries WHERE date = :date AND category = :category")
    suspend fun getTotalForDateAndCategorySync(date: String, category: String): Float

    // Bulk operations for backup/restore
    @Query("SELECT * FROM nutrition_entries")
    suspend fun getAllEntriesSync(): List<NutritionEntry>

    @Query("SELECT * FROM nutrition_targets")
    suspend fun getAllTargetsSync(): List<NutritionTarget>

    @Query("DELETE FROM nutrition_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM nutrition_targets")
    suspend fun deleteAllTargets()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(items: List<NutritionEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTargets(items: List<NutritionTarget>)

    // Food log
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY id DESC")
    fun getFoodLogForDate(date: String): Flow<List<FoodLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(entry: FoodLogEntry)

    @Delete
    suspend fun deleteFoodLog(entry: FoodLogEntry)

    @Query("SELECT * FROM food_log")
    suspend fun getAllFoodLogSync(): List<FoodLogEntry>

    @Query("DELETE FROM food_log")
    suspend fun deleteAllFoodLog()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFoodLog(items: List<FoodLogEntry>)

    // Custom foods
    @Query("SELECT * FROM custom_foods ORDER BY name ASC")
    fun getAllCustomFoods(): Flow<List<CustomFoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(item: CustomFoodItem)

    @Delete
    suspend fun deleteCustomFood(item: CustomFoodItem)

    @Query("SELECT * FROM custom_foods")
    suspend fun getAllCustomFoodsSync(): List<CustomFoodItem>

    @Query("DELETE FROM custom_foods")
    suspend fun deleteAllCustomFoods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomFoods(items: List<CustomFoodItem>)
}
