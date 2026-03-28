package com.example.gymworkout.ui.components

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.notification.TimerAlertService
import com.example.gymworkout.notification.TimerReceiver
import com.example.gymworkout.viewmodel.RestTimerState
import kotlinx.coroutines.delay

private const val REST_TIMER_REQUEST_CODE = 7700

private fun buildTimerPendingIntent(context: Context, exerciseName: String): PendingIntent {
    val intent = Intent(context, TimerReceiver::class.java).apply {
        putExtra(TimerReceiver.EXTRA_LABEL, "Rest - $exerciseName")
        putExtra(TimerReceiver.EXTRA_NOTIFY, true)
        putExtra(TimerReceiver.EXTRA_NOTIFICATION_ID, "rest_$exerciseName".hashCode())
        putExtra(TimerReceiver.EXTRA_TIMER_TYPE, "rest")
    }
    return PendingIntent.getBroadcast(
        context,
        REST_TIMER_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun scheduleRestTimerAlarm(context: Context, exerciseName: String, seconds: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildTimerPendingIntent(context, exerciseName)
    val triggerAt = SystemClock.elapsedRealtime() + seconds * 1000L
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        triggerAt,
        pendingIntent
    )
}

fun cancelRestTimerAlarm(context: Context, exerciseName: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(buildTimerPendingIntent(context, exerciseName))
}

/**
 * Composable tick that reads remaining seconds from the ViewModel timer state.
 * Returns remaining seconds, updating every second.
 */
@Composable
fun rememberTimerTick(timerState: RestTimerState): Int {
    var tick by remember { mutableIntStateOf(timerState.remainingSeconds()) }

    LaunchedEffect(timerState.isRunning, timerState.endElapsedRealtime, timerState.pausedRemainingMs) {
        if (timerState.isRunning && !timerState.isFinished) {
            while (true) {
                tick = timerState.remainingSeconds()
                if (tick <= 0) break
                delay(1000L)
            }
            tick = 0
        } else {
            tick = timerState.remainingSeconds()
        }
    }

    return tick
}

@Composable
fun RestTimerDialog(
    timerState: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onSetInline: (Boolean) -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val remainingSeconds = rememberTimerTick(timerState)
    val finished = remainingSeconds <= 0 && (timerState.isRunning || timerState.isFinished)
    val progress = if (timerState.totalSeconds > 0) remainingSeconds.toFloat() / timerState.totalSeconds else 0f

    // Schedule alarm
    DisposableEffect(timerState.isRunning, timerState.endElapsedRealtime) {
        if (timerState.isRunning && remainingSeconds > 0) {
            scheduleRestTimerAlarm(context, timerState.exerciseName, remainingSeconds)
        }
        onDispose {}
    }

    // Handle finish
    LaunchedEffect(finished) {
        if (finished) {
            cancelRestTimerAlarm(context, timerState.exerciseName)
            try {
                TimerAlertService.start(context, "Rest - ${timerState.exerciseName}", "rest")
            } catch (_: Exception) {}
            onFinished()
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = if (minutes > 0) String.format("%d:%02d", minutes, seconds) else "$seconds"

    AlertDialog(
        onDismissRequest = {
            if (finished) TimerAlertService.stop(context)
            cancelRestTimerAlarm(context, timerState.exerciseName)
            onDismiss()
        },
        title = {
            Text(
                if (finished) "Rest Complete!" else "Rest Timer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(250.dp)
            ) {
                Text(
                    timerState.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(140.dp),
                        color = if (finished) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (finished) "Done!" else timeText,
                        fontSize = if (finished) 24.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (finished) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!finished) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (timerState.isRunning) {
                                    cancelRestTimerAlarm(context, timerState.exerciseName)
                                    onPause()
                                } else {
                                    onResume()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (timerState.isRunning) "Pause" else "Resume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                onReset()
                                scheduleRestTimerAlarm(context, timerState.exerciseName, timerState.totalSeconds)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Checkbox to switch to inline mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { onSetInline(true) }
                        )
                        Text(
                            "Show on screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (finished) TimerAlertService.stop(context)
                cancelRestTimerAlarm(context, timerState.exerciseName)
                onDismiss()
            }) {
                Text(if (finished) "Done" else "Dismiss")
            }
        }
    )
}

/**
 * Inline rest timer bar shown at the bottom of the workout screen.
 */
@Composable
fun InlineRestTimer(
    timerState: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onSwitchToPopup: () -> Unit
) {
    val context = LocalContext.current
    val remainingSeconds = rememberTimerTick(timerState)
    val finished = remainingSeconds <= 0 && (timerState.isRunning || timerState.isFinished)
    val progress = if (timerState.totalSeconds > 0) remainingSeconds.toFloat() / timerState.totalSeconds else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    // Schedule alarm
    DisposableEffect(timerState.isRunning, timerState.endElapsedRealtime) {
        if (timerState.isRunning && remainingSeconds > 0) {
            scheduleRestTimerAlarm(context, timerState.exerciseName, remainingSeconds)
        }
        onDispose {}
    }

    // Handle finish
    LaunchedEffect(finished) {
        if (finished) {
            cancelRestTimerAlarm(context, timerState.exerciseName)
            try {
                TimerAlertService.start(context, "Rest - ${timerState.exerciseName}", "rest")
            } catch (_: Exception) {}
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = if (minutes > 0) String.format("%d:%02d", minutes, seconds) else "${seconds}s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Circular progress
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(40.dp),
                    color = if (finished) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = if (finished) "!" else timeText,
                    fontSize = if (finished) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (finished) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Exercise name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (finished) "Rest Complete!" else "Rest Timer",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (finished) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = timerState.exerciseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (finished) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            if (finished) {
                // Done button
                TextButton(onClick = {
                    TimerAlertService.stop(context)
                    onDismiss()
                }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            } else {
                // Pause/Resume
                IconButton(
                    onClick = {
                        if (timerState.isRunning) {
                            cancelRestTimerAlarm(context, timerState.exerciseName)
                            onPause()
                        } else {
                            onResume()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "Pause" else "Resume",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset
                IconButton(
                    onClick = {
                        onReset()
                        scheduleRestTimerAlarm(context, timerState.exerciseName, timerState.totalSeconds)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close
                IconButton(
                    onClick = {
                        cancelRestTimerAlarm(context, timerState.exerciseName)
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
