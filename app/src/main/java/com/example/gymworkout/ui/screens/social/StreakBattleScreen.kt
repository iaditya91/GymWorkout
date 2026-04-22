package com.example.gymworkout.ui.screens.social

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.NutritionTarget
import com.example.gymworkout.data.social.StreakBattle
import com.example.gymworkout.viewmodel.NutritionViewModel
import com.example.gymworkout.viewmodel.SocialViewModel

private val nutritionRelatedLabels = setOf(
    "fat", "fiber",
    "vitamin a", "vitamin b1", "vitamin b2", "vitamin b3",
    "vitamin b6", "vitamin b12", "vitamin c", "vitamin d",
    "vitamin e", "vitamin k",
    "folate", "iron", "calcium"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakBattleScreen(
    socialViewModel: SocialViewModel,
    nutritionViewModel: NutritionViewModel,
    onBack: () -> Unit
) {
    val battles by socialViewModel.battles.collectAsState()
    val friends by socialViewModel.friends.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val allTargets by nutritionViewModel.getAllTargets().collectAsState(initial = emptyList())

    val habitTargets = allTargets.filter {
        it.isCustom && it.label.lowercase() !in nutritionRelatedLabels
    }
    val categoryLabels: Map<String, String> = remember(habitTargets) {
        habitTargets.associate { it.category to it.label }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    val pendingBattles = battles.filter { it.status == "pending" }
    val activeBattles = battles.filter { it.status == "active" }
    val completedBattles = battles.filter { it.status == "completed" }

    // Sync battle streaks on screen load
    LaunchedEffect(Unit) {
        socialViewModel.syncBattleStreaks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streak Battles", fontWeight = FontWeight.Bold) },
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
            val acceptedFriends = friends.filter { !it.isPending }
            if (acceptedFriends.isNotEmpty()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, "New Battle")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Compete with friends!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Challenge a friend to maintain a longer streak in any habit category. The one with the most consecutive days wins!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Pending invites (incoming)
            val myUid = currentUser?.uid ?: ""
            val incomingBattles = pendingBattles.filter { it.opponentId == myUid }
            val outgoingBattles = pendingBattles.filter { it.creatorId == myUid }

            if (incomingBattles.isNotEmpty()) {
                item {
                    Text(
                        "Battle Invites",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(incomingBattles, key = { it.id }) { battle ->
                    BattleInviteCard(
                        battle = battle,
                        categoryLabels = categoryLabels,
                        onAccept = { socialViewModel.acceptBattle(battle.id) },
                        onDecline = { socialViewModel.declineBattle(battle.id) }
                    )
                }
            }

            // Pending outgoing (waiting for opponent to accept)
            if (outgoingBattles.isNotEmpty()) {
                item {
                    Text(
                        "Pending \u2013 Waiting for Accept",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(outgoingBattles, key = { it.id }) { battle ->
                    PendingSentCard(battle = battle, categoryLabels = categoryLabels)
                }
            }

            // Active battles
            if (activeBattles.isNotEmpty()) {
                item {
                    Text(
                        "Active Battles (${activeBattles.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(activeBattles, key = { it.id }) { battle ->
                    ActiveBattleCard(battle = battle, myUid = myUid, categoryLabels = categoryLabels)
                }
            }

            // Completed battles
            if (completedBattles.isNotEmpty()) {
                item {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(completedBattles.take(10), key = { it.id }) { battle ->
                    CompletedBattleCard(battle = battle, myUid = myUid, categoryLabels = categoryLabels)
                }
            }

            if (battles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.SportsKabaddi,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No battles yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Challenge a friend to a streak battle!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Create battle dialog
    if (showCreateDialog) {
        CreateBattleDialog(
            friends = friends.filter { !it.isPending },
            habitTargets = habitTargets,
            onDismiss = { showCreateDialog = false },
            onCreate = { opponentId, opponentName, category, days ->
                socialViewModel.createStreakBattle(opponentId, opponentName, category, days)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun BattleInviteCard(
    battle: StreakBattle,
    categoryLabels: Map<String, String>,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val displayCategory = categoryLabels[battle.category] ?: battle.category
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${battle.creatorName} challenged you!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$displayCategory streak battle for ${daysBetween(battle.startDate, battle.endDate)} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
private fun PendingSentCard(battle: StreakBattle, categoryLabels: Map<String, String>) {
    val displayCategory = categoryLabels[battle.category] ?: battle.category
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(categoryEmoji(battle.category), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$displayCategory Battle vs ${battle.opponentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${daysBetween(battle.startDate, battle.endDate)} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "Pending",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ActiveBattleCard(battle: StreakBattle, myUid: String, categoryLabels: Map<String, String>) {
    val isCreator = battle.creatorId == myUid
    val myStreak = if (isCreator) battle.creatorStreak else battle.opponentStreak
    val theirStreak = if (isCreator) battle.opponentStreak else battle.creatorStreak
    val opponentName = if (isCreator) battle.opponentName else battle.creatorName
    val isWinning = myStreak > theirStreak
    val displayCategory = categoryLabels[battle.category] ?: battle.category

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    categoryEmoji(battle.category),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$displayCategory Battle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "vs $opponentName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isWinning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        if (isWinning) "Winning!" else "Behind",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWinning) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Score comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$myStreak",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("days", style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    "VS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(opponentName.take(10), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$theirStreak",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("days", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Ends: ${battle.endDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletedBattleCard(battle: StreakBattle, myUid: String, categoryLabels: Map<String, String>) {
    val isCreator = battle.creatorId == myUid
    val myStreak = if (isCreator) battle.creatorStreak else battle.opponentStreak
    val theirStreak = if (isCreator) battle.opponentStreak else battle.creatorStreak
    val opponentName = if (isCreator) battle.opponentName else battle.creatorName
    val iWon = battle.winnerId == myUid
    val isTie = battle.winnerId == "tie"
    val displayCategory = categoryLabels[battle.category] ?: battle.category

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(categoryEmoji(battle.category), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$displayCategory vs $opponentName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$myStreak - $theirStreak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when {
                    isTie -> MaterialTheme.colorScheme.surfaceVariant
                    iWon -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
            ) {
                Text(
                    when {
                        isTie -> "Tie"
                        iWon -> "Won!"
                        else -> "Lost"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isTie -> MaterialTheme.colorScheme.onSurfaceVariant
                        iWon -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onError
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBattleDialog(
    friends: List<com.example.gymworkout.data.social.FriendInfo>,
    habitTargets: List<NutritionTarget>,
    onDismiss: () -> Unit,
    onCreate: (opponentId: String, opponentName: String, category: String, days: Int) -> Unit
) {
    var selectedFriendIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("WATER") }
    var durationDays by remember { mutableStateOf("7") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var friendExpanded by remember { mutableStateOf(false) }

    // Built-in categories + user's custom habit objectives.
    // Each entry is (categoryKey, displayLabel) — the key is stored on the battle
    // so computeStreakForCategory can look up the target, but the label is what we show.
    val builtInCategories = listOf("WORKOUT", "HABITS", "WATER", "PROTEIN", "CALORIES", "SLEEP")
    val categoryItems: List<Pair<String, String>> =
        builtInCategories.map { it to it } +
        habitTargets.map { it.category to it.label }
    val selectedLabel = categoryItems.firstOrNull { it.first == selectedCategory }?.second ?: selectedCategory

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Streak Battle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Friend selector
                ExposedDropdownMenuBox(
                    expanded = friendExpanded,
                    onExpandedChange = { friendExpanded = it }
                ) {
                    OutlinedTextField(
                        value = friends.getOrNull(selectedFriendIndex)?.user?.displayName ?: "Select friend",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Opponent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(friendExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = friendExpanded,
                        onDismissRequest = { friendExpanded = false }
                    ) {
                        friends.forEachIndexed { index, friend ->
                            DropdownMenuItem(
                                text = { Text(friend.user.displayName.ifEmpty { "Unknown" }) },
                                onClick = {
                                    selectedFriendIndex = index
                                    friendExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${categoryEmoji(selectedCategory)} $selectedLabel",
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
                        categoryItems.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text("${categoryEmoji(key)} $label") },
                                onClick = {
                                    selectedCategory = key
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Duration
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
                    val friend = friends.getOrNull(selectedFriendIndex) ?: return@TextButton
                    val days = durationDays.toIntOrNull() ?: 7
                    onCreate(friend.user.uid, friend.user.displayName, selectedCategory, days)
                }
            ) {
                Text("Challenge!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun categoryEmoji(category: String): String = when (category.uppercase()) {
    "WATER" -> "\uD83D\uDCA7"
    "PROTEIN" -> "\uD83E\uDD69"
    "CALORIES" -> "\uD83D\uDD25"
    "SLEEP" -> "\uD83D\uDE34"
    "WORKOUT" -> "\uD83C\uDFCB\uFE0F"
    "HABITS" -> "\u2705"
    else -> "\uD83C\uDFAF"
}

private fun daysBetween(start: String, end: String): Long {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val s = java.time.LocalDate.parse(start, formatter)
        val e = java.time.LocalDate.parse(end, formatter)
        java.time.temporal.ChronoUnit.DAYS.between(s, e)
    } catch (e: Exception) {
        7
    }
}
