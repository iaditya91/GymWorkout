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

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_checkins ADD COLUMN habitsDone INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new mineral columns to food_log
        db.execSQL("ALTER TABLE food_log ADD COLUMN magnesium REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_log ADD COLUMN potassium REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_log ADD COLUMN zinc REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_log ADD COLUMN copper REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_log ADD COLUMN selenium REAL NOT NULL DEFAULT 0")
        // Add new mineral columns to custom_foods
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN magnesiumPerBase REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN potassiumPerBase REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN zincPerBase REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN copperPerBase REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_foods ADD COLUMN seleniumPerBase REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE nutrition_targets ADD COLUMN timerSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE nutrition_targets ADD COLUMN timerNotifyEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add ringtoneUri to nutrition_reminders
        db.execSQL("ALTER TABLE nutrition_reminders ADD COLUMN ringtoneUri TEXT NOT NULL DEFAULT ''")

        // Fix nutrition_targets: old DB may have timerSoundEnabled/timerVibrateEnabled
        // instead of timerNotifyEnabled. Recreate table with correct schema.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS nutrition_targets_new (
                category TEXT NOT NULL PRIMARY KEY DEFAULT '',
                targetValue REAL NOT NULL DEFAULT 0,
                label TEXT NOT NULL DEFAULT '',
                unit TEXT NOT NULL DEFAULT '',
                isCustom INTEGER NOT NULL DEFAULT 0,
                notes TEXT NOT NULL DEFAULT '',
                timerSeconds INTEGER NOT NULL DEFAULT 0,
                timerNotifyEnabled INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        // Copy data, mapping old columns if they exist
        try {
            // Try with old column names (timerSoundEnabled OR timerVibrateEnabled)
            db.execSQL("""
                INSERT INTO nutrition_targets_new (category, targetValue, label, unit, isCustom, notes, timerSeconds, timerNotifyEnabled)
                SELECT category, targetValue, label, unit, isCustom, notes, timerSeconds,
                    CASE WHEN timerSoundEnabled = 1 OR timerVibrateEnabled = 1 THEN 1 ELSE 0 END
                FROM nutrition_targets
            """.trimIndent())
        } catch (_: Exception) {
            try {
                // Try with new column name (timerNotifyEnabled already exists)
                db.execSQL("""
                    INSERT INTO nutrition_targets_new (category, targetValue, label, unit, isCustom, notes, timerSeconds, timerNotifyEnabled)
                    SELECT category, targetValue, label, unit, isCustom, notes, timerSeconds, timerNotifyEnabled
                    FROM nutrition_targets
                """.trimIndent())
            } catch (_: Exception) {
                // Fallback: copy only base columns
                db.execSQL("""
                    INSERT INTO nutrition_targets_new (category, targetValue, label, unit, isCustom, notes)
                    SELECT category, targetValue, label, unit, isCustom, notes
                    FROM nutrition_targets
                """.trimIndent())
            }
        }
        db.execSQL("DROP TABLE nutrition_targets")
        db.execSQL("ALTER TABLE nutrition_targets_new RENAME TO nutrition_targets")
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
    version = 20,
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
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
