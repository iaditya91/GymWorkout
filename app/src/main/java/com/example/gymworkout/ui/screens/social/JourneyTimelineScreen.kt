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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.social.TimelineEvent
import com.example.gymworkout.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTimelineScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val timeline by socialViewModel.timeline.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journey Timeline", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { socialViewModel.syncStreaksToCloud() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (timeline.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No events yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Milestones, victories, and achievements\nfrom you and your friends will appear here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(timeline, key = { it.id }) { event ->
                    TimelineEventCard(
                        event = event,
                        isMe = event.userId == (currentUser?.uid ?: "")
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: TimelineEvent,
    isMe: Boolean
) {
    val eventStyle = getEventStyle(event.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = eventStyle.color.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        eventStyle.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = eventStyle.color
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Content
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.userName.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "(You)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatTimestamp(event.createdAt.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (event.description.isNotEmpty()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Value badge if present
                if (event.value > 0f) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = eventStyle.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            when (event.type) {
                                "streak_milestone" -> "${event.value.toInt()} days"
                                "challenge_won" -> "${"%.0f".format(event.value)} ${event.category}"
                                "battle_won" -> "${event.value.toInt()}-day streak"
                                "goal_reached" -> "Day ${event.value.toInt()}"
                                else -> "${event.value}"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = eventStyle.color
                        )
                    }
                }
            }
        }
    }
}

private data class EventStyle(val icon: ImageVector, val color: Color)

@Composable
private fun getEventStyle(type: String): EventStyle = when (type) {
    "streak_milestone" -> EventStyle(Icons.Filled.LocalFireDepartment, MaterialTheme.colorScheme.error)
    "challenge_won" -> EventStyle(Icons.Filled.EmojiEvents, Color(0xFFFFB300))
    "challenge_created" -> EventStyle(Icons.Filled.Flag, MaterialTheme.colorScheme.primary)
    "battle_won" -> EventStyle(Icons.Filled.MilitaryTech, Color(0xFFFFB300))
    "workout_complete" -> EventStyle(Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.primary)
    "goal_reached" -> EventStyle(Icons.Filled.Star, Color(0xFF4CAF50))
    else -> EventStyle(Icons.Filled.Celebration, MaterialTheme.colorScheme.tertiary)
}

private fun formatTimestamp(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
