package com.example.gymworkout.ui.screens.social

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.social.TemplateExercise
import com.example.gymworkout.data.social.WorkoutTemplate
import com.example.gymworkout.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: String,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val template by socialViewModel.selectedTemplate.collectAsState()
    val reviews by socialViewModel.templateReviews.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val localDayCounts by socialViewModel.localDayCounts.collectAsState()
    val context = LocalContext.current
    var showCopyConfirm by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Non-null while the "import a single day" dialog is open; holds the template dayOfWeek tapped.
    var importDaySource by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(templateId) {
        socialViewModel.selectTemplate(templateId)
        socialViewModel.refreshLocalPlanInfo()
    }

    DisposableEffect(templateId) {
        onDispose { socialViewModel.clearSelectedTemplate() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        template?.title?.ifEmpty { "Template" } ?: "Template",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    template?.let { t ->
                        IconButton(onClick = { shareTemplate(context, t) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share template")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val t = template
        if (t == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val isOwnTemplate = t.creatorId == currentUser?.uid

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeaderSection(template = t) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Import is available for ANY template (own or others').
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // Only warn when there is an existing plan to overwrite.
                                if (localDayCounts.values.any { it > 0 }) {
                                    showCopyConfirm = true
                                } else {
                                    socialViewModel.downloadTemplate(t)
                                    onBack()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import All")
                        }
                        if (!isOwnTemplate) {
                            OutlinedButton(
                                onClick = { showReviewDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RateReview, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Review")
                            }
                        }
                    }
                    // Owner-only management actions.
                    if (isOwnTemplate) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Workout Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (t.exercises.isNotEmpty()) {
                    Text(
                        "Tap a day to import just that day into your plan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (t.exercises.isEmpty()) {
                item {
                    Text(
                        "No exercises in this template",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val exercisesByDay = t.exercises.groupBy { it.dayOfWeek }.toSortedMap()
                exercisesByDay.forEach { (day, dayExercises) ->
                    item {
                        DayPlanSection(
                            day = day,
                            dayExercises = dayExercises,
                            templateTitle = t.title,
                            onImport = { importDaySource = day }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reviews (${reviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (reviews.isEmpty()) {
                item {
                    Text(
                        "No reviews yet. Be the first to review!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(reviews) { review ->
                    ReviewRow(
                        userName = review.userName,
                        rating = review.rating,
                        comment = review.comment,
                        createdAt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(review.createdAt.toDate())
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCopyConfirm) {
        val t = template
        if (t != null) {
            AlertDialog(
                onDismissRequest = { showCopyConfirm = false },
                title = { Text("Import Entire Plan?") },
                text = {
                    Text("This will REPLACE your current workout plan (all days) with \"${t.title}\". This cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        socialViewModel.downloadTemplate(t)
                        showCopyConfirm = false
                        onBack()
                    }) { Text("Replace", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showCopyConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }

    if (showReviewDialog) {
        TemplateReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                socialViewModel.addTemplateReview(templateId, rating, comment)
                showReviewDialog = false
            }
        )
    }

    if (showEditDialog) {
        val t = template
        if (t != null) {
            EditTemplateDialog(
                initialTitle = t.title,
                initialDescription = t.description,
                initialLevel = t.fitnessLevel.ifEmpty { "beginner" },
                onDismiss = { showEditDialog = false },
                onSave = { newTitle, newDescription, newLevel ->
                    socialViewModel.updateWorkoutTemplate(
                        templateId = templateId,
                        title = newTitle,
                        description = newDescription,
                        fitnessLevel = newLevel
                    )
                    showEditDialog = false
                }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Template?") },
            text = {
                Text("This will permanently remove \"${template?.title.orEmpty()}\" and its reviews. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    socialViewModel.deleteWorkoutTemplate(templateId) { success ->
                        if (success) onBack()
                    }
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val sourceDay = importDaySource
    val t = template
    if (sourceDay != null && t != null) {
        val sourceCount = t.exercises.count { it.dayOfWeek == sourceDay }
        ImportDayDialog(
            sourceDay = sourceDay,
            sourceExerciseCount = sourceCount,
            localDayCounts = localDayCounts,
            onDismiss = { importDaySource = null },
            onConfirm = { targetDay, replace ->
                socialViewModel.importTemplateDay(t, sourceDay, targetDay, replace)
                importDaySource = null
            }
        )
    }
}

private val DAY_NAMES = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)
private val DAY_ABBREV = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun dayLabel(day: Int): String =
    if (day in 0..6) DAY_NAMES[day] else "Day ${day + 1}"

@Composable
private fun ImportDayDialog(
    sourceDay: Int,
    sourceExerciseCount: Int,
    localDayCounts: Map<Int, Int>,
    onDismiss: () -> Unit,
    onConfirm: (targetDay: Int, replace: Boolean) -> Unit
) {
    var targetDay by remember { mutableStateOf(sourceDay.coerceIn(0, 6)) }
    var replace by remember { mutableStateOf(true) }
    val occupied = (localDayCounts[targetDay] ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ${dayLabel(sourceDay)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$sourceExerciseCount exercise${if (sourceExerciseCount == 1) "" else "s"} " +
                            "will be imported into the day you pick below.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Import into", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..6).forEach { d ->
                        FilterChip(
                            selected = targetDay == d,
                            onClick = { targetDay = d },
                            label = { Text(DAY_ABBREV[d]) }
                        )
                    }
                }

                Text("Action", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = replace,
                        onClick = { replace = true },
                        label = { Text("Replace day") }
                    )
                    FilterChip(
                        selected = !replace,
                        onClick = { replace = false },
                        label = { Text("Append") }
                    )
                }

                if (occupied > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (replace)
                                    "${dayLabel(targetDay)} already has $occupied exercise${if (occupied == 1) "" else "s"}. They will be REPLACED."
                                else
                                    "${dayLabel(targetDay)} already has $occupied exercise${if (occupied == 1) "" else "s"}. The imported ones will be ADDED after them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(targetDay, replace) }) {
                Text(
                    if (replace) "Import (Replace)" else "Import (Append)",
                    color = if (replace && occupied > 0) MaterialTheme.colorScheme.error
                    else Color.Unspecified
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HeaderSection(template: WorkoutTemplate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "by ${template.creatorName.ifEmpty { "Unknown" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        template.fitnessLevel.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (template.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaItem(Icons.Default.CalendarMonth, "${template.daysPerWeek} days/week")
                MetaItem(Icons.Default.FitnessCenter, "${template.exercises.size} exercises")
                MetaItem(Icons.Default.Download, "${template.downloads}")
            }

            if (template.ratingCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < template.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "%.1f (${template.ratingCount})".format(template.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DayPlanSection(
    day: Int,
    dayExercises: List<TemplateExercise>,
    templateTitle: String,
    onImport: (() -> Unit)? = null
) {
    val dayName = dayLabel(day)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onImport != null) Modifier.clickable { onImport() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        dayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (onImport != null) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onImport, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            val sorted = dayExercises.sortedBy { it.orderIndex }
            val items = mutableListOf<Any>()
            var i = 0
            while (i < sorted.size) {
                val ex = sorted[i]
                if (ex.supersetGroupId.isNotBlank()) {
                    val group = mutableListOf(ex)
                    var j = i + 1
                    while (j < sorted.size && sorted[j].supersetGroupId == ex.supersetGroupId) {
                        group.add(sorted[j])
                        j++
                    }
                    if (group.size > 1) items.add(group.toList()) else items.add(ex)
                    i = j
                } else {
                    items.add(ex)
                    i++
                }
            }

            items.forEach { item ->
                when (item) {
                    is TemplateExercise -> ExerciseLine(item, templateTitle = templateTitle)
                    is List<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val supersetExercises = item as List<TemplateExercise>
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SwapVert, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Superset",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            supersetExercises.forEach { ExerciseLine(it, templateTitle = templateTitle) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLine(exercise: TemplateExercise, templateTitle: String?) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.FitnessCenter, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${exercise.sets} sets x ${exercise.reps} reps" +
                        if (exercise.restTimeSeconds > 0) " · ${exercise.restTimeSeconds}s rest" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { shareExercise(context, exercise, templateTitle) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share exercise",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewRow(userName: String, rating: Int, comment: String, createdAt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    userName.ifEmpty { "Anonymous" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                repeat(5) { i ->
                    Icon(
                        if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFFB300)
                    )
                }
            }
            if (comment.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(comment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun shareExercise(context: Context, exercise: TemplateExercise, templateTitle: String?) {
    val rest = if (exercise.restTimeSeconds > 0) " · ${exercise.restTimeSeconds}s rest" else ""
    val header = if (!templateTitle.isNullOrBlank()) "From \"$templateTitle\"\n\n" else ""
    val text = buildString {
        append(header)
        append("💪 ${exercise.name}\n")
        append("${exercise.sets} sets x ${exercise.reps} reps$rest")
        append("\n\n— Shared via GymWorkout")
    }
    launchShare(context, subject = exercise.name, text = text)
}

private fun shareTemplate(context: Context, template: WorkoutTemplate) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val text = buildString {
        append("🏋️ ${template.title}\n")
        if (template.creatorName.isNotBlank()) append("by ${template.creatorName}\n")
        append("Level: ${template.fitnessLevel.replaceFirstChar { it.uppercase() }}  ·  ${template.daysPerWeek} days/week\n")
        if (template.description.isNotBlank()) {
            append("\n${template.description}\n")
        }

        val byDay = template.exercises.groupBy { it.dayOfWeek }.toSortedMap()
        byDay.forEach { (day, list) ->
            val dayName = if (day in 0..6) dayNames[day] else "Day ${day + 1}"
            append("\n— $dayName —\n")
            val sorted = list.sortedBy { it.orderIndex }
            var i = 0
            while (i < sorted.size) {
                val ex = sorted[i]
                if (ex.supersetGroupId.isNotBlank()) {
                    val group = mutableListOf(ex)
                    var j = i + 1
                    while (j < sorted.size && sorted[j].supersetGroupId == ex.supersetGroupId) {
                        group.add(sorted[j]); j++
                    }
                    if (group.size > 1) {
                        append("↕ Superset:\n")
                        group.forEach { append("   • ${formatExerciseLine(it)}\n") }
                    } else {
                        append("• ${formatExerciseLine(ex)}\n")
                    }
                    i = j
                } else {
                    append("• ${formatExerciseLine(ex)}\n")
                    i++
                }
            }
        }

        append("\n— Shared via GymWorkout")
    }
    launchShare(context, subject = template.title, text = text)
}

private fun formatExerciseLine(ex: TemplateExercise): String {
    val rest = if (ex.restTimeSeconds > 0) " · ${ex.restTimeSeconds}s rest" else ""
    return "${ex.name} — ${ex.sets} x ${ex.reps}$rest"
}

private fun launchShare(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share workout"))
}

@Composable
private fun EditTemplateDialog(
    initialTitle: String,
    initialDescription: String,
    initialLevel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var level by remember { mutableStateOf(initialLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Text("Fitness Level", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = level == "beginner",
                        onClick = { level = "beginner" },
                        label = { Text("Beginner") }
                    )
                    FilterChip(
                        selected = level == "intermediate",
                        onClick = { level = "intermediate" },
                        label = { Text("Intermediate") }
                    )
                    FilterChip(
                        selected = level == "advanced",
                        onClick = { level = "advanced" },
                        label = { Text("Advanced") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), description.trim(), level) },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TemplateReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        IconButton(onClick = { rating = i + 1 }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                null, tint = Color(0xFFFFB300)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, comment) }, enabled = rating > 0) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
