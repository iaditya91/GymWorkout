package com.example.gymworkout.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gymworkout.MainActivity
import com.example.gymworkout.R
import com.example.gymworkout.data.ProgressNotificationPreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.roundToInt

class ProgressNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "progress_notification"
        const val NOTIFICATION_ID = 9903
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 min

        fun start(context: Context) {
            val intent = Intent(context, ProgressNotificationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("ProgressNotifService", "Could not start service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProgressNotificationService::class.java))
        }

        /** Pokes the service to refresh its notification now (e.g., after a workout check-off). */
        fun refresh(context: Context) {
            if (!ProgressNotificationPreference.getEnabled(context)) return
            start(context)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createProgressNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground within 5 seconds — show placeholder immediately,
        // then async-load real progress and re-notify.
        startForeground(NOTIFICATION_ID, buildNotification(ProgressSnapshot()))
        updateNotification()

        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        scope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        scope.launch {
            val snapshot = loadTodaySnapshot()
            val notification = buildNotification(snapshot)
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadTodaySnapshot(): ProgressSnapshot {
        val db = WorkoutDatabase.getDatabase(applicationContext)
        val exerciseDao = db.exerciseDao()
        val nutritionDao = db.nutritionDao()

        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        // Exercise uses 0=Mon ... 6=Sun
        val dayIndex = today.dayOfWeek.value - 1

        val totalExercises = exerciseDao.getExerciseCountForDaySync(dayIndex)
        val completedExercises = exerciseDao.getCompletedCountForDaySync(dayIndex)

        val proteinActual = nutritionDao.getTotalForDateAndCategorySync(todayStr, "PROTEIN")
        val caloriesActual = nutritionDao.getTotalForDateAndCategorySync(todayStr, "CALORIES")
        val sleepActual = nutritionDao.getTotalForDateAndCategorySync(todayStr, "SLEEP")
        val waterActual = nutritionDao.getTotalForDateAndCategorySync(todayStr, "WATER")

        val proteinTarget = nutritionDao.getTargetSync("PROTEIN")?.targetValue ?: 0f
        val caloriesTarget = nutritionDao.getTargetSync("CALORIES")?.targetValue ?: 0f
        val sleepTarget = nutritionDao.getTargetSync("SLEEP")?.targetValue ?: 0f
        val waterTarget = nutritionDao.getTargetSync("WATER")?.targetValue ?: 0f

        val workoutScore = if (totalExercises > 0) {
            min(completedExercises.toFloat() / totalExercises, 1f)
        } else 0f
        val proteinScore = if (proteinTarget > 0f) min(proteinActual / proteinTarget, 1f) else 0f
        val caloriesScore = if (caloriesTarget > 0f) min(caloriesActual / caloriesTarget, 1f) else 0f
        val sleepScore = if (sleepTarget > 0f) min(sleepActual / sleepTarget, 1f) else 0f
        val hydrationScore = if (waterTarget > 0f) min(waterActual / waterTarget, 1f) else 0f

        // DMGS = weighted sum (matches StatsViewModel.computeDailyScore)
        val dmgs = (proteinScore * 0.35f) +
                (caloriesScore * 0.20f) +
                (workoutScore * 0.20f) +
                (sleepScore * 0.15f) +
                (hydrationScore * 0.10f)

        return ProgressSnapshot(
            totalExercises = totalExercises,
            completedExercises = completedExercises,
            proteinActual = proteinActual,
            proteinTarget = proteinTarget,
            caloriesActual = caloriesActual,
            caloriesTarget = caloriesTarget,
            sleepActual = sleepActual,
            sleepTarget = sleepTarget,
            waterActual = waterActual,
            waterTarget = waterTarget,
            dmgs = dmgs
        )
    }

    private fun buildNotification(s: ProgressSnapshot): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overallPct = (s.dmgs * 100).roundToInt()
        val title = "Today's Progress — $overallPct%"

        val workoutLine = if (s.totalExercises > 0) {
            "Workout: ${s.completedExercises}/${s.totalExercises} exercises"
        } else {
            "Workout: no exercises scheduled"
        }
        val proteinLine = formatLine("Protein", s.proteinActual, s.proteinTarget, "g")
        val caloriesLine = formatLine("Calories", s.caloriesActual, s.caloriesTarget, " kcal")
        val sleepLine = formatLine("Sleep", s.sleepActual, s.sleepTarget, "h")
        val waterLine = formatLine("Water", s.waterActual, s.waterTarget, "L")

        val bigText = listOf(workoutLine, proteinLine, caloriesLine, sleepLine, waterLine)
            .joinToString("\n")

        val summary = "W ${s.completedExercises}/${s.totalExercises} · " +
                "P ${formatNum(s.proteinActual)}g · " +
                "S ${formatNum(s.sleepActual)}h · " +
                "H ${formatNum(s.waterActual)}L"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    private fun formatLine(label: String, actual: Float, target: Float, unit: String): String {
        if (target <= 0f) return "$label: ${formatNum(actual)}$unit"
        val pct = min((actual / target) * 100f, 100f).roundToInt()
        return "$label: ${formatNum(actual)}/${formatNum(target)}$unit ($pct%)"
    }

    private fun formatNum(value: Float): String {
        val rounded = (value * 10).roundToInt() / 10f
        return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    }

    private data class ProgressSnapshot(
        val totalExercises: Int = 0,
        val completedExercises: Int = 0,
        val proteinActual: Float = 0f,
        val proteinTarget: Float = 0f,
        val caloriesActual: Float = 0f,
        val caloriesTarget: Float = 0f,
        val sleepActual: Float = 0f,
        val sleepTarget: Float = 0f,
        val waterActual: Float = 0f,
        val waterTarget: Float = 0f,
        val dmgs: Float = 0f
    )
}
