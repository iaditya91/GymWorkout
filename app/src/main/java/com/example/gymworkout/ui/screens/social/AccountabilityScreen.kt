package com.example.gymworkout.ui.screens.social

import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.AccountabilityCheckPreference
import com.example.gymworkout.data.social.AccountabilityPartnership
import com.example.gymworkout.data.social.FriendInfo
import com.example.gymworkout.notification.AccountabilityCheckScheduler
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val partnerships by socialViewModel.partnerships.collectAsState()
    val friends by socialViewModel.friends.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val myUid = currentUser?.uid ?: ""
    var showCreateDialog by remember { mutableStateOf(false) }

    val acceptedFriends = friends.filter { !it.isPending }
    val pending = partnerships.filter { it.status == "pending" }
    val active = partnerships.filter { it.status == "active" }

    // Ensure every active partnership has a scheduled alarm with its current/default time.
    LaunchedEffect(active.map { it.id }) {
        active.forEach { p ->
            AccountabilityCheckPreference.ensureTimeForPartner(context, p.id)
            AccountabilityCheckScheduler.scheduleForPartner(context, p.id)
        }
    }

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

            // Outgoing pending requests
            val outgoing = pending.filter { it.user1Id == myUid }
            if (outgoing.isNotEmpty()) {
                item { Text("Pending – Waiting for Accept", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(outgoing) { p ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(p.user2Name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("Pending", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
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
                    ActivePartnerCard(
                        partnership = p,
                        partnerName = partnerName,
                        onToggleWorkout = { socialViewModel.togglePartnershipNotify(p.id, "notifyWorkout", p.notifyWorkout) },
                        onToggleHabits = { socialViewModel.togglePartnershipNotify(p.id, "notifyHabits", p.notifyHabits) },
                        onRemove = {
                            AccountabilityCheckScheduler.cancelForPartner(context, p.id)
                            AccountabilityCheckPreference.clearPartner(context, p.id)
                            socialViewModel.removePartnership(p.id)
                        }
                    )
                }
            }

            // Empty state
            if (partnerships.isEmpty()) {
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
private fun ActivePartnerCard(
    partnership: AccountabilityPartnership,
    partnerName: String,
    onToggleWorkout: () -> Unit,
    onToggleHabits: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val partnerTimes by AccountabilityCheckPreference.partnerTimes.collectAsState()
    val time = partnerTimes[partnership.id] ?: AccountabilityCheckPreference.DEFAULT_TIME

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Handshake, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(partnerName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = partnership.notifyWorkout,
                    onClick = onToggleWorkout,
                    label = { Text("Workout") },
                    leadingIcon = { Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = partnership.notifyHabits,
                    onClick = onToggleHabits,
                    label = { Text("Habits") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp)) }
                )
            }
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        TimePickerDialog(context, { _, h, m ->
                            val newTime = String.format("%02d:%02d", h, m)
                            AccountabilityCheckPreference.setTimeForPartner(context, partnership.id, newTime)
                            AccountabilityCheckScheduler.scheduleForPartner(context, partnership.id)
                        }, hour, minute, false).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check daily at $time", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
