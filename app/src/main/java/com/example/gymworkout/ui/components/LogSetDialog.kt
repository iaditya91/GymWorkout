package com.example.gymworkout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.VolumeAnalytics

/**
 * One entry row in the dialog. For a single exercise the list has 1 entry;
 * for a superset one per exercise in the group.
 */
data class LogSetEntry(
    val exerciseId: Int,
    val exerciseName: String,
    val setIndex: Int,           // 1-based: which set is being logged
    val totalSets: Int,
    val prefillReps: Int,
    val prefillWeightKg: Double
)

/** Result returned on Save: parallel arrays keyed to the input entries. */
data class LogSetResult(
    val reps: List<Int>,
    val weightKg: List<Double>
)

@Composable
fun LogSetDialog(
    entries: List<LogSetEntry>,
    displayUnit: String,   // "kg" or "lb"
    onDismiss: () -> Unit,
    onSave: (LogSetResult) -> Unit
) {
    // Local editable state per row. Weight is shown in user's preferred unit.
    val repsState = remember(entries) {
        entries.map { mutableStateOf(it.prefillReps.takeIf { r -> r > 0 }?.toString() ?: "") }
    }
    val weightState = remember(entries) {
        entries.map {
            val displayVal = VolumeAnalytics.kgToDisplay(it.prefillWeightKg, displayUnit)
            val s = if (displayVal <= 0.0) "" else {
                val raw = "%.2f".format(displayVal)
                // trim trailing zeros: 20.00 -> 20, 22.50 -> 22.5
                raw.trimEnd('0').trimEnd('.')
            }
            mutableStateOf(s)
        }
    }

    val isSuperset = entries.size > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isSuperset) "Log superset set" else "Log set ${entries.first().setIndex} of ${entries.first().totalSets}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                entries.forEachIndexed { idx, entry ->
                    Column {
                        if (isSuperset) {
                            Text(
                                "${('A' + idx)}. ${entry.exerciseName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Set ${entry.setIndex} of ${entry.totalSets}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Text(
                                entry.exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = repsState[idx].value,
                                onValueChange = { v ->
                                    repsState[idx].value = v.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Reps") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weightState[idx].value,
                                onValueChange = { v ->
                                    // allow digits and a single dot
                                    val cleaned = v.filter { it.isDigit() || it == '.' }
                                    val dotCount = cleaned.count { it == '.' }
                                    weightState[idx].value = if (dotCount <= 1) cleaned.take(6) else weightState[idx].value
                                },
                                label = { Text("Weight ($displayUnit)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tip: enter 0 weight for bodyweight exercises.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val reps = repsState.map { it.value.toIntOrNull()?.coerceAtLeast(0) ?: 0 }
                val weightKg = weightState.map { state ->
                    val v = state.value.toDoubleOrNull() ?: 0.0
                    VolumeAnalytics.displayToKg(v.coerceAtLeast(0.0), displayUnit)
                }
                onSave(LogSetResult(reps = reps, weightKg = weightKg))
            }) { Text("Save set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
