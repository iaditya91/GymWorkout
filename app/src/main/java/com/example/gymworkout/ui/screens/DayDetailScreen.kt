package com.example.gymworkout.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.Exercise
import com.example.gymworkout.ui.components.AddExerciseDialog
import com.example.gymworkout.ui.components.NotesDialog
import com.example.gymworkout.ui.components.RestTimerDialog
import com.example.gymworkout.viewmodel.WorkoutViewModel

// Group exercises: consecutive exercises with same non-empty supersetGroupId form a superset
sealed class ExerciseItem {
    data class Single(val exercise: Exercise, val displayIndex: Int) : ExerciseItem()
    data class Superset(val exercises: List<Exercise>, val displayIndex: Int) : ExerciseItem()
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
    onPlayVideo: (exerciseName: String, youtubeUrl: String) -> Unit = { _, _ -> }
) {
    val exercises by viewModel.getExercisesForDay(dayIndex).collectAsState(initial = emptyList())
    val groupedItems = remember(exercises) { groupExercises(exercises) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var notesExercise by remember { mutableStateOf<Exercise?>(null) }
    var restTimerExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${dayEmojis[dayIndex]} - ${dayNames[dayIndex]}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
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
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupedItems, key = { item ->
                    when (item) {
                        is ExerciseItem.Single -> "s_${item.exercise.id}"
                        is ExerciseItem.Superset -> "ss_${item.exercises.first().supersetGroupId}"
                    }
                }) { item ->
                    when (item) {
                        is ExerciseItem.Single -> {
                            ExerciseCard(
                                index = item.displayIndex,
                                exercise = item.exercise,
                                onToggleCompleted = { viewModel.toggleCompleted(item.exercise.id, !item.exercise.isCompleted) },
                                onPlayVideo = { if (item.exercise.youtubeUrl.isNotBlank()) onPlayVideo(item.exercise.name, item.exercise.youtubeUrl) },
                                onEditNotes = { notesExercise = item.exercise },
                                onEdit = { editingExercise = item.exercise },
                                onDelete = { viewModel.deleteExercise(item.exercise) },
                                onStartRest = { restTimerExercise = item.exercise }
                            )
                        }
                        is ExerciseItem.Superset -> {
                            SupersetCard(
                                displayIndex = item.displayIndex,
                                exercises = item.exercises,
                                onToggleCompleted = { ex -> viewModel.toggleCompleted(ex.id, !ex.isCompleted) },
                                onPlayVideo = { ex -> if (ex.youtubeUrl.isNotBlank()) onPlayVideo(ex.name, ex.youtubeUrl) },
                                onEditNotes = { ex -> notesExercise = ex },
                                onEdit = { ex -> editingExercise = ex },
                                onDelete = { ex -> viewModel.deleteExercise(ex) },
                                onStartRest = { ex -> restTimerExercise = ex }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
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

    if (restTimerExercise != null) {
        RestTimerDialog(
            exerciseName = restTimerExercise!!.name,
            totalSeconds = restTimerExercise!!.restTimeSeconds,
            onDismiss = { restTimerExercise = null }
        )
    }
}

// --- Superset Card ---

@Composable
fun SupersetCard(
    displayIndex: Int,
    exercises: List<Exercise>,
    onToggleCompleted: (Exercise) -> Unit,
    onPlayVideo: (Exercise) -> Unit,
    onEditNotes: (Exercise) -> Unit,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit,
    onStartRest: (Exercise) -> Unit
) {
    val allDone = exercises.all { it.isCompleted }
    val bgColor by animateColorAsState(
        targetValue = if (allDone)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        label = "supersetBg"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
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
            }
            Spacer(modifier = Modifier.height(8.dp))

            exercises.forEachIndexed { idx, exercise ->
                SupersetExerciseRow(
                    exercise = exercise,
                    label = if (exercises.size > 1) "${('A' + idx)}" else null,
                    onToggleCompleted = { onToggleCompleted(exercise) },
                    onPlayVideo = { onPlayVideo(exercise) },
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
    onToggleCompleted: () -> Unit,
    onPlayVideo: () -> Unit,
    onEditNotes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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

            Checkbox(
                checked = exercise.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(36.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlayVideo() }
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (exercise.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (exercise.youtubeUrl.isNotBlank()) {
                IconButton(onClick = onPlayVideo, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play video",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Badges
        Row(
            modifier = Modifier.padding(start = if (label != null) 64.dp else 44.dp, top = 2.dp),
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
                modifier = Modifier.padding(start = if (label != null) 64.dp else 44.dp, top = 4.dp)
            )
        }

        // Action icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (label != null) 56.dp else 36.dp, top = 2.dp),
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
    onToggleCompleted: () -> Unit,
    onPlayVideo: () -> Unit,
    onEditNotes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartRest: () -> Unit = {}
) {
    val bgColor by animateColorAsState(
        targetValue = if (exercise.isCompleted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
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
                            if (exercise.isCompleted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (exercise.isCompleted) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = exercise.isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.tertiary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPlayVideo() }
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (exercise.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (exercise.youtubeUrl.isNotBlank()) {
                    IconButton(onClick = onPlayVideo, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play YouTube video", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                    }
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
