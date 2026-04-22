package com.example.gymworkout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.ai.AiCapabilityManager
import com.example.gymworkout.ui.components.AddExerciseDialog
import com.example.gymworkout.ui.components.NotesDialog
import com.example.gymworkout.ui.components.InlineRestTimer
import com.example.gymworkout.ui.components.RestTimerDialog
import com.example.gymworkout.viewmodel.WorkoutViewModel

// Group exercises: consecutive exercises with same non-empty supersetGroupId form a superset
sealed class ExerciseItem {
    data class Single(val exercise: Exercise, val displayIndex: Int) : ExerciseItem()
    data class Superset(val exercises: List<Exercise>, val displayIndex: Int) : ExerciseItem()

    val stableKey: String get() = when (this) {
        is Single -> "s_${exercise.id}"
        is Superset -> "ss_${exercises.first().id}"
    }
}

fun groupExercises(exercises: List<Exercise>): List<ExerciseItem> {
    val result = mutableListOf<ExerciseItem>()
    var i = 0
    var displayIdx = 1
    while (i < exercises.size) {
        val ex = exercises[i]
        if (ex.supersetGroupId.isNotBlank()) {
            // Collect all exercises with same supersetGroupId
            val group = mutableListOf(ex)
            var j = i + 1
            while (j < exercises.size && exercises[j].supersetGroupId == ex.supersetGroupId) {
                group.add(exercises[j])
                j++
            }
            result.add(ExerciseItem.Superset(group, displayIdx))
            displayIdx++
            i = j
        } else {
            result.add(ExerciseItem.Single(ex, displayIdx))
            displayIdx++
            i++
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    dayIndex: Int,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onPlayVideo: (exerciseName: String, youtubeUrl: String) -> Unit = { _, _ -> },
    onViewExerciseDetail: (String) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {}
) {
    val exercises by viewModel.getExercisesForDay(dayIndex).collectAsState(initial = emptyList())
    val groupedItems = remember(exercises) { groupExercises(exercises) }
    val dayHeading by viewModel.getDayHeading(dayIndex).collectAsState(initial = null)
    val aiSupported by AiCapabilityManager.isAiSupported.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showHeadingDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var notesExercise by remember { mutableStateOf<Exercise?>(null) }
    val restTimerState by viewModel.restTimerState.collectAsState()

    // Drag-and-drop state
    val listState = rememberLazyListState()
    val reorderableItems = remember { mutableStateListOf<ExerciseItem>() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    var overlapTargetIndex by remember { mutableIntStateOf(-1) }  // index of item being overlapped for superset merge
    val density = LocalDensity.current
    val spacingPx = with(density) { 10.dp.toPx() }
    val overlapThreshold = with(density) { 30.dp.toPx() }  // how close centers must be to trigger superset hint
    val view = LocalView.current

    // Sync items from DB when not dragging
    LaunchedEffect(groupedItems, draggedIndex) {
        if (draggedIndex < 0) {
            reorderableItems.clear()
            reorderableItems.addAll(groupedItems)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { showHeadingDialog = true }) {
                        Text(
                            "${dayEmojis[dayIndex]} - ${dayNames[dayIndex]}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val headingText = dayHeading?.heading.orEmpty()
                        if (headingText.isNotBlank()) {
                            Text(
                                headingText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "Tap to add heading",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        if (exercises.isNotEmpty()) {
                            val done = exercises.count { it.isCompleted }
                            Text(
                                "$done of ${exercises.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (exercises.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetDay(dayIndex) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset all checkboxes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (aiSupported == true) {
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
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Inline rest timer bar (shown when checkbox is ticked)
            restTimerState?.let { timerState ->
                if (timerState.isInline && timerState.dayIndex == dayIndex) {
                    InlineRestTimer(
                        timerState = timerState,
                        onPause = { viewModel.pauseRestTimer() },
                        onResume = { viewModel.resumeRestTimer() },
                        onReset = { viewModel.resetRestTimer() },
                        onDismiss = { viewModel.dismissRestTimer() },
                        onSwitchToPopup = { viewModel.setRestTimerInline(false) },
                        onFinished = { viewModel.markRestTimerAlertFired() }
                    )
                }
            }

        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).alpha(0.3f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No exercises yet", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap \"Add Exercise\" to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(reorderableItems, key = { _, item -> item.stableKey }) { index, item ->
                    val isDragged = index == draggedIndex
                    val isOverlapTarget = index == overlapTargetIndex && draggedIndex >= 0

                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragged) draggedOffset else 0f
                                scaleX = if (isDragged) 1.03f else if (isOverlapTarget) 1.02f else 1f
                                scaleY = if (isDragged) 1.03f else if (isOverlapTarget) 1.02f else 1f
                                alpha = if (isDragged) 0.92f else 1f
                                shadowElevation = if (isDragged) 8f else 0f
                                shape = RoundedCornerShape(16.dp)
                                clip = isDragged || isOverlapTarget
                            }
                            .then(
                                if (isOverlapTarget) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.tertiary,
                                    RoundedCornerShape(16.dp)
                                ) else Modifier
                            )
                            .then(if (!isDragged) Modifier.animateItem() else Modifier)
                            .pointerInput(item.stableKey) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        val currentIdx = reorderableItems.indexOfFirst { it.stableKey == item.stableKey }
                                        if (currentIdx >= 0) {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            draggedIndex = currentIdx
                                            draggedOffset = 0f
                                            overlapTargetIndex = -1
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggedOffset += dragAmount.y

                                        val layoutInfo = listState.layoutInfo
                                        val draggedItemInfo = layoutInfo.visibleItemsInfo
                                            .find { it.index == draggedIndex }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val draggedCenter = draggedItemInfo.offset + draggedItemInfo.size / 2 + draggedOffset.toInt()

                                        // Check overlap with adjacent items for superset merge hint
                                        var newOverlap = -1
                                        for (visibleItem in layoutInfo.visibleItemsInfo) {
                                            if (visibleItem.index == draggedIndex) continue
                                            val itemCenter = visibleItem.offset + visibleItem.size / 2
                                            val distance = kotlin.math.abs(draggedCenter - itemCenter)
                                            if (distance < overlapThreshold) {
                                                newOverlap = visibleItem.index
                                                break
                                            }
                                        }

                                        if (newOverlap != overlapTargetIndex) {
                                            overlapTargetIndex = newOverlap
                                            if (newOverlap >= 0) {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                        }

                                        // Only swap if NOT overlapping (normal reorder)
                                        if (overlapTargetIndex < 0) {
                                            // Check swap with item above
                                            if (draggedIndex > 0) {
                                                val above = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex - 1 }
                                                if (above != null && draggedCenter < above.offset + above.size / 2) {
                                                    reorderableItems.add(draggedIndex - 1, reorderableItems.removeAt(draggedIndex))
                                                    draggedOffset += above.size.toFloat() + spacingPx
                                                    draggedIndex--
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                }
                                            }
                                            // Check swap with item below
                                            if (draggedIndex < reorderableItems.size - 1) {
                                                val below = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex + 1 }
                                                if (below != null && draggedCenter > below.offset + below.size / 2) {
                                                    reorderableItems.add(draggedIndex + 1, reorderableItems.removeAt(draggedIndex))
                                                    draggedOffset -= below.size.toFloat() + spacingPx
                                                    draggedIndex++
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (overlapTargetIndex >= 0 && overlapTargetIndex < reorderableItems.size) {
                                            // Merge into superset
                                            val draggedItem = reorderableItems[draggedIndex]
                                            val targetItem = reorderableItems[overlapTargetIndex]
                                            val draggedExercises = when (draggedItem) {
                                                is ExerciseItem.Single -> listOf(draggedItem.exercise)
                                                is ExerciseItem.Superset -> draggedItem.exercises
                                            }
                                            val targetExercises = when (targetItem) {
                                                is ExerciseItem.Single -> listOf(targetItem.exercise)
                                                is ExerciseItem.Superset -> targetItem.exercises
                                            }
                                            viewModel.mergeIntoSuperset(draggedExercises, targetExercises)
                                        } else {
                                            // Normal reorder
                                            val orderedExercises = reorderableItems.flatMap { reorderItem ->
                                                when (reorderItem) {
                                                    is ExerciseItem.Single -> listOf(reorderItem.exercise)
                                                    is ExerciseItem.Superset -> reorderItem.exercises
                                                }
                                            }
                                            viewModel.reorderExercises(orderedExercises)
                                        }
                                        draggedIndex = -1
                                        draggedOffset = 0f
                                        overlapTargetIndex = -1
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        draggedOffset = 0f
                                        overlapTargetIndex = -1
                                    }
                                )
                            }
                    ) {
                        when (item) {
                            is ExerciseItem.Single -> {
                                ExerciseCard(
                                    index = item.displayIndex,
                                    exercise = item.exercise,
                                    onIncrementSet = { viewModel.incrementSet(item.exercise) },
                                    onDecrementSet = { viewModel.decrementSet(item.exercise) },
                                    onPlayVideo = { onPlayVideo(item.exercise.name, item.exercise.youtubeUrl) },
                                    onExerciseClick = { onViewExerciseDetail(item.exercise.name) },
                                    onEditNotes = { notesExercise = item.exercise },
                                    onEdit = { editingExercise = item.exercise },
                                    onDelete = { viewModel.deleteExercise(item.exercise) },
                                    onStartRest = { viewModel.startRestTimer(item.exercise.name, item.exercise.restTimeSeconds, dayIndex) }
                                )
                            }
                            is ExerciseItem.Superset -> {
                                SupersetCard(
                                    displayIndex = item.displayIndex,
                                    exercises = item.exercises,
                                    onIncrementSet = { ex -> viewModel.incrementSet(ex) },
                                    onDecrementSet = { ex -> viewModel.decrementSet(ex) },
                                    onPlayVideo = { ex -> onPlayVideo(ex.name, ex.youtubeUrl) },
                                    onExerciseClick = { ex -> onViewExerciseDetail(ex.name) },
                                    onEditNotes = { ex -> notesExercise = ex },
                                    onEdit = { ex -> editingExercise = ex },
                                    onDelete = { ex -> viewModel.deleteExercise(ex) },
                                    onStartRest = { ex -> viewModel.startRestTimer(ex.name, ex.restTimeSeconds, dayIndex) }
                                )
                            }
                        }

                        // Superset merge indicator overlay
                        if (isOverlapTarget) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiary)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "Drop to create Superset",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        } // Column
    }

    if (showAddDialog) {
        AddExerciseDialog(
            dayOfWeek = dayIndex,
            exerciseCount = exercises.size,
            onDismiss = { showAddDialog = false },
            onSave = { exercise ->
                viewModel.addExercise(exercise)
                showAddDialog = false
            },
            onSaveSuperset = { ex1, ex2 ->
                viewModel.addExercise(ex1)
                viewModel.addExercise(ex2)
                showAddDialog = false
            },
            onViewExerciseDetail = { exerciseId ->
                showAddDialog = false
                onViewExerciseDetail(exerciseId)
            }
        )
    }

    if (editingExercise != null) {
        AddExerciseDialog(
            dayOfWeek = dayIndex,
            exerciseCount = exercises.size,
            existingExercise = editingExercise,
            onDismiss = { editingExercise = null },
            onSave = { exercise ->
                viewModel.updateExercise(exercise)
                editingExercise = null
            },
            onConvertToSuperset = { existing, newEx ->
                viewModel.convertToSuperset(existing, newEx)
                editingExercise = null
            },
            onViewExerciseDetail = { exerciseId ->
                editingExercise = null
                onViewExerciseDetail(exerciseId)
            }
        )
    }

    if (notesExercise != null) {
        NotesDialog(
            exerciseName = notesExercise!!.name,
            currentNotes = notesExercise!!.notes,
            onDismiss = { notesExercise = null },
            onSave = { notes ->
                viewModel.updateNotes(notesExercise!!.id, notes)
                notesExercise = null
            }
        )
    }

    restTimerState?.let { timerState ->
        if (!timerState.isInline) {
            RestTimerDialog(
                timerState = timerState,
                onPause = { viewModel.pauseRestTimer() },
                onResume = { viewModel.resumeRestTimer() },
                onReset = { viewModel.resetRestTimer() },
                onDismiss = { viewModel.dismissRestTimer() },
                onSetInline = { viewModel.setRestTimerInline(true) },
                onFinished = { viewModel.markRestTimerAlertFired() }
            )
        }
    }

    if (showHeadingDialog) {
        DayHeadingDialog(
            currentHeading = dayHeading?.heading ?: "",
            dayName = dayNames[dayIndex],
            onDismiss = { showHeadingDialog = false },
            onSave = { heading ->
                viewModel.saveDayHeading(dayIndex, heading)
                showHeadingDialog = false
            }
        )
    }
}

@Composable
fun DayHeadingDialog(
    currentHeading: String,
    dayName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentHeading) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$dayName Heading") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Heading") },
                placeholder = { Text("e.g. Shoulder + Legs") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --- Superset Card ---

@Composable
fun SupersetCard(
    displayIndex: Int,
    exercises: List<Exercise>,
    onIncrementSet: (Exercise) -> Unit,
    onDecrementSet: (Exercise) -> Unit,
    onPlayVideo: (Exercise) -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onEditNotes: (Exercise) -> Unit,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit,
    onStartRest: (Exercise) -> Unit
) {
    val allDone = exercises.all { it.isCompleted }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (allDone) BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Superset header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "SUPERSET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$displayIndex",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Combined set counter for the whole superset
                val refExercise = exercises.first()
                SetCounter(
                    completedSets = refExercise.completedSets,
                    totalSets = refExercise.sets,
                    onIncrement = { onIncrementSet(refExercise) },
                    onDecrement = { onDecrementSet(refExercise) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            exercises.forEachIndexed { idx, exercise ->
                SupersetExerciseRow(
                    exercise = exercise,
                    label = if (exercises.size > 1) "${('A' + idx)}" else null,
                    onPlayVideo = { onPlayVideo(exercise) },
                    onExerciseClick = { onExerciseClick(exercise) },
                    onEditNotes = { onEditNotes(exercise) },
                    onEdit = { onEdit(exercise) },
                    onDelete = { onDelete(exercise) }
                )
                if (idx < exercises.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Rest timer for the superset (use last exercise's rest time)
            val restExercise = exercises.lastOrNull { it.restTimeSeconds > 0 }
            if (restExercise != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .clickable { onStartRest(restExercise) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Start rest timer",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Rest ${restExercise.restTimeSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupersetExerciseRow(
    exercise: Exercise,
    label: String?,
    onPlayVideo: () -> Unit,
    onExerciseClick: () -> Unit,
    onEditNotes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val allSetsDone = exercise.completedSets >= exercise.sets

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (label != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExerciseClick() }
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (allSetsDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (allSetsDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayVideo, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (exercise.youtubeUrl.isNotBlank()) Icons.Default.PlayCircle else Icons.Default.Search,
                    contentDescription = if (exercise.youtubeUrl.isNotBlank()) "Play video" else "Search YouTube",
                    tint = if (exercise.youtubeUrl.isNotBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
}

        // Badges
        Row(
            modifier = Modifier.padding(start = if (label != null) 28.dp else 0.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${exercise.sets} sets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${exercise.reps} reps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        // Notes preview
        if (exercise.notes.isNotBlank()) {
            Text(
                exercise.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = if (label != null) 28.dp else 0.dp, top = 4.dp)
            )
        }

        // Action icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (label != null) 20.dp else 0.dp, top = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onEditNotes, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "Notes",
                    tint = if (exercise.notes.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- Single Exercise Card (unchanged) ---

@Composable
fun ExerciseCard(
    index: Int,
    exercise: Exercise,
    onIncrementSet: () -> Unit,
    onDecrementSet: () -> Unit,
    onPlayVideo: () -> Unit,
    onExerciseClick: () -> Unit = {},
    onEditNotes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartRest: () -> Unit = {}
) {
    val allSetsDone = exercise.completedSets >= exercise.sets

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (allSetsDone) BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (allSetsDone) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (allSetsDone) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Set counter
                SetCounter(
                    completedSets = exercise.completedSets,
                    totalSets = exercise.sets,
                    onIncrement = onIncrementSet,
                    onDecrement = onDecrementSet
                )

                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExerciseClick() }
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (allSetsDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (allSetsDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPlayVideo, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (exercise.youtubeUrl.isNotBlank()) Icons.Default.PlayCircle else Icons.Default.Search,
                        contentDescription = if (exercise.youtubeUrl.isNotBlank()) "Play YouTube video" else "Search YouTube",
                        tint = if (exercise.youtubeUrl.isNotBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${exercise.sets} sets", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${exercise.reps} reps", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (exercise.restTimeSeconds > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${exercise.restTimeSeconds}s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }

            if (exercise.notes.isNotBlank()) {
                Text(exercise.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 76.dp, top = 6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (exercise.restTimeSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .clickable(onClick = onStartRest)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = "Start rest timer", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rest ${exercise.restTimeSeconds}s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                IconButton(onClick = onEditNotes, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add/edit notes", tint = if (exercise.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit exercise", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete exercise", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SetCounter(
    completedSets: Int,
    totalSets: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val allDone = completedSets >= totalSets

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Minus button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (completedSets > 0) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(enabled = completedSets > 0) { onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "−",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (completedSets > 0) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }

        // Count display
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (allDone) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$completedSets/$totalSets",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (allDone) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (!allDone) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(enabled = !allDone) { onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (!allDone) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
