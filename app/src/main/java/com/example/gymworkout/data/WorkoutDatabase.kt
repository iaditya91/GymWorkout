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

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_foods (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                servingUnit TEXT NOT NULL DEFAULT 'g',
                defaultServing REAL NOT NULL DEFAULT 100,
                caloriesPerBase REAL NOT NULL DEFAULT 0,
                proteinPerBase REAL NOT NULL DEFAULT 0,
                carbsPerBase REAL NOT NULL DEFAULT 0,
                fatPerBase REAL NOT NULL DEFAULT 0,
                fiberPerBase REAL NOT NULL DEFAULT 0,
                vitAPerBase REAL NOT NULL DEFAULT 0,
                vitB1PerBase REAL NOT NULL DEFAULT 0,
                vitB2PerBase REAL NOT NULL DEFAULT 0,
                vitB3PerBase REAL NOT NULL DEFAULT 0,
                vitB6PerBase REAL NOT NULL DEFAULT 0,
                vitB12PerBase REAL NOT NULL DEFAULT 0,
                vitCPerBase REAL NOT NULL DEFAULT 0,
                vitDPerBase REAL NOT NULL DEFAULT 0,
                vitEPerBase REAL NOT NULL DEFAULT 0,
                vitKPerBase REAL NOT NULL DEFAULT 0,
                folatePerBase REAL NOT NULL DEFAULT 0,
                ironPerBase REAL NOT NULL DEFAULT 0,
                calciumPerBase REAL NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE nutrition_targets ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
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
        MotivationalQuote::class,
        CustomFoodItem::class
    ],
    version = 16,
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
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
