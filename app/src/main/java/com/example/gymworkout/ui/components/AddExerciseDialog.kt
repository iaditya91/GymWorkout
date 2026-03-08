package com.example.gymworkout.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.Exercise
import java.util.UUID

@Composable
fun AddExerciseDialog(
    dayOfWeek: Int,
    exerciseCount: Int,
    existingExercise: Exercise? = null,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    onSaveSuperset: (Exercise, Exercise) -> Unit = { _, _ -> }
) {
    val isEditing = existingExercise != null
    var isSuperset by remember { mutableStateOf(false) }

    // Exercise 1
    var name by remember { mutableStateOf(existingExercise?.name ?: "") }
    var youtubeUrl by remember { mutableStateOf(existingExercise?.youtubeUrl ?: "") }
    var sets by remember { mutableStateOf(existingExercise?.sets?.toString() ?: "3") }
    var reps by remember { mutableStateOf(existingExercise?.reps ?: "10-12") }
    var restTime by remember {
        mutableStateOf(
            if (existingExercise != null && existingExercise.restTimeSeconds > 0)
                existingExercise.restTimeSeconds.toString()
            else ""
        )
    }

    // Exercise 2 (superset)
    var name2 by remember { mutableStateOf("") }
    var youtubeUrl2 by remember { mutableStateOf("") }
    var sets2 by remember { mutableStateOf("3") }
    var reps2 by remember { mutableStateOf("10-12") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isEditing -> "Edit Exercise"
                    isSuperset -> "Add Superset"
                    else -> "Add Exercise"
                }
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Superset checkbox (only for new exercises)
                if (!isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isSuperset,
                            onCheckedChange = { isSuperset = it }
                        )
                        Text(
                            "Superset (2 exercises back-to-back)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Exercise 1 label for superset
                if (isSuperset) {
                    Text(
                        "Exercise 1",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                ExerciseFields(
                    name = name,
                    onNameChange = { name = it },
                    youtubeUrl = youtubeUrl,
                    onYoutubeUrlChange = { youtubeUrl = it },
                    sets = sets,
                    onSetsChange = { sets = it },
                    reps = reps,
                    onRepsChange = { reps = it }
                )

                // Exercise 2 fields for superset
                if (isSuperset) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Exercise 2",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExerciseFields(
                        name = name2,
                        onNameChange = { name2 = it },
                        youtubeUrl = youtubeUrl2,
                        onYoutubeUrlChange = { youtubeUrl2 = it },
                        sets = sets2,
                        onSetsChange = { sets2 = it },
                        reps = reps2,
                        onRepsChange = { reps2 = it }
                    )
                }

                // Rest time (shared for superset)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = restTime,
                    onValueChange = { restTime = it },
                    label = {
                        Text(
                            if (isSuperset) "Rest after superset (seconds, optional)"
                            else "Rest Time (seconds, optional)"
                        )
                    },
                    placeholder = { Text("e.g. 60") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isSuperset && !isEditing) {
                        if (name.isNotBlank() && name2.isNotBlank()) {
                            val groupId = UUID.randomUUID().toString()
                            val rest = restTime.toIntOrNull() ?: 0
                            val ex1 = Exercise(
                                dayOfWeek = dayOfWeek,
                                name = name.trim(),
                                youtubeUrl = youtubeUrl.trim(),
                                sets = sets.toIntOrNull() ?: 3,
                                reps = reps.ifBlank { "10-12" },
                                restTimeSeconds = 0,
                                orderIndex = exerciseCount,
                                supersetGroupId = groupId
                            )
                            val ex2 = Exercise(
                                dayOfWeek = dayOfWeek,
                                name = name2.trim(),
                                youtubeUrl = youtubeUrl2.trim(),
                                sets = sets2.toIntOrNull() ?: 3,
                                reps = reps2.ifBlank { "10-12" },
                                restTimeSeconds = rest,
                                orderIndex = exerciseCount + 1,
                                supersetGroupId = groupId
                            )
                            onSaveSuperset(ex1, ex2)
                        }
                    } else if (name.isNotBlank()) {
                        val exercise = Exercise(
                            id = existingExercise?.id ?: 0,
                            dayOfWeek = dayOfWeek,
                            name = name.trim(),
                            youtubeUrl = youtubeUrl.trim(),
                            sets = sets.toIntOrNull() ?: 3,
                            reps = reps.ifBlank { "10-12" },
                            restTimeSeconds = restTime.toIntOrNull() ?: 0,
                            isCompleted = existingExercise?.isCompleted ?: false,
                            notes = existingExercise?.notes ?: "",
                            orderIndex = existingExercise?.orderIndex ?: exerciseCount,
                            supersetGroupId = existingExercise?.supersetGroupId ?: ""
                        )
                        onSave(exercise)
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

@Composable
private fun ExerciseFields(
    name: String,
    onNameChange: (String) -> Unit,
    youtubeUrl: String,
    onYoutubeUrlChange: (String) -> Unit,
    sets: String,
    onSetsChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Exercise Name") },
        placeholder = { Text("e.g. Dumbbell Shoulder Press") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = youtubeUrl,
        onValueChange = onYoutubeUrlChange,
        label = { Text("YouTube Link (optional)") },
        placeholder = { Text("https://youtube.com/...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = sets,
            onValueChange = onSetsChange,
            label = { Text("Sets") },
            placeholder = { Text("3") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = reps,
            onValueChange = onRepsChange,
            label = { Text("Reps") },
            placeholder = { Text("10-12") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
