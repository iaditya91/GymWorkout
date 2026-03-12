package com.example.gymworkout.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.gymworkout.MainActivity
import com.example.gymworkout.R
import com.example.gymworkout.data.TimerSoundPreference

class TimerAlertService : Service() {

    companion object {
        private const val ALERT_CHANNEL_ID = "timer_alert"
        private const val ALERT_NOTIFICATION_ID = 9901
        const val EXTRA_LABEL = "alert_label"
        const val EXTRA_TIMER_TYPE = "alert_timer_type"

        // Vibration: 800ms on, 600ms off, repeat
        private val VIBRATION_PATTERN = longArrayOf(0L, 800L, 600L)

        fun start(context: Context, label: String, timerType: String) {
            val intent = Intent(context, TimerAlertService::class.java).apply {
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_TIMER_TYPE, timerType)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerAlertService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    // Loops ringtone on API < 28 where isLooping isn't available
    private val ringtoneLooper = object : Runnable {
        override fun run() {
            ringtone?.let { if (!it.isPlaying) it.play() }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createAlertChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Timer"
        val timerType = intent?.getStringExtra(EXTRA_TIMER_TYPE) ?: "habit"

        startForeground(ALERT_NOTIFICATION_ID, buildNotification(label))
        startAlertForProfile(timerType)

        return START_NOT_STICKY
    }

    private fun startAlertForProfile(timerType: String) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                startRingtone(timerType)
                startVibration()
            }
            AudioManager.RINGER_MODE_VIBRATE -> startVibration()
            // RINGER_MODE_SILENT: no audio, but still vibrate for important timer alerts
            else -> startVibration()
        }
    }

    private fun startRingtone(timerType: String) {
        val soundUri = when (timerType) {
            "rest" -> TimerSoundPreference.getRestTimerSoundUri(this)
            else -> TimerSoundPreference.getHabitTimerSoundUri(this)
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        ringtone = RingtoneManager.getRingtone(this, soundUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
            ringtone?.play()
        } else {
            handler.post(ringtoneLooper)
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // 0 = repeat from index 0
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VIBRATION_PATTERN, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ringtoneLooper)
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(label: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$label Timer Done!")
            .setContentText("Open the app and tap Done to stop the alert")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent alert when a timer finishes — tap Acknowledge to stop"
                setSound(null, null)       // sound handled by ringtone API
                enableVibration(false)     // vibration handled by Vibrator API
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
