package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.DescriptionChecklistItem
import com.example.gymworkout.data.HabitDescription
import com.example.gymworkout.data.JournalEntry
import com.example.gymworkout.ui.components.JournalEntryDialog
import com.example.gymworkout.viewmodel.NutritionViewModel

private val HabitGreen = Color(0xFF4CAF50)
private val HabitOrange = Color(0xFFFFA726)
private val HabitRed = Color(0xFFEF5350)

private data class GuidanceTemplate(val title: String, val examples: List<String>)

private val cueTemplates = listOf(
    GuidanceTemplate("Habit Stacking", listOf(
        "After I [CURRENT HABIT], I will [NEW HABIT].",
        "After I pour my morning coffee, I will meditate for 1 minute.",
        "After I sit down for dinner, I will say one thing I'm grateful for."
    )),
    GuidanceTemplate("Environment Design", listOf(
        "Put your running shoes next to the bed.",
        "Leave your journal on your pillow.",
        "Set out workout clothes the night before."
    )),
    GuidanceTemplate("Time & Location", listOf(
        "I will [HABIT] at [TIME] in [LOCATION].",
        "I will exercise at 7 AM in my home gym.",
        "I will read for 20 minutes at 9 PM in bed."
    ))
)

private val cravingTemplates = listOf(
    GuidanceTemplate("Temptation Bundling", listOf(
        "After I [HABIT I NEED], I get to [HABIT I WANT].",
        "After I finish my workout, I get to watch my show.",
        "While I do cardio, I listen to my favorite podcast."
    )),
    GuidanceTemplate("Motivation Ritual", listOf(
        "Listen to an energizing song before starting.",
        "Watch a 30-second motivational clip.",
        "Do 3 deep breaths and visualize success."
    )),
    GuidanceTemplate("Social Influence", listOf(
        "Join a group where this habit is the norm.",
        "Find an accountability partner.",
        "Share your progress with a friend."
    ))
)

private val responseTemplates = listOf(
    GuidanceTemplate("Two-Minute Rule", listOf(
        "Scale down to just 2 minutes to start.",
        "\"Read before bed\" \u2192 Read one page.",
        "\"Run 3 miles\" \u2192 Put on running shoes.",
        "\"Study for class\" \u2192 Open your notes."
    )),
    GuidanceTemplate("Reduce Friction", listOf(
        "Prepare everything in advance.",
        "Remove steps between you and the habit.",
        "Use the 'gateway habit' \u2014 the smallest version."
    )),
    GuidanceTemplate("Commitment Device", listOf(
        "Ask someone to check on you.",
        "Pay a friend if you skip.",
        "Use an app that locks distractions."
    ))
)

