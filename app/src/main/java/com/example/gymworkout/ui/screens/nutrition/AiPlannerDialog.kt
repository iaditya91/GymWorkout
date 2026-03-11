package com.example.gymworkout.ui.screens.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymworkout.ai.AiPlannerEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlannerDialog(
    progressList: List<AiPlannerEngine.TargetProgress>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<AiPlannerEngine.PlannerResult?>(null) }
    var refreshCounter by remember { mutableStateOf(0) }

    LaunchedEffect(refreshCounter, progressList) {
        if (progressList.isEmpty()) {
            isLoading = true
            return@LaunchedEffect
        }
        isLoading = true
        result = AiPlannerEngine.generateSuggestions(context, progressList)
        isLoading = false
    }

    val completedCount = progressList.count { it.isComplete }
    val totalCount = progressList.size
    val overallProgress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "AI Daily Planner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Smart suggestions for your goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshCounter++ },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh suggestions")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI Badge
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (result?.isFromLlm == true) "Gemini Nano" else "On-Device AI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (result?.isFromLlm == true)
                                    "Creative suggestions powered by on-device Gemini Nano LLM. 100% offline."
                                else
                                    "Personalized suggestions based on your progress. Runs 100% offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Overall progress card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Today's Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$completedCount / $totalCount targets",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                overallProgress >= 1f -> Color(0xFF4CAF50)
                                overallProgress >= 0.7f -> Color(0xFF8BC34A)
                                overallProgress >= 0.4f -> Color(0xFFFFC107)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Individual target progress bars
                        progressList.forEach { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    target.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(90.dp),
                                    maxLines = 1
                                )
                                LinearProgressIndicator(
                                    progress = { (target.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (target.isComplete) Color(0xFF4CAF50)
                                        else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${target.percentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (target.isComplete) Color(0xFF4CAF50)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Analyzing your progress...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── PER-TARGET ACTION PLANS ──
            if (!isLoading && result != null && result!!.actionPlans.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiObjects,
                                contentDescription = null,
                                tint = Color(0xFF7C4DFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Action Plans (${result!!.actionPlans.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(
                    result!!.actionPlans,
                    key = { "action_plan_${it.label}" }
                ) { plan ->
                    ActionPlanCard(plan = plan)
                }
            }

            // ── GENERIC SUGGESTIONS ──
            if (!isLoading && result != null) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Quick Tips (${result!!.suggestions.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                itemsIndexed(
                    result!!.suggestions,
                    key = { index, _ -> "suggestion_$index" }
                ) { index, suggestion ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        SuggestionCard(index = index + 1, text = suggestion)
                    }
                }

                // Refresh button at bottom
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = { refreshCounter++ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (result?.isFromLlm == true) "Generate New Suggestions" else "Refresh Suggestions"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Per-Target Action Plan Card (Expandable) ──

@Composable
private fun ActionPlanCard(plan: AiPlannerEngine.HabitActionPlan) {
    var expanded by remember { mutableStateOf(false) }

    val progressColor = when {
        plan.percentage >= 100f -> Color(0xFF4CAF50)
        plan.percentage >= 60f -> Color(0xFF8BC34A)
        plan.percentage >= 30f -> Color(0xFFFFC107)
        else -> Color(0xFFFF7043)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (expanded) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: icon + label + progress + expand toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Habit icon
                Text(
                    plan.icon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${formatPlanValue(plan.remaining)} ${plan.unit} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress circle
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (plan.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(40.dp),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "${plan.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Expanded content ──
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // ── Schedule section ──
                if (plan.schedule.isNotEmpty()) {
                    SectionHeader(
                        icon = Icons.Default.AccessTime,
                        title = "Your Schedule",
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    plan.schedule.forEachIndexed { index, item ->
                        ScheduleRow(item = item, isLast = index == plan.schedule.lastIndex)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Tips section ──
                if (plan.tips.isNotEmpty()) {
                    SectionHeader(
                        icon = Icons.Default.Lightbulb,
                        title = "Pro Tips",
                        color = Color(0xFFFFA000)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    plan.tips.take(3).forEach { tip ->
                        TipRow(text = tip)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Fresh Ideas section ──
                if (plan.freshIdeas.isNotEmpty()) {
                    SectionHeader(
                        icon = Icons.Default.AutoAwesome,
                        title = "Fresh Ideas",
                        color = Color(0xFF7C4DFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    plan.freshIdeas.take(3).forEach { idea ->
                        FreshIdeaRow(text = idea)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ScheduleRow(item: AiPlannerEngine.ScheduleItem, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = if (isLast) 0.dp else 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp)
        ) {
            Text(
                item.time,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color(0xFF2196F3).copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2196F3).copy(alpha = 0.08f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.action,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    item.amount,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "\u2022",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFFA000),
            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FreshIdeaRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuggestionCard(index: Int, text: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatPlanValue(value: Float): String {
    return if (value == value.toLong().toFloat()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
