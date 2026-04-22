package com.example.gymworkout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymworkout.data.ExerciseRepository
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import com.example.gymworkout.ui.screens.AiChatScreen
import com.example.gymworkout.ui.screens.DayDetailScreen
import com.example.gymworkout.ui.screens.ExerciseDetailScreen
import com.example.gymworkout.ui.screens.WeeklyPlanScreen
import com.example.gymworkout.ui.screens.YouTubePlayerScreen
import com.example.gymworkout.viewmodel.AiChatViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import com.example.gymworkout.ui.screens.nutrition.NutritionScreen
import com.example.gymworkout.ui.screens.nutrition.HabitDetailScreen
import com.example.gymworkout.ui.screens.stats.StatsScreen
import com.example.gymworkout.ui.screens.user.UserScreen
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.AiPlannerPreference
import com.example.gymworkout.data.ProgressNotificationPreference
import com.example.gymworkout.data.ThemePreference
import com.example.gymworkout.data.sync.SyncPreference
import com.example.gymworkout.notification.NotificationHelper
import com.example.gymworkout.notification.ProgressNotificationService
import com.example.gymworkout.ui.theme.GymWorkoutTheme
import com.example.gymworkout.viewmodel.NutritionViewModel
import com.example.gymworkout.viewmodel.StatsViewModel
import com.example.gymworkout.viewmodel.UserViewModel
import com.example.gymworkout.viewmodel.SocialViewModel
import com.example.gymworkout.viewmodel.WorkoutViewModel
import com.example.gymworkout.ui.screens.auth.LoginScreen
import com.example.gymworkout.ui.screens.social.SocialHubScreen
import com.example.gymworkout.ui.screens.social.FriendsScreen
import com.example.gymworkout.ui.screens.social.FriendDetailScreen
import com.example.gymworkout.ui.screens.social.StreakBattleScreen
import com.example.gymworkout.ui.screens.social.WeeklyChallengeScreen
import com.example.gymworkout.ui.screens.social.ProgressShareScreen
import com.example.gymworkout.ui.screens.social.JourneyTimelineScreen
import com.example.gymworkout.ui.screens.social.AccountabilityScreen
import com.example.gymworkout.ui.screens.social.TeamGoalsScreen
import com.example.gymworkout.ui.screens.social.NutritionDuelScreen
import com.example.gymworkout.ui.screens.social.LeaderboardScreen
import com.example.gymworkout.ui.screens.social.WorkoutTemplatesScreen
import com.example.gymworkout.ui.screens.social.TemplateDetailScreen
import com.example.gymworkout.ui.screens.social.AchievementBadgesScreen

class MainActivity : ComponentActivity() {
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.init(this)
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.createWorkoutNotificationChannel(this)
        NotificationHelper.createQuoteNotificationChannel(this)
        NotificationHelper.createAutoBackupNotificationChannel(this)
        NotificationHelper.createAiPlannerNotificationChannel(this)
        NotificationHelper.createProgressNotificationChannel(this)
        requestNotificationPermission()
        ExerciseRepository.load(this)
        QuotePreference.init(this)
        AiPlannerPreference.init(this)
        ProgressNotificationPreference.init(this)
        SyncPreference.init(this)