private val rewardTemplates = listOf(
    GuidanceTemplate("Immediate Rewards", listOf(
        "Treat yourself to something small right after.",
        "Check it off visually (this tracker!).",
        "Say 'I'm the type of person who...' after finishing."
    )),
    GuidanceTemplate("Habit Tracking", listOf(
        "Never miss twice \u2014 if you miss one day, get back immediately.",
        "Use this streak tracker to stay motivated.",
        "Review your weekly completion percentage."
    )),
    GuidanceTemplate("Identity Reinforcement", listOf(
        "Each rep is a vote for your new identity.",
        "\"I'm a runner\" not \"I'm trying to run\".",
        "Celebrate small wins to wire the identity."
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    categoryKey: String,
    viewModel: NutritionViewModel,
    onBack: () -> Unit,
    onChatClick: (() -> Unit)? = null
) {
    val targetState by viewModel.getTarget(categoryKey).collectAsState(initial = null)
    val atomicHabit by viewModel.getAtomicHabit(categoryKey).collectAsState(initial = null)
    val today = viewModel.todayString()

    val historyResult by viewModel.getHabitHistoryDetailed(categoryKey, 49)
        .collectAsState(initial = null)

    val journalEntries by viewModel.getJournalEntriesForCategory(categoryKey)
        .collectAsState(initial = emptyList())

    var editingField by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var showGuidance by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var frameworkExpanded by remember { mutableStateOf(true) }
    var journalExpanded by remember { mutableStateOf(true) }
    var descriptionExpanded by remember { mutableStateOf(true) }
    var journalDialogEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var addJournalForDate by remember { mutableStateOf<String?>(null) }
    var deleteJournalConfirm by remember { mutableStateOf<JournalEntry?>(null) }
    var calendarMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var selectedJournalDate by remember { mutableStateOf(today) }

    val scoreChecklistItems = remember(targetState?.description, targetState?.descriptionMode) {
        val mode = targetState?.descriptionMode ?: "text"
        if (mode != "checklist") emptyList()
        else {
            HabitDescription.parse(mode, targetState?.description ?: "")
                .items.map { it.text }.filter { it.isNotBlank() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        targetState?.label ?: "Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onChatClick != null) {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = onChatClick,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Chat", modifier = Modifier.size(22.dp))
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = { viewModel.markHabitDone(today, categoryKey) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === QUICK STATS (like ExerciseDetail stat chips) ===
            historyResult?.let { result ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChip(
                            icon = Icons.Filled.LocalFireDepartment,
                            label = "Streak",
                            value = "${result.currentStreak}",
                            color = if (result.currentStreak > 0) HabitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = "Best",
                            value = "${result.longestStreak}",
                            color = HabitOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Filled.TaskAlt,
                            label = "Done",
                            value = "${result.totalMet}/${result.totalDays}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Filled.AutoGraph,
                            label = "Rate",
                            value = "${result.completionPercent}%",
                            color = when {
                                result.completionPercent >= 80 -> HabitGreen
                                result.completionPercent >= 50 -> HabitOrange
                                else -> HabitRed
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // === HEATMAP CARD ===
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Filled.GridView, title = "Last 7 Weeks")
                        Spacer(modifier = Modifier.height(12.dp))
                        HabitHeatmapContent(
                            days = result.days,
                            selectedIndex = selectedDayIndex,
                            onDayClick = { idx ->
                                selectedDayIndex = if (selectedDayIndex == idx) null else idx
                            }
                        )
                        // Legend
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { level ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (level == 0f) MaterialTheme.colorScheme.surfaceVariant
                                            else HabitGreen.copy(alpha = 0.25f + level * 0.75f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Interactive day detail
                selectedDayIndex?.let { idx ->
                    val day = result.days.getOrNull(idx)
                    if (day != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (day.met) HabitGreen.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date circle
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (day.met) HabitGreen.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.date.substring(8),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (day.met) HabitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        day.date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        if (day.target > 0f) "${"%.1f".format(day.value)} / ${"%.1f".format(day.target)} ${targetState?.unit ?: ""}"
                                        else if (day.met) "Completed" else "Not done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (day.met) HabitGreen.copy(alpha = 0.15f)
                                            else HabitRed.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (day.met) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (day.met) HabitGreen else HabitRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // === WEEKLY BAR CHART ===
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Filled.BarChart, title = "This Week")
                        Spacer(modifier = Modifier.height(12.dp))
                        WeeklyBarChartContent(
                            days = result.days.takeLast(7),
                            unit = targetState?.unit ?: ""
                        )
                    }
                }
            }

            // === ATOMIC HABITS FRAMEWORK ===
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { frameworkExpanded = !frameworkExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Atomic Habits Framework",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "4 Laws of Behavior Change",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (frameworkExpanded) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = frameworkExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val habit = atomicHabit
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Spacer(modifier = Modifier.height(4.dp))

                            AtomicHabitFieldCard(
                                law = "1st Law", title = "Cue \u2014 Make it obvious",
                                value = habit?.cue ?: "",
                                isEditing = editingField == "cue",
                                editText = if (editingField == "cue") editText else "",
                                onEditClick = { editingField = "cue"; editText = habit?.cue ?: "" },
                                onEditTextChange = { editText = it },
                                onSave = {
                                    viewModel.saveAtomicHabit(categoryKey, cue = editText, craving = habit?.craving ?: "", response = habit?.response ?: "", reward = habit?.reward ?: "")
                                    editingField = null
                                },
                                onCancel = { editingField = null },
                                onDelete = { viewModel.saveAtomicHabit(categoryKey, cue = "", craving = habit?.craving ?: "", response = habit?.response ?: "", reward = habit?.reward ?: "") },
                                onGuidanceClick = { showGuidance = "cue" },
                                accentColor = Color(0xFF2196F3)
                            )

                            AtomicHabitFieldCard(
                                law = "2nd Law", title = "Craving \u2014 Make it attractive",
                                value = habit?.craving ?: "",
                                isEditing = editingField == "craving",
                                editText = if (editingField == "craving") editText else "",
                                onEditClick = { editingField = "craving"; editText = habit?.craving ?: "" },
                                onEditTextChange = { editText = it },
                                onSave = {
                                    viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = editText, response = habit?.response ?: "", reward = habit?.reward ?: "")
                                    editingField = null
                                },
                                onCancel = { editingField = null },
                                onDelete = { viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = "", response = habit?.response ?: "", reward = habit?.reward ?: "") },
                                onGuidanceClick = { showGuidance = "craving" },
                                accentColor = Color(0xFFE91E63)
                            )

                            AtomicHabitFieldCard(
                                law = "3rd Law", title = "Response \u2014 Make it easy",
                                value = habit?.response ?: "",
                                isEditing = editingField == "response",
                                editText = if (editingField == "response") editText else "",
                                onEditClick = { editingField = "response"; editText = habit?.response ?: "" },
                                onEditTextChange = { editText = it },
                                onSave = {
                                    viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = habit?.craving ?: "", response = editText, reward = habit?.reward ?: "")
                                    editingField = null
                                },
                                onCancel = { editingField = null },
                                onDelete = { viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = habit?.craving ?: "", response = "", reward = habit?.reward ?: "") },
                                onGuidanceClick = { showGuidance = "response" },
                                accentColor = Color(0xFF9C27B0)
                            )

                            AtomicHabitFieldCard(
                                law = "4th Law", title = "Reward \u2014 Make it satisfying",
                                value = habit?.reward ?: "",
                                isEditing = editingField == "reward",
                                editText = if (editingField == "reward") editText else "",
                                onEditClick = { editingField = "reward"; editText = habit?.reward ?: "" },
                                onEditTextChange = { editText = it },
                                onSave = {
                                    viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = habit?.craving ?: "", response = habit?.response ?: "", reward = editText)
                                    editingField = null
                                },
                                onCancel = { editingField = null },
                                onDelete = { viewModel.saveAtomicHabit(categoryKey, cue = habit?.cue ?: "", craving = habit?.craving ?: "", response = habit?.response ?: "", reward = "") },
                                onGuidanceClick = { showGuidance = "reward" },
                                accentColor = Color(0xFFFF9800)
                            )

                            if (habit != null && (habit.cue.isNotEmpty() || habit.craving.isNotEmpty() || habit.response.isNotEmpty() || habit.reward.isNotEmpty())) {
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitRed)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clear All Habit Design")
                                }
                            }
                        }
                    }
                }
            }

            // === DESCRIPTION ===
            DescriptionCard(
                categoryKey = categoryKey,
                expanded = descriptionExpanded,
                onToggleExpand = { descriptionExpanded = !descriptionExpanded },
                rawDescription = targetState?.description ?: "",
                mode = targetState?.descriptionMode ?: "text",
                onSave = { desc -> viewModel.updateDescription(categoryKey, desc) }
            )

            // === JOURNAL ===
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { journalExpanded = !journalExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Journal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (journalEntries.isEmpty()) "Record how today went"
                                else "${journalEntries.size} ${if (journalEntries.size == 1) "entry" else "entries"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { addJournalForDate = selectedJournalDate },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add journal entry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            if (journalExpanded) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = journalExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))

                            JournalCalendar(
                                month = calendarMonth,
                                entries = journalEntries,
                                today = today,
                                selectedDate = selectedJournalDate,
                                onPrevMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                                onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                                onDayClick = { dateStr ->
                                    selectedJournalDate = dateStr
                                }
                            )

                            val entriesForSelected = journalEntries.filter { it.date == selectedJournalDate }
                            val selectedLabel = remember(selectedJournalDate, today) {
                                try {
                                    val d = java.time.LocalDate.parse(selectedJournalDate)
                                    val t = java.time.LocalDate.parse(today)
                                    when {
                                        d == t -> "Today"
                                        d == t.minusDays(1) -> "Yesterday"
                                        else -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                    }
                                } catch (_: Exception) { selectedJournalDate }
                            }

                            if (entriesForSelected.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                        .clickable { addJournalForDate = selectedJournalDate }
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.Book,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No entry for $selectedLabel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Tap to add one",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    selectedLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                entriesForSelected.forEach { entry ->
                                    JournalEntryRow(
                                        entry = entry,
                                        onClick = { journalDialogEntry = entry },
                                        onDelete = { deleteJournalConfirm = entry }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacer for FAB clearance
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    addJournalForDate?.let { dateForNewEntry ->
        JournalEntryDialog(
            objectiveLabel = targetState?.label ?: "Objective",
            dateLabel = dateForNewEntry,
            initialMood = "",
            initialText = "",
            checklistItems = scoreChecklistItems,
            initialScoreChecked = 0,
            initialScoreTotal = 0,
            onDismiss = { addJournalForDate = null },
            onSave = { mood, text, scoreChecked, scoreTotal ->
                viewModel.saveJournalEntry(
                    category = categoryKey,
                    date = dateForNewEntry,
                    mood = mood,
                    text = text,
                    scoreChecked = scoreChecked,
                    scoreTotal = scoreTotal
                )
                addJournalForDate = null
            }
        )
    }

    journalDialogEntry?.let { entry ->
        JournalEntryDialog(
            objectiveLabel = targetState?.label ?: "Objective",
            dateLabel = entry.date,
            initialMood = entry.mood,
            initialText = entry.text,
            checklistItems = scoreChecklistItems,
            initialScoreChecked = entry.scoreChecked,
            initialScoreTotal = entry.scoreTotal,
            onDismiss = { journalDialogEntry = null },
            onSave = { mood, text, scoreChecked, scoreTotal ->
                viewModel.saveJournalEntry(
                    category = categoryKey,
                    date = entry.date,
                    mood = mood,
                    text = text,
                    existingId = entry.id,
                    existingCreatedAt = entry.createdAt,
                    scoreChecked = scoreChecked,
                    scoreTotal = scoreTotal
                )
                journalDialogEntry = null
            }
        )
    }

    deleteJournalConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteJournalConfirm = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove this journal entry from ${entry.date}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteJournalEntry(entry)
                    deleteJournalConfirm = null
                }) { Text("Delete", color = HabitRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteJournalConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // Guidance dialog
    showGuidance?.let { field ->
        val templates = when (field) {
            "cue" -> cueTemplates; "craving" -> cravingTemplates
            "response" -> responseTemplates; "reward" -> rewardTemplates
            else -> emptyList()
        }
        val fieldTitle = when (field) {
            "cue" -> "Cue \u2014 Make it obvious"; "craving" -> "Craving \u2014 Make it attractive"
            "response" -> "Response \u2014 Make it easy"; "reward" -> "Reward \u2014 Make it satisfying"
            else -> ""
        }
        GuidanceDialog(title = fieldTitle, templates = templates, onDismiss = { showGuidance = null },
            onSelect = { text -> editingField = field; editText = text; showGuidance = null })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear Habit Design?") },
            text = { Text("This will remove all cue, craving, response, and reward entries for this habit.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAtomicHabit(categoryKey); showDeleteConfirm = false }) {
                    Text("Clear", color = HabitRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

// ─── Reusable Components (matching ExerciseDetailScreen style) ───

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitHeatmapContent(
    days: List<NutritionViewModel.HabitDayInfo>,
    selectedIndex: Int?,
    onDayClick: (Int) -> Unit
) {
    if (days.isEmpty()) return

    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val firstDayOfWeek = try {
        java.time.LocalDate.parse(days.first().date).dayOfWeek.value
    } catch (_: Exception) { 1 }

    val paddedDays = MutableList<NutritionViewModel.HabitDayInfo?>(firstDayOfWeek - 1) { null } + days
    val weeks = paddedDays.chunked(7)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { week ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val fullWeek = week + List(7 - week.size) { null }
                    fullWeek.forEach { dayInfo ->
                        if (dayInfo == null) {
                            Box(modifier = Modifier.size(26.dp))
                        } else {
                            val globalIndex = days.indexOf(dayInfo)
                            val isSelected = selectedIndex == globalIndex
                            val intensity = if (dayInfo.target > 0f && dayInfo.met) (dayInfo.value / dayInfo.target).coerceIn(0f, 1f)
                            else if (dayInfo.met) 1f else 0f
                            val bgColor by animateColorAsState(
                                targetValue = if (intensity > 0f) HabitGreen.copy(alpha = 0.25f + intensity * 0.75f) else surfaceVariant,
                                label = "heatCell"
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(bgColor)
                                    .then(if (isSelected) Modifier.border(2.dp, primaryColor, RoundedCornerShape(5.dp)) else Modifier)
                                    .clickable { if (globalIndex >= 0) onDayClick(globalIndex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayInfo.met) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChartContent(
    days: List<NutritionViewModel.HabitDayInfo>,
    unit: String
) {
    if (days.isEmpty()) return
    val maxVal = days.maxOf { maxOf(it.value, it.target) }.coerceAtLeast(1f)
    val dayAbbreviations = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val fraction by animateFloatAsState(
                targetValue = (day.value / maxVal).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, delayMillis = index * 80),
                label = "bar$index"
            )
            val targetFraction = (day.target / maxVal).coerceIn(0f, 1f)
            val barColor = when {
                day.met -> HabitGreen
                day.value > 0f -> HabitOrange
                else -> surfaceVariant
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (day.value > 0f) {
                    Text(
                        if (day.value == day.value.toLong().toFloat()) "${day.value.toLong()}" else "${"%.1f".format(day.value)}",
                        style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = barColor, fontWeight = FontWeight.Bold
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    if (day.target > 0f) {
                        Box(modifier = Modifier.fillMaxWidth().height((100 * targetFraction).dp)) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)) {
                                val dashW = 4.dp.toPx(); val gapW = 3.dp.toPx(); var x = 0f
                                while (x < size.width) {
                                    drawLine(Color.Gray.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset((x + dashW).coerceAtMost(size.width), 0f), strokeWidth = 2f)
                                    x += dashW + gapW
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height((100 * fraction).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            val dow = try { java.time.LocalDate.parse(day.date).dayOfWeek.value - 1 } catch (_: Exception) { 0 }
            Text(dayAbbreviations.getOrElse(dow) { "" }, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (unit.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text("Dashed line = target ($unit)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AtomicHabitFieldCard(
    law: String, title: String, value: String, isEditing: Boolean, editText: String,
    onEditClick: () -> Unit, onEditTextChange: (String) -> Unit, onSave: () -> Unit,
    onCancel: () -> Unit, onDelete: () -> Unit, onGuidanceClick: () -> Unit, accentColor: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Law pill badge (like muscle chips in ExerciseDetail)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(law, style = MaterialTheme.typography.labelLarge, color = accentColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = onGuidanceClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Lightbulb, contentDescription = "Guidance", tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isEditing) {
            OutlinedTextField(
                value = editText, onValueChange = onEditTextChange,
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                placeholder = { Text("Describe your strategy...") },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Save") }
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        } else if (value.isNotEmpty()) {
            // Value in a meta-row style box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp)
            ) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Edit pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .clickable { onEditClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = accentColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
                }
                // Delete pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HabitRed.copy(alpha = 0.08f))
                        .clickable { onDelete() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = HabitRed.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelSmall, color = HabitRed.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                    .clickable { onEditClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Tap to add your strategy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DescriptionCard(
    categoryKey: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    rawDescription: String,
    mode: String,
    onSave: (HabitDescription) -> Unit
) {
    val parsed = remember(rawDescription, mode) { HabitDescription.parse(mode, rawDescription) }

    // Drafts are keyed on the category AND the loaded raw value/mode so that the
    // initial null emission of the target Flow doesn't lock the drafts to empty
    // values (which would then overwrite the real saved data on the next save).
    // Saving goes through onSave -> DB -> Flow, which updates rawDescription to
    // exactly the snapshot just written, so re-keying here is a no-op for the
    // active editor.
    var localMode by remember(categoryKey, mode) { mutableStateOf(mode) }
    var textValue by remember(categoryKey, rawDescription) { mutableStateOf(parsed.text) }
    var checklistItems by remember(categoryKey, rawDescription) { mutableStateOf(parsed.items) }
    var isEditingText by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var showAllChecklist by remember(categoryKey) { mutableStateOf(false) }

    // Build a complete HabitDescription using the latest in-memory drafts of
    // BOTH sides, so saving one mode never erases the other.
    fun snapshot(modeOverride: String = localMode) =
        HabitDescription(text = textValue, items = checklistItems, mode = modeOverride)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (localMode == "checklist") "${checklistItems.count { it.checked }} of ${checklistItems.size} done"
                        else if (textValue.isBlank()) "Add notes for this habit"
                        else "${textValue.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeToggleButton(
                            icon = Icons.Filled.Description,
                            label = "Text",
                            selected = localMode == "text",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (localMode != "text") {
                                    // Flush any pending checklist new-item input so it isn't lost on switch.
                                    val pending = newItemText.trim()
                                    if (pending.isNotEmpty()) {
                                        checklistItems = checklistItems + DescriptionChecklistItem(text = pending, checked = false)
                                        newItemText = ""
                                    }
                                    localMode = "text"
                                    onSave(snapshot("text"))
                                }
                            }
                        )
                        ModeToggleButton(
                            icon = Icons.Filled.Checklist,
                            label = "Checklist",
                            selected = localMode == "checklist",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (localMode != "checklist") {
                                    localMode = "checklist"
                                    onSave(snapshot("checklist"))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (localMode == "text") {
                        if (isEditingText) {
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                placeholder = { Text("Describe what this habit is about, your motivation, rules...") },
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 8
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        textValue = textValue.trim()
                                        onSave(snapshot())
                                        isEditingText = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Save") }
                                OutlinedButton(
                                    onClick = {
                                        textValue = parsed.text
                                        isEditingText = false
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Cancel") }
                            }
                        } else if (textValue.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { isEditingText = true }
                                    .padding(14.dp)
                            ) {
                                Text(
                                    textValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .clickable { isEditingText = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Edit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(HabitRed.copy(alpha = 0.08f))
                                        .clickable {
                                            textValue = ""
                                            onSave(snapshot())
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = HabitRed.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HabitRed.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                    .clickable { isEditingText = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Tap to add a description",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Checklist mode
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val collapsedLimit = 5
                            val isCollapsed = !showAllChecklist && checklistItems.size > collapsedLimit
                            val visibleCount =
                                if (isCollapsed) collapsedLimit else checklistItems.size
                            checklistItems.take(visibleCount).forEachIndexed { index, item ->
                                ChecklistRow(
                                    item = item,
                                    onToggle = {
                                        val updated = checklistItems.toMutableList()
                                        updated[index] = item.copy(checked = !item.checked)
                                        checklistItems = updated
                                        onSave(snapshot())
                                    },
                                    onDelete = {
                                        val updated = checklistItems.toMutableList()
                                        updated.removeAt(index)
                                        checklistItems = updated
                                        onSave(snapshot())
                                    },
                                    onTextChange = { newText ->
                                        val updated = checklistItems.toMutableList()
                                        updated[index] = item.copy(text = newText)
                                        checklistItems = updated
                                    },
                                    onCommitText = {
                                        onSave(snapshot())
                                    }
                                )
                            }

                            if (checklistItems.size > collapsedLimit) {
                                val hiddenCount = checklistItems.size - collapsedLimit
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showAllChecklist = !showAllChecklist }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        if (showAllChecklist) "Show less"
                                        else "Show $hiddenCount more",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (showAllChecklist) Icons.Filled.KeyboardArrowUp
                                        else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Add new item row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newItemText,
                                    onValueChange = { newItemText = it },
                                    placeholder = { Text("Add a new item") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 1,
                                    maxLines = 4
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (newItemText.isNotBlank())
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable(enabled = newItemText.isNotBlank()) {
                                            val updated = checklistItems + DescriptionChecklistItem(
                                                text = newItemText.trim(),
                                                checked = false
                                            )
                                            checklistItems = updated
                                            newItemText = ""
                                            onSave(snapshot())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add item",
                                        tint = if (newItemText.isNotBlank())
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (checklistItems.isEmpty()) {
                                Text(
                                    "No items yet. Add a step above to build your checklist.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
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
private fun ModeToggleButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ChecklistRow(
    item: DescriptionChecklistItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onTextChange: (String) -> Unit,
    onCommitText: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (item.checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    1.dp,
                    if (item.checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(6.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (item.checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (isEditing) {
            OutlinedTextField(
                value = item.text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                minLines = 1,
                maxLines = 4
            )
            IconButton(
                onClick = { onCommitText(); isEditing = false },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isEditing = true }
                    .padding(vertical = 8.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete item",
                    tint = HabitRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun JournalCalendar(
    month: java.time.YearMonth,
    entries: List<JournalEntry>,
    today: String,
    selectedDate: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (String) -> Unit
) {
    val datesWithEntries = remember(entries) { entries.map { it.date }.toSet() }
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    // dayOfWeek: Monday=1..Sunday=7 → grid cells before day 1
    val leadingBlanks = (firstDay.dayOfWeek.value - 1).coerceAtLeast(0)
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7
    val monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
    val isoFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val todayDate = try { java.time.LocalDate.parse(today) } catch (_: Exception) { java.time.LocalDate.now() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column {
            // Month header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    month.format(monthFmt),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next month",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Day grid
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            val dateStr = date.format(isoFmt)
                            val hasEntry = dateStr in datesWithEntries
                            val isToday = date == todayDate
                            val isFuture = date.isAfter(todayDate)
                            val isSelected = dateStr == selectedDate
                            CalendarDayCell(
                                day = dayNumber,
                                hasEntry = hasEntry,
                                isToday = isToday,
                                isFuture = isFuture,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { if (!isFuture) onDayClick(dateStr) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "has entry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    hasEntry: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasEntry -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        hasEntry -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (isToday && !isSelected) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(enabled = !isFuture) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (isToday || hasEntry || isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (hasEntry) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}


@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val displayDate = remember(entry.date) {
        try {
            val d = java.time.LocalDate.parse(entry.date)
            val today = java.time.LocalDate.now()
            when {
                d == today -> "Today"
                d == today.minusDays(1) -> "Yesterday"
                else -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        } catch (_: Exception) { entry.date }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Mood / fallback letter
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (entry.mood.isNotEmpty()) {
                Text(entry.mood, fontSize = 22.sp)
            } else {
                Icon(
                    Icons.Filled.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayDate,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (entry.scoreTotal > 0) {
                    val ratio = entry.scoreChecked.toFloat() / entry.scoreTotal.toFloat()
                    val scoreColor = when {
                        ratio >= 0.75f -> HabitGreen
                        ratio >= 0.4f -> HabitOrange
                        else -> HabitRed
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                            .border(1.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${entry.scoreChecked}/${entry.scoreTotal}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }
            if (entry.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete entry",
                tint = HabitRed.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GuidanceDialog(
    title: String, templates: List<GuidanceTemplate>, onDismiss: () -> Unit, onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = HabitOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guidance & Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                templates.forEach { template ->
                    Text(template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                    template.examples.forEach { example ->
                        Text(
                            text = example, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                                .clickable { onSelect(example) }.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
