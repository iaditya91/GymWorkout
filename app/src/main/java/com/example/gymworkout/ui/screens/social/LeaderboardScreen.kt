package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.gymworkout.data.social.LeaderboardEntry
import com.example.gymworkout.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val leaderboard by socialViewModel.leaderboard.collectAsState()
    val isProfilePublic by socialViewModel.isProfilePublic.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedFilter) {
        socialViewModel.loadLeaderboard(selectedFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboards", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Public profile toggle
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Public Profile", fontWeight = FontWeight.SemiBold)
                            Text("Show your stats on the leaderboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isProfilePublic, onCheckedChange = { socialViewModel.toggleProfilePublic() })
                    }
                }
            }

            // Fitness level filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedFilter == null, onClick = { selectedFilter = null }, label = { Text("All") })
                    FilterChip(selected = selectedFilter == "beginner", onClick = { selectedFilter = "beginner" }, label = { Text("Beginner") })
                    FilterChip(selected = selectedFilter == "intermediate", onClick = { selectedFilter = "intermediate" }, label = { Text("Intermediate") })
                    FilterChip(selected = selectedFilter == "advanced", onClick = { selectedFilter = "advanced" }, label = { Text("Advanced") })
                }
            }

            if (leaderboard.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Leaderboard, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No public profiles yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Toggle your profile to public to appear here!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            itemsIndexed(leaderboard) { index, entry ->
                LeaderboardEntryCard(rank = index + 1, entry = entry)
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(rank: Int, entry: LeaderboardEntry) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (rank <= 3) medalColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("#$rank", fontWeight = FontWeight.Bold, color = medalColor, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.fitnessLevel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    entry.streaks.entries.take(2).forEach { (cat, streak) ->
                        Text("$cat: ${streak}d", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // DMGS score
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                Text(
                    "${(entry.dmgs * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
