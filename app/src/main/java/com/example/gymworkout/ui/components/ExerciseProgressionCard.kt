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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.roundToInt
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
                    SectionLabel("Top-set weight ($displayUnit) over time")
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
    val textMeasurer = rememberTextMeasurer()

    // x = session (even spacing, oldest → newest), y = top-set weight in display unit
    val weightsDisplay = sessions.map { VolumeAnalytics.kgToDisplay(it.topWeightKg, displayUnit) }
    val minY = (weightsDisplay.minOrNull() ?: 0.0).coerceAtLeast(0.0)
    val maxY = (weightsDisplay.maxOrNull() ?: 0.0)
    // Add a small margin so the top/bottom points aren't flush against the edges
    val span = (maxY - minY).coerceAtLeast(1.0)
    val padded = span * 0.15
    val yLow = (minY - padded).coerceAtLeast(0.0)
    val yHigh = maxY + padded
    val yRange = (yHigh - yLow).coerceAtLeast(0.0001)

    val lineColor = scheme.primary
    val gridColor = scheme.outline.copy(alpha = 0.25f)
    val axisColor = scheme.outline.copy(alpha = 0.6f)
    val tooltipBg = scheme.inverseSurface
    val dotHole = scheme.surface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = scheme.inverseOnSurface,
        fontWeight = FontWeight.SemiBold
    )

    // Y-axis ticks (weight), top to bottom
    val yTickCount = 4
    val yTicks = (0 until yTickCount).map { yHigh - yRange * it / (yTickCount - 1) }
    val yTickLabels = remember(yTicks, labelStyle) {
        yTicks.map { textMeasurer.measure(formatDisplayValue(it), labelStyle) }
    }
    val leftGutterPx = (yTickLabels.maxOfOrNull { it.size.width } ?: 0) +
        with(density) { 8.dp.toPx() }
    val rightPadPx = with(density) { 10.dp.toPx() }

    // X-axis ticks (dates): up to 4 evenly spread sessions
    val n = sessions.size
    val xTickIndices = when {
        n <= 4 -> sessions.indices.toList()
        else -> listOf(0, (n - 1) / 3, (n - 1) * 2 / 3, n - 1).distinct()
    }

    var selected by remember(sessions) { mutableStateOf<Int?>(null) }

    fun indexAt(x: Float, width: Float): Int {
        if (n <= 1) return 0
        val plotW = (width - leftGutterPx - rightPadPx).coerceAtLeast(1f)
        return ((x - leftGutterPx) / plotW * (n - 1)).roundToInt().coerceIn(0, n - 1)
    }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceVariant.copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .pointerInput(n, leftGutterPx) {
                    detectTapGestures { pos ->
                        val idx = indexAt(pos.x, size.width.toFloat())
                        selected = if (selected == idx) null else idx
                    }
                }
                .pointerInput(n, leftGutterPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { pos -> selected = indexAt(pos.x, size.width.toFloat()) }
                    ) { change, _ ->
                        selected = indexAt(change.position.x, size.width.toFloat())
                    }
                }
        ) {
            val topPad = 8.dp.toPx()
            val xLabelHeight = (yTickLabels.firstOrNull()?.size?.height ?: 0).toFloat()
            val bottomGutter = xLabelHeight + 6.dp.toPx()
            val plotLeft = leftGutterPx
            val plotRight = size.width - rightPadPx
            val plotTop = topPad
            val plotBottom = size.height - bottomGutter
            val plotW = plotRight - plotLeft
            val plotH = plotBottom - plotTop

            fun xPx(index: Int): Float =
                if (n <= 1) plotLeft + plotW / 2f else plotLeft + plotW * index / (n - 1)

            fun yPx(value: Double): Float {
                val t = ((value - yLow) / yRange).toFloat().coerceIn(0f, 1f)
                return plotBottom - t * plotH
            }

            // Y gridlines + weight labels
            yTicks.forEachIndexed { i, value ->
                val y = yPx(value)
                drawLine(
                    color = gridColor,
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 0.8.dp.toPx()
                )
                val label = yTickLabels[i]
                drawText(
                    label,
                    topLeft = Offset(
                        plotLeft - 6.dp.toPx() - label.size.width,
                        (y - label.size.height / 2f).coerceIn(0f, size.height - label.size.height)
                    )
                )
            }

            // Axis lines
            drawLine(axisColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), 1.dp.toPx())
            drawLine(axisColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), 1.dp.toPx())

            // X date labels + tick marks
            xTickIndices.forEach { idx ->
                val x = xPx(idx)
                drawLine(axisColor, Offset(x, plotBottom), Offset(x, plotBottom + 3.dp.toPx()), 1.dp.toPx())
                val label = textMeasurer.measure(formatShortDate(sessions[idx].dayStartMs), labelStyle)
                drawText(
                    label,
                    topLeft = Offset(
                        (x - label.size.width / 2f)
                            .coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f)),
                        plotBottom + 5.dp.toPx()
                    )
                )
            }

            // Line
            if (n >= 2) {
                val path = Path()
                weightsDisplay.forEachIndexed { index, v ->
                    if (index == 0) path.moveTo(xPx(index), yPx(v)) else path.lineTo(xPx(index), yPx(v))
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Dots
            weightsDisplay.forEachIndexed { index, v ->
                drawCircle(lineColor, 3.5.dp.toPx(), Offset(xPx(index), yPx(v)))
            }

            // Selection: guideline, highlighted dot, tooltip
            val sel = selected?.takeIf { it in sessions.indices }
            if (sel != null) {
                val session = sessions[sel]
                val px = xPx(sel)
                val py = yPx(weightsDisplay[sel])
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(px, plotTop),
                    end = Offset(px, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
                drawCircle(lineColor, 6.dp.toPx(), Offset(px, py))
                drawCircle(dotHole, 3.dp.toPx(), Offset(px, py))

                val tip = textMeasurer.measure(
                    "${formatLongDate(session.dayStartMs)}\n" +
                        "${VolumeAnalytics.formatWeight(session.topWeightKg, displayUnit)} × ${session.topWeightReps} reps",
                    tooltipStyle
                )
                val padH = 8.dp.toPx()
                val padV = 5.dp.toPx()
                val boxW = tip.size.width + padH * 2
                val boxH = tip.size.height + padV * 2
                val gap = 10.dp.toPx()
                val boxX = (px - boxW / 2f).coerceIn(0f, (size.width - boxW).coerceAtLeast(0f))
                val boxY = if (py - gap - boxH >= 0f) py - gap - boxH else py + gap
                drawRoundRect(
                    color = tooltipBg,
                    topLeft = Offset(boxX, boxY),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawText(tip, topLeft = Offset(boxX + padH, boxY + padV))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap or drag across the chart to see each session",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
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
