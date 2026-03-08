package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.viewmodel.NutritionViewModel
import com.example.gymworkout.data.NutritionReminder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: NutritionViewModel) {

    val selectedDate by viewModel.selectedDate.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var reminderCategory by remember { mutableStateOf<NutritionCategory?>(null) }
    val displayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val today = viewModel.todayString()

    LaunchedEffect(Unit) {
        viewModel.initDefaultTargets()
    }

    val dateDisplay = try {
        val ld = LocalDate.parse(selectedDate)
        when (selectedDate) {
            today -> "Today"
            LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) -> "Yesterday"
            else -> ld.format(displayFormatter)
        }
    } catch (_: Exception) { selectedDate }

    val entries by viewModel.getEntriesForDate(selectedDate).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nutrition", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Track your daily intake",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTargetDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Set targets")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log intake")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val prev = LocalDate.parse(selectedDate).minusDays(1)
                            viewModel.setDate(prev.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
                        }
                        Text(
                            text = dateDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {
                            val next = LocalDate.parse(selectedDate).plusDays(1)
                            if (!next.isAfter(LocalDate.now())) {
                                viewModel.setDate(next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
                        }
                    }
                }
            }

            // Category cards
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(NutritionCategory.entries) { category ->
                NutritionCategoryCard(
                    category = category,
                    date = selectedDate,
                    viewModel = viewModel,
                    onReminderClick = { reminderCategory = category }
                )
            }

            // Recent entries for today
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Today's Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "No entries yet. Tap + to log.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddNutritionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category, value ->
                viewModel.addEntry(selectedDate, category, value)
                showAddDialog = false
            }
        )
    }

    if (showTargetDialog) {
        SetTargetsDialog(
            viewModel = viewModel,
            onDismiss = { showTargetDialog = false }
        )
    }

    if (reminderCategory != null) {
        ReminderListDialog(
            category = reminderCategory!!,
            color = getCategoryColor(reminderCategory!!),
            viewModel = viewModel,
            onDismiss = { reminderCategory = null }
        )
    }
}

@Composable
fun NutritionCategoryCard(
    category: NutritionCategory,
    date: String,
    viewModel: NutritionViewModel,
    onReminderClick: () -> Unit = {}
) {
    val total by viewModel.getTotalForCategory(date, category.name).collectAsState(initial = 0f)
    val target by viewModel.getTarget(category.name).collectAsState(initial = null)
    val reminders by viewModel.getRemindersForCategory(category.name).collectAsState(initial = emptyList())
    val hasActiveReminders = reminders.any { it.enabled }
    val targetVal = target?.targetValue ?: 0f
    val progress = if (targetVal > 0) (total / targetVal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val met = targetVal > 0 && total >= targetVal

    val icon = getCategoryIcon(category)
    val color = getCategoryColor(category)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (met)
                color.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(52.dp),
                    color = if (met) color else color.copy(alpha = 0.7f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (met) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Target Met",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatValue(total)} / ${formatValue(targetVal)} ${category.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reminder bell icon
            IconButton(
                onClick = onReminderClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Set reminder for ${category.label}",
                    tint = if (hasActiveReminders) color
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EntryRow(entry: NutritionEntry, onDelete: () -> Unit) {
    val category = try { NutritionCategory.valueOf(entry.category) } catch (_: Exception) { null }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category != null) {
                Icon(
                    getCategoryIcon(category),
                    contentDescription = null,
                    tint = getCategoryColor(category),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = category?.label ?: entry.category,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatValue(entry.value)} ${category?.unit ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddNutritionDialog(
    onDismiss: () -> Unit,
    onSave: (NutritionCategory, Float) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(NutritionCategory.WATER) }
    var value by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Intake") },
        text = {
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                // Category selector as chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NutritionCategory.entries.take(3).forEach { cat ->
                        CategoryChip(
                            category = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NutritionCategory.entries.drop(3).forEach { cat ->
                        CategoryChip(
                            category = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (${selectedCategory.unit})") },
                    placeholder = {
                        Text(
                            when (selectedCategory) {
                                NutritionCategory.WATER -> "e.g. 0.5"
                                NutritionCategory.CARBS -> "e.g. 300"
                                NutritionCategory.PROTEIN -> "e.g. 30"
                                NutritionCategory.VITAMINS -> "e.g. 1"
                                NutritionCategory.SLEEP -> "e.g. 7.5"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toFloatOrNull()
                    if (v != null && v > 0) onSave(selectedCategory, v)
                }
            ) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CategoryChip(
    category: NutritionCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = getCategoryColor(category)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                getCategoryIcon(category),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                category.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SetTargetsDialog(
    viewModel: NutritionViewModel,
    onDismiss: () -> Unit
) {
    val targets by viewModel.getAllTargets().collectAsState(initial = emptyList())
    val targetMap = targets.associateBy { it.category }

    val values = remember(targets) {
        NutritionCategory.entries.associate { cat ->
            cat to mutableStateOf(targetMap[cat.name]?.targetValue?.toString() ?: "0")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Targets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NutritionCategory.entries.forEach { cat ->
                    OutlinedTextField(
                        value = values[cat]!!.value,
                        onValueChange = { values[cat]!!.value = it },
                        label = { Text("${cat.label} (${cat.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                NutritionCategory.entries.forEach { cat ->
                    val v = values[cat]!!.value.toFloatOrNull()
                    if (v != null && v > 0) viewModel.setTarget(cat, v)
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getCategoryIcon(category: NutritionCategory): ImageVector = when (category) {
    NutritionCategory.WATER -> Icons.Default.WaterDrop
    NutritionCategory.CARBS -> Icons.Default.LocalFireDepartment
    NutritionCategory.PROTEIN -> Icons.Default.Egg
    NutritionCategory.VITAMINS -> Icons.Default.Medication
    NutritionCategory.SLEEP -> Icons.Default.BedtimeOff
}

fun getCategoryColor(category: NutritionCategory): Color = when (category) {
    NutritionCategory.WATER -> Color(0xFF42A5F5)
    NutritionCategory.CARBS -> Color(0xFFFF8A65)
    NutritionCategory.PROTEIN -> Color(0xFFEF5350)
    NutritionCategory.VITAMINS -> Color(0xFF66BB6A)
    NutritionCategory.SLEEP -> Color(0xFFAB47BC)
}

fun formatValue(v: Float): String {
    return if (v == v.toLong().toFloat()) v.toLong().toString()
    else String.format("%.1f", v)
}
