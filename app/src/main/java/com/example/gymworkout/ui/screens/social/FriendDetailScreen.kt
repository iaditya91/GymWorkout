package com.example.gymworkout.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymworkout.data.social.AchievementBadge
import com.example.gymworkout.data.social.DailyProgress
import com.example.gymworkout.data.social.SocialUser
import kotlin.math.min
import com.example.gymworkout.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    friendUid: String,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val friend by socialViewModel.selectedFriend.collectAsState()
    val friendBadges by socialViewModel.selectedFriendBadges.collectAsState()
    val isLoading by socialViewModel.selectedFriendLoading.collectAsState()

    LaunchedEffect(friendUid) {
        socialViewModel.loadFriendDetail(friendUid)
    }

    DisposableEffect(friendUid) {
        onDispose { socialViewModel.clearFriendDetail() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        friend?.displayName?.ifEmpty { "Friend" } ?: "Friend",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        if (friend == null && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val user = friend ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FriendProgressCard(user = user)

            BadgesSection(earnedBadges = friendBadges)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FriendProgressCard(user: SocialUser) {
    val daysOnJourney = remember(user.joinedAt, user.dailyProgress.daysOnJourney) {
        if (user.dailyProgress.daysOnJourney > 0) {
            user.dailyProgress.daysOnJourney
        } else {
            val joinedDate = user.joinedAt.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            ChronoUnit.DAYS.between(joinedDate, LocalDate.now()).toInt().coerceAtLeast(0)
        }
    }
    val joinedFormatted = remember(user.joinedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(user.joinedAt.toDate())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user.displayName.ifEmpty { "Unknown" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = if (user.isOnline) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (user.isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                " • ${user.fitnessLevel.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${(user.dmgs * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "DMGS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                val progress = user.dailyProgress
                val hasProgress = progress.date.isNotBlank()

                if (hasProgress) {
                    Text(
                        "Today's Progress",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Workout status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (progress.workoutDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (progress.workoutDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (progress.workoutDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val workoutDay = progress.workoutName.ifEmpty { "Today" }
                        Text(
                            if (progress.workoutDone) "Workout Complete ($workoutDay)"
                            else "Workout Pending ($workoutDay)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Nutrition grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutritionTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Egg,
                            label = "Protein",
                            current = progress.proteinProgress,
                            target = progress.proteinTarget,
                            unit = "g"
                        )
                        NutritionTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.LocalFireDepartment,
                            label = "Calories",
                            current = progress.caloriesProgress,
                            target = progress.caloriesTarget,
                            unit = ""
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutritionTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.WaterDrop,
                            label = "Water",
                            current = progress.waterProgress,
                            target = progress.waterTarget,
                            unit = "L",
                            isDecimal = true
                        )
                        NutritionTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Bedtime,
                            label = "Sleep",
                            current = progress.sleepProgress,
                            target = progress.sleepTarget,
                            unit = "h",
                            isDecimal = true
                        )
                    }
                }

                // Streaks row
                Text(
                    "Current Streaks",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (user.streaks.isEmpty() || user.streaks.values.all { it == 0 }) {
                    Text(
                        "No active streaks yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        user.streaks.forEach { (cat, streak) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$streak",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    cat.take(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Journey info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Day $daysOnJourney of fitness journey",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Joined $joinedFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgesSection(earnedBadges: List<AchievementBadge>) {
    val allBadgeDefs = remember { friendBadgeDefs() }
    val earnedKeys = earnedBadges.map { it.key }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${earnedBadges.size} / ${allBadgeDefs.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Use a non-scrolling grid by computing height from row count
        val columns = 3
        val rows = (allBadgeDefs.size + columns - 1) / columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .height((rows * 130).dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            items(allBadgeDefs) { def ->
                val earned = earnedKeys.contains(def.key)
                FriendBadgeTile(def = def, earned = earned)
            }
        }
    }
}

@Composable
private fun FriendBadgeTile(def: FriendBadgeDef, earned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (earned) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (earned) {
                    Text(def.icon, fontSize = 22.sp)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                def.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (earned) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun NutritionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    current: Float,
    target: Float,
    unit: String,
    isDecimal: Boolean = false
) {
    val fraction = if (target > 0f) min(current / target, 1f) else 0f
    val met = current >= target && target > 0f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (met) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (met) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isDecimal) "${"%.1f".format(current)}/${"%.1f".format(target)}$unit"
                else "${current.toInt()}/${target.toInt()}$unit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = if (met) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

private data class FriendBadgeDef(val key: String, val title: String, val icon: String)

private fun friendBadgeDefs(): List<FriendBadgeDef> = listOf(
    FriendBadgeDef("streak_7", "7-Day Streak", "🔥"),
    FriendBadgeDef("streak_30", "30-Day Warrior", "💪"),
    FriendBadgeDef("streak_100", "100-Day Legend", "🏆"),
    FriendBadgeDef("workouts_10", "Getting Started", "🏋️"),
    FriendBadgeDef("workouts_50", "Dedicated", "⭐"),
    FriendBadgeDef("workouts_100", "Century Club", "💯"),
    FriendBadgeDef("macros_month", "Macro Master", "🥩"),
    FriendBadgeDef("journey_30", "One Month In", "📅"),
    FriendBadgeDef("journey_90", "Quarter Champion", "🗓️"),
    FriendBadgeDef("journey_365", "Year-Round", "🎉"),
    FriendBadgeDef("first_duel_win", "Duelist", "⚔️"),
    FriendBadgeDef("first_battle_win", "Battle Victor", "🥇"),
    FriendBadgeDef("template_shared", "Sharing is Caring", "📤"),
    FriendBadgeDef("five_friends", "Social Butterfly", "🦋")
)

