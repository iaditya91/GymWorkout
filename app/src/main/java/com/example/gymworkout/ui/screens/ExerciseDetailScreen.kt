package com.example.gymworkout.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.gymworkout.data.ExerciseInfo
import com.example.gymworkout.data.ExerciseRepository
import com.example.gymworkout.data.MuscleTarget
import com.example.gymworkout.ui.components.MuscleBodyMapCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    onBack: () -> Unit,
    onNavigateToExercise: (String) -> Unit = {}
) {
    val foundExercise = remember { ExerciseRepository.findByName(exerciseName) }
    val isCustom = foundExercise == null
    var isEditing by remember { mutableStateOf(isCustom) }

    // Editable state
    var force by remember { mutableStateOf(foundExercise?.force ?: "") }
    var level by remember { mutableStateOf(foundExercise?.level ?: "") }
    var mechanic by remember { mutableStateOf(foundExercise?.mechanic ?: "") }
    var equipment by remember { mutableStateOf(foundExercise?.equipment ?: "") }
    var category by remember { mutableStateOf(foundExercise?.category ?: "") }
    val primaryMuscleNames = remember {
        mutableStateListOf<String>().apply {
            addAll(foundExercise?.primaryMuscles?.map { it.target } ?: emptyList())
        }
    }
    val secondaryMuscleNames = remember {
        mutableStateListOf<String>().apply {
            addAll(foundExercise?.secondaryMuscles?.map { it.target } ?: emptyList())
        }
    }
    val instructions = remember {
        mutableStateListOf<String>().apply { addAll(foundExercise?.instructions ?: emptyList()) }
    }

    val generatedId = remember(exerciseName) { ExerciseRepository.generateId(exerciseName) }
    val exerciseId = foundExercise?.id ?: generatedId

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        exerciseName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (isEditing) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val exercise = ExerciseInfo(
                            name = exerciseName,
                            force = force.ifBlank { null },
                            level = level,
                            mechanic = mechanic.ifBlank { null },
                            equipment = equipment.ifBlank { null },
                            primaryMuscles = primaryMuscleNames.map { name ->
                                foundExercise?.primaryMuscles?.find { it.target == name }
                                    ?: MuscleTarget(target = name)
                            },
                            secondaryMuscles = secondaryMuscleNames.map { name ->
                                foundExercise?.secondaryMuscles?.find { it.target == name }
                                    ?: MuscleTarget(target = name)
                            },
                            instructions = instructions.toList(),
                            category = category,
                            images = foundExercise?.images ?: emptyList(),
                            id = exerciseId
                        )
                        ExerciseRepository.save(exercise)
                        isEditing = false
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick stats
            if (isEditing) {
                EditableQuickStatsCard(
                    force = force,
                    onForceChange = { force = it },
                    level = level,
                    onLevelChange = { level = it },
                    mechanic = mechanic,
                    onMechanicChange = { mechanic = it }
                )
            } else {
                QuickStatsRow(force = force, level = level, mechanic = mechanic)
            }

            // Muscle body map visualization
            if (!isEditing) {
                MuscleBodyMapCard(
                    primaryMuscles = foundExercise?.primaryMuscles ?: primaryMuscleNames.map { MuscleTarget(target = it) },
                    secondaryMuscles = foundExercise?.secondaryMuscles ?: secondaryMuscleNames.map { MuscleTarget(target = it) }
                )
            }

            // Muscles
            MusclesCard(
                primaryMuscles = foundExercise?.primaryMuscles ?: primaryMuscleNames.map { MuscleTarget(target = it) },
                secondaryMuscles = foundExercise?.secondaryMuscles ?: secondaryMuscleNames.map { MuscleTarget(target = it) },
                isEditable = isEditing,
                editablePrimaryNames = primaryMuscleNames,
                editableSecondaryNames = secondaryMuscleNames,
                onAddPrimary = { primaryMuscleNames.add(it) },
                onRemovePrimary = { primaryMuscleNames.removeAt(it) },
                onAddSecondary = { secondaryMuscleNames.add(it) },
                onRemoveSecondary = { secondaryMuscleNames.removeAt(it) }
            )

            // Instructions
            InstructionsCard(
                instructions = instructions,
                isEditable = isEditing,
                onAddInstruction = { instructions.add(it) },
                onRemoveInstruction = { instructions.removeAt(it) },
                onUpdateInstruction = { index, text -> instructions[index] = text }
            )

            // Meta info
            if (isEditing) {
                EditableMetaInfoCard(
                    category = category,
                    onCategoryChange = { category = it },
                    equipment = equipment,
                    onEquipmentChange = { equipment = it },
                    id = exerciseId
                )
            } else {
                MetaInfoCard(
                    category = category,
                    equipment = equipment,
                    force = force,
                    level = level,
                    mechanic = mechanic,
                    id = exerciseId
                )
            }

            // Replacement exercises
            if (!isEditing && (foundExercise?.replacementExercises?.isNotEmpty() == true)) {
                ReplacementExercisesCard(
                    replacements = foundExercise.replacementExercises,
                    onExerciseClick = onNavigateToExercise
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ─── Quick Stats (read-only) ──────────────────────────────────────────

@Composable
private fun QuickStatsRow(force: String, level: String, mechanic: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            icon = Icons.Default.Speed,
            label = "Force",
            value = force.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Star,
            label = "Level",
            value = level.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() },
            color = when (level.lowercase()) {
                "beginner" -> Color(0xFF4CAF50)
                "intermediate" -> Color(0xFFFFA726)
                "advanced" -> Color(0xFFEF5350)
                else -> MaterialTheme.colorScheme.secondary
            },
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Settings,
            label = "Mechanic",
            value = mechanic.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() },
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

// ─── Quick Stats (editable) ──────────────────────────────────────────

@Composable
private fun EditableQuickStatsCard(
    force: String,
    onForceChange: (String) -> Unit,
    level: String,
    onLevelChange: (String) -> Unit,
    mechanic: String,
    onMechanicChange: (String) -> Unit
) {
    val levelColor = when (level.lowercase()) {
        "beginner" -> Color(0xFF4CAF50)
        "intermediate" -> Color(0xFFFFA726)
        "advanced" -> Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditableStatChip(
            icon = Icons.Default.Speed,
            label = "Force",
            value = force,
            color = MaterialTheme.colorScheme.primary,
            options = listOf("push", "pull", "static"),
            onSelect = onForceChange,
            modifier = Modifier.weight(1f)
        )
        EditableStatChip(
            icon = Icons.Default.Star,
            label = "Level",
            value = level,
            color = levelColor,
            options = listOf("beginner", "intermediate", "advanced"),
            onSelect = onLevelChange,
            modifier = Modifier.weight(1f)
        )
        EditableStatChip(
            icon = Icons.Default.Settings,
            label = "Mechanic",
            value = mechanic,
            color = MaterialTheme.colorScheme.tertiary,
            options = listOf("compound", "isolation"),
            onSelect = onMechanicChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value.ifBlank { "Select" }.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 140.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Muscles Card ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusclesCard(
    primaryMuscles: List<MuscleTarget>,
    secondaryMuscles: List<MuscleTarget>,
    isEditable: Boolean,
    editablePrimaryNames: List<String>,
    editableSecondaryNames: List<String>,
    onAddPrimary: (String) -> Unit,
    onRemovePrimary: (Int) -> Unit,
    onAddSecondary: (String) -> Unit,
    onRemoveSecondary: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Target Muscles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Primary muscles
            Text("Primary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            if (isEditable) {
                if (editablePrimaryNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        editablePrimaryNames.forEachIndexed { index, muscle ->
                            MuscleChip(
                                muscle = muscle.replaceFirstChar { it.uppercase() },
                                isPrimary = true,
                                isEditable = true,
                                onRemove = { onRemovePrimary(index) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                MuscleDropdown(
                    placeholder = "Add primary muscle",
                    existingMuscles = editablePrimaryNames + editableSecondaryNames,
                    onAdd = onAddPrimary
                )
            } else {
                if (primaryMuscles.isNotEmpty()) {
                    primaryMuscles.forEach { mt ->
                        MuscleWithSubTargets(muscleTarget = mt, isPrimary = true)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    Text("None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Secondary muscles
            Spacer(modifier = Modifier.height(12.dp))
            Text("Secondary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            if (isEditable) {
                if (editableSecondaryNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        editableSecondaryNames.forEachIndexed { index, muscle ->
                            MuscleChip(
                                muscle = muscle.replaceFirstChar { it.uppercase() },
                                isPrimary = false,
                                isEditable = true,
                                onRemove = { onRemoveSecondary(index) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                MuscleDropdown(
                    placeholder = "Add secondary muscle",
                    existingMuscles = editablePrimaryNames + editableSecondaryNames,
                    onAdd = onAddSecondary
                )
            } else {
                if (secondaryMuscles.isNotEmpty()) {
                    secondaryMuscles.forEach { mt ->
                        MuscleWithSubTargets(muscleTarget = mt, isPrimary = false)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    Text("None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleWithSubTargets(muscleTarget: MuscleTarget, isPrimary: Boolean) {
    Column {
        MuscleChip(
            muscle = muscleTarget.target.replaceFirstChar { it.uppercase() },
            isPrimary = isPrimary
        )
        if (muscleTarget.subTargets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                muscleTarget.subTargets.forEach { sub ->
                    SubTargetChip(
                        label = sub.replaceFirstChar { it.uppercase() },
                        isPrimary = isPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleChip(muscle: String, isPrimary: Boolean, isEditable: Boolean = false, onRemove: () -> Unit = {}) {
    val bgColor = if (isPrimary)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    val textColor = if (isPrimary)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(start = 14.dp, end = if (isEditable) 4.dp else 14.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                muscle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (isEditable) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textColor,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onRemove() }
                        .padding(start = 2.dp)
                )
            }
        }
    }
}

private val allMuscleGroups = listOf(
    "abdominals", "abductors", "adductors", "biceps", "calves",
    "chest", "forearms", "glutes", "hamstrings", "lats",
    "lower back", "middle back", "neck", "quadriceps", "shoulders",
    "traps", "triceps"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDropdown(
    placeholder: String,
    existingMuscles: List<String>,
    onAdd: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val available = allMuscleGroups.filter { it !in existingMuscles }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (available.isNotEmpty()) expanded = it }
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.Add, contentDescription = "Add muscle", tint = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(50.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            available.forEach { muscle ->
                DropdownMenuItem(
                    text = { Text(muscle.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onAdd(muscle)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Instructions Card ───────────────────────────────────────────────

@Composable
private fun InstructionsCard(
    instructions: List<String>,
    isEditable: Boolean,
    onAddInstruction: (String) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onUpdateInstruction: (Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Collapse" else "Expand")
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (instructions.isEmpty() && !isEditable) {
                        Text("No instructions available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    instructions.forEachIndexed { index, instruction ->
                        if (isEditable) {
                            EditableInstructionStep(
                                stepNumber = index + 1,
                                instruction = instruction,
                                onUpdate = { onUpdateInstruction(index, it) },
                                onRemove = { onRemoveInstruction(index) }
                            )
                        } else {
                            InstructionStep(stepNumber = index + 1, instruction = instruction)
                        }
                    }

                    if (isEditable) {
                        AddInstructionField(onAdd = onAddInstruction)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(stepNumber: Int, instruction: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$stepNumber",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            instruction,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditableInstructionStep(
    stepNumber: Int,
    instruction: String,
    onUpdate: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$stepNumber",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = instruction,
            onValueChange = onUpdate,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddInstructionField(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Add instruction step...", style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onAdd(text.trim())
                    text = ""
                }
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Meta Info (read-only) ───────────────────────────────────────────

@Composable
private fun MetaInfoCard(
    category: String,
    equipment: String,
    force: String,
    level: String,
    mechanic: String,
    id: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            MetaRow("Category", category.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() })
            MetaRow("Equipment", equipment.ifBlank { "None" }.replaceFirstChar { it.uppercase() })
            MetaRow("Force", force.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() })
            MetaRow("Level", level.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() })
            MetaRow("Mechanic", mechanic.ifBlank { "N/A" }.replaceFirstChar { it.uppercase() })
            MetaRow("ID", id.ifBlank { "N/A" })
        }
    }
}

// ─── Meta Info (editable) ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableMetaInfoCard(
    category: String,
    onCategoryChange: (String) -> Unit,
    equipment: String,
    onEquipmentChange: (String) -> Unit,
    id: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            EditableMetaRow(
                label = "Category",
                value = category,
                options = listOf("strength", "stretching", "cardio", "powerlifting", "strongman", "olympic weightlifting", "plyometrics"),
                onSelect = onCategoryChange
            )
            EditableMetaRow(
                label = "Equipment",
                value = equipment,
                options = listOf("body only", "machine", "dumbbell", "barbell", "cable", "kettlebells", "bands", "medicine ball", "exercise ball", "foam roll", "e-z curl bar", "other"),
                onSelect = onEquipmentChange
            )
            MetaRow("ID", id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableMetaRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .menuAnchor(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value.ifBlank { "Select" }.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Sub-Target Chip ─────────────────────────────────────────────────

@Composable
private fun SubTargetChip(label: String, isPrimary: Boolean) {
    val borderColor = if (isPrimary)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    val textColor = if (isPrimary)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    else
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

// ─── Replacement Exercises Card ──────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReplacementExercisesCard(
    replacements: List<com.example.gymworkout.data.ReplacementExercise>,
    onExerciseClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Replacement Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                replacements.forEach { replacement ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { onExerciseClick(replacement.name) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            replacement.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ─── Shared ──────────────────────────────────────────────────────────

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
