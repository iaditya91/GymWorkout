package com.example.gymworkout.ui.screens.nutrition

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import com.example.gymworkout.notification.NotificationHelper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.NutritionCategory
import com.example.gymworkout.data.NutritionReminder
import com.example.gymworkout.data.TimerSoundPreference
import com.example.gymworkout.viewmodel.NutritionViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val displayTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

private fun formatTimeDisplay(timeStr: String): String {
    return try {
        LocalTime.parse(timeStr, timeFormatter).format(displayTimeFormatter)
    } catch (_: Exception) {
        timeStr
    }
}

// --- Main Reminder Management Dialog ---

@Composable
fun ReminderListDialog(
    category: NutritionCategory,
    color: Color,
    viewModel: NutritionViewModel,
    onDismiss: () -> Unit
) {
    ReminderListDialog(
        categoryKey = category.name,
        categoryLabel = category.label,
        categoryIcon = { Icon(getCategoryIcon(category), contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) },
        color = color,
        viewModel = viewModel,
        onDismiss = onDismiss
    )
}

@Composable
fun ReminderListDialog(
    categoryKey: String,
    categoryLabel: String,
    categoryIcon: @Composable (() -> Unit)? = null,
    color: Color,
    viewModel: NutritionViewModel,
    onDismiss: () -> Unit
) {
    val reminders by viewModel.getRemindersForCategory(categoryKey)
        .collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<NutritionReminder?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (categoryIcon != null) {
                    categoryIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("$categoryLabel Reminders")
            }
        },
        text = {
            Column {
                if (reminders.isEmpty()) {
                    Text(
                        "No reminders set.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height((reminders.size * 88).coerceAtMost(350).dp)
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                color = color,
                                onToggle = { viewModel.toggleReminderEnabled(reminder) },
                                onEdit = { editingReminder = reminder },
                                onDelete = { viewModel.deleteReminder(reminder) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.1f))
                        .clickable { showAddDialog = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add reminder",
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add Reminder",
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showAddDialog) {
        AddEditReminderDialog(
            categoryKey = categoryKey,
            color = color,
            existingReminder = null,
            onDismiss = { showAddDialog = false },
            onSave = { reminder ->
                viewModel.saveReminder(reminder)
                showAddDialog = false
            }
        )
    }

    if (editingReminder != null) {
        AddEditReminderDialog(
            categoryKey = categoryKey,
            color = color,
            existingReminder = editingReminder,
            onDismiss = { editingReminder = null },
            onSave = { reminder ->
                viewModel.updateReminder(reminder)
                editingReminder = null
            }
        )
    }
}

@Composable
fun ReminderItem(
    reminder: NutritionReminder,
    color: Color,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (reminder.enabled) Icons.Default.Notifications
                    else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (reminder.enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (reminder.type == "SPECIFIC") "Specific Times"
                        else "Every ${reminder.intervalMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = getReminderSummary(reminder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = color),
                    modifier = Modifier.height(24.dp)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (reminder.customText.isNotBlank()) {
                Text(
                    "\"${reminder.customText}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(start = 26.dp, top = 4.dp)
                )
            }
            if (reminder.ringtoneUri.isNotBlank()) {
                val ctx = LocalContext.current
                val soundName = remember(reminder.ringtoneUri) {
                    TimerSoundPreference.getRingtoneName(ctx, Uri.parse(reminder.ringtoneUri))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = color.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        soundName,
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getReminderSummary(reminder: NutritionReminder): String {
    return when (reminder.type) {
        "SPECIFIC" -> {
            val times = reminder.specificTimes.split(",").filter { it.isNotBlank() }
            times.joinToString(", ") { formatTimeDisplay(it.trim()) }
        }
        "INTERVAL" -> {
            val start = formatTimeDisplay(reminder.startTime)
            val end = formatTimeDisplay(reminder.endTime)
            "$start - $end"
        }
        else -> ""
    }
}

// --- Add/Edit Reminder Dialog ---

@Composable
fun AddEditReminderDialog(
    category: NutritionCategory,
    color: Color,
    existingReminder: NutritionReminder?,
    onDismiss: () -> Unit,
    onSave: (NutritionReminder) -> Unit
) {
    AddEditReminderDialog(
        categoryKey = category.name,
        color = color,
        existingReminder = existingReminder,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun AddEditReminderDialog(
    categoryKey: String,
    color: Color,
    existingReminder: NutritionReminder?,
    onDismiss: () -> Unit,
    onSave: (NutritionReminder) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(existingReminder?.type ?: "SPECIFIC") }
    var customText by remember { mutableStateOf(existingReminder?.customText ?: "") }
    var ringtoneUri by remember {
        mutableStateOf(
            if (existingReminder?.ringtoneUri?.isNotBlank() == true) Uri.parse(existingReminder.ringtoneUri)
            else null
        )
    }
    var ringtoneName by remember {
        mutableStateOf(
            if (existingReminder?.ringtoneUri?.isNotBlank() == true)
                TimerSoundPreference.getRingtoneName(context, Uri.parse(existingReminder.ringtoneUri))
            else "Default"
        )
    }
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        ringtoneUri = uri
        ringtoneName = if (uri != null) TimerSoundPreference.getRingtoneName(context, uri) else "Silent"
        NotificationHelper.cleanupOldSoundChannels(context, NotificationHelper.CHANNEL_ID, uri)
    }

    // Specific times
    val specificTimes = remember {
        mutableStateListOf<String>().apply {
            if (existingReminder?.type == "SPECIFIC" && existingReminder.specificTimes.isNotBlank()) {
                addAll(existingReminder.specificTimes.split(",").map { it.trim() })
            }
        }
    }

    // Interval fields
    var startTime by remember { mutableStateOf(existingReminder?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(existingReminder?.endTime ?: "22:00") }
    var intervalMinutes by remember {
        mutableIntStateOf(existingReminder?.intervalMinutes ?: 60)
    }
    var intervalText by remember {
        mutableStateOf(
            (existingReminder?.intervalMinutes ?: 60).toString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingReminder != null) "Edit Reminder" else "New Reminder"
            )
        },
        text = {
            Column {
                // Type selector
                Text(
                    "Schedule Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(
                        label = "Specific Times",
                        selected = type == "SPECIFIC",
                        color = color,
                        onClick = { type = "SPECIFIC" }
                    )
                    TypeChip(
                        label = "Repeating",
                        selected = type == "INTERVAL",
                        color = color,
                        onClick = { type = "INTERVAL" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (type == "SPECIFIC") {
                    // Specific times UI
                    Text(
                        "Times",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    specificTimes.forEachIndexed { index, time ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.1f))
                                    .clickable {
                                        val parts = time.split(":")
                                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        TimePickerDialog(context, { _, hour, minute ->
                                            specificTimes[index] = String.format("%02d:%02d", hour, minute)
                                        }, h, m, false).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        formatTimeDisplay(time),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color
                                    )
                                }
                            }
                            IconButton(
                                onClick = { specificTimes.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add time button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                TimePickerDialog(context, { _, hour, minute ->
                                    specificTimes.add(String.format("%02d:%02d", hour, minute))
                                }, 8, 0, false).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Add Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                } else {
                    // Interval UI
                    Text(
                        "Start Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePickerButton(
                        time = startTime,
                        color = color,
                        onTimeSelected = { startTime = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "End Time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePickerButton(
                        time = endTime,
                        color = color,
                        onTimeSelected = { endTime = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Repeat Every (minutes)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = {
                            intervalText = it
                            it.toIntOrNull()?.let { v -> intervalMinutes = v }
                        },
                        placeholder = { Text("60") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Custom text
                Text(
                    "Custom Reminder Text (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("e.g. Drink a glass of water!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Notification sound picker
                Text(
                    "Notification Sound",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Notification Sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                if (ringtoneUri != null) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                                }
                            }
                            ringtonePicker.launch(pickerIntent)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        ringtoneName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminder = NutritionReminder(
                        id = existingReminder?.id ?: 0,
                        category = categoryKey,
                        type = type,
                        customText = customText,
                        enabled = existingReminder?.enabled ?: true,
                        specificTimes = if (type == "SPECIFIC")
                            specificTimes.joinToString(",") else "",
                        startTime = if (type == "INTERVAL") startTime else "",
                        endTime = if (type == "INTERVAL") endTime else "",
                        intervalMinutes = if (type == "INTERVAL") intervalMinutes else 0,
                        ringtoneUri = ringtoneUri?.toString() ?: ""
                    )
                    onSave(reminder)
                },
                enabled = when (type) {
                    "SPECIFIC" -> specificTimes.isNotEmpty()
                    "INTERVAL" -> intervalMinutes > 0 && startTime.isNotBlank() && endTime.isNotBlank()
                    else -> false
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TimePickerButton(
    time: String,
    color: Color,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable {
                val parts = time.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                TimePickerDialog(context, { _, hour, minute ->
                    onTimeSelected(String.format("%02d:%02d", hour, minute))
                }, h, m, false).show()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                formatTimeDisplay(time),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun TypeChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
