package com.example.gymworkout.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay

@Composable
fun RestTimerDialog(
    exerciseName: String,
    totalSeconds: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val finished = remainingSeconds <= 0

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    // Notify when timer finishes
    LaunchedEffect(finished) {
        if (finished) {
            val intent = Intent(context, TimerReceiver::class.java).apply {
                putExtra(TimerReceiver.EXTRA_LABEL, "Rest - $exerciseName")
                putExtra(TimerReceiver.EXTRA_NOTIFY, true)
                putExtra(TimerReceiver.EXTRA_NOTIFICATION_ID, "rest_$exerciseName".hashCode())
                putExtra(TimerReceiver.EXTRA_TIMER_TYPE, "rest")
            }
            context.sendBroadcast(intent)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = if (minutes > 0) {
        String.format("%d:%02d", minutes, seconds)
    } else {
        "$seconds"
    }

    AlertDialog(
        onDismissRequest = {
            if (finished) TimerAlertService.stop(context)
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
                    exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Circular timer
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(140.dp),
                        color = if (finished)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (finished) "Done!" else timeText,
                        fontSize = if (finished) 24.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (finished)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                if (!finished) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause/Resume
                        IconButton(
                            onClick = { isRunning = !isRunning },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Resume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Reset
                        IconButton(
                            onClick = {
                                remainingSeconds = totalSeconds
                                isRunning = true
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
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (finished) TimerAlertService.stop(context)
                onDismiss()
            }) {
                Text(if (finished) "Done" else "Dismiss")
            }
        }
    )
}
