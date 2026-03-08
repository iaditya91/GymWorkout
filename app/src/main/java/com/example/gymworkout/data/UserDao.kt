package com.example.gymworkout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

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
}
