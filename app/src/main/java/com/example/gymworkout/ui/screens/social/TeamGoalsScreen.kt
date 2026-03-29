package com.example.gymworkout.ui.screens.social

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.social.TeamGoal
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamGoalsScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val teamGoals by socialViewModel.teamGoals.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Create Goal")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Team Goals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Work together with friends to hit group targets!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (teamGoals.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No team goals yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(teamGoals) { goal ->
                TeamGoalCard(goal = goal, myUid = currentUser?.uid ?: "", onJoin = { socialViewModel.joinTeamGoal(goal.id) })
            }
        }
    }

    if (showCreateDialog) {
        CreateTeamGoalDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, category, target, unit, days ->
                socialViewModel.createTeamGoal(title, category, target, unit, days)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TeamGoalCard(goal: TeamGoal, myUid: String, onJoin: () -> Unit) {
    val progress = if (goal.targetValue > 0f) (goal.currentTotal / goal.targetValue).coerceIn(0f, 1f) else 0f
    val isMember = goal.members.any { it.userId == myUid }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(goal.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Pooled progress bar
            Text("${"%.1f".format(goal.currentTotal)} / ${"%.0f".format(goal.targetValue)} ${goal.targetUnit}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text("${(progress * 100).toInt()}% complete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            // Members
            Text("Members (${goal.members.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            goal.members.forEach { member ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(member.displayName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(member.contribution)} ${goal.targetUnit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("${goal.startDate} — ${goal.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!isMember) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) { Text("Join Goal") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTeamGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Float, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("WATER") }
    var targetValue by remember { mutableStateOf("50") }
    var duration by remember { mutableStateOf("7") }
    var expanded by remember { mutableStateOf(false) }

    data class CategoryOption(val key: String, val label: String, val unit: String, val defaultTarget: String)
    val categories = listOf(
        CategoryOption("WATER", "Water", "L", "50"),
        CategoryOption("PROTEIN", "Protein", "g", "1000"),
        CategoryOption("CALORIES", "Calories", "kcal", "18000"),
        CategoryOption("WORKOUT", "Workouts", "days", "20")
    )
    val selectedCat = categories.first { it.key == category }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Team Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedCat.label, onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.label) }, onClick = {
                                category = cat.key; targetValue = cat.defaultTarget; expanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = targetValue, onValueChange = { targetValue = it }, label = { Text("Group Target (${selectedCat.unit})") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (days)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, category, targetValue.toFloatOrNull() ?: 0f, selectedCat.unit, duration.toIntOrNull() ?: 7) },
                enabled = title.isNotBlank() && (targetValue.toFloatOrNull() ?: 0f) > 0f
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
