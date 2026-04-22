package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gymworkout.data.MotivationalQuote
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileSync(): UserProfile?

    // Checklist
    @Query("SELECT * FROM checklist_items WHERE type = :type ORDER BY id ASC")
    fun getChecklistItems(type: String): Flow<List<ChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)

    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :id")
    suspend fun toggleChecklistItem(id: Int, checked: Boolean)

    @Query("UPDATE checklist_items SET isChecked = 0")
    suspend fun resetAllChecklistItems()

    // Bulk operations for backup/restore
    @Query("SELECT * FROM user_profile")
    suspend fun getAllProfilesSync(): List<UserProfile>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItemsSync(): List<ChecklistItem>

    @Query("DELETE FROM user_profile")
    suspend fun deleteAllProfiles()

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProfiles(items: List<UserProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChecklistItems(items: List<ChecklistItem>)

    // Workout reminders
    @Query("SELECT * FROM workout_reminders ORDER BY dayOfWeek ASC")
    fun getAllWorkoutReminders(): Flow<List<WorkoutReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutReminder(reminder: WorkoutReminder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllWorkoutReminders(reminders: List<WorkoutReminder>)

    @Query("SELECT * FROM workout_reminders WHERE enabled = 1")
    suspend fun getEnabledWorkoutReminders(): List<WorkoutReminder>

    // Bulk operations for backup/restore
    @Query("SELECT * FROM workout_reminders")
    suspend fun getAllWorkoutRemindersSync(): List<WorkoutReminder>

    @Query("DELETE FROM workout_reminders")
    suspend fun deleteAllWorkoutReminders()

    // Motivational quotes
    @Query("SELECT * FROM motivational_quotes ORDER BY id ASC")
    fun getAllCustomQuotes(): Flow<List<MotivationalQuote>>

    @Query("SELECT * FROM motivational_quotes ORDER BY id ASC")
    suspend fun getAllCustomQuotesSync(): List<MotivationalQuote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomQuote(quote: MotivationalQuote)

    @Delete
    suspend fun deleteCustomQuote(quote: MotivationalQuote)

    @Query("DELETE FROM motivational_quotes")
    suspend fun deleteAllCustomQuotes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomQuotes(quotes: List<MotivationalQuote>)

    // Weight entries
    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    fun getAllWeightEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun getLatestWeightEntry(): Flow<WeightEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightEntry(entry: WeightEntry)

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteWeightEntryByDate(date: String)

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    suspend fun getAllWeightEntriesSync(): List<WeightEntry>

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAllWeightEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeightEntries(entries: List<WeightEntry>)
}
