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
import com.example.gymworkout.ui.screens.DayDetailScreen
import com.example.gymworkout.ui.screens.WeeklyPlanScreen
import com.example.gymworkout.ui.screens.YouTubePlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder
import com.example.gymworkout.ui.screens.nutrition.NutritionScreen
import com.example.gymworkout.ui.screens.stats.StatsScreen
import com.example.gymworkout.ui.screens.user.UserScreen
import com.example.gymworkout.data.ThemePreference
import com.example.gymworkout.data.sync.SyncPreference
import com.example.gymworkout.notification.NotificationHelper
import com.example.gymworkout.ui.theme.GymWorkoutTheme
import com.example.gymworkout.viewmodel.NutritionViewModel
import com.example.gymworkout.viewmodel.StatsViewModel
import com.example.gymworkout.viewmodel.UserViewModel
import com.example.gymworkout.viewmodel.WorkoutViewModel

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
        requestNotificationPermission()
        SyncPreference.init(this)
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
    BottomNavItem("nutrition", "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    BottomNavItem("stats", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem("user", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

// Routes where bottom bar should be visible
val bottomBarRoutes = setOf("workout", "nutrition", "stats", "user")

@Composable
fun WorkoutApp() {
    val navController = rememberNavController()
    val workoutViewModel: WorkoutViewModel = viewModel()
    val nutritionViewModel: NutritionViewModel = viewModel()
    val statsViewModel: StatsViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            startDestination = "workout",
            modifier = Modifier.padding(innerPadding)
        ) {
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
                    }
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
            composable("nutrition") {
                NutritionScreen(viewModel = nutritionViewModel)
            }
            composable("stats") {
                StatsScreen(viewModel = statsViewModel)
            }
            composable("user") {
                UserScreen(viewModel = userViewModel)
            }
        }
    }
}
