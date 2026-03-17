package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.FoodDatabase
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.FoodLogEntry
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.NutritionEntry
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.viewmodel.NutritionViewModel
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.notification.TimerAlertService
import com.example.gymworkout.notification.TimerReceiver
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onNavigateToAiChat: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {}
) {

    val selectedDate by viewModel.selectedDate.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showAddObjectiveDialog by remember { mutableStateOf(false) }
    var reminderCategory by remember { mutableStateOf<NutritionCategory?>(null) }
    var customReminderTarget by remember { mutableStateOf<NutritionTarget?>(null) }
    var deleteTargetCategory by remember { mutableStateOf<String?>(null) }
    var showFoodLogDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAiObjectiveDialog by remember { mutableStateOf(false) }
    var showAiPlannerDialog by remember { mutableStateOf(false) }
    val barcodeLookupState by viewModel.barcodeLookupState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Nutrition, 1 = Habits
    val customFoodItems by viewModel.customFoods.collectAsState(initial = emptyList())
    val customFoodsAsFoodItems = remember(customFoodItems) { customFoodItems.map { it.toFoodItem() } }
    val allTargets by viewModel.getAllTargets().collectAsState(initial = emptyList())
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
                        Text("Nutrition & Habits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Track your daily intake & habits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadPlannerProgress(selectedDate)
                        showAiPlannerDialog = true
                    }) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = "AI Daily Planner")
                    }
                    IconButton(onClick = { showAiObjectiveDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI generate objectives")
                    }
                    IconButton(onClick = { showAddObjectiveDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add objective")
                    }
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (Build.VERSION.SDK_INT >= 31) {
                    FloatingActionButton(
                        onClick = onNavigateToAiChat,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Chat",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log intake")
                }
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

            // Tab switcher
            item {
                val tabOptions = listOf("Nutrition", "Habits")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabOptions.size
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Split custom objectives into nutrition vs habits
            val customNutritionTargets = allTargets.filter {
                it.isCustom && it.label.lowercase() in nutritionRelatedNames
            }
            val customHabitTargets = allTargets.filter {
                it.isCustom && it.label.lowercase() !in nutritionRelatedNames
            }

            // Collect all nutrition-related category keys for entry filtering
            val nutritionCategoryNames = setOf(
                NutritionCategory.CALORIES.name,
                NutritionCategory.PROTEIN.name,
                NutritionCategory.CARBS.name
            ) + customNutritionTargets.map { it.category }.toSet()

            if (selectedTab == 0) {
                // === NUTRITION TAB ===
                val nutritionCategories = listOf(
                    NutritionCategory.CALORIES,
                    NutritionCategory.PROTEIN,
                    NutritionCategory.CARBS,
                ).filter { cat -> allTargets.any { it.category == cat.name } }

                items(nutritionCategories) { category ->
                    NutritionCategoryCard(
                        category = category,
                        date = selectedDate,
                        viewModel = viewModel,
                        onReminderClick = { reminderCategory = category },
                        onDelete = { deleteTargetCategory = category.name }
                    )
                }

                // Nutrition-related custom objectives (vitamins, minerals, etc.)
                items(customNutritionTargets, key = { it.category }) { target ->
                    CustomCategoryCard(
                        target = target,
                        date = selectedDate,
                        viewModel = viewModel,
                        onReminderClick = { customReminderTarget = target },
                        onDelete = { deleteTargetCategory = target.category }
                    )
                }

                // Food log section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FoodLogSection(
                        date = selectedDate,
                        viewModel = viewModel,
                        onLogFood = { showFoodLogDialog = true }
                    )
                }

                // Recent entries for today (nutrition only)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Today's Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val nutritionEntries = entries.filter { it.category in nutritionCategoryNames }

                if (nutritionEntries.isEmpty()) {
                    item {
                        Text(
                            "No entries yet. Tap + to log.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(nutritionEntries, key = { it.id }) { entry ->
                        EntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) }, allTargets = allTargets)
                    }
                }
            } else {
                // === HABITS TAB ===
                val habitCategories = listOf(
                    NutritionCategory.WATER,
                    NutritionCategory.SLEEP
                ).filter { cat -> allTargets.any { it.category == cat.name } }

                items(habitCategories) { category ->
                    NutritionCategoryCard(
                        category = category,
                        date = selectedDate,
                        viewModel = viewModel,
                        onReminderClick = { reminderCategory = category },
                        onDelete = { deleteTargetCategory = category.name }
                    )
                }

                // Non-nutrition custom objectives (habits only)
                items(customHabitTargets, key = { it.category }) { target ->
                    CustomCategoryCard(
                        target = target,
                        date = selectedDate,
                        viewModel = viewModel,
                        onReminderClick = { customReminderTarget = target },
                        onDelete = { deleteTargetCategory = target.category },
                        isHabit = true,
                        onOpenHabit = { onOpenHabit(target.category) }
                    )
                }

                // Recent entries for today (habits only)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Today's Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val habitEntries = entries.filter { it.category !in nutritionCategoryNames }

                if (habitEntries.isEmpty()) {
                    item {
                        Text(
                            "No entries yet. Tap + to log.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(habitEntries, key = { it.id }) { entry ->
                        EntryRow(entry = entry, onDelete = { viewModel.deleteEntry(entry) }, allTargets = allTargets)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddNutritionDialog(
            allTargets = allTargets,
            initialTab = selectedTab,
            onDismiss = { showAddDialog = false },
            onSaveBuiltIn = { category, value ->
                viewModel.addEntry(selectedDate, category, value)
                showAddDialog = false
            },
            onSaveCustom = { categoryKey, value ->
                viewModel.addEntryByKey(selectedDate, categoryKey, value)
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

    if (showAddObjectiveDialog) {
        AddObjectiveDialog(
            onDismiss = { showAddObjectiveDialog = false },
            onSave = { name, unit, target, timerSeconds, notifyEnabled ->
                viewModel.addCustomObjective(name, unit, target, timerSeconds, notifyEnabled)
                showAddObjectiveDialog = false
            }
        )
    }

    if (deleteTargetCategory != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetCategory = null },
            title = { Text("Delete Objective?") },
            text = { Text("This will remove this nutrition objective and all its logged entries. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteObjective(deleteTargetCategory!!)
                    deleteTargetCategory = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetCategory = null }) { Text("Cancel") }
            }
        )
    }

    if (showAiPlannerDialog) {
        val plannerProgress by viewModel.plannerProgress.collectAsState()
        AiPlannerDialog(
            progressList = plannerProgress,
            onDismiss = { showAiPlannerDialog = false }
        )
    }

    if (showAiObjectiveDialog) {
        AiObjectiveDialog(
            existingTargets = allTargets,
            onDismiss = { showAiObjectiveDialog = false },
            onAddObjectives = { selections ->
                selections.forEach { sel ->
                    if (sel.objective.isBuiltIn && sel.objective.builtInCategory != null) {
                        viewModel.setTarget(sel.objective.builtInCategory, sel.editedTarget)
                    } else {
                        viewModel.addCustomObjective(
                            sel.editedName,
                            sel.editedUnit,
                            sel.editedTarget
                        )
                    }
                }
                showAiObjectiveDialog = false
            }
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

    if (customReminderTarget != null) {
        ReminderListDialog(
            categoryKey = customReminderTarget!!.category,
            categoryLabel = customReminderTarget!!.label,
            color = Color(0xFF78909C),
            viewModel = viewModel,
            onDismiss = { customReminderTarget = null }
        )
    }

    if (showFoodLogDialog) {
        FoodLogDialog(
            customFoods = customFoodsAsFoodItems,
            onDismiss = { showFoodLogDialog = false },
            onLog = { food, qty ->
                viewModel.logFood(selectedDate, food, qty)
                showFoodLogDialog = false
            },
            onSaveCustomFood = { customFood ->
                viewModel.saveCustomFood(customFood)
            },
            onScanBarcode = {
                showFoodLogDialog = false
                showBarcodeScanner = true
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { barcode ->
                showBarcodeScanner = false
                viewModel.lookupBarcode(barcode)
            },
            onBack = { showBarcodeScanner = false }
        )
    }

    if (barcodeLookupState !is NutritionViewModel.BarcodeLookupState.Idle) {
        ScannedFoodDialog(
            state = barcodeLookupState,
            onDismiss = { viewModel.resetBarcodeLookup() },
            onLog = { food, qty ->
                viewModel.logFood(selectedDate, food, qty)
                viewModel.resetBarcodeLookup()
            },
            onSave = { product ->
                viewModel.saveScannedAsCustomFood(product)
            },
            onSaveAndLog = { product, qty ->
                viewModel.saveScannedAsCustomFood(product)
                viewModel.logFood(selectedDate, product.foodItem, qty)
                viewModel.resetBarcodeLookup()
            }
        )
    }
}

@Composable
fun NutritionCategoryCard(
    category: NutritionCategory,
    date: String,
    viewModel: NutritionViewModel,
    onReminderClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val total by viewModel.getTotalForCategory(date, category.name).collectAsState(initial = 0f)
    val target by viewModel.getTarget(category.name).collectAsState(initial = null)
    val reminders by viewModel.getRemindersForCategory(category.name).collectAsState(initial = emptyList())
    val hasActiveReminders = reminders.any { it.enabled }
    val targetVal = target?.targetValue ?: 0f
    val currentNotes = target?.notes ?: ""
    val progress = if (targetVal > 0) (total / targetVal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val met = targetVal > 0 && total >= targetVal
    var showFoodSuggestions by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    val icon = getCategoryIcon(category)
    val color = getCategoryColor(category)

    val hasFoodSuggestions = category in listOf(
        NutritionCategory.PROTEIN, NutritionCategory.CALORIES, NutritionCategory.CARBS
    )

    if (showFoodSuggestions) {
        FoodSuggestionsDialog(
            category = category,
            targetValue = targetVal,
            onDismiss = { showFoodSuggestions = false }
        )
    }

    if (showNotesDialog) {
        NotesDialog(
            title = category.label,
            currentNotes = currentNotes,
            onDismiss = { showNotesDialog = false },
            onSave = { notes ->
                viewModel.updateNotes(category.name, notes)
                showNotesDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (met) BorderStroke(2.dp, color) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Name + target met badge
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
                }

                // Action icons in horizontal row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Note,
                            contentDescription = "Notes for ${category.label}",
                            tint = if (currentNotes.isNotBlank()) color
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (hasFoodSuggestions) {
                        IconButton(
                            onClick = { showFoodSuggestions = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Food sources for ${category.label}",
                                tint = color.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onReminderClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Set reminder for ${category.label}",
                            tint = if (hasActiveReminders) color
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ${category.label}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress info below
            Spacer(modifier = Modifier.height(8.dp))
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

            // Show notes preview if present
            if (currentNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EntryRow(entry: NutritionEntry, onDelete: () -> Unit, allTargets: List<NutritionTarget> = emptyList()) {
    val category = try { NutritionCategory.valueOf(entry.category) } catch (_: Exception) { null }
    val customTarget = if (category == null) allTargets.find { it.category == entry.category } else null
    val displayLabel = category?.label ?: customTarget?.label ?: entry.category
    val displayUnit = category?.unit ?: customTarget?.unit ?: ""

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
            } else {
                // Custom category indicator
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF78909C).copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayLabel.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78909C)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatValue(entry.value)} $displayUnit",
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
@OptIn(ExperimentalLayoutApi::class)
fun AddNutritionDialog(
    allTargets: List<NutritionTarget> = emptyList(),
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onSaveBuiltIn: (NutritionCategory, Float) -> Unit,
    onSaveCustom: (String, Float) -> Unit
) {
    // 0 = Log Intake (nutrition), 1 = Log Habit
    var dialogTab by remember { mutableIntStateOf(initialTab) }
    var selectedBuiltIn by remember { mutableStateOf<NutritionCategory?>(null) }
    var selectedCustomKey by remember { mutableStateOf<String?>(null) }
    var value by remember { mutableStateOf("") }

    // Split targets into nutrition vs habits using same logic as main screen
    val customNutritionTargets = allTargets.filter {
        it.isCustom && it.label.lowercase() in nutritionRelatedNames
    }
    val customHabitTargets = allTargets.filter {
        it.isCustom && it.label.lowercase() !in nutritionRelatedNames
    }

    // Nutrition built-ins: Calories, Protein, Carbs (only those with targets set)
    val nutritionBuiltIns = listOf(
        NutritionCategory.CALORIES,
        NutritionCategory.PROTEIN,
        NutritionCategory.CARBS
    ).filter { cat -> allTargets.any { it.category == cat.name } }

    // Habit built-ins: Water, Sleep (only those with targets set)
    val habitBuiltIns = listOf(
        NutritionCategory.WATER,
        NutritionCategory.SLEEP
    ).filter { cat -> allTargets.any { it.category == cat.name } }

    // Auto-select first item when switching tabs
    LaunchedEffect(dialogTab) {
        selectedBuiltIn = null
        selectedCustomKey = null
        value = ""
        if (dialogTab == 0) {
            selectedBuiltIn = nutritionBuiltIns.firstOrNull()
            if (selectedBuiltIn == null) {
                selectedCustomKey = customNutritionTargets.firstOrNull()?.category
            }
        } else {
            val firstHabitBuiltIn = habitBuiltIns.firstOrNull()
            if (firstHabitBuiltIn != null) {
                selectedBuiltIn = firstHabitBuiltIn
            } else {
                selectedCustomKey = customHabitTargets.firstOrNull()?.category
            }
        }
    }

    val currentUnit = when {
        selectedBuiltIn != null -> selectedBuiltIn!!.unit
        selectedCustomKey != null -> allTargets.find { it.category == selectedCustomKey }?.unit ?: ""
        else -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (dialogTab == 0) "Log Intake" else "Log Habit")
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = dialogTab == 0,
                        onClick = { dialogTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Intake", style = MaterialTheme.typography.labelMedium) }
                    SegmentedButton(
                        selected = dialogTab == 1,
                        onClick = { dialogTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Habit", style = MaterialTheme.typography.labelMedium) }
                }
            }
        },
        text = {
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable chips area with max height so input stays visible
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (dialogTab == 0) {
                        // === INTAKE TAB: nutrition built-ins + custom nutrition ===
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            nutritionBuiltIns.forEach { cat ->
                                CategoryChip(
                                    category = cat,
                                    selected = selectedBuiltIn == cat,
                                    onClick = { selectedBuiltIn = cat; selectedCustomKey = null }
                                )
                            }
                            customNutritionTargets.forEach { t ->
                                CustomChip(
                                    label = t.label,
                                    selected = selectedCustomKey == t.category,
                                    onClick = { selectedCustomKey = t.category; selectedBuiltIn = null }
                                )
                            }
                        }
                    } else {
                        // === HABIT TAB: Sleep + custom habits ===
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            habitBuiltIns.forEach { cat ->
                                CategoryChip(
                                    category = cat,
                                    selected = selectedBuiltIn == cat,
                                    onClick = { selectedBuiltIn = cat; selectedCustomKey = null }
                                )
                            }
                            customHabitTargets.forEach { t ->
                                CustomChip(
                                    label = t.label,
                                    selected = selectedCustomKey == t.category,
                                    onClick = { selectedCustomKey = t.category; selectedBuiltIn = null }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value ($currentUnit)") },
                    placeholder = { Text("e.g. 1") },
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
                    if (v != null && v > 0) {
                        if (selectedBuiltIn != null) {
                            onSaveBuiltIn(selectedBuiltIn!!, v)
                        } else if (selectedCustomKey != null) {
                            onSaveCustom(selectedCustomKey!!, v)
                        }
                    }
                }
            ) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CustomChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(0xFF78909C)
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
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
fun CustomCategoryCard(
    target: NutritionTarget,
    date: String,
    viewModel: NutritionViewModel,
    onReminderClick: () -> Unit = {},
    onDelete: () -> Unit,
    isHabit: Boolean = false,
    onOpenHabit: () -> Unit = {}
) {
    val context = LocalContext.current
    val total by viewModel.getTotalForCategory(date, target.category).collectAsState(initial = 0f)
    val reminders by viewModel.getRemindersForCategory(target.category).collectAsState(initial = emptyList())
    val hasActiveReminders = reminders.any { it.enabled }
    val targetVal = target.targetValue
    val currentNotes = target.notes
    val isTrulyCustom = target.label.lowercase() !in presetObjectiveNames
    val progress = if (targetVal > 0) (total / targetVal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val met = targetVal > 0 && total >= targetVal
    val color = Color(0xFF78909C) // neutral color for custom
    var showFoodSuggestions by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Timer state (only for truly custom objectives)
    val hasTimer = isTrulyCustom && target.timerSeconds > 0
    var timerRunning by remember { mutableStateOf(false) }
    var timerPaused by remember { mutableStateOf(false) }
    var timerRemaining by remember { mutableLongStateOf(target.timerSeconds.toLong()) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var timerJustFinished by remember { mutableStateOf(false) }

    // Countdown effect
    if (isTrulyCustom) {
        LaunchedEffect(timerRunning, timerPaused) {
            if (timerRunning && !timerPaused) {
                while (timerRemaining > 0) {
                    delay(1000L)
                    timerRemaining--
                }
                if (timerRemaining <= 0L) {
                    timerRunning = false
                    timerPaused = false
                    timerJustFinished = true
                    val intent = Intent(context, TimerReceiver::class.java).apply {
                        putExtra(TimerReceiver.EXTRA_LABEL, target.label)
                        putExtra(TimerReceiver.EXTRA_NOTIFY, target.timerNotifyEnabled)
                        putExtra(TimerReceiver.EXTRA_NOTIFICATION_ID, target.category.hashCode())
                        putExtra(TimerReceiver.EXTRA_TIMER_TYPE, "habit")
                    }
                    context.sendBroadcast(intent)
                    timerRemaining = target.timerSeconds.toLong()
                }
            }
        }
    }

    // Auto-reopen the popup when timer finishes (even if user had minimized it)
    if (isTrulyCustom) {
        LaunchedEffect(timerJustFinished) {
            if (timerJustFinished) showTimerDialog = true
        }
    }

    val foodFieldKey = getCustomFoodFieldKey(target.label)

    if (showFoodSuggestions && foodFieldKey != null) {
        CustomFoodSuggestionsDialog(
            label = target.label,
            unit = target.unit,
            fieldKey = foodFieldKey,
            targetValue = targetVal,
            color = color,
            onDismiss = { showFoodSuggestions = false }
        )
    }

    if (showNotesDialog) {
        NotesDialog(
            title = target.label,
            currentNotes = currentNotes,
            onDismiss = { showNotesDialog = false },
            onSave = { notes ->
                viewModel.updateNotes(target.category, notes)
                showNotesDialog = false
            }
        )
    }

    if (showEditDialog) {
        EditCustomObjectiveDialog(
            target = target,
            onDismiss = { showEditDialog = false },
            onSave = { label, unit, timerSecs, notifyOn ->
                viewModel.updateCustomObjective(
                    target.category, label, unit, timerSecs, notifyOn
                )
                timerRemaining = timerSecs.toLong()
                showEditDialog = false
            }
        )
    }

    if (showTimerDialog && hasTimer) {
        com.example.gymworkout.ui.components.HabitTimerDialog(
            label = target.label,
            totalSeconds = target.timerSeconds,
            remaining = timerRemaining,
            isRunning = timerRunning,
            isPaused = timerPaused,
            isFinished = timerJustFinished,
            onPauseResume = { timerPaused = !timerPaused },
            onReset = {
                timerRemaining = target.timerSeconds.toLong()
                timerRunning = true
                timerPaused = false
            },
            onStop = {
                timerRunning = false
                timerPaused = false
                timerRemaining = target.timerSeconds.toLong()
                showTimerDialog = false
            },
            onDismiss = {
                if (timerJustFinished) {
                    TimerAlertService.stop(context)
                }
                showTimerDialog = false
                timerJustFinished = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (met) BorderStroke(2.dp, color) else null
    ) {
        val columnModifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(if (isHabit) Modifier.clickable { onOpenHabit() } else Modifier)

        Column(
            modifier = columnModifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        target.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name + target met badge (inline)
                Text(
                    text = buildAnnotatedString {
                        append(target.label)
                        if (met) {
                            append("  ")
                            withStyle(SpanStyle(
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                background = color.copy(alpha = 0.2f)
                            )) {
                                append(" Target Met ")
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Action icons in horizontal row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Note,
                            contentDescription = "Notes for ${target.label}",
                            tint = if (currentNotes.isNotBlank()) color
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isHabit) {
                        IconButton(
                            onClick = {
                                if (!met) {
                                    viewModel.markHabitDone(date, target.category)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Mark ${target.label} as done",
                                tint = if (met) color
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (foodFieldKey != null) {
                        IconButton(
                            onClick = { showFoodSuggestions = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Food sources for ${target.label}",
                                tint = color.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onReminderClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Set reminder for ${target.label}",
                            tint = if (hasActiveReminders) color
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isTrulyCustom) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit ${target.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ${target.label}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress info below
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatValue(total)} / ${formatValue(targetVal)} ${target.unit}",
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

            // Timer section
            if (hasTimer) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (timerRunning) MaterialTheme.colorScheme.primary else color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimerDisplay(timerRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (timerRunning && !timerPaused)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = timerRunning) { showTimerDialog = true }
                    )

                    if (!timerRunning) {
                        // Start button
                        IconButton(
                            onClick = {
                                timerRemaining = target.timerSeconds.toLong()
                                timerRunning = true
                                timerPaused = false
                                showTimerDialog = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "Start timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        // Pause/Resume
                        IconButton(
                            onClick = { timerPaused = !timerPaused },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (timerPaused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                contentDescription = if (timerPaused) "Resume" else "Pause",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        // Stop
                        IconButton(
                            onClick = {
                                timerRunning = false
                                timerPaused = false
                                timerRemaining = target.timerSeconds.toLong()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.StopCircle,
                                contentDescription = "Stop timer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Show notes preview if present
            if (currentNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FoodLogSection(
    date: String,
    viewModel: NutritionViewModel,
    onLogFood: () -> Unit
) {
    val foodLogs by viewModel.getFoodLogForDate(date).collectAsState(initial = emptyList())
    val totalCalories = foodLogs.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = foodLogs.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = foodLogs.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = foodLogs.sumOf { it.fat.toDouble() }.toFloat()
    // Build vitamin/mineral summary dynamically
    val vitaminSummary = mutableListOf<Triple<String, Float, String>>()
    fun addIfPositive(label: String, value: Float, unit: String) { if (value > 0) vitaminSummary.add(Triple(label, value, unit)) }
    addIfPositive("Vit A", foodLogs.sumOf { it.vitaminA.toDouble() }.toFloat(), "mcg")
    addIfPositive("B1", foodLogs.sumOf { it.vitaminB1.toDouble() }.toFloat(), "mg")
    addIfPositive("B2", foodLogs.sumOf { it.vitaminB2.toDouble() }.toFloat(), "mg")
    addIfPositive("B3", foodLogs.sumOf { it.vitaminB3.toDouble() }.toFloat(), "mg")
    addIfPositive("B6", foodLogs.sumOf { it.vitaminB6.toDouble() }.toFloat(), "mg")
    addIfPositive("B12", foodLogs.sumOf { it.vitaminB12.toDouble() }.toFloat(), "mcg")
    addIfPositive("Vit C", foodLogs.sumOf { it.vitaminC.toDouble() }.toFloat(), "mg")
    addIfPositive("Vit D", foodLogs.sumOf { it.vitaminD.toDouble() }.toFloat(), "mcg")
    addIfPositive("Vit E", foodLogs.sumOf { it.vitaminE.toDouble() }.toFloat(), "mg")
    addIfPositive("Vit K", foodLogs.sumOf { it.vitaminK.toDouble() }.toFloat(), "mcg")
    addIfPositive("Folate", foodLogs.sumOf { it.folate.toDouble() }.toFloat(), "mcg")
    addIfPositive("Iron", foodLogs.sumOf { it.iron.toDouble() }.toFloat(), "mg")
    addIfPositive("Calcium", foodLogs.sumOf { it.calcium.toDouble() }.toFloat(), "mg")
    addIfPositive("Magnesium", foodLogs.sumOf { it.magnesium.toDouble() }.toFloat(), "mg")
    addIfPositive("Potassium", foodLogs.sumOf { it.potassium.toDouble() }.toFloat(), "mg")
    addIfPositive("Zinc", foodLogs.sumOf { it.zinc.toDouble() }.toFloat(), "mg")
    addIfPositive("Copper", foodLogs.sumOf { it.copper.toDouble() }.toFloat(), "mg")
    addIfPositive("Selenium", foodLogs.sumOf { it.selenium.toDouble() }.toFloat(), "mcg")

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        Icons.Default.Egg,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Food Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (foodLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${foodLogs.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                TextButton(onClick = onLogFood) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Food")
                }
            }

            if (foodLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Macro summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutrientSummaryChip("Cal", totalCalories, "cal")
                    NutrientSummaryChip("Protein", totalProtein, "g")
                    NutrientSummaryChip("Carbs", totalCarbs, "g")
                    NutrientSummaryChip("Fat", totalFat, "g")
                }

                // Vitamin & mineral summary rows (4 per row)
                if (vitaminSummary.isNotEmpty()) {
                    vitaminSummary.chunked(4).forEach { row ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { (label, value, unit) ->
                                NutrientSummaryChip(label, value, unit)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Food entries
                foodLogs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                log.foodName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            val qtyDisplay = if (log.unit == "pc") "${log.quantity.toInt()} pc"
                                else "${log.quantity.toInt()} ${log.unit}"
                            Text(
                                "$qtyDisplay  |  ${String.format("%.0f", log.calories)} cal  |  P: ${String.format("%.1f", log.protein)}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteFoodLog(log) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Tap + to log what you ate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NutrientSummaryChip(label: String, value: Float, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            String.format("%.0f", value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "$label ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotesDialog(
    title: String,
    currentNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes - $title") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Add notes...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val objectivePresets = listOf(
    "Calories" to "cal",
    "Protein" to "g",
    "Carbs" to "g",
    "Fat" to "g",
    "Fiber" to "g",
    "Vitamin A" to "mcg",
    "Vitamin B1" to "mg",
    "Vitamin B2" to "mg",
    "Vitamin B3" to "mg",
    "Vitamin B6" to "mg",
    "Vitamin B12" to "mcg",
    "Vitamin C" to "mg",
    "Vitamin D" to "mcg",
    "Vitamin E" to "mg",
    "Vitamin K" to "mcg",
    "Folate" to "mcg",
    "Iron" to "mg",
    "Calcium" to "mg",
    "Water" to "L",
    "Sleep" to "hrs"
)

// Preset objective names - these get no timer/edit (not truly custom)
private val presetObjectiveNames = objectivePresets.map { it.first.lowercase() }.toSet()

// Nutrition-related custom objective names (shown under Nutrition tab)
private val nutritionRelatedNames = setOf(
    "fat", "fiber",
    "vitamin a", "vitamin b1", "vitamin b2", "vitamin b3",
    "vitamin b6", "vitamin b12", "vitamin c", "vitamin d",
    "vitamin e", "vitamin k",
    "folate", "iron", "calcium"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectiveDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, target: Float, timerSeconds: Int, notifyEnabled: Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    // Timer fields (only for custom)
    var timerMinutes by remember { mutableStateOf("") }
    var timerSecs by remember { mutableStateOf("") }
    var notifyEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Objective") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Name") },
                        placeholder = { Text("Select nutrition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        objectivePresets.forEach { (name, presetUnit) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedName = name
                                    unit = presetUnit
                                    isCustom = false
                                    expanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text("Custom...", fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                selectedName = ""
                                unit = ""
                                isCustom = true
                                expanded = false
                            }
                        )
                    }
                }

                if (isCustom) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = { selectedName = it },
                        label = { Text("Custom Name") },
                        placeholder = { Text("e.g. Creatine") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("e.g. g, mg, ml") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (unit.isNotBlank()) {
                    Text(
                        "Unit: $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Daily Target") },
                    placeholder = { Text("e.g. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Timer options (only for custom objectives)
                if (isCustom) {
                    Text(
                        "Timer (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = timerMinutes,
                            onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Min") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = timerSecs,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }
                                val num = filtered.toIntOrNull() ?: 0
                                timerSecs = if (num > 59) "59" else filtered
                            },
                            label = { Text("Sec") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notify when done", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = target.toFloatOrNull()
                    if (selectedName.isNotBlank() && unit.isNotBlank() && v != null && v > 0) {
                        val mins = timerMinutes.toIntOrNull() ?: 0
                        val secs = timerSecs.toIntOrNull() ?: 0
                        val totalTimerSeconds = if (isCustom) (mins * 60) + secs else 0
                        onSave(selectedName.trim(), unit.trim(), v, totalTimerSeconds, notifyEnabled)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SetTargetsDialog(
    viewModel: NutritionViewModel,
    onDismiss: () -> Unit
) {
    val targets by viewModel.getAllTargets().collectAsState(initial = emptyList())
    val targetMap = targets.associateBy { it.category }
    val customTargets = targets.filter { it.isCustom }

    val builtInValues = remember(targets) {
        NutritionCategory.entries.associate { cat ->
            cat.name to mutableStateOf(targetMap[cat.name]?.targetValue?.toString() ?: "0")
        }
    }

    val customValues = remember(targets) {
        customTargets.associate { t ->
            t.category to mutableStateOf(t.targetValue.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Targets") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutritionCategory.entries.forEach { cat ->
                    val state = builtInValues[cat.name]
                    if (state != null) {
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { state.value = it },
                            label = { Text("${cat.label} (${cat.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                customTargets.forEach { t ->
                    val state = customValues[t.category]
                    if (state != null) {
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { state.value = it },
                            label = { Text("${t.label} (${t.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                NutritionCategory.entries.forEach { cat ->
                    val v = builtInValues[cat.name]?.value?.toFloatOrNull()
                    if (v != null && v > 0) viewModel.setTarget(cat, v)
                }
                customTargets.forEach { t ->
                    val v = customValues[t.category]?.value?.toFloatOrNull()
                    if (v != null && v > 0) viewModel.setTargetByKey(t.category, v)
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTimerDisplay(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

@Composable
fun EditCustomObjectiveDialog(
    target: NutritionTarget,
    onDismiss: () -> Unit,
    onSave: (label: String, unit: String, timerSeconds: Int, notifyEnabled: Boolean) -> Unit
) {
    var label by remember { mutableStateOf(target.label) }
    var unit by remember { mutableStateOf(target.unit) }
    var timerMinutes by remember { mutableStateOf(
        if (target.timerSeconds > 0) (target.timerSeconds / 60).toString() else ""
    ) }
    var timerSecs by remember { mutableStateOf(
        if (target.timerSeconds > 0) (target.timerSeconds % 60).toString() else ""
    ) }
    var notifyEnabled by remember { mutableStateOf(target.timerNotifyEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Objective") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    placeholder = { Text("e.g. min, hrs, steps, days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Timer",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = timerMinutes,
                        onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = timerSecs,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            val num = filtered.toIntOrNull() ?: 0
                            timerSecs = if (num > 59) "59" else filtered
                        },
                        label = { Text("Sec") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Leave empty for no timer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify when done", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (label.isNotBlank()) {
                        val mins = timerMinutes.toIntOrNull() ?: 0
                        val secs = timerSecs.toIntOrNull() ?: 0
                        val totalSeconds = (mins * 60) + secs
                        onSave(label.trim(), unit.trim().ifBlank { "days" }, totalSeconds, notifyEnabled)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Maps custom objective labels to FoodItem field extractors
fun getCustomFoodFieldKey(label: String): String? {
    return when (label.lowercase().trim()) {
        "fat", "fats" -> "fat"
        "fiber", "fibre" -> "fiber"
        "vitamin a", "vit a" -> "vitA"
        "vitamin b1", "vit b1", "thiamine" -> "vitB1"
        "vitamin b2", "vit b2", "riboflavin" -> "vitB2"
        "vitamin b3", "vit b3", "niacin" -> "vitB3"
        "vitamin b6", "vit b6" -> "vitB6"
        "vitamin b12", "vit b12" -> "vitB12"
        "vitamin c", "vit c" -> "vitC"
        "vitamin d", "vit d" -> "vitD"
        "vitamin e", "vit e" -> "vitE"
        "vitamin k", "vit k" -> "vitK"
        "folate", "folic acid" -> "folate"
        "iron" -> "iron"
        "calcium" -> "calcium"
        "calories", "calorie", "cal" -> "calories"
        "protein" -> "protein"
        "carbs", "carbohydrates" -> "carbs"
        else -> null
    }
}

fun getFoodFieldValue(food: FoodItem, fieldKey: String): Float {
    return when (fieldKey) {
        "fat" -> food.fatPerBase
        "fiber" -> food.fiberPerBase
        "vitA" -> food.vitAPerBase
        "vitB1" -> food.vitB1PerBase
        "vitB2" -> food.vitB2PerBase
        "vitB3" -> food.vitB3PerBase
        "vitB6" -> food.vitB6PerBase
        "vitB12" -> food.vitB12PerBase
        "vitC" -> food.vitCPerBase
        "vitD" -> food.vitDPerBase
        "vitE" -> food.vitEPerBase
        "vitK" -> food.vitKPerBase
        "folate" -> food.folatePerBase
        "iron" -> food.ironPerBase
        "calcium" -> food.calciumPerBase
        "magnesium" -> food.magnesiumPerBase
        "potassium" -> food.potassiumPerBase
        "zinc" -> food.zincPerBase
        "copper" -> food.copperPerBase
        "selenium" -> food.seleniumPerBase
        "calories" -> food.caloriesPerBase
        "protein" -> food.proteinPerBase
        "carbs" -> food.carbsPerBase
        else -> 0f
    }
}

@Composable
fun CustomFoodSuggestionsDialog(
    label: String,
    unit: String,
    fieldKey: String,
    targetValue: Float,
    color: Color,
    onDismiss: () -> Unit
) {
    data class FoodNutrient(val food: FoodItem, val value: Float)

    val sortedFoods = remember(fieldKey) {
        FoodDatabase.foods.map { food ->
            FoodNutrient(food, getFoodFieldValue(food, fieldKey))
        }
            .filter { it.value > 0f }
            .sortedByDescending { it.value }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Top $label Foods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (sortedFoods.isEmpty()) {
                Text(
                    "No food data available for $label.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(sortedFoods) { item ->
                        val perLabel = if (item.food.servingUnit == com.example.gymworkout.data.ServingUnit.PIECE) {
                            "per piece"
                        } else {
                            "per 100${item.food.servingUnit.label}"
                        }
                        val dvPercent = if (targetValue > 0f) {
                            ((item.value / targetValue) * 100).toInt()
                        } else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${formatValue(item.value)}$unit $perLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (dvPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${dvPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
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

@Composable
fun FoodSuggestionsDialog(
    category: NutritionCategory,
    targetValue: Float,
    onDismiss: () -> Unit
) {
    val color = getCategoryColor(category)

    // Get nutrient value per base amount for each food, and sort descending
    data class FoodNutrient(val food: FoodItem, val valuePer100: Float)

    val sortedFoods = remember(category) {
        FoodDatabase.foods.map { food ->
            val baseAmount = food.baseAmount
            val rawValue = when (category) {
                NutritionCategory.PROTEIN -> food.proteinPerBase
                NutritionCategory.CALORIES -> food.caloriesPerBase
                NutritionCategory.CARBS -> food.carbsPerBase
                else -> 0f
            }
            // Normalize to per-100g/ml for display; pieces stay as per-piece
            val valuePer100 = if (food.servingUnit == com.example.gymworkout.data.ServingUnit.PIECE) {
                rawValue // per piece
            } else {
                rawValue // already per 100g/ml
            }
            FoodNutrient(food, valuePer100)
        }
            .filter { it.valuePer100 > 0f }
            .sortedByDescending { it.valuePer100 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    getCategoryIcon(category),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Top ${category.label} Foods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(sortedFoods) { item ->
                    val unit = category.unit
                    val perLabel = if (item.food.servingUnit == com.example.gymworkout.data.ServingUnit.PIECE) {
                        "per piece"
                    } else {
                        "per 100${item.food.servingUnit.label}"
                    }
                    val dvPercent = if (targetValue > 0f) {
                        ((item.valuePer100 / targetValue) * 100).toInt()
                    } else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.food.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatValue(item.valuePer100)}$unit $perLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dvPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${dvPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
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

fun getCategoryIcon(category: NutritionCategory): ImageVector = when (category) {
    NutritionCategory.WATER -> Icons.Default.WaterDrop
    NutritionCategory.CARBS -> Icons.Default.LocalFireDepartment
    NutritionCategory.CALORIES -> Icons.Default.LocalFireDepartment
    NutritionCategory.PROTEIN -> Icons.Default.Egg
    NutritionCategory.SLEEP -> Icons.Default.BedtimeOff
}

fun getCategoryColor(category: NutritionCategory): Color = when (category) {
    NutritionCategory.WATER -> Color(0xFF42A5F5)
    NutritionCategory.CARBS -> Color(0xFFFF8A65)
    NutritionCategory.CALORIES -> Color(0xFFFF8A65)
    NutritionCategory.PROTEIN -> Color(0xFFEF5350)
    NutritionCategory.SLEEP -> Color(0xFFAB47BC)
}

fun formatValue(v: Float): String {
    return if (v == v.toLong().toFloat()) v.toLong().toString()
    else String.format("%.1f", v)
}
