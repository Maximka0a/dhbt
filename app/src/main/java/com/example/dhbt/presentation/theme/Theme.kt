package com.example.dhbt.presentation.theme

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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PrimaryVariantLight,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = SecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB8E4B8),
    onSecondaryContainer = Color(0xFF002200),
    tertiary = CategoryPurple,
    background = BackgroundLight,
    onBackground = Color(0xFF121212),
    surface = SurfaceLight,
    onSurface = Color(0xFF121212),
    error = HighPriority,
    onError = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF000000),
    primaryContainer = PrimaryVariantDark.copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = SecondaryDark,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF094409),
    onSecondaryContainer = Color(0xFFB8E4B8),
    tertiary = CategoryPurple.copy(alpha = 0.7f),
    background = BackgroundDark,
    onBackground = Color(0xFFF5F5F5),
    surface = SurfaceDark,
    onSurface = Color(0xFFF5F5F5),
    error = HighPriority.copy(alpha = 0.8f),
    onError = Color(0xFF000000)
)

@Composable
fun DHbtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}