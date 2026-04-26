package com.example.gymworkout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MOODS = listOf("😀", "🙂", "😐", "😔", "😣", "😡", "💪", "🔥", "😴", "🤒")

@Composable
fun JournalEntryDialog(
    objectiveLabel: String,
    dateLabel: String,
    initialMood: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (mood: String, text: String, scoreChecked: Int, scoreTotal: Int) -> Unit,
    checklistItems: List<String> = emptyList(),
    initialScoreChecked: Int = 0,
    initialScoreTotal: Int = 0
) {
    var mood by remember { mutableStateOf(initialMood) }
    var text by remember { mutableStateOf(initialText) }

    val hasChecklist = checklistItems.isNotEmpty()
    val checkedStates = remember(checklistItems, initialScoreChecked, initialScoreTotal) {
        mutableStateListOf<Boolean>().apply {
            // Pre-fill: if there's a saved score that matches this list size, pre-check the
            // first N. Otherwise start unchecked.
            val preCheck = if (initialScoreTotal == checklistItems.size) initialScoreChecked else 0
            checklistItems.forEachIndexed { i, _ -> add(i < preCheck) }
        }
    }
    var scoreExpanded by remember { mutableStateOf(false) }
    var hasScored by remember { mutableStateOf(initialScoreTotal > 0 && initialScoreTotal == checklistItems.size) }
    var savedChecked by remember { mutableStateOf(if (initialScoreTotal == checklistItems.size) initialScoreChecked else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Journal — $objectiveLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "How did it feel?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MOODS.take(5).forEach { emoji ->
                        MoodChip(emoji = emoji, selected = mood == emoji, onClick = {
                            mood = if (mood == emoji) "" else emoji
                        })
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MOODS.drop(5).forEach { emoji ->
                        MoodChip(emoji = emoji, selected = mood == emoji, onClick = {
                            mood = if (mood == emoji) "" else emoji
                        })
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("How was your day?") },
                    placeholder = { Text("What went well, what was hard, what's next...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp)
                )

                if (hasChecklist) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ScoreChecklistSection(
                        items = checklistItems,
                        checkedStates = checkedStates,
                        expanded = scoreExpanded,
                        hasScored = hasScored,
                        savedChecked = savedChecked,
                        onToggleExpand = { scoreExpanded = !scoreExpanded },
                        onItemToggle = { idx -> checkedStates[idx] = !checkedStates[idx] },
                        onDone = {
                            savedChecked = checkedStates.count { it }
                            hasScored = true
                            scoreExpanded = false
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = if (hasChecklist && hasScored) checklistItems.size else 0
                    val checked = if (total > 0) savedChecked else 0
                    onSave(mood, text.trim(), checked, total)
                },
                enabled = text.trim().isNotEmpty() || mood.isNotEmpty() || hasScored
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ScoreChecklistSection(
    items: List<String>,
    checkedStates: List<Boolean>,
    expanded: Boolean,
    hasScored: Boolean,
    savedChecked: Int,
    onToggleExpand: () -> Unit,
    onItemToggle: (Int) -> Unit,
    onDone: () -> Unit
) {
    val total = items.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Checklist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Score checklist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (hasScored) "Score: $savedChecked/$total"
                    else "Tap to score how much you achieved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { idx, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onItemToggle(idx) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedStates.getOrElse(idx) { false },
                                onCheckedChange = { onItemToggle(idx) }
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val liveChecked = checkedStates.count { it }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$liveChecked / $total",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
