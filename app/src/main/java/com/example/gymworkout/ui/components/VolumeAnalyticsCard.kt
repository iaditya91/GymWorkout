package com.example.gymworkout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.VolumeAnalytics
import com.example.gymworkout.data.VolumeAnalytics.MuscleGroupVolume
import com.example.gymworkout.data.VolumeAnalytics.PerExerciseVolume
import com.example.gymworkout.data.WorkoutSetLog

@Composable
fun VolumeAnalyticsCard(
    todayLogs: List<WorkoutSetLog>,
    weekLogs: List<WorkoutSetLog>,
    displayUnit: String,
    muscleLookup: (String) -> Pair<List<String>, List<String>>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = VolumeAnalytics.summarizeWorkout(todayLogs)
    val weeklyByMuscle = VolumeAnalytics.aggregateByMuscleGroup(weekLogs, muscleLookup)
        .sortedWith(
            compareBy<MuscleGroupVolume> { VolumeAnalytics.canonicalIndex(it.muscle) }
                .thenByDescending { it.hardSets }
        )
    val weeklyTotalVolumeKg = weekLogs.sumOf { it.volumeKg }
    val weeklyTotalSets = weekLogs.size

    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
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
                        Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Volume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        if (today.totalSets == 0) "No sets logged today yet"
                        else "${today.totalSets} set${if (today.totalSets == 1) "" else "s"} · ${today.totalReps} reps",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Always-visible tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "Today",
                    primary = VolumeAnalytics.formatVolume(today.totalVolumeKg, displayUnit),
                    secondary = "volume",
                    bg = scheme.primaryContainer,
                    fg = scheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "This week",
                    primary = VolumeAnalytics.formatVolume(weeklyTotalVolumeKg, displayUnit),
                    secondary = "$weeklyTotalSets hard sets",
                    bg = scheme.tertiaryContainer,
                    fg = scheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (today.perExercise.isNotEmpty()) {
                        SectionLabel("Today by exercise")
                        Spacer(modifier = Modifier.height(6.dp))
                        PerExerciseBars(
                            items = today.perExercise,
                            displayUnit = displayUnit
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    SectionLabel("Weekly hard sets by muscle (MEV ${VolumeAnalytics.MEV_SETS} · MAV ${VolumeAnalytics.MAV_SETS})")
                    Spacer(modifier = Modifier.height(6.dp))
                    if (weeklyByMuscle.isEmpty()) {
                        Text(
                            "No sets logged this week yet. Check off a set and enter weight + reps to start tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        MuscleGroupBars(items = weeklyByMuscle)
                    }
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            primary,
            style = MaterialTheme.typography.titleLarge,
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
private fun PerExerciseBars(items: List<PerExerciseVolume>, displayUnit: String) {
    val max = items.maxOf { it.totalVolumeKg }.coerceAtLeast(1.0)
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { ex ->
            val fraction = (ex.totalVolumeKg / max).toFloat().coerceIn(0f, 1f)
            val animated by animateFloatAsState(targetValue = fraction, label = "exBar")
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ex.exerciseName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        VolumeAnalytics.formatVolume(ex.totalVolumeKg, displayUnit),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animated)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(scheme.primary)
                    )
                }
                Text(
                    "${ex.sets} sets · ${ex.totalReps} reps · top ${VolumeAnalytics.formatWeight(ex.topWeightKg, displayUnit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Horizontal-bar chart, one row per muscle.
 * The x axis is hard-sets-per-week. We draw two reference guides (MEV, MAV)
 * and color each bar by zone: under-MEV (warning), MEV→MAV (primary), over-MAV (tertiary).
 */
@Composable
private fun MuscleGroupBars(items: List<MuscleGroupVolume>) {
    val scheme = MaterialTheme.colorScheme
    // Scale: cap axis at max(25, ceil(max bar) + buffer) so MAV is always visible
    val maxSets = items.maxOf { it.hardSets }
    val axisMax = maxOf(25.0, kotlin.math.ceil(maxSets + 2.0))

    val underColor = scheme.outline
    val mevColor = scheme.primary
    val mavColor = scheme.tertiary

    val labelTextColor = scheme.onSurface
    val mutedTextColor = scheme.onSurfaceVariant

    val density = LocalDensity.current
    val rowHeight = 22.dp
    val rowSpacing = 6.dp

    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
        items.forEach { m ->
            val fraction = (m.hardSets / axisMax).toFloat().coerceIn(0f, 1f)
            val animated by animateFloatAsState(targetValue = fraction, label = "muscleBar")
            val barColor = when {
                m.hardSets >= VolumeAnalytics.MAV_SETS -> mavColor
                m.hardSets >= VolumeAnalytics.MEV_SETS -> mevColor
                else -> underColor
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    m.muscle.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = labelTextColor,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight)
                ) {
                    val h = size.height
                    val w = size.width
                    val barH = (h * 0.7f)
                    val barTop = (h - barH) / 2f

                    // Background track
                    drawRoundRect(
                        color = underColor.copy(alpha = 0.18f),
                        topLeft = Offset(0f, barTop),
                        size = Size(w, barH),
                        cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                    )
                    // Bar
                    val barW = w * animated
                    if (barW > 0f) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(0f, barTop),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                        )
                    }
                    // MEV guide
                    val mevX = (VolumeAnalytics.MEV_SETS.toFloat() / axisMax.toFloat()) * w
                    if (mevX >= 0f && mevX <= w) {
                        drawLine(
                            color = mevColor.copy(alpha = 0.55f),
                            start = Offset(mevX, 0f),
                            end = Offset(mevX, h),
                            strokeWidth = with(density) { 1.2.dp.toPx() }
                        )
                    }
                    // MAV guide
                    val mavX = (VolumeAnalytics.MAV_SETS.toFloat() / axisMax.toFloat()) * w
                    if (mavX >= 0f && mavX <= w) {
                        drawLine(
                            color = mavColor.copy(alpha = 0.55f),
                            start = Offset(mavX, 0f),
                            end = Offset(mavX, h),
                            strokeWidth = with(density) { 1.2.dp.toPx() },
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(with(density) { 3.dp.toPx() }, with(density) { 3.dp.toPx() })
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    formatSets(m.hardSets),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = mutedTextColor,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    // Legend
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color = scheme.outline)
        Spacer(modifier = Modifier.width(4.dp))
        Text("< MEV", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(10.dp))
        LegendDot(color = scheme.primary)
        Spacer(modifier = Modifier.width(4.dp))
        Text("MEV–MAV", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(10.dp))
        LegendDot(color = scheme.tertiary)
        Spacer(modifier = Modifier.width(4.dp))
        Text("≥ MAV", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun formatSets(sets: Double): String {
    return if (sets == sets.toLong().toDouble()) sets.toLong().toString()
    else "%.1f".format(sets)
}
