package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    onNavigateToShare: () -> Unit
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
    val isLoading by socialViewModel.isLoading.collectAsState()
    val error by socialViewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User info card
                currentUser?.let { user ->
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
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.displayName.ifEmpty { "Athlete" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Friend Code: ${user.friendCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            // DMGS badge
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "${(user.dmgs * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Feature cards grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Group,
                        title = "Friends",
                        subtitle = "${friends.count { !it.isPending }} friends",
                        pendingCount = friends.count { it.isPending && it.isIncoming },
                        onClick = onNavigateToFriends
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocalFireDepartment,
                        title = "Streak Battles",
                        subtitle = "${battles.count { it.status == "active" }} active",
                        pendingCount = battles.count { it.status == "pending" && it.opponentId == (currentUser?.uid ?: "") },
                        onClick = onNavigateToBattles
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.EmojiEvents,
                        title = "Challenges",
                        subtitle = "${challenges.size} active",
                        onClick = onNavigateToChallenges
                    )
                    SocialFeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Share,
                        title = "Share Progress",
                        subtitle = "Daily card",
                        onClick = onNavigateToShare
                    )
                }

                // Timeline card (full width)
                SocialFeatureCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Timeline,
                    title = "Journey Timeline",
                    subtitle = "${timeline.size} recent events from you & friends",
                    onClick = onNavigateToTimeline
                )

                // Sync button
                OutlinedButton(
                    onClick = { socialViewModel.syncStreaksToCloud() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Progress")
                }
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
