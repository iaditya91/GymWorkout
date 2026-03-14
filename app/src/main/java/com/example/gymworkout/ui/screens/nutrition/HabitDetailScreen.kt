package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.viewmodel.NutritionViewModel

private val HabitGreen = Color(0xFF4CAF50)
private val HabitOrange = Color(0xFFFF9800)
private val HabitRed = Color(0xFFF44336)

// Guidance templates for each Atomic Habits field
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
        "\"Read before bed\" → Read one page.",
        "\"Run 3 miles\" → Put on running shoes.",
        "\"Study for class\" → Open your notes."
    )),
    GuidanceTemplate("Reduce Friction", listOf(
        "Prepare everything in advance.",
        "Remove steps between you and the habit.",
        "Use the 'gateway habit' — the smallest version."
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
        "Never miss twice — if you miss one day, get back immediately.",
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
    onBack: () -> Unit
) {
    val targetState by viewModel.getTarget(categoryKey).collectAsState(initial = null)
    val atomicHabit by viewModel.getAtomicHabit(categoryKey).collectAsState(initial = null)
    val today = viewModel.todayString()

    val historyResult by viewModel.getHabitHistoryDetailed(categoryKey, 30)
        .collectAsState(initial = null)

    var editingField by remember { mutableStateOf<String?>(null) } // "cue", "craving", "response", "reward"
    var editText by remember { mutableStateOf("") }
    var showGuidance by remember { mutableStateOf<String?>(null) } // which field to show guidance for
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(targetState?.label ?: "Habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // === STREAK & STATS SECTION ===
            historyResult?.let { result ->
                StatsRow(result)
                Spacer(modifier = Modifier.height(16.dp))

                // === IMPROVED CHART ===
                Text("Last 30 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                HabitCalendarGrid(
                    days = result.days,
                    selectedIndex = selectedDayIndex,
                    onDayClick = { idx ->
                        selectedDayIndex = if (selectedDayIndex == idx) null else idx
                    }
                )

                // Interactive day detail
                selectedDayIndex?.let { idx ->
                    val day = result.days.getOrNull(idx)
                    if (day != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (day.met) HabitGreen.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(day.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    if (day.target > 0f) "${"%.1f".format(day.value)} / ${"%.1f".format(day.target)} ${targetState?.unit ?: ""}"
                                    else if (day.met) "Completed" else "Not done",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(
                                    if (day.met) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (day.met) HabitGreen else HabitRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Completion bar
                val animatedPercent by animateFloatAsState(
                    targetValue = result.completionPercent / 100f,
                    label = "percent"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animatedPercent },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            result.completionPercent >= 80 -> HabitGreen
                            result.completionPercent >= 50 -> HabitOrange
                            else -> HabitRed
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${result.completionPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mark done button
            Button(
                onClick = {
                    val targetVal = targetState?.targetValue ?: 0f
                    viewModel.addEntryByKey(today, categoryKey, if (targetVal > 0f) targetVal else 1f)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Done Today")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === ATOMIC HABITS FRAMEWORK ===
            Text("Atomic Habits Framework", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Design your habit using the 4 Laws of Behavior Change",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val habit = atomicHabit
            AtomicHabitFieldCard(
                law = "1st Law",
                title = "Cue — Make it obvious",
                value = habit?.cue ?: "",
                isEditing = editingField == "cue",
                editText = if (editingField == "cue") editText else "",
                onEditClick = {
                    editingField = "cue"
                    editText = habit?.cue ?: ""
                },
                onEditTextChange = { editText = it },
                onSave = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = editText,
                        craving = habit?.craving ?: "",
                        response = habit?.response ?: "",
                        reward = habit?.reward ?: ""
                    )
                    editingField = null
                },
                onCancel = { editingField = null },
                onDelete = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = "",
                        craving = habit?.craving ?: "",
                        response = habit?.response ?: "",
                        reward = habit?.reward ?: ""
                    )
                },
                onGuidanceClick = { showGuidance = "cue" },
                accentColor = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(10.dp))

            AtomicHabitFieldCard(
                law = "2nd Law",
                title = "Craving — Make it attractive",
                value = habit?.craving ?: "",
                isEditing = editingField == "craving",
                editText = if (editingField == "craving") editText else "",
                onEditClick = {
                    editingField = "craving"
                    editText = habit?.craving ?: ""
                },
                onEditTextChange = { editText = it },
                onSave = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = editText,
                        response = habit?.response ?: "",
                        reward = habit?.reward ?: ""
                    )
                    editingField = null
                },
                onCancel = { editingField = null },
                onDelete = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = "",
                        response = habit?.response ?: "",
                        reward = habit?.reward ?: ""
                    )
                },
                onGuidanceClick = { showGuidance = "craving" },
                accentColor = Color(0xFFE91E63)
            )

            Spacer(modifier = Modifier.height(10.dp))

            AtomicHabitFieldCard(
                law = "3rd Law",
                title = "Response — Make it easy",
                value = habit?.response ?: "",
                isEditing = editingField == "response",
                editText = if (editingField == "response") editText else "",
                onEditClick = {
                    editingField = "response"
                    editText = habit?.response ?: ""
                },
                onEditTextChange = { editText = it },
                onSave = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = habit?.craving ?: "",
                        response = editText,
                        reward = habit?.reward ?: ""
                    )
                    editingField = null
                },
                onCancel = { editingField = null },
                onDelete = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = habit?.craving ?: "",
                        response = "",
                        reward = habit?.reward ?: ""
                    )
                },
                onGuidanceClick = { showGuidance = "response" },
                accentColor = Color(0xFF9C27B0)
            )

            Spacer(modifier = Modifier.height(10.dp))

            AtomicHabitFieldCard(
                law = "4th Law",
                title = "Reward — Make it satisfying",
                value = habit?.reward ?: "",
                isEditing = editingField == "reward",
                editText = if (editingField == "reward") editText else "",
                onEditClick = {
                    editingField = "reward"
                    editText = habit?.reward ?: ""
                },
                onEditTextChange = { editText = it },
                onSave = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = habit?.craving ?: "",
                        response = habit?.response ?: "",
                        reward = editText
                    )
                    editingField = null
                },
                onCancel = { editingField = null },
                onDelete = {
                    viewModel.saveAtomicHabit(
                        categoryKey,
                        cue = habit?.cue ?: "",
                        craving = habit?.craving ?: "",
                        response = habit?.response ?: "",
                        reward = ""
                    )
                },
                onGuidanceClick = { showGuidance = "reward" },
                accentColor = Color(0xFFFF9800)
            )

            // Delete all atomic habit data
            if (habit != null && (habit.cue.isNotEmpty() || habit.craving.isNotEmpty() || habit.response.isNotEmpty() || habit.reward.isNotEmpty())) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitRed)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Habit Design")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Guidance dialog
    showGuidance?.let { field ->
        val templates = when (field) {
            "cue" -> cueTemplates
            "craving" -> cravingTemplates
            "response" -> responseTemplates
            "reward" -> rewardTemplates
            else -> emptyList()
        }
        val fieldTitle = when (field) {
            "cue" -> "Cue — Make it obvious"
            "craving" -> "Craving — Make it attractive"
            "response" -> "Response — Make it easy"
            "reward" -> "Reward — Make it satisfying"
            else -> ""
        }
        GuidanceDialog(
            title = fieldTitle,
            templates = templates,
            onDismiss = { showGuidance = null },
            onSelect = { text ->
                editingField = field
                editText = text
                showGuidance = null
            }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear Habit Design?") },
            text = { Text("This will remove all cue, craving, response, and reward entries for this habit.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAtomicHabit(categoryKey)
                    showDeleteConfirm = false
                }) {
                    Text("Clear", color = HabitRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatsRow(result: NutritionViewModel.HabitHistoryResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            value = "${result.currentStreak}",
            label = "Current\nStreak",
            color = if (result.currentStreak > 0) HabitGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        StatCard(
            value = "${result.longestStreak}",
            label = "Longest\nStreak",
            color = HabitOrange
        )
        StatCard(
            value = "${result.totalMet}/${result.totalDays}",
            label = "Days\nCompleted",
            color = MaterialTheme.colorScheme.primary
        )
        StatCard(
            value = "${result.completionPercent}%",
            label = "Completion\nRate",
            color = when {
                result.completionPercent >= 80 -> HabitGreen
                result.completionPercent >= 50 -> HabitOrange
                else -> HabitRed
            }
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun HabitCalendarGrid(
    days: List<NutritionViewModel.HabitDayInfo>,
    selectedIndex: Int?,
    onDayClick: (Int) -> Unit
) {
    // Show as scrollable row of day cells
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedIndex == index
            val bgColor by animateColorAsState(
                targetValue = when {
                    day.met -> HabitGreen
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "dayColor"
            )
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onDayClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .then(
                            if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(6.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.met) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Show day number
                val dayNum = day.date.substring(8) // dd from yyyy-MM-dd
                Text(
                    dayNum,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AtomicHabitFieldCard(
    law: String,
    title: String,
    value: String,
    isEditing: Boolean,
    editText: String,
    onEditClick: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onGuidanceClick: () -> Unit,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Law badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        law,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                // Guidance button
                IconButton(onClick = onGuidanceClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = "Guidance",
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = onEditTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Describe your strategy...") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (value.isNotEmpty()) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = accentColor)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = HabitRed.copy(alpha = 0.7f))
                    }
                }
            } else {
                // Empty state — tap to add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onEditClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tap to add your strategy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidanceDialog(
    title: String,
    templates: List<GuidanceTemplate>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = HabitOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guidance & Templates", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                templates.forEach { template ->
                    Text(
                        template.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    template.examples.forEach { example ->
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onSelect(example) }
                                .padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
