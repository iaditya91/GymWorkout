package com.example.gymworkout.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.DailyCheckIn
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
val JourneyColor = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Journey")

    Scaffold(
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
    val daysInMonth = selectedMonth.lengthOfMonth()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTab(viewModel: StatsViewModel) {
    val journeyData by viewModel.journeyData.collectAsState()
    var showSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadJourneyData()
    }

    if (showSetupDialog) {
        JourneySetupDialog(
            currentProfile = journeyData.profile,
            onDismiss = { showSetupDialog = false },
            onSave = { targetWeight, startingWeight, fitnessLevel ->
                viewModel.saveJourneySetup(targetWeight, startingWeight, fitnessLevel)
                showSetupDialog = false
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
                "Set your target body weight and track your transformation progress with daily scoring.",
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
        targetValue = journeyData.weightProgress,
        label = "progress"
    )

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
                    "Transformation Progress",
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
            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar with labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${profile.startingWeight} ${profile.weightUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${profile.weight} ${profile.weightUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = JourneyColor
                    )
                    Text(
                        "${profile.targetWeight} ${profile.weightUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = JourneyColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

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

            Text(
                "${(journeyData.weightProgress * 100).toInt()}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (journeyData.daysElapsed > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Day ${journeyData.daysElapsed} of your journey",
                    style = MaterialTheme.typography.labelSmall,
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

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Estimated Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimelineStat(
                    label = "Weekly Avg",
                    value = "$weeklyPercent%",
                    subtitle = "efficiency",
                    color = when {
                        weeklyPercent >= 80 -> Color(0xFF2E7D32)
                        weeklyPercent >= 60 -> Color(0xFFF9A825)
                        else -> JourneyColor
                    }
                )
                TimelineStat(
                    label = "Est. Days",
                    value = "${journeyData.estimatedDays}",
                    subtitle = "to target",
                    color = MaterialTheme.colorScheme.primary
                )
                TimelineStat(
                    label = "Elapsed",
                    value = "${journeyData.daysElapsed}",
                    subtitle = "days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (journeyData.daysElapsed > 0 && journeyData.estimatedDays > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val timeProgress = (journeyData.daysElapsed.toFloat() / journeyData.estimatedDays).coerceIn(0f, 1f)
                val animatedTimeProgress by animateFloatAsState(targetValue = timeProgress, label = "time")

                LinearProgressIndicator(
                    progress = { animatedTimeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))

                val remaining = (journeyData.estimatedDays - journeyData.daysElapsed).coerceAtLeast(0)
                Text(
                    if (remaining > 0) "$remaining days remaining (estimated)"
                    else "You've passed your estimated timeline!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (weeklyPercent > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val baseDays = when (profile.fitnessLevel) {
                    "beginner" -> 90
                    "intermediate" -> 120
                    "advanced" -> 180
                    else -> 90
                }
                Text(
                    "Base time: $baseDays days (${profile.fitnessLevel}). " +
                            "At $weeklyPercent% efficiency, adjusted to ~${journeyData.estimatedDays} days.",
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
    onSave: (targetWeight: Float, startingWeight: Float, fitnessLevel: String) -> Unit
) {
    var targetWeight by remember {
        mutableStateOf(
            if ((currentProfile?.targetWeight ?: 0f) > 0f) currentProfile?.targetWeight.toString() else ""
        )
    }
    var startingWeight by remember {
        mutableStateOf(
            if ((currentProfile?.startingWeight ?: 0f) > 0f) currentProfile?.startingWeight.toString()
            else if ((currentProfile?.weight ?: 0f) > 0f) currentProfile?.weight.toString()
            else ""
        )
    }
    var fitnessLevel by remember {
        mutableStateOf(currentProfile?.fitnessLevel ?: "beginner")
    }
    var expanded by remember { mutableStateOf(false) }
    val levels = listOf("beginner", "intermediate", "advanced")
    val weightUnit = currentProfile?.weightUnit ?: "kg"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Journey Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startingWeight,
                    onValueChange = { startingWeight = it },
                    label = { Text("Starting Weight ($weightUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = { Text("Target Weight ($weightUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = fitnessLevel.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fitness Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        levels.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    fitnessLevel = level
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    when (fitnessLevel) {
                        "beginner" -> "0-1 year training. ~1-1.5% bodyweight muscle gain/month. Base: 90 days."
                        "intermediate" -> "1-3 years training. ~0.5-1% gain/month. Base: 120 days."
                        "advanced" -> "3+ years training. ~0.25-0.5% gain/month. Base: 180 days."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val tw = targetWeight.toFloatOrNull() ?: 0f
                    val sw = startingWeight.toFloatOrNull() ?: 0f
                    if (tw > 0f && sw > 0f) {
                        onSave(tw, sw, fitnessLevel)
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

// ============ Original Overview Components ============

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
