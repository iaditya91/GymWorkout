package com.example.gymworkout.data.sync

import android.content.Context
import android.util.Base64
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.ThemePreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
        // Encode progress photos as Base64
        val profiles = userDao.getAllProfilesSync()
        val photos = mutableListOf<BackupPhoto>()
        for (profile in profiles) {
            if (profile.photoUris.isBlank()) continue
            for (path in profile.photoUris.split(",")) {
                if (path.isBlank()) continue
                try {
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        photos.add(BackupPhoto(fileName = file.name, base64Data = base64))
                    }
                } catch (_: Exception) {}
            }
        }

        BackupData(
            exercises = exerciseDao.getAllSync(),
            nutritionEntries = nutritionDao.getAllEntriesSync(),
            nutritionTargets = nutritionDao.getAllTargetsSync(),
            dailyCheckIns = checkInDao.getAllSync(),
            userProfiles = profiles,
            checklistItems = userDao.getAllChecklistItemsSync(),
            nutritionReminders = reminderDao.getAllSync(),
            workoutReminders = userDao.getAllWorkoutRemindersSync(),
            dayHeadings = exerciseDao.getAllDayHeadingsSync(),
            foodLogEntries = nutritionDao.getAllFoodLogSync(),
            themePreference = ThemePreference.isDarkMode.value,
            customQuotes = userDao.getAllCustomQuotesSync(),
            quoteEnabled = QuotePreference.getEnabled(context),
            quoteSource = QuotePreference.getSource(context),
            quoteTime = QuotePreference.getTime(context),
            progressPhotos = photos,
            customFoods = nutritionDao.getAllCustomFoodsSync()
        )
    }

    suspend fun restoreBackup(data: BackupData) = withContext(Dispatchers.IO) {
        // Delete all existing data
        exerciseDao.deleteAll()
        exerciseDao.deleteAllDayHeadings()
        nutritionDao.deleteAllEntries()
        nutritionDao.deleteAllTargets()
        nutritionDao.deleteAllFoodLog()
        nutritionDao.deleteAllCustomFoods()
        checkInDao.deleteAll()
        userDao.deleteAllProfiles()
        userDao.deleteAllChecklistItems()
        userDao.deleteAllWorkoutReminders()
        userDao.deleteAllCustomQuotes()
        reminderDao.deleteAll()

        // Restore progress photos to internal storage
        val photosDir = File(context.filesDir, "progress_photos")
        if (photosDir.exists()) photosDir.deleteRecursively()
        photosDir.mkdirs()

        val restoredPaths = mutableListOf<String>()
        for (photo in data.progressPhotos) {
            try {
                val bytes = Base64.decode(photo.base64Data, Base64.NO_WRAP)
                val file = File(photosDir, photo.fileName)
                file.writeBytes(bytes)
                restoredPaths.add(file.absolutePath)
            } catch (_: Exception) {}
        }

        // Update user profiles with new internal storage paths
        val updatedProfiles = if (restoredPaths.isNotEmpty() && data.userProfiles.isNotEmpty()) {
            data.userProfiles.map { profile ->
                if (profile.photoUris.isNotBlank()) {
                    // Map old file names to new paths
                    val oldNames = profile.photoUris.split(",").filter { it.isNotBlank() }.map {
                        File(it).name
                    }
                    val newPaths = oldNames.mapNotNull { name ->
                        restoredPaths.find { it.endsWith(name) }
                    }
                    profile.copy(photoUris = newPaths.joinToString(","))
                } else profile
            }
        } else data.userProfiles

        // Insert backup data
        if (data.exercises.isNotEmpty()) exerciseDao.insertAll(data.exercises)
        if (data.dayHeadings.isNotEmpty()) exerciseDao.insertAllDayHeadings(data.dayHeadings)
        if (data.nutritionEntries.isNotEmpty()) nutritionDao.insertAllEntries(data.nutritionEntries)
        if (data.nutritionTargets.isNotEmpty()) nutritionDao.insertAllTargets(data.nutritionTargets)
        if (data.dailyCheckIns.isNotEmpty()) checkInDao.insertAll(data.dailyCheckIns)
        if (updatedProfiles.isNotEmpty()) userDao.insertAllProfiles(updatedProfiles)
        if (data.checklistItems.isNotEmpty()) userDao.insertAllChecklistItems(data.checklistItems)
        if (data.nutritionReminders.isNotEmpty()) reminderDao.insertAll(data.nutritionReminders)
        if (data.workoutReminders.isNotEmpty()) userDao.upsertAllWorkoutReminders(data.workoutReminders)
        if (data.foodLogEntries.isNotEmpty()) nutritionDao.insertAllFoodLog(data.foodLogEntries)
        if (data.customQuotes.isNotEmpty()) userDao.insertAllCustomQuotes(data.customQuotes)
        if (data.customFoods.isNotEmpty()) nutritionDao.insertAllCustomFoods(data.customFoods)

        // Restore theme preference
        ThemePreference.setDarkMode(context, data.themePreference)

        // Restore quote preferences
        QuotePreference.setEnabled(context, data.quoteEnabled)
        QuotePreference.setSource(context, data.quoteSource)
        QuotePreference.setTime(context, data.quoteTime)
    }
}
