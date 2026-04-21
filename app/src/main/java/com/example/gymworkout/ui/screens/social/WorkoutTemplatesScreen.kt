package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.gymworkout.data.social.WorkoutTemplate
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplatesScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit,
    onOpenTemplate: (String) -> Unit = {}
) {
    val templates by socialViewModel.templates.collectAsState()
    val myTemplates by socialViewModel.myTemplates.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var filterLevel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        socialViewModel.loadTemplates()
        socialViewModel.loadMyTemplates()
    }

    LaunchedEffect(filterLevel) {
        socialViewModel.loadTemplates(filterLevel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Templates", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPublishDialog = true }) {
                Icon(Icons.Default.Upload, "Publish")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Browse") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Templates") })
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Filter chips
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = filterLevel == null, onClick = { filterLevel = null }, label = { Text("All") })
                                FilterChip(selected = filterLevel == "beginner", onClick = { filterLevel = "beginner" }, label = { Text("Beginner") })
                                FilterChip(selected = filterLevel == "intermediate", onClick = { filterLevel = "intermediate" }, label = { Text("Intermediate") })
                                FilterChip(selected = filterLevel == "advanced", onClick = { filterLevel = "advanced" }, label = { Text("Advanced") })
                            }
                        }

                        if (templates.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No templates yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Be the first to share your workout plan!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        items(templates) { template ->
                            TemplateCard(
                                template = template,
                                onClick = { onOpenTemplate(template.id) }
                            )
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (myTemplates.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No published templates", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Share your current workout plan with the community!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        items(myTemplates) { template ->
                            TemplateCard(
                                template = template,
                                onClick = { onOpenTemplate(template.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishTemplateDialog(
            onDismiss = { showPublishDialog = false },
            onPublish = { title, description, level ->
                socialViewModel.publishWorkoutTemplate(title, description, level)
                showPublishDialog = false
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("by ${template.creatorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(template.fitnessLevel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (template.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(template.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${template.daysPerWeek} days/week", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${template.exercises.size} exercises", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${template.downloads}", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (template.ratingCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < template.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            null, modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("(${template.ratingCount})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

        }
    }
}

@Composable
private fun PublishTemplateDialog(
    onDismiss: () -> Unit,
    onPublish: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("beginner") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Workout Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Share your current workout plan with the community.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("Fitness Level", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = level == "beginner", onClick = { level = "beginner" }, label = { Text("Beginner") })
                    FilterChip(selected = level == "intermediate", onClick = { level = "intermediate" }, label = { Text("Intermediate") })
                    FilterChip(selected = level == "advanced", onClick = { level = "advanced" }, label = { Text("Advanced") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPublish(title, description, level) }, enabled = title.isNotBlank()) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

