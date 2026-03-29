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
import com.example.gymworkout.data.social.FriendInfo
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val friends by socialViewModel.friends.collectAsState()
    val searchResult by socialViewModel.friendSearchResult.collectAsState()
    val searchError by socialViewModel.friendSearchError.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val isLoading by socialViewModel.isLoading.collectAsState()

    var friendCode by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val incomingRequests = friends.filter { it.isPending && it.isIncoming }
    val outgoingRequests = friends.filter { it.isPending && !it.isIncoming }
    val acceptedFriends = friends.filter { !it.isPending }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, "Add Friend")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My friend code
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Your Friend Code",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                currentUser?.friendCode ?: "---",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Incoming requests
            if (incomingRequests.isNotEmpty()) {
                item {
                    Text(
                        "Friend Requests (${incomingRequests.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(incomingRequests, key = { it.friendshipId }) { request ->
                    FriendRequestCard(
                        friendInfo = request,
                        onAccept = { socialViewModel.acceptFriendRequest(request.friendshipId) },
                        onDecline = { socialViewModel.declineFriendRequest(request.friendshipId) }
                    )
                }
            }

            // Outgoing requests
            if (outgoingRequests.isNotEmpty()) {
                item {
                    Text(
                        "Pending Sent (${outgoingRequests.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(outgoingRequests, key = { it.friendshipId }) { request ->
                    PendingFriendCard(friendInfo = request)
                }
            }

            // Friends list
            item {
                Text(
                    "Friends (${acceptedFriends.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (acceptedFriends.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No friends yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Share your friend code or add friends by theirs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(acceptedFriends, key = { it.friendshipId }) { friend ->
                FriendCard(
                    friendInfo = friend,
                    onRemove = { socialViewModel.removeFriend(friend.friendshipId) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Add Friend Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                socialViewModel.clearFriendSearch()
                friendCode = ""
            },
            title = { Text("Add Friend") },
            text = {
                Column {
                    OutlinedTextField(
                        value = friendCode,
                        onValueChange = { friendCode = it.uppercase() },
                        label = { Text("Friend Code") },
                        placeholder = { Text("Enter 8-character code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    searchError?.let { err ->
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    searchResult?.let { user ->
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        user.displayName.ifEmpty { "Unknown" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        user.fitnessLevel,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                FilledTonalButton(onClick = {
                                    socialViewModel.sendFriendRequest(user.uid)
                                    showAddDialog = false
                                    friendCode = ""
                                }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { socialViewModel.searchFriendByCode(friendCode) }) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    socialViewModel.clearFriendSearch()
                    friendCode = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FriendRequestCard(
    friendInfo: FriendInfo,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendInfo.user.displayName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Wants to be friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            FilledTonalButton(onClick = onAccept, modifier = Modifier.padding(end = 4.dp)) {
                Text("Accept")
            }
            OutlinedButton(onClick = onDecline) {
                Text("Decline")
            }
        }
    }
}

@Composable
private fun PendingFriendCard(friendInfo: FriendInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.HourglassTop, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendInfo.user.displayName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Request sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FriendCard(
    friendInfo: FriendInfo,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Online indicator
            Icon(
                Icons.Filled.Circle,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = if (friendInfo.user.isOnline) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendInfo.user.displayName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    friendInfo.user.streaks.forEach { (cat, streak) ->
                        if (streak > 0) {
                            Text(
                                "$cat: ${streak}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (friendInfo.user.dmgs > 0f) {
                    Text(
                        "DMGS: ${(friendInfo.user.dmgs * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { showRemoveDialog = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Remove ${friendInfo.user.displayName} from your friends?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveDialog = false
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
