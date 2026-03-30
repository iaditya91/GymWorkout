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
import com.example.gymworkout.data.social.AccountabilityPartnership
import com.example.gymworkout.data.social.FriendInfo
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val partnerships by socialViewModel.partnerships.collectAsState()
    val friends by socialViewModel.friends.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val myUid = currentUser?.uid ?: ""
    var showCreateDialog by remember { mutableStateOf(false) }

    val acceptedFriends = friends.filter { !it.isPending }
    val pending = partnerships.filter { it.status == "pending" }
    val active = partnerships.filter { it.status == "active" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accountability Partners", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (acceptedFriends.isNotEmpty()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Add Partner")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handshake, null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Accountability Partners", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Pair up with a friend. Get notified when they miss a workout or habit.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Incoming requests
            val incoming = pending.filter { it.user2Id == myUid }
            if (incoming.isNotEmpty()) {
                item { Text("Incoming Requests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(incoming) { p ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(p.user1Name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { socialViewModel.acceptPartnership(p.id) }) {
                                Icon(Icons.Default.Check, "Accept", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { socialViewModel.declinePartnership(p.id) }) {
                                Icon(Icons.Default.Close, "Decline", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Active partners
            if (active.isNotEmpty()) {
                item { Text("Your Partners", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(active) { p ->
                    val partnerName = if (p.user1Id == myUid) p.user2Name else p.user1Name
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Handshake, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(partnerName, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = p.notifyWorkout,
                                        onClick = { socialViewModel.togglePartnershipNotify(p.id, "notifyWorkout", p.notifyWorkout) },
                                        label = { Text("Workout") },
                                        leadingIcon = { Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(14.dp)) }
                                    )
                                    FilterChip(
                                        selected = p.notifyHabits,
                                        onClick = { socialViewModel.togglePartnershipNotify(p.id, "notifyHabits", p.notifyHabits) },
                                        label = { Text("Habits") },
                                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                            IconButton(onClick = { socialViewModel.removePartnership(p.id) }) {
                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Empty state
            if (active.isEmpty() && incoming.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Handshake, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No partners yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Pair up with a friend for mutual motivation!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePartnerDialog(
            friends = acceptedFriends,
            onDismiss = { showCreateDialog = false },
            onCreate = { friendId, friendName ->
                socialViewModel.createPartnership(friendId, friendName)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePartnerDialog(
    friends: List<FriendInfo>,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var selectedFriend by remember { mutableStateOf<FriendInfo?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Accountability Partner") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pick a friend to hold each other accountable.", style = MaterialTheme.typography.bodySmall)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedFriend?.user?.displayName ?: "Select friend",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        friends.forEach { friend ->
                            DropdownMenuItem(
                                text = { Text(friend.user.displayName) },
                                onClick = { selectedFriend = friend; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedFriend?.let { onCreate(it.user.uid, it.user.displayName) } },
                enabled = selectedFriend != null
            ) { Text("Send Request") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
