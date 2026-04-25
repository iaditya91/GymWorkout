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

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS atomic_habits (
                category TEXT NOT NULL PRIMARY KEY DEFAULT '',
                cue TEXT NOT NULL DEFAULT '',
                craving TEXT NOT NULL DEFAULT '',
                response TEXT NOT NULL DEFAULT '',
                reward TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        // Migrate existing JSON data from nutrition_targets.notes into atomic_habits
        val cursor = db.query("SELECT category, notes FROM nutrition_targets WHERE notes LIKE '{%'")
        while (cursor.moveToNext()) {
            val category = cursor.getString(0)
            val notes = cursor.getString(1)
            try {
                val json = org.json.JSONObject(notes)
                val cue = json.optString("cue", "")
                val craving = json.optString("craving", "")
                val response = json.optString("response", "")
                val reward = json.optString("reward", "")
                if (cue.isNotEmpty() || craving.isNotEmpty() || response.isNotEmpty() || reward.isNotEmpty()) {
                    db.execSQL(
                        "INSERT OR REPLACE INTO atomic_habits (category, cue, craving, response, reward, updatedAt) VALUES (?, ?, ?, ?, ?, '')",
                        arrayOf(category, cue, craving, response, reward)
                    )
                    // Clear the JSON from notes since it's now in dedicated table
                    db.execSQL("UPDATE nutrition_targets SET notes = '' WHERE category = ?", arrayOf(category))
                }
            } catch (_: Exception) {
                // Not valid JSON, skip
            }
        }
        cursor.close()
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN completedSets INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS weight_entries (
                date TEXT NOT NULL PRIMARY KEY DEFAULT '',
                weight REAL NOT NULL DEFAULT 0,
                unit TEXT NOT NULL DEFAULT 'kg'
            )
        """.trimIndent())
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_set_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exerciseId INTEGER NOT NULL DEFAULT 0,
                exerciseName TEXT NOT NULL DEFAULT '',
                dayOfWeek INTEGER NOT NULL DEFAULT 0,
                setIndex INTEGER NOT NULL DEFAULT 0,
                reps INTEGER NOT NULL DEFAULT 0,
                weightKg REAL NOT NULL DEFAULT 0,
                loggedAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_set_logs_exerciseId ON workout_set_logs(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_set_logs_loggedAt ON workout_set_logs(loggedAt)")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL DEFAULT '',
                mood TEXT NOT NULL DEFAULT '',
                text TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_category_date ON journal_entries(category, date)")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE nutrition_targets ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE nutrition_targets ADD COLUMN descriptionMode TEXT NOT NULL DEFAULT 'text'")
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
        CustomFoodItem::class,
        AtomicHabit::class,
        WeightEntry::class,
        WorkoutSetLog::class,
        JournalEntry::class
    ],
    version = 26,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
    abstract fun userDao(): UserDao
    abstract fun reminderDao(): ReminderDao
    abstract fun workoutSetLogDao(): WorkoutSetLogDao

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
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
