package com.example.gymworkout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exercise::class,
        NutritionEntry::class,
        NutritionTarget::class,
        DailyCheckIn::class,
        UserProfile::class,
        ChecklistItem::class,
        NutritionReminder::class,
        DayHeading::class,
        WorkoutReminder::class,
        FoodLogEntry::class,
        MotivationalQuote::class
    ],
    version = 12,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
    abstract fun userDao(): UserDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
