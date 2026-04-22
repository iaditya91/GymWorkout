package com.example.gymworkout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.VolumeAnalytics
import com.example.gymworkout.data.VolumeAnalytics.ExerciseProgression
import com.example.gymworkout.data.VolumeAnalytics.ExerciseSession
import com.example.gymworkout.data.WorkoutSetLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExerciseProgressionCard(
    logs: List<WorkoutSetLog>,
    displayUnit: String,
    modifier: Modifier = Modifier
) {
    val progression = remember(logs) { VolumeAnalytics.buildProgression(logs) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        if (progression.totalSessions == 0) "No sets logged yet"
                        else "${progression.totalSessions} session${if (progression.totalSessions == 1) "" else "s"} · ${progression.totalSets} sets",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = scheme.onSurfaceVariant
                )
            }

            if (progression.totalSessions == 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Log a set from the workout page to start tracking progress here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Always-visible stat tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "Est. 1RM",
                    primary = VolumeAnalytics.formatWeight(progression.allTimeEstimatedOneRepMaxKg, displayUnit),
                    secondary = "personal best",
                    bg = scheme.primaryContainer,
                    fg = scheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Top set",
                    primary = VolumeAnalytics.formatWeight(progression.allTimeTopWeightKg, displayUnit),
                    secondary = "× ${progression.allTimeTopWeightReps} reps",
                    bg = scheme.tertiaryContainer,
                    fg = scheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Total",
                    primary = VolumeAnalytics.formatVolume(progression.totalVolumeKg, displayUnit),
                    secondary = "volume",
                    bg = scheme.secondaryContainer,
                    fg = scheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionLabel("Top-set weight over time")
                    Spacer(modifier = Modifier.height(6.dp))
                    ProgressionChart(
                        sessions = progression.sessions,
                        displayUnit = displayUnit
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    SectionLabel("Recent sessions")
                    Spacer(modifier = Modifier.height(6.dp))
                    RecentSessionsList(
                        sessions = progression.sessions.takeLast(5).reversed(),
                        displayUnit = displayUnit
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatTile(
    label: String,
    primary: String,
    secondary: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
        Text(
            secondary,
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun ProgressionChart(
    sessions: List<ExerciseSession>,
    displayUnit: String
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Build display points: x = session index (even spacing), y = topWeightKg
    val weightsDisplay = sessions.map { VolumeAnalytics.kgToDisplay(it.topWeightKg, displayUnit) }
    val minY = (weightsDisplay.minOrNull() ?: 0.0).coerceAtLeast(0.0)
    val maxY = (weightsDisplay.maxOrNull() ?: 0.0)
    // Add a small margin so the top/bottom points aren't flush against the edges
    val span = (maxY - minY).coerceAtLeast(1.0)
    val padded = span * 0.15
    val yLow = (minY - padded).coerceAtLeast(0.0)
    val yHigh = maxY + padded

    val lineColor = scheme.primary
    val dotColor = scheme.primary
    val gridColor = scheme.outline.copy(alpha = 0.25f)
    val mutedText = scheme.onSurfaceVariant

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceVariant.copy(alpha = 0.25f))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height

                // Gridlines (3 horizontal)
                val gridStrokePx = with(density) { 0.8.dp.toPx() }
                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = gridStrokePx
                    )
                }

                if (sessions.size < 2) {
                    // Single-session: just draw one dot centered
                    val cx = w / 2f
                    val cy = h / 2f
                    drawCircle(
                        color = dotColor,
                        radius = with(density) { 4.dp.toPx() },
                        center = Offset(cx, cy)
                    )
                    return@Canvas
                }

                val yRange = (yHigh - yLow).coerceAtLeast(0.0001)
                val xStep = if (sessions.size > 1) w / (sessions.size - 1).toFloat() else 0f

                fun yPx(value: Double): Float {
                    val t = ((value - yLow) / yRange).toFloat().coerceIn(0f, 1f)
                    return h - t * h
                }

                // Line path
                val path = Path()
                weightsDisplay.forEachIndexed { index, v ->
                    val x = index * xStep
                    val y = yPx(v)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = with(density) { 2.dp.toPx() }
                    )
                )

                // Dots
                val dotRadius = with(density) { 3.5.dp.toPx() }
                weightsDisplay.forEachIndexed { index, v ->
                    val x = index * xStep
                    val y = yPx(v)
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
        // Y-axis labels (min/max) and session count
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "min ${formatDisplayValue(yLow)} $displayUnit",
                style = MaterialTheme.typography.labelSmall,
                color = mutedText
            )
            if (sessions.size >= 2) {
                val first = sessions.first().dayStartMs
                val last = sessions.last().dayStartMs
                Text(
                    "${formatShortDate(first)} → ${formatShortDate(last)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText
                )
            }
            Text(
                "max ${formatDisplayValue(yHigh)} $displayUnit",
                style = MaterialTheme.typography.labelSmall,
                color = mutedText
            )
        }
    }
}

@Composable
private fun RecentSessionsList(
    sessions: List<ExerciseSession>,
    displayUnit: String
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sessions.forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatLongDate(session.dayStartMs),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface
                    )
                    Text(
                        "${session.sets} sets · ${session.totalReps} reps · top ${VolumeAnalytics.formatWeight(session.topWeightKg, displayUnit)} × ${session.topWeightReps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Text(
                    VolumeAnalytics.formatVolume(session.totalVolumeKg, displayUnit),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
            }
        }
    }
}

private fun formatDisplayValue(v: Double): String {
    val s = "%.1f".format(v)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

private fun formatShortDate(ms: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))

private fun formatLongDate(ms: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(ms))
