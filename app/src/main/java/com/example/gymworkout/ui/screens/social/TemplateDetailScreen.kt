package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var showCopyConfirm by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(templateId) {
        socialViewModel.selectTemplate(templateId)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isOwnTemplate) {
                        Button(
                            onClick = { showCopyConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy to My Plan")
                        }
                        OutlinedButton(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RateReview, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Review")
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "This is your published template",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
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
                    item { DayPlanSection(day = day, dayExercises = dayExercises) }
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
                title = { Text("Copy Template?") },
                text = {
                    Text("This will REPLACE your current workout plan with \"${t.title}\". This cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        socialViewModel.downloadTemplate(t)
                        showCopyConfirm = false
                        onBack()
                    }) { Text("Copy") }
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
private fun DayPlanSection(day: Int, dayExercises: List<TemplateExercise>) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val dayName = if (day in 0..6) dayNames[day] else "Day ${day + 1}"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    is TemplateExercise -> ExerciseLine(item)
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
                            supersetExercises.forEach { ExerciseLine(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLine(exercise: TemplateExercise) {
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
