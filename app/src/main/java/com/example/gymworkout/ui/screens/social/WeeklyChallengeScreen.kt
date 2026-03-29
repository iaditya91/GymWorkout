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
import com.example.gymworkout.data.social.WeeklyChallenge
import com.example.gymworkout.viewmodel.SocialViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyChallengeScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val myChallenges by socialViewModel.myChallenges.collectAsState()
    val availableChallenges by socialViewModel.availableChallenges.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Sync progress on load
    LaunchedEffect(Unit) {
        socialViewModel.syncChallengeProgress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, "New Challenge")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Challenges") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Discover") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (myChallenges.isEmpty()) {
                            item {
                                EmptyChallengeState()
                            }
                        }
                        items(myChallenges, key = { it.id }) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                myUid = currentUser?.uid ?: "",
                                isJoined = true
                            )
                        }
                    }
                    1 -> {
                        // Available challenges from friends that user hasn't joined
                        val myUid = currentUser?.uid ?: ""
                        val joinable = availableChallenges.filter { c ->
                            c.creatorId != myUid && c.participants.none { it.userId == myUid }
                        }
                        if (joinable.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No challenges to discover",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Ask your friends to create challenges!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        items(joinable, key = { it.id }) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                myUid = myUid,
                                isJoined = false,
                                onJoin = { socialViewModel.joinChallenge(challenge.id) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateChallengeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description, category, target, unit, days ->
                socialViewModel.createChallenge(title, description, category, target, unit, days)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ChallengeCard(
    challenge: WeeklyChallenge,
    myUid: String,
    isJoined: Boolean,
    onJoin: (() -> Unit)? = null
) {
    val sortedParticipants = challenge.participants.sortedByDescending { it.progress }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        challenge.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        challenge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        challenge.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Target info
            Text(
                "Goal: ${challenge.targetValue.toInt()} ${challenge.targetUnit}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${challenge.startDate} - ${challenge.endDate} | ${challenge.participants.size} participants",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Leaderboard
            Text(
                "Leaderboard",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            sortedParticipants.forEachIndexed { index, participant ->
                val progressPercent = if (challenge.targetValue > 0f)
                    min(participant.progress / challenge.targetValue, 1f)
                else 0f
                val isMe = participant.userId == myUid

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp),
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        participant.displayName + if (isMe) " (You)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.0f".format(participant.progress)}/${challenge.targetValue.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(start = 24.dp),
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Join button for discoverable challenges
            if (!isJoined && onJoin != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Join Challenge")
                }
            }
        }
    }
}

@Composable
private fun EmptyChallengeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No active challenges",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Create one and invite your friends!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, category: String, target: Float, unit: String, days: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PROTEIN") }
    var targetValue by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("7") }
    var categoryExpanded by remember { mutableStateOf(false) }

    data class CategoryInfo(val name: String, val defaultTarget: Float, val unit: String)

    val categories = listOf(
        CategoryInfo("PROTEIN", 700f, "g"),
        CategoryInfo("CALORIES", 18000f, "kcal"),
        CategoryInfo("WATER", 21f, "L"),
        CategoryInfo("SLEEP", 49f, "h"),
        CategoryInfo("WORKOUT", 5f, "days")
    )
    val selectedCat = categories.find { it.name == category } ?: categories[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Challenge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Protein Week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Hit 100g protein every day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.name} (${cat.unit})") },
                                onClick = {
                                    category = cat.name
                                    if (targetValue.isEmpty()) {
                                        targetValue = cat.defaultTarget.toInt().toString()
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetValue.ifEmpty { selectedCat.defaultTarget.toInt().toString() },
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target (${selectedCat.unit})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationDays,
                    onValueChange = { durationDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Duration (days)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = (targetValue.ifEmpty { selectedCat.defaultTarget.toString() }).toFloatOrNull() ?: selectedCat.defaultTarget
                    val days = durationDays.toIntOrNull() ?: 7
                    onCreate(
                        title.ifEmpty { "${category} Challenge" },
                        description.ifEmpty { "Reach ${target.toInt()}${selectedCat.unit} in $days days" },
                        category,
                        target,
                        selectedCat.unit,
                        days
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
