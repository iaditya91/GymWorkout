package com.example.gymworkout.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.viewmodel.StatsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val WorkoutColor = Color(0xFF6C9FFF)
val NutritionColor = Color(0xFF66BB6A)
val SleepColor = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val checkIns by viewModel.getCheckInsForMonth(selectedMonth).collectAsState(initial = emptyList())
    val todayStr = viewModel.todayString()
    val todayCheckIn by viewModel.getCheckIn(todayStr).collectAsState(initial = null)

    val workoutDays by viewModel.getWorkoutDaysCount(selectedMonth).collectAsState(initial = 0)
    val nutritionDays by viewModel.getNutritionDaysCount(selectedMonth).collectAsState(initial = 0)
    val sleepDays by viewModel.getSleepDaysCount(selectedMonth).collectAsState(initial = 0)
    val daysInMonth = selectedMonth.lengthOfMonth()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Track your consistency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's check-in
            item {
                TodayCheckInCard(
                    todayStr = todayStr,
                    checkIn = todayCheckIn,
                    onToggleWorkout = { viewModel.toggleWorkout(todayStr, todayCheckIn) },
                    onToggleNutrition = { viewModel.toggleNutrition(todayStr, todayCheckIn) },
                    onToggleSleep = { viewModel.toggleSleep(todayStr, todayCheckIn) }
                )
            }

            // Monthly summary bars
            item {
                MonthlyBarChart(
                    workoutDays = workoutDays,
                    nutritionDays = nutritionDays,
                    sleepDays = sleepDays,
                    totalDays = daysInMonth,
                    monthLabel = selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                )
            }

            // Calendar
            item {
                CalendarCard(
                    yearMonth = selectedMonth,
                    checkIns = checkIns,
                    onPrev = { viewModel.prevMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            // Streak info
            item {
                StreakCard(checkIns = checkIns, viewModel = viewModel)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TodayCheckInCard(
    todayStr: String,
    checkIn: DailyCheckIn?,
    onToggleWorkout: () -> Unit,
    onToggleNutrition: () -> Unit,
    onToggleSleep: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Today's Check-in",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CheckInItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "Workout",
                    checked = checkIn?.workoutDone ?: false,
                    color = WorkoutColor,
                    onToggle = onToggleWorkout
                )
                CheckInItem(
                    icon = Icons.Default.Restaurant,
                    label = "Nutrition",
                    checked = checkIn?.nutritionDone ?: false,
                    color = NutritionColor,
                    onToggle = onToggleNutrition
                )
                CheckInItem(
                    icon = Icons.Default.BedtimeOff,
                    label = "Sleep",
                    checked = checkIn?.sleepDone ?: false,
                    color = SleepColor,
                    onToggle = onToggleSleep
                )
            }
        }
    }
}

@Composable
fun CheckInItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (checked) color.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (checked) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = color),
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MonthlyBarChart(
    workoutDays: Int,
    nutritionDays: Int,
    sleepDays: Int,
    totalDays: Int,
    monthLabel: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$monthLabel Consistency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val maxHeight = size.height - 30f
                val barCount = 3
                // Match SpaceEvenly: centers at width/(barCount+1), 2*width/(barCount+1), ...
                val spacing = size.width / (barCount + 1)
                val actualBarWidth = spacing * 0.6f

                data class BarData(val label: String, val value: Int, val color: Color)
                val bars = listOf(
                    BarData("Workout", workoutDays, WorkoutColor),
                    BarData("Nutrition", nutritionDays, NutritionColor),
                    BarData("Sleep", sleepDays, SleepColor)
                )

                bars.forEachIndexed { i, bar ->
                    val barHeight = if (totalDays > 0) (bar.value.toFloat() / totalDays) * maxHeight else 0f
                    val centerX = spacing * (i + 1)
                    val x = centerX - actualBarWidth / 2

                    // Background bar
                    drawRoundRect(
                        color = bar.color.copy(alpha = 0.1f),
                        topLeft = Offset(x, 0f),
                        size = Size(actualBarWidth, maxHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Value bar
                    drawRoundRect(
                        color = bar.color,
                        topLeft = Offset(x, maxHeight - barHeight),
                        size = Size(actualBarWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatLabel("Workout", "$workoutDays/$totalDays", WorkoutColor)
                StatLabel("Nutrition", "$nutritionDays/$totalDays", NutritionColor)
                StatLabel("Sleep", "$sleepDays/$totalDays", SleepColor)
            }
        }
    }
}

@Composable
fun StatLabel(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CalendarCard(
    yearMonth: YearMonth,
    checkIns: List<DailyCheckIn>,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val checkInMap = checkIns.associateBy { it.date }
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    // Monday = 1
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon ... 7=Sun
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month")
                }
                Text(
                    text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            val totalCells = startDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - (startDayOfWeek - 1) + 1

                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum).format(formatter)
                            val checkIn = checkInMap[date]
                            val today = date == LocalDate.now().format(formatter)

                            CalendarDayCell(
                                day = dayNum,
                                checkIn = checkIn,
                                isToday = today,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(WorkoutColor, "Workout")
                Spacer(modifier = Modifier.width(12.dp))
                LegendDot(NutritionColor, "Nutrition")
                Spacer(modifier = Modifier.width(12.dp))
                LegendDot(SleepColor, "Sleep")
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    checkIn: DailyCheckIn?,
    isToday: Boolean,
    modifier: Modifier
) {
    val workout = checkIn?.workoutDone ?: false
    val nutrition = checkIn?.nutritionDone ?: false
    val sleep = checkIn?.sleepDone ?: false
    val allDone = workout && nutrition && sleep

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    allDone -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                    workout || nutrition || sleep -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
            // Color dots
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (workout) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(WorkoutColor)
                    )
                }
                if (nutrition) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(NutritionColor)
                    )
                }
                if (sleep) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(SleepColor)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StreakCard(checkIns: List<DailyCheckIn>, viewModel: StatsViewModel) {
    // Calculate current streak (consecutive days with all 3 done)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    var streak = 0
    var date = today
    val checkInMap = checkIns.associateBy { it.date }

    while (true) {
        val dateStr = date.format(formatter)
        val ci = checkInMap[dateStr]
        if (ci != null && ci.workoutDone && ci.nutritionDone && ci.sleepDone) {
            streak++
            date = date.minusDays(1)
        } else {
            break
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (streak > 0)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Current Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (streak > 0) "Keep it going! All 3 targets for $streak day${if (streak > 1) "s" else ""} straight."
                    else "Complete all 3 check-ins today to start your streak!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (streak > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$streak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (streak > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
