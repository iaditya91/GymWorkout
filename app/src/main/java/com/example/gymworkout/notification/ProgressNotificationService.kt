package com.example.gymworkout.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.room.InvalidationTracker
import com.example.gymworkout.MainActivity
import com.example.gymworkout.R
import com.example.gymworkout.data.DailyFocusPreference
import com.example.gymworkout.data.ProgressNotificationPreference
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.roundToInt

class ProgressNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "progress_notification"
        const val NOTIFICATION_ID = 9903
        const val ACTION_RESTART = "com.example.gymworkout.PROGRESS_NOTIF_RESTART"
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 min
        // Short coalesce window so multi-row DB writes (e.g. logFood inserts 4 rows) map to one re-render.
        private const val DB_CHANGE_DEBOUNCE_MS = 250L

        // Metric ring colors — matched to progress_*.xml drawable fills.
        private val METRIC_COLOR_WORKOUT = Color.parseColor("#6C9FFF")  // blue
        private val METRIC_COLOR_PROTEIN = Color.parseColor("#66DE93")  // green
        private val METRIC_COLOR_CALORIES = Color.parseColor("#FFB74D") // amber
        private val METRIC_COLOR_SLEEP = Color.parseColor("#AB47BC")    // purple
        private val METRIC_COLOR_WATER = Color.parseColor("#56D6C2")    // teal

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

        /**
         * True if the app is already exempt from battery optimizations (or running on pre-M where
         * the concept does not apply). When false, the OS may doze/throttle this foreground service
         * and the progress notification can disappear after the device sleeps for a while.
         */
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

        /**
         * Opens the system dialog to request that this app be exempted from battery optimizations.
         * Uses the direct-prompt intent (allowed by the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
         * permission); falls back to the general settings list if the direct prompt is unavailable.
         */
        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
            val pkg = context.packageName
            val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(direct)
            } catch (e: Exception) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e2: Exception) {
                    Log.w("ProgressNotifService", "Could not open battery optimization settings", e2)
                }
            }
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

    // Fires when any of the observed Room tables change; coalesced into one update per ~250ms burst.
    private var dbObserver: InvalidationTracker.Observer? = null
    private var pendingDbUpdate: Job? = null
    private var focusObserverJob: Job? = null

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

        registerDbObserver()
        registerFocusObserver()

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        dbObserver?.let {
            WorkoutDatabase.getDatabase(applicationContext).invalidationTracker.removeObserver(it)
        }
        dbObserver = null
        focusObserverJob?.cancel()
        focusObserverJob = null
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Android 14+ lets users dismiss foreground-service notifications by swiping.
     * The user enabled this as a persistent card, so when the system dismisses it we
     * re-post immediately via a tiny broadcast that restarts this service.
     */
    private fun buildRestartDeleteIntent(): PendingIntent {
        val intent = Intent(this, ProgressNotificationRestartReceiver::class.java).apply {
            action = ACTION_RESTART
            setPackage(packageName)
        }
        return PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerDbObserver() {
        if (dbObserver != null) return
        val db = WorkoutDatabase.getDatabase(applicationContext)
        val observer = object : InvalidationTracker.Observer(
            arrayOf("exercises", "nutrition_entries", "nutrition_targets", "food_log")
        ) {
            override fun onInvalidated(tables: Set<String>) {
                pendingDbUpdate?.cancel()
                pendingDbUpdate = scope.launch {
                    delay(DB_CHANGE_DEBOUNCE_MS)
                    updateNotification()
                }
            }
        }
        db.invalidationTracker.addObserver(observer)
        dbObserver = observer
    }

    private fun registerFocusObserver() {
        focusObserverJob?.cancel()
        focusObserverJob = scope.launch {
            // StateFlow emits current value on subscribe — skip the initial tick; onStartCommand already rendered.
            var first = true
            DailyFocusPreference.focus.collect {
                if (first) { first = false; return@collect }
                updateNotification()
            }
        }
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

        val focus = DailyFocusPreference.get(applicationContext)
        val (focusLabel, focusText) = when (focus.mode) {
            DailyFocusPreference.MODE_GOAL -> {
                val text = focus.goalText.trim()
                if (text.isNotEmpty()) "Goal for today" to text else null to null
            }
            DailyFocusPreference.MODE_HABIT -> {
                val habit = if (focus.habitCategory.isNotEmpty()) {
                    nutritionDao.getTargetSync(focus.habitCategory)
                } else null
                if (habit != null) "Habit for today" to habit.label else null to null
            }
            else -> null to null
        }

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
            dmgs = dmgs,
            focusLabel = focusLabel,
            focusText = focusText
        )
    }

    private fun buildNotification(s: ProgressSnapshot): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, "nutrition")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overallPct = (s.dmgs * 100).roundToInt()
        val headline = motivationalHeadline(overallPct)
        val accentColor = accentForProgress(overallPct)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val primaryTextColor = if (isDark) 0xFFECECEC.toInt() else 0xFF1C1B1F.toInt()
        val secondaryTextColor = if (isDark) 0xFFB0B0B0.toInt() else 0xFF5A5A5A.toInt()

        val workoutPct = if (s.totalExercises > 0) {
            ((s.completedExercises.toFloat() / s.totalExercises) * 100f).roundToInt().coerceIn(0, 100)
        } else 0
        val proteinPct = pctOf(s.proteinActual, s.proteinTarget)
        val caloriesPct = pctOf(s.caloriesActual, s.caloriesTarget)
        val sleepPct = pctOf(s.sleepActual, s.sleepTarget)
        val waterPct = pctOf(s.waterActual, s.waterTarget)

        // Fallback summary text (shown by system UIs that ignore custom views)
        val summary = "W ${s.completedExercises}/${s.totalExercises} · " +
                "P ${formatNum(s.proteinActual)}g · " +
                "S ${formatNum(s.sleepActual)}h · " +
                "H ${formatNum(s.waterActual)}L"

        // Collapsed view: headline + percent on top, row of lettered progress rings below
        val collapsed = RemoteViews(packageName, R.layout.notification_progress_collapsed).apply {
            setTextViewText(R.id.notif_collapsed_headline, headline)
            setTextColor(R.id.notif_collapsed_headline, primaryTextColor)
            setTextViewText(R.id.notif_collapsed_subline, summary)
            setTextColor(R.id.notif_collapsed_subline, secondaryTextColor)
            setTextViewText(R.id.notif_collapsed_percent, "$overallPct%")
            setTextColor(R.id.notif_collapsed_percent, accentColor)

            setImageViewBitmap(
                R.id.notif_ring_workout,
                renderMetricRing("G", workoutPct, METRIC_COLOR_WORKOUT, isDark, primaryTextColor)
            )
            setImageViewBitmap(
                R.id.notif_ring_protein,
                renderMetricRing("P", proteinPct, METRIC_COLOR_PROTEIN, isDark, primaryTextColor)
            )
            setImageViewBitmap(
                R.id.notif_ring_calories,
                renderMetricRing("C", caloriesPct, METRIC_COLOR_CALORIES, isDark, primaryTextColor)
            )
            setImageViewBitmap(
                R.id.notif_ring_sleep,
                renderMetricRing("S", sleepPct, METRIC_COLOR_SLEEP, isDark, primaryTextColor)
            )
            setImageViewBitmap(
                R.id.notif_ring_water,
                renderMetricRing("W", waterPct, METRIC_COLOR_WATER, isDark, primaryTextColor)
            )
        }

        // Expanded view: headline, big percent, overall bar, optional focus chip, 5 metric rows
        val expanded = RemoteViews(packageName, R.layout.notification_progress_expanded).apply {
            setTextViewText(R.id.notif_headline, headline)
            setTextColor(R.id.notif_headline, primaryTextColor)
            setTextViewText(R.id.notif_subline, subline(s, overallPct))
            setTextColor(R.id.notif_subline, secondaryTextColor)
            setTextViewText(R.id.notif_percent, "$overallPct%")
            setTextColor(R.id.notif_percent, accentColor)
            setProgressBar(R.id.notif_overall_bar, 100, overallPct, false)

            if (s.focusLabel != null && s.focusText != null) {
                val icon = if (s.focusLabel.startsWith("Goal", ignoreCase = true)) "🎯" else "⚡"
                setTextViewText(R.id.notif_focus, "$icon  ${s.focusLabel} · ${s.focusText}")
                setTextColor(R.id.notif_focus, primaryTextColor)
                setViewVisibility(R.id.notif_focus, View.VISIBLE)
            } else {
                setViewVisibility(R.id.notif_focus, View.GONE)
            }

            // Workout
            setProgressBar(R.id.notif_bar_workout, 100, workoutPct, false)
            setTextColor(R.id.notif_lbl_workout, primaryTextColor)
            setTextViewText(
                R.id.notif_val_workout,
                if (s.totalExercises > 0) "${s.completedExercises}/${s.totalExercises}" else "—"
            )
            setTextColor(R.id.notif_val_workout, secondaryTextColor)

            // Protein
            setProgressBar(R.id.notif_bar_protein, 100, proteinPct, false)
            setTextColor(R.id.notif_lbl_protein, primaryTextColor)
            setTextViewText(R.id.notif_val_protein, metricValue(s.proteinActual, s.proteinTarget, "g"))
            setTextColor(R.id.notif_val_protein, secondaryTextColor)

            // Calories
            setProgressBar(R.id.notif_bar_calories, 100, caloriesPct, false)
            setTextColor(R.id.notif_lbl_calories, primaryTextColor)
            setTextViewText(
                R.id.notif_val_calories,
                metricValue(s.caloriesActual, s.caloriesTarget, " kcal")
            )
            setTextColor(R.id.notif_val_calories, secondaryTextColor)

            // Sleep
            setProgressBar(R.id.notif_bar_sleep, 100, sleepPct, false)
            setTextColor(R.id.notif_lbl_sleep, primaryTextColor)
            setTextViewText(R.id.notif_val_sleep, metricValue(s.sleepActual, s.sleepTarget, "h"))
            setTextColor(R.id.notif_val_sleep, secondaryTextColor)

            // Water
            setProgressBar(R.id.notif_bar_water, 100, waterPct, false)
            setTextColor(R.id.notif_lbl_water, primaryTextColor)
            setTextViewText(R.id.notif_val_water, metricValue(s.waterActual, s.waterTarget, "L"))
            setTextColor(R.id.notif_val_water, secondaryTextColor)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$headline — $overallPct%")
            .setContentText(summary)
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openIntent)
            .setDeleteIntent(buildRestartDeleteIntent())
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    private fun motivationalHeadline(pct: Int): String = when {
        pct >= 100 -> "Day complete 🏆"
        pct >= 80 -> "Almost there 🎯"
        pct >= 50 -> "Strong day 🚀"
        pct >= 20 -> "Momentum building 🔥"
        else -> "Let's start 💪"
    }

    private fun subline(s: ProgressSnapshot, pct: Int): String {
        // Psychology: highlight the nearest gap to completion (goal-gradient effect)
        if (pct >= 100) return "Every target hit today"
        val candidates = listOfNotNull(
            gapText("workout", s.completedExercises.toFloat(), s.totalExercises.toFloat()),
            gapText("protein", s.proteinActual, s.proteinTarget),
            gapText("sleep", s.sleepActual, s.sleepTarget),
            gapText("water", s.waterActual, s.waterTarget),
            gapText("calories", s.caloriesActual, s.caloriesTarget)
        ).filter { it.first in 0.01f..0.99f }
        val closest = candidates.maxByOrNull { it.first }
        return if (closest != null) "Closest to goal · ${closest.second}" else "Tap to see details"
    }

    private fun gapText(label: String, actual: Float, target: Float): Pair<Float, String>? {
        if (target <= 0f) return null
        val ratio = (actual / target).coerceIn(0f, 1f)
        return ratio to "$label ${(ratio * 100).roundToInt()}%"
    }

    private fun pctOf(actual: Float, target: Float): Int {
        if (target <= 0f) return 0
        return min((actual / target) * 100f, 100f).roundToInt()
    }

    private fun metricValue(actual: Float, target: Float, unit: String): String {
        if (target <= 0f) return "${formatNum(actual)}$unit"
        return "${formatNum(actual)}/${formatNum(target)}$unit"
    }

    private fun renderMetricRing(
        letter: String,
        pct: Int,
        accentColor: Int,
        isDark: Boolean,
        letterColor: Int
    ): Bitmap {
        val density = resources.displayMetrics.density
        val size = (44 * density).toInt().coerceAtLeast(44)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val stroke = size * 0.14f
        val half = stroke / 2f
        val rect = RectF(half, half, size - half, size - half)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.argb(70, 255, 255, 255) else Color.argb(45, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        val sweep = (pct.coerceIn(0, 100) / 100f) * 360f
        if (sweep > 0f) {
            val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(rect, -90f, sweep, false, progPaint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = letterColor
            textAlign = Paint.Align.CENTER
            textSize = size * 0.42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fm = textPaint.fontMetrics
        val textY = size / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(letter, size / 2f, textY, textPaint)

        return bitmap
    }

    private fun accentForProgress(pct: Int): Int = when {
        pct >= 80 -> Color.parseColor("#66DE93") // green — reward
        pct >= 50 -> Color.parseColor("#56D6C2") // teal — on-track
        pct >= 20 -> Color.parseColor("#FFB74D") // amber — momentum
        else -> Color.parseColor("#FF8A80")      // soft red — nudge, not alarm
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
        val dmgs: Float = 0f,
        val focusLabel: String? = null,
        val focusText: String? = null
    )
}
