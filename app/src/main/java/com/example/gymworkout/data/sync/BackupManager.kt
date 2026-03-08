package com.example.gymworkout.data.sync

import android.content.Context
import com.example.gymworkout.data.ThemePreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val database: WorkoutDatabase,
    private val context: Context
) {
    private val exerciseDao = database.exerciseDao()
    private val nutritionDao = database.nutritionDao()
    private val checkInDao = database.dailyCheckInDao()
    private val userDao = database.userDao()
    private val reminderDao = database.reminderDao()

    suspend fun createBackup(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            exercises = exerciseDao.getAllSync(),
            nutritionEntries = nutritionDao.getAllEntriesSync(),
            nutritionTargets = nutritionDao.getAllTargetsSync(),
            dailyCheckIns = checkInDao.getAllSync(),
            userProfiles = userDao.getAllProfilesSync(),
            checklistItems = userDao.getAllChecklistItemsSync(),
            nutritionReminders = reminderDao.getAllSync(),
            workoutReminders = userDao.getAllWorkoutRemindersSync(),
            dayHeadings = exerciseDao.getAllDayHeadingsSync(),
            foodLogEntries = nutritionDao.getAllFoodLogSync(),
            themePreference = ThemePreference.isDarkMode.value,
            customQuotes = userDao.getAllCustomQuotesSync()
        )
    }

    suspend fun restoreBackup(data: BackupData) = withContext(Dispatchers.IO) {
        // Delete all existing data
        exerciseDao.deleteAll()
        exerciseDao.deleteAllDayHeadings()
        nutritionDao.deleteAllEntries()
        nutritionDao.deleteAllTargets()
        nutritionDao.deleteAllFoodLog()
        checkInDao.deleteAll()
        userDao.deleteAllProfiles()
        userDao.deleteAllChecklistItems()
        userDao.deleteAllWorkoutReminders()
        userDao.deleteAllCustomQuotes()
        reminderDao.deleteAll()

        // Insert backup data
        if (data.exercises.isNotEmpty()) exerciseDao.insertAll(data.exercises)
        if (data.dayHeadings.isNotEmpty()) exerciseDao.insertAllDayHeadings(data.dayHeadings)
        if (data.nutritionEntries.isNotEmpty()) nutritionDao.insertAllEntries(data.nutritionEntries)
        if (data.nutritionTargets.isNotEmpty()) nutritionDao.insertAllTargets(data.nutritionTargets)
        if (data.dailyCheckIns.isNotEmpty()) checkInDao.insertAll(data.dailyCheckIns)
        if (data.userProfiles.isNotEmpty()) userDao.insertAllProfiles(data.userProfiles)
        if (data.checklistItems.isNotEmpty()) userDao.insertAllChecklistItems(data.checklistItems)
        if (data.nutritionReminders.isNotEmpty()) reminderDao.insertAll(data.nutritionReminders)
        if (data.workoutReminders.isNotEmpty()) userDao.upsertAllWorkoutReminders(data.workoutReminders)
        if (data.foodLogEntries.isNotEmpty()) nutritionDao.insertAllFoodLog(data.foodLogEntries)
        if (data.customQuotes.isNotEmpty()) userDao.insertAllCustomQuotes(data.customQuotes)

        // Restore theme preference
        ThemePreference.setDarkMode(context, data.themePreference)
    }
}
