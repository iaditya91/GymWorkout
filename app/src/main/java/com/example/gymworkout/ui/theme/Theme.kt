package com.example.gymworkout.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = TealSecondary,
    onSecondary = Color.Black,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = GreenSuccess,
    error = RedAccent,
    background = DarkBackground,
    onBackground = Color(0xFFE3E2E6),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C6D0)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightContainer,
    onPrimaryContainer = Color(0xFF0D2B5E),
    secondary = Color(0xFF2BA690),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F1E5),
    onSecondaryContainer = Color(0xFF0D4F45),
    tertiary = Color(0xFF2E8B57),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFBFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = LightSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E8F0),
    onSurfaceVariant = Color(0xFF44474E)
)

@Composable
fun GymWorkoutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
