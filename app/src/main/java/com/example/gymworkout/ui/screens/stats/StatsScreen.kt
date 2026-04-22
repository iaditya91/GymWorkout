package com.example.gymworkout.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.DailyCheckIn
import com.example.gymworkout.data.WeightEntry
import com.example.gymworkout.viewmodel.DailyScoreBreakdown
import com.example.gymworkout.viewmodel.JourneyData
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
val HabitsColor = Color(0xFFFFB74D)
val JourneyColor = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Journey")

    // Auto-refresh today's check-in whenever the screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshTodayCheckIn()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (selectedTab == 0) "Track your consistency" else "Your transformation journey",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(viewModel)
                1 -> JourneyTab(viewModel)
            }
        }
    }
}

@Composable
fun OverviewTab(viewModel: StatsViewModel) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val checkIns by viewModel.getCheckInsForMonth(selectedMonth).collectAsState(initial = emptyList())
    val todayStr = viewModel.todayString()
    val todayCheckIn by viewModel.getCheckIn(todayStr).collectAsState(initial = null)

    val workoutDays by viewModel.getWorkoutDaysCount(selectedMonth).collectAsState(initial = 0)
    val nutritionDays by viewModel.getNutritionDaysCount(selectedMonth).collectAsState(initial = 0)
    val sleepDays by viewModel.getSleepDaysCount(selectedMonth).collectAsState(initial = 0)
    val habitsDays by viewModel.getHabitsDaysCount(selectedMonth).collectAsState(initial = 0)
    val daysInMonth = selectedMonth.lengthOfMonth()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Today's check-in (automatic, non-interactive)
        item {
            TodayCheckInCard(checkIn = todayCheckIn)
        }

        // Monthly summary bars
        item {
            MonthlyBarChart(
                workoutDays = workoutDays,
                nutritionDays = nutritionDays,
                sleepDays = sleepDays,
                habitsDays = habitsDays,
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
            StreakCard(checkIns = checkIns)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTab(viewModel: StatsViewModel) {
    val journeyData by viewModel.journeyData.collectAsState()
    val weightEntries by viewModel.getWeightEntries().collectAsState(initial = emptyList())
    var showSetupDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadJourneyData()
    }

    if (showSetupDialog) {
        JourneySetupDialog(
            currentProfile = journeyData.profile,
            onDismiss = { showSetupDialog = false },
            onSave = { requiredShape, idealDays ->
                viewModel.saveJourneySetup(requiredShape, idealDays)
                showSetupDialog = false
            }
        )
    }

    if (showWeightDialog) {
        LogWeightDialog(
            defaultUnit = journeyData.profile?.weightUnit?.ifEmpty { "kg" } ?: "kg",
            existingForToday = weightEntries.lastOrNull { it.date == viewModel.todayString() },
            onDismiss = { showWeightDialog = false },
            onSave = { date, weight, unit ->
                viewModel.logWeight(date, weight, unit)
                showWeightDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        if (!journeyData.isSetup) {
            item {
                JourneySetupCard(onSetup = { showSetupDialog = true })
            }
        } else {
            // Progress Card
            item {
                JourneyProgressCard(
                    journeyData = journeyData,
                    onEdit = { showSetupDialog = true }
                )
            }

            // Weight Journey Card with line graph
            item {
                WeightJourneyCard(
                    entries = weightEntries,
                    targetWeight = journeyData.profile?.targetWeight ?: 0f,
                    unit = journeyData.profile?.weightUnit?.ifEmpty { "kg" } ?: "kg",
                    onLogWeight = { showWeightDialog = true },
                    onDeleteEntry = { viewModel.deleteWeightEntry(it) }
                )
            }

            // Today's Score Card
            item {
                TodayScoreCard(score = journeyData.todayScore)
            }

            // Score Breakdown
            item {
                ScoreBreakdownCard(score = journeyData.todayScore)
            }

            // Estimated Timeline
            item {
                TimelineCard(journeyData = journeyData)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ============ Journey Components ============

@Composable
fun JourneySetupCard(onSetup: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = JourneyColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Start Your Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Set your required body shape and track your transformation progress with daily scoring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = onSetup,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(JourneyColor.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp)
            ) {
                Text("Set Up Journey", color = JourneyColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun JourneyProgressCard(journeyData: JourneyData, onEdit: () -> Unit) {
    val profile = journeyData.profile ?: return
    val animatedProgress by animateFloatAsState(
        targetValue = journeyData.shapeProgress,
        label = "progress"
    )

    val shapeLabel = journeyData.requiredShape.replaceFirstChar { it.uppercase() }
    val remaining = (journeyData.estimatedDays - journeyData.daysElapsed).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shape Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Target shape badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(JourneyColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = JourneyColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Target: $shapeLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = JourneyColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar with day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Day 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Day ${journeyData.daysElapsed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Day ${journeyData.estimatedDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JourneyColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = JourneyColor,
                trackColor = JourneyColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${journeyData.daysElapsed} days done",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$remaining days left",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = JourneyColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ideal vs Estimated comparison
            if (journeyData.idealDays != journeyData.estimatedDays) {
                Text(
                    "Ideal: ${journeyData.idealDays} days  |  Your pace: ~${journeyData.estimatedDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Target: ${journeyData.idealDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TodayScoreCard(score: DailyScoreBreakdown) {
    val scorePercent = (score.dmgs * 100).toInt()
    val scoreColor = when {
        scorePercent >= 80 -> Color(0xFF2E7D32)
        scorePercent >= 60 -> Color(0xFFF9A825)
        scorePercent >= 40 -> JourneyColor
        else -> Color(0xFFE53935)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Today's Growth Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when {
                        scorePercent >= 80 -> "Excellent! You're maximizing your gains."
                        scorePercent >= 60 -> "Good effort! A few areas to improve."
                        scorePercent >= 40 -> "Moderate day. Try to hit more targets."
                        scorePercent > 0 -> "Low score today. Tomorrow is a fresh start!"
                        else -> "Log your meals and workout to see your score."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${scorePercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
fun ScoreBreakdownCard(score: DailyScoreBreakdown) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Score Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ScoreRow(
                label = "Protein",
                weight = "35%",
                score = score.proteinScore,
                icon = Icons.Default.Egg,
                color = Color(0xFFEF5350)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScoreRow(
                label = "Calories",
                weight = "20%",
                score = score.caloriesScore,
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFFA726)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScoreRow(
                label = "Workout",
                weight = "20%",
                score = score.workoutScore,
                icon = Icons.Default.FitnessCenter,
                color = WorkoutColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScoreRow(
                label = "Sleep",
                weight = "15%",
                score = score.sleepScore,
                icon = Icons.Default.BedtimeOff,
                color = SleepColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScoreRow(
                label = "Hydration",
                weight = "10%",
                score = score.hydrationScore,
                icon = Icons.Default.WaterDrop,
                color = Color(0xFF42A5F5)
            )
        }
    }
}

@Composable
fun ScoreRow(
    label: String,
    weight: String,
    score: Float,
    icon: ImageVector,
    color: Color
) {
    val animatedScore by animateFloatAsState(targetValue = score, label = "score")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$label ($weight)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TimelineCard(journeyData: JourneyData) {
    val profile = journeyData.profile ?: return
    val weeklyPercent = (journeyData.weeklyAverage * 100).toInt()
    val overallPercent = (journeyData.overallAverage * 100).toInt()

    val progressMeaning = when {
        overallPercent >= 95 -> "Optimal growth"
        overallPercent >= 85 -> "Very good"
        overallPercent >= 75 -> "Moderate progress"
        overallPercent > 0 -> "Slow progress"
        else -> "No data yet"
    }
    val progressColor = when {
        overallPercent >= 85 -> Color(0xFF2E7D32)
        overallPercent >= 75 -> Color(0xFFF9A825)
        overallPercent >= 60 -> JourneyColor
        else -> Color(0xFFE53935)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Progress Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimelineStat(
                    label = "This Week",
                    value = "$weeklyPercent%",
                    subtitle = "avg score",
                    color = when {
                        weeklyPercent >= 85 -> Color(0xFF2E7D32)
                        weeklyPercent >= 75 -> Color(0xFFF9A825)
                        weeklyPercent >= 60 -> JourneyColor
                        else -> Color(0xFFE53935)
                    }
                )
                TimelineStat(
                    label = "Overall",
                    value = "$overallPercent%",
                    subtitle = progressMeaning,
                    color = progressColor
                )
                TimelineStat(
                    label = "Your Pace",
                    value = "${journeyData.estimatedDays}",
                    subtitle = "of ${journeyData.idealDays} days",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (overallPercent > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                val delay = journeyData.estimatedDays - journeyData.idealDays
                Text(
                    if (delay > 0) {
                        "Overall $overallPercent% consistency across ${journeyData.daysElapsed} days. ~$delay extra days needed beyond ideal ${journeyData.idealDays} days."
                    } else {
                        "Overall $overallPercent% consistency. On track to hit your goal in ${journeyData.idealDays} days or sooner!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimelineStat(label: String, value: String, subtitle: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneySetupDialog(
    currentProfile: com.example.gymworkout.data.UserProfile?,
    onDismiss: () -> Unit,
    onSave: (requiredShape: String, idealDays: Int) -> Unit
) {
    var requiredShape by remember {
        mutableStateOf(
            if ((currentProfile?.requiredShape ?: "").isNotEmpty()) currentProfile?.requiredShape ?: "lean"
            else "lean"
        )
    }
    var idealDays by remember {
        mutableStateOf(
            if ((currentProfile?.idealDays ?: 90) > 0) currentProfile?.idealDays?.toString() ?: "90"
            else "90"
        )
    }
    var expanded by remember { mutableStateOf(false) }

    data class ShapeOption(val key: String, val label: String, val description: String, val suggestedDays: Int)
    val shapes = listOf(
        ShapeOption("lean", "Lean", "Low body fat, toned muscles. Good for cutting.", 60),
        ShapeOption("athletic", "Athletic", "Balanced muscle and definition. Fit look.", 90),
        ShapeOption("muscular", "Muscular", "Noticeable muscle mass. Gym-built physique.", 120),
        ShapeOption("bodybuilder", "Bodybuilder", "Maximum muscle size and definition.", 180)
    )
    val selectedShape = shapes.find { it.key == requiredShape } ?: shapes[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Journey Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedShape.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Required Shape") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        shapes.forEach { shape ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(shape.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            shape.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    requiredShape = shape.key
                                    idealDays = shape.suggestedDays.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    selectedShape.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = idealDays,
                    onValueChange = { idealDays = it },
                    label = { Text("Ideal Days to Achieve") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Suggested: ${selectedShape.suggestedDays} days for ${selectedShape.label}")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val days = idealDays.toIntOrNull() ?: 90
                    if (requiredShape.isNotEmpty() && days > 0) {
                        onSave(requiredShape, days)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============ Weight Journey Components ============

val WeightColor = Color(0xFF9C27B0)

@Composable
fun WeightJourneyCard(
    entries: List<WeightEntry>,
    targetWeight: Float,
    unit: String,
    onLogWeight: () -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val sorted = remember(entries) { entries.sortedBy { it.date } }
    val current = sorted.lastOrNull()
    val starting = sorted.firstOrNull()
    val change = if (current != null && starting != null && current != starting) {
        current.weight - starting.weight
    } else 0f

    val trendIcon = when {
        change > 0.1f -> Icons.Default.TrendingUp
        change < -0.1f -> Icons.Default.TrendingDown
        else -> Icons.Default.TrendingFlat
    }
    // Interpretation depends on goal direction: if target < starting, losing weight is "good"
    val isLossGoal = targetWeight > 0f && starting != null && targetWeight < starting.weight
    val isGainGoal = targetWeight > 0f && starting != null && targetWeight > starting.weight
    val trendColor = when {
        change == 0f || current == starting -> MaterialTheme.colorScheme.onSurfaceVariant
        isLossGoal && change < 0f -> Color(0xFF2E7D32)
        isLossGoal && change > 0f -> Color(0xFFE53935)
        isGainGoal && change > 0f -> Color(0xFF2E7D32)
        isGainGoal && change < 0f -> Color(0xFFE53935)
        else -> WeightColor
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = WeightColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Weight Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onLogWeight,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WeightColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Log weight",
                        tint = WeightColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Stats row: Current | Change | Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (current != null) "${formatWeight(current.weight)} ${current.unit}" else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WeightColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (sorted.size >= 2) "${if (change >= 0) "+" else ""}${formatWeight(change)}" else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (targetWeight > 0f) "${formatWeight(targetWeight)} $unit" else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sorted.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WeightColor.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = WeightColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Log your first weight to see your journey graph",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                WeightLineGraph(
                    entries = sorted,
                    targetWeight = targetWeight,
                    unit = unit
                )

                if (sorted.size >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val dateFmt = DateTimeFormatter.ofPattern("MMM d")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            LocalDate.parse(sorted.first().date).format(dateFmt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${sorted.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            LocalDate.parse(sorted.last().date).format(dateFmt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Recent entries list with delete
                Spacer(modifier = Modifier.height(12.dp))
                val recent = sorted.takeLast(5).reversed()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recent.forEach { entry ->
                        val prevEntry = sorted.getOrNull(sorted.indexOf(entry) - 1)
                        val delta = if (prevEntry != null) entry.weight - prevEntry.weight else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${formatWeight(entry.weight)} ${entry.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (prevEntry != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${if (delta >= 0) "+" else ""}${formatWeight(delta)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (delta >= 0f) Color(0xFFE57373) else Color(0xFF66BB6A)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEntry(entry.date) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightLineGraph(
    entries: List<WeightEntry>,
    targetWeight: Float,
    unit: String
) {
    if (entries.isEmpty()) return

    val weights = entries.map { it.weight }
    val minRaw = weights.min()
    val maxRaw = weights.max()
    // Include target weight in range if set
    val minAll = if (targetWeight > 0f) minOf(minRaw, targetWeight) else minRaw
    val maxAll = if (targetWeight > 0f) maxOf(maxRaw, targetWeight) else maxRaw
    // Pad range so points aren't flush with edges
    val pad = ((maxAll - minAll) * 0.15f).coerceAtLeast(0.5f)
    val minY = minAll - pad
    val maxY = maxAll + pad
    val rangeY = (maxY - minY).coerceAtLeast(0.1f)

    val lineColor = WeightColor
    val targetColor = JourneyColor
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val leftPad = 40f
            val rightPad = 12f
            val topPad = 12f
            val bottomPad = 12f
            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad

            // Horizontal grid lines (4 divisions)
            val divisions = 4
            for (i in 0..divisions) {
                val y = topPad + chartH * i / divisions
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f
                )
            }

            // Target line
            if (targetWeight > 0f) {
                val targetNorm = (targetWeight - minY) / rangeY
                val targetYPx = topPad + chartH * (1f - targetNorm)
                val dashLen = 10f
                var x = leftPad
                while (x < size.width - rightPad) {
                    drawLine(
                        color = targetColor,
                        start = Offset(x, targetYPx),
                        end = Offset(minOf(x + dashLen, size.width - rightPad), targetYPx),
                        strokeWidth = 2f
                    )
                    x += dashLen * 2
                }
            }

            // Compute point positions
            val n = entries.size
            val points = entries.mapIndexed { idx, entry ->
                val xFrac = if (n == 1) 0.5f else idx.toFloat() / (n - 1)
                val yFrac = (entry.weight - minY) / rangeY
                Offset(
                    x = leftPad + chartW * xFrac,
                    y = topPad + chartH * (1f - yFrac)
                )
            }

            // Line path
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (p in points.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }

            // Data points
            for (p in points) {
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = p
                )
            }
        }

        // Y-axis labels (min / max weight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${formatWeight(minAll)} $unit",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            if (targetWeight > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(targetColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Target ${formatWeight(targetWeight)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
            Text(
                "${formatWeight(maxAll)} $unit",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWeightDialog(
    defaultUnit: String,
    existingForToday: WeightEntry?,
    onDismiss: () -> Unit,
    onSave: (date: String, weight: Float, unit: String) -> Unit
) {
    val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    var weightText by remember {
        mutableStateOf(existingForToday?.weight?.let { formatWeight(it) } ?: "")
    }
    var unit by remember { mutableStateOf(existingForToday?.unit?.ifEmpty { defaultUnit } ?: defaultUnit) }
    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("kg", "lb")

    val parsed = weightText.toFloatOrNull()
    val valid = parsed != null && parsed > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingForToday != null) "Update Today's Weight" else "Log Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Date: ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { new ->
                        // Allow digits and a single dot
                        if (new.matches(Regex("^\\d*\\.?\\d*$"))) weightText = new
                    },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded }
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    unit = u
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid) onSave(todayStr, parsed!!, unit)
                },
                enabled = valid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatWeight(value: Float): String {
    // 1 decimal, strip trailing .0
    val rounded = (kotlin.math.round(value * 10f) / 10f)
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString()
    else "%.1f".format(rounded)
}

// ============ Original Overview Components ============

@Composable
fun TodayCheckInCard(
    checkIn: DailyCheckIn?
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Auto-tracked from your activity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = WorkoutColor
                )
                CheckInItem(
                    icon = Icons.Default.Restaurant,
                    label = "Nutrition",
                    checked = checkIn?.nutritionDone ?: false,
                    color = NutritionColor
                )
                CheckInItem(
                    icon = Icons.Default.BedtimeOff,
                    label = "Sleep",
                    checked = checkIn?.sleepDone ?: false,
                    color = SleepColor
                )
                CheckInItem(
                    icon = Icons.Default.Checklist,
                    label = "Habits",
                    checked = checkIn?.habitsDone ?: false,
                    color = HabitsColor
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
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
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
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (checked) color else MaterialTheme.colorScheme.surfaceVariant
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
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
    habitsDays: Int,
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
                val barCount = 4
                val spacing = size.width / (barCount + 1)
                val actualBarWidth = spacing * 0.55f

                data class BarData(val label: String, val value: Int, val color: Color)
                val bars = listOf(
                    BarData("Workout", workoutDays, WorkoutColor),
                    BarData("Nutrition", nutritionDays, NutritionColor),
                    BarData("Sleep", sleepDays, SleepColor),
                    BarData("Habits", habitsDays, HabitsColor)
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
                StatLabel("Habits", "$habitsDays/$totalDays", HabitsColor)
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
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
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
                Spacer(modifier = Modifier.width(8.dp))
                LegendDot(NutritionColor, "Nutrition")
                Spacer(modifier = Modifier.width(8.dp))
                LegendDot(SleepColor, "Sleep")
                Spacer(modifier = Modifier.width(8.dp))
                LegendDot(HabitsColor, "Habits")
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
    val habits = checkIn?.habitsDone ?: false
    val allDone = workout && nutrition && sleep && habits

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    allDone -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                    workout || nutrition || sleep || habits -> MaterialTheme.colorScheme.surfaceVariant
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
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                if (workout) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(WorkoutColor)
                    )
                }
                if (nutrition) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(NutritionColor)
                    )
                }
                if (sleep) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(SleepColor)
                    )
                }
                if (habits) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(HabitsColor)
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
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}

@Composable
fun StreakCard(checkIns: List<DailyCheckIn>) {
    // Calculate current streak (consecutive days with all 4 done)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    var streak = 0
    var date = today
    val checkInMap = checkIns.associateBy { it.date }

    while (true) {
        val dateStr = date.format(formatter)
        val ci = checkInMap[dateStr]
        if (ci != null && ci.workoutDone && ci.nutritionDone && ci.sleepDone && ci.habitsDone) {
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
                    if (streak > 0) "Keep it going! All 4 targets for $streak day${if (streak > 1) "s" else ""} straight."
                    else "Complete all 4 check-ins today to start your streak!",
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
