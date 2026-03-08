package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM nutrition_reminders WHERE category = :category ORDER BY id ASC")
    fun getRemindersForCategory(category: String): Flow<List<NutritionReminder>>

    @Query("SELECT * FROM nutrition_reminders ORDER BY id ASC")
    fun getAllReminders(): Flow<List<NutritionReminder>>

    @Query("SELECT * FROM nutrition_reminders WHERE enabled = 1")
    suspend fun getAllEnabledReminders(): List<NutritionReminder>

    @Query("SELECT * FROM nutrition_reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): NutritionReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: NutritionReminder): Long

    @Update
    suspend fun updateReminder(reminder: NutritionReminder)

    @Delete
    suspend fun deleteReminder(reminder: NutritionReminder)

    @Query("DELETE FROM nutrition_reminders WHERE category = :category")
    suspend fun deleteRemindersForCategory(category: String)
}
