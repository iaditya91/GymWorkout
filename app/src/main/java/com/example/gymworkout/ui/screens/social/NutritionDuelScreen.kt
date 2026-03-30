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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.social.FriendInfo
import com.example.gymworkout.data.social.NutritionDuel
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDuelScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val duels by socialViewModel.duels.collectAsState()
    val friends by socialViewModel.friends.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val myUid = currentUser?.uid ?: ""
    var showCreateDialog by remember { mutableStateOf(false) }

    val acceptedFriends = friends.filter { !it.isPending }
    val pendingIncoming = duels.filter { it.status == "pending" && it.opponentId == myUid }
    val pendingOutgoing = duels.filter { it.status == "pending" && it.challengerId == myUid }
    val active = duels.filter { it.status == "active" }
    val completed = duels.filter { it.status == "completed" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Duels", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (acceptedFriends.isNotEmpty()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Create Duel")
                }
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
                        Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Nutrition Duels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Challenge a friend in a nutrition category. Real-time progress bars!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Incoming invites
            if (pendingIncoming.isNotEmpty()) {
                item { Text("Incoming Duels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(pendingIncoming) { duel ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${duel.challengerName} challenged you!", fontWeight = FontWeight.SemiBold)
                                Text("${categoryEmoji(duel.category)} ${duel.category} — ${duel.duration}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { socialViewModel.acceptDuel(duel.id) }) {
                                Icon(Icons.Default.Check, "Accept", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { socialViewModel.declineDuel(duel.id) }) {
                                Icon(Icons.Default.Close, "Decline", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Outgoing pending duels
            if (pendingOutgoing.isNotEmpty()) {
                item { Text("Pending \u2013 Waiting for Accept", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(pendingOutgoing) { duel ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${categoryEmoji(duel.category)} ${duel.category} vs ${duel.opponentName}", fontWeight = FontWeight.SemiBold)
                                Text(duel.duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("Pending", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            // Active duels
            if (active.isNotEmpty()) {
                item { Text("Active Duels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(active) { duel -> ActiveDuelCard(duel, myUid) }
            }

            // Completed duels
            if (completed.isNotEmpty()) {
                item { Text("Completed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(completed.take(10)) { duel -> CompletedDuelCard(duel, myUid) }
            }

            if (duels.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No duels yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDuelDialog(
            friends = acceptedFriends,
            onDismiss = { showCreateDialog = false },
            onCreate = { opponentId, opponentName, category, duration ->
                socialViewModel.createDuel(opponentId, opponentName, category, duration)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ActiveDuelCard(duel: NutritionDuel, myUid: String) {
    val isChallenger = duel.challengerId == myUid
    val myProgress = if (isChallenger) duel.challengerProgress else duel.opponentProgress
    val theirProgress = if (isChallenger) duel.opponentProgress else duel.challengerProgress
    val opponentName = if (isChallenger) duel.opponentName else duel.challengerName
    val maxVal = maxOf(myProgress, theirProgress, 1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${categoryEmoji(duel.category)} ${duel.category}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(duel.duration, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))

            // My progress
            Text("You: ${"%.1f".format(myProgress)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { (myProgress / maxVal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            // Opponent progress
            Text("$opponentName: ${"%.1f".format(theirProgress)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { (theirProgress / maxVal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            Text("${duel.startDate} — ${duel.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompletedDuelCard(duel: NutritionDuel, myUid: String) {
    val isWinner = duel.winnerId == myUid
    val isTie = duel.winnerId == "tie"
    val resultText = when { isTie -> "Tie!"; isWinner -> "You Won!"; else -> "You Lost" }
    val resultColor = when { isTie -> MaterialTheme.colorScheme.secondary; isWinner -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.error }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${categoryEmoji(duel.category)} ${duel.category} — ${duel.duration}", fontWeight = FontWeight.SemiBold)
                val opponentName = if (duel.challengerId == myUid) duel.opponentName else duel.challengerName
                Text("vs $opponentName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = MaterialTheme.shapes.small, color = resultColor) {
                Text(resultText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun categoryEmoji(cat: String): String = when (cat) {
    "PROTEIN" -> "🥩"
    "WATER" -> "💧"
    "CALORIES" -> "🔥"
    "SLEEP" -> "😴"
    else -> "📊"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDuelDialog(
    friends: List<FriendInfo>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var selectedFriend by remember { mutableStateOf<FriendInfo?>(null) }
    var category by remember { mutableStateOf("PROTEIN") }
    var duration by remember { mutableStateOf("day") }
    var friendExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val categories = listOf("PROTEIN", "WATER", "CALORIES", "SLEEP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Nutrition Duel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = friendExpanded, onExpandedChange = { friendExpanded = it }) {
                    OutlinedTextField(
                        value = selectedFriend?.user?.displayName ?: "Select opponent",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Opponent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(friendExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = friendExpanded, onDismissRequest = { friendExpanded = false }) {
                        friends.forEach { friend ->
                            DropdownMenuItem(text = { Text(friend.user.displayName) }, onClick = { selectedFriend = friend; friendExpanded = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = "${categoryEmoji(category)} $category",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text("${categoryEmoji(cat)} $cat") }, onClick = { category = cat; catExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = duration == "day", onClick = { duration = "day" }, label = { Text("1 Day") })
                    FilterChip(selected = duration == "week", onClick = { duration = "week" }, label = { Text("1 Week") })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedFriend?.let { onCreate(it.user.uid, it.user.displayName, category, duration) } },
                enabled = selectedFriend != null
            ) { Text("Challenge!") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
