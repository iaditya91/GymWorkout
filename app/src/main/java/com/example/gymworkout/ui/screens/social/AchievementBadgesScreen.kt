package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.social.AchievementBadge
import com.example.gymworkout.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementBadgesScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val badges by socialViewModel.badges.collectAsState()

    LaunchedEffect(Unit) {
        socialViewModel.checkAndAwardBadges()
    }

    // All possible badges for display (earned + locked)
    val allBadgeDefs = listOf(
        BadgeDef("streak_7", "First 7-Day Streak", "Maintain a 7-day streak in any category", "🔥"),
        BadgeDef("streak_30", "30-Day Warrior", "Maintain a 30-day streak", "💪"),
        BadgeDef("streak_100", "100-Day Legend", "Maintain a 100-day streak", "🏆"),
        BadgeDef("workouts_10", "Getting Started", "Log 10 workouts", "🏋️"),
        BadgeDef("workouts_50", "Dedicated Athlete", "Log 50 workouts", "⭐"),
        BadgeDef("workouts_100", "Century Club", "Log 100 workouts", "💯"),
        BadgeDef("macros_month", "Macro Master", "Track every macro for 30 days", "🥩"),
        BadgeDef("journey_30", "One Month In", "30 days on fitness journey", "📅"),
        BadgeDef("journey_90", "Quarter Champion", "90 days on fitness journey", "🗓️"),
        BadgeDef("journey_365", "Year-Round Athlete", "365 days on fitness journey", "🎉"),
        BadgeDef("first_duel_win", "Duelist", "Win your first nutrition duel", "⚔️"),
        BadgeDef("first_battle_win", "Battle Victor", "Win your first streak battle", "🥇"),
        BadgeDef("template_shared", "Sharing is Caring", "Share a workout template", "📤"),
        BadgeDef("five_friends", "Social Butterfly", "Make 5 friends", "🦋")
    )

    val earnedKeys = badges.map { it.key }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievement Badges", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${badges.size} / ${allBadgeDefs.size} Badges Earned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Keep going to unlock them all!", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Badge grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(allBadgeDefs) { def ->
                    val earned = earnedKeys.contains(def.key)
                    val earnedBadge = badges.find { it.key == def.key }
                    BadgeCard(def = def, earned = earned, earnedAt = earnedBadge?.earnedAt?.toDate()?.let {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
                    })
                }
            }
        }
    }
}

private data class BadgeDef(val key: String, val title: String, val description: String, val icon: String)

@Composable
private fun BadgeCard(def: BadgeDef, earned: Boolean, earnedAt: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    def.icon,
                    fontSize = 28.sp,
                    modifier = if (!earned) Modifier then androidx.compose.ui.Modifier else Modifier
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                def.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Text(
                def.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (earned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 2
            )

            if (earned && earnedAt != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    earnedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!earned) {
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}