        if (ProgressNotificationPreference.getEnabled(this)) {
            ProgressNotificationService.start(this)
        }
        setContent {
            val darkMode by ThemePreference.isDarkMode.collectAsState()
            GymWorkoutTheme(
                darkTheme = darkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            ) {
                WorkoutApp()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("workout", "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavItem("nutrition", "Objectives", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    BottomNavItem("stats", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("social", "Social", Icons.Filled.Group, Icons.Outlined.Group),
    BottomNavItem("user", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

// Routes where bottom bar should be visible
val bottomBarRoutes = setOf("workout", "nutrition", "stats", "social", "user")

@Composable
fun WorkoutApp() {
    val navController = rememberNavController()
    val workoutViewModel: WorkoutViewModel = viewModel()
    val nutritionViewModel: NutritionViewModel = viewModel()
    val statsViewModel: StatsViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val aiChatViewModel: AiChatViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()

    // Check if user is already signed in or has previously skipped/signed in
    val context = androidx.compose.ui.platform.LocalContext.current
    val loginPrefs = remember { context.getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE) }
    val hasCompletedLogin = remember { loginPrefs.getBoolean("has_completed_login", false) }
    val startDestination = if (hasCompletedLogin) "workout" else "login"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes
    val showFab = Build.VERSION.SDK_INT >= 31 &&
            currentRoute != "ai_chat" &&
            currentRoute != "nutrition" &&
            currentRoute != "login" &&
            currentRoute?.startsWith("day/") != true &&
            currentRoute?.startsWith("habit/") != true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate("ai_chat") },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Chat",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    userViewModel = userViewModel,
                    socialViewModel = socialViewModel,
                    onSignInComplete = {
                        loginPrefs.edit().putBoolean("has_completed_login", true).apply()
                        navController.navigate("workout") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onSkip = {
                        loginPrefs.edit().putBoolean("has_completed_login", true).apply()
                        navController.navigate("workout") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("workout") {
                WeeklyPlanScreen(
                    viewModel = workoutViewModel,
                    onDayClick = { dayIndex ->
                        navController.navigate("day/$dayIndex")
                    }
                )
            }
            composable(
                route = "day/{dayIndex}",
                arguments = listOf(navArgument("dayIndex") { type = NavType.IntType })
            ) { backStackEntry ->
                val dayIndex = backStackEntry.arguments?.getInt("dayIndex") ?: 0
                DayDetailScreen(
                    dayIndex = dayIndex,
                    viewModel = workoutViewModel,
                    onBack = { navController.popBackStack() },
                    onPlayVideo = { name, url ->
                        val encodedName = URLEncoder.encode(name, "UTF-8")
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("youtube/$encodedName?url=$encodedUrl")
                    },
                    onViewExerciseDetail = { exerciseName ->
                        val encodedName = URLEncoder.encode(exerciseName, "UTF-8")
                        navController.navigate("exercise_detail/$encodedName")
                    },
                    onNavigateToAiChat = { navController.navigate("ai_chat") }
                )
            }
            composable(
                route = "youtube/{name}?url={url}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("url") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val name = URLDecoder.decode(
                    backStackEntry.arguments?.getString("name") ?: "", "UTF-8"
                )
                val url = URLDecoder.decode(
                    backStackEntry.arguments?.getString("url") ?: "", "UTF-8"
                )
                YouTubePlayerScreen(
                    exerciseName = name,
                    youtubeUrl = url,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "exercise_detail/{exerciseName}",
                arguments = listOf(navArgument("exerciseName") { type = NavType.StringType })
            ) { backStackEntry ->
                val exerciseName = URLDecoder.decode(
                    backStackEntry.arguments?.getString("exerciseName") ?: "", "UTF-8"
                )
                ExerciseDetailScreen(
                    exerciseName = exerciseName,
                    onBack = { navController.popBackStack() },
                    onNavigateToExercise = { targetName ->
                        val encoded = URLEncoder.encode(targetName, "UTF-8")
                        navController.navigate("exercise_detail/$encoded")
                    }
                )
            }
            composable("nutrition") {
                NutritionScreen(
                    viewModel = nutritionViewModel,
                    onNavigateToAiChat = { navController.navigate("ai_chat") },
                    onOpenHabit = { categoryKey ->
                        val encoded = URLEncoder.encode(categoryKey, "UTF-8")
                        navController.navigate("habit/$encoded")
                    }
                )
            }
            composable(
                route = "habit/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryKey = backStackEntry.arguments?.getString("category") ?: ""
                HabitDetailScreen(
                    categoryKey = URLDecoder.decode(categoryKey, "UTF-8"),
                    viewModel = nutritionViewModel,
                    onBack = { navController.popBackStack() },
                    onChatClick = if (Build.VERSION.SDK_INT >= 31) {{ navController.navigate("ai_chat") }} else null
                )
            }
            composable("stats") {
                StatsScreen(viewModel = statsViewModel)
            }
            composable("user") {
                UserScreen(
                    viewModel = userViewModel,
                    socialViewModel = socialViewModel,
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("ai_chat") {
                AiChatScreen(
                    viewModel = aiChatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social") {
                SocialHubScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToFriends = { navController.navigate("social/friends") },
                    onNavigateToBattles = { navController.navigate("social/battles") },
                    onNavigateToChallenges = { navController.navigate("social/challenges") },
                    onNavigateToTimeline = { navController.navigate("social/timeline") },
                    onNavigateToShare = { navController.navigate("social/share") },
                    onNavigateToAccountability = { navController.navigate("social/accountability") },
                    onNavigateToTeamGoals = { navController.navigate("social/team_goals") },
                    onNavigateToNutritionDuels = { navController.navigate("social/duels") },
                    onNavigateToLeaderboard = { navController.navigate("social/leaderboard") },
                    onNavigateToTemplates = { navController.navigate("social/templates") },
                    onNavigateToBadges = { navController.navigate("social/badges") }
                )
            }
            composable("social/friends") {
                FriendsScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() },
                    onFriendClick = { uid ->
                        navController.navigate("social/friend_detail/${URLEncoder.encode(uid, "UTF-8")}")
                    }
                )
            }
            composable(
                route = "social/friend_detail/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { backStackEntry ->
                val uid = URLDecoder.decode(
                    backStackEntry.arguments?.getString("uid") ?: "",
                    "UTF-8"
                )
                FriendDetailScreen(
                    friendUid = uid,
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/battles") {
                StreakBattleScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/challenges") {
                WeeklyChallengeScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/share") {
                ProgressShareScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/timeline") {
                JourneyTimelineScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/accountability") {
                AccountabilityScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/team_goals") {
                TeamGoalsScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/duels") {
                NutritionDuelScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/leaderboard") {
                LeaderboardScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/templates") {
                WorkoutTemplatesScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenTemplate = { id ->
                        navController.navigate("social/template_detail/${URLEncoder.encode(id, "UTF-8")}")
                    }
                )
            }
            composable(
                route = "social/template_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = URLDecoder.decode(
                    backStackEntry.arguments?.getString("id") ?: "",
                    "UTF-8"
                )
                TemplateDetailScreen(
                    templateId = id,
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("social/badges") {
                AchievementBadgesScreen(
                    socialViewModel = socialViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
