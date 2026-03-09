package com.example.gymworkout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN targetWeight REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN startingWeight REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN fitnessLevel TEXT NOT NULL DEFAULT 'beginner'")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN journeyStartDate TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN requiredShape TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN idealDays INTEGER NOT NULL DEFAULT 90")
    }
}

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
    version = 14,
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
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
