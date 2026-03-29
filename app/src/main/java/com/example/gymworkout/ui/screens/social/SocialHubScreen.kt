package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.viewmodel.SocialViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialHubScreen(
    socialViewModel: SocialViewModel,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToBattles: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToShare: () -> Unit,
    onNavigateToAccountability: () -> Unit,
    onNavigateToTeamGoals: () -> Unit,
    onNavigateToNutritionDuels: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToBadges: () -> Unit
) {
    // Refresh sign-in state each time this screen appears
    LaunchedEffect(Unit) {
        socialViewModel.refreshSignInState()
    }

    val isSignedIn by socialViewModel.isSignedIn.collectAsState()
    val currentUser by socialViewModel.currentSocialUser.collectAsState()
    val friends by socialViewModel.friends.collectAsState()
    val battles by socialViewModel.battles.collectAsState()
    val challenges by socialViewModel.myChallenges.collectAsState()
    val timeline by socialViewModel.timeline.collectAsState()
    val partnerships by socialViewModel.partnerships.collectAsState()
    val duels by socialViewModel.duels.collectAsState()
    val badges by socialViewModel.badges.collectAsState()
    val isLoading by socialViewModel.isLoading.collectAsState()
    val error by socialViewModel.error.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Social",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Connect & compete with friends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (!isSignedIn) {
            // Sign-in prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Group,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Connect with Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in to compete in streak battles, join challenges, and share your progress with friends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNavigateToLogin) {
                    Text("Sign In")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User info card
                currentUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName.ifEmpty { "Athlete" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Friend Code: ${user.friendCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                                Text("${(user.dmgs * 100).toInt()}%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Row 1: Friends + Streak Battles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Group, title = "Friends",
                        subtitle = "${friends.count { !it.isPending }} friends",
                        pendingCount = friends.count { it.isPending && it.isIncoming },
                        onClick = onNavigateToFriends
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.LocalFireDepartment, title = "Streak Battles",
                        subtitle = "${battles.count { it.status == "active" }} active",
                        pendingCount = battles.count { it.status == "pending" && it.opponentId == (currentUser?.uid ?: "") },
                        onClick = onNavigateToBattles
                    )
                }

                // Row 2: Challenges + Nutrition Duels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.EmojiEvents, title = "Challenges",
                        subtitle = "${challenges.size} active", onClick = onNavigateToChallenges
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Restaurant, title = "Nutrition Duels",
                        subtitle = "${duels.count { it.status == "active" }} active",
                        pendingCount = duels.count { it.status == "pending" && it.opponentId == (currentUser?.uid ?: "") },
                        onClick = onNavigateToNutritionDuels
                    )
                }

                // Row 3: Accountability + Team Goals
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Handshake, title = "Accountability",
                        subtitle = "${partnerships.count { it.status == "active" }} partners",
                        pendingCount = partnerships.count { it.status == "pending" && it.user2Id == (currentUser?.uid ?: "") },
                        onClick = onNavigateToAccountability
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Groups, title = "Team Goals",
                        subtitle = "${socialViewModel.teamGoals.collectAsState().value.size} active",
                        onClick = onNavigateToTeamGoals
                    )
                }

                // Row 4: Leaderboard + Templates
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Leaderboard, title = "Leaderboards",
                        subtitle = "Global rankings", onClick = onNavigateToLeaderboard
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.FitnessCenter, title = "Templates",
                        subtitle = "Share & download", onClick = onNavigateToTemplates
                    )
                }

                // Row 5: Badges + Share Progress
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.MilitaryTech, title = "Badges",
                        subtitle = "${badges.size} earned", onClick = onNavigateToBadges
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f), icon = Icons.Filled.Share, title = "Share Progress",
                        subtitle = "Daily card", onClick = onNavigateToShare
                    )
                }

                // Timeline card (full width)
                SocialFeatureCard(
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Timeline, title = "Journey Timeline",
                    subtitle = "${timeline.size} recent events from you & friends", onClick = onNavigateToTimeline
                )

                // Sync button
                OutlinedButton(onClick = { socialViewModel.syncStreaksToCloud() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Progress")
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // Error snackbar
        error?.let { msg ->
            LaunchedEffect(msg) {
                // Auto-clear after showing
                kotlinx.coroutines.delay(3000)
                socialViewModel.clearError()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialFeatureCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    pendingCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                if (pendingCount > 0) {
                    Spacer(Modifier.weight(1f))
                    Badge { Text("$pendingCount") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
