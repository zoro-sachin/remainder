package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BentoDarkColorScheme = darkColorScheme(
    primary = BentoLilac,
    onPrimary = BentoLilacDark,
    primaryContainer = BentoCardPurple,
    onPrimaryContainer = BentoCardPurpleText,
    secondary = BentoCardCoral,
    onSecondary = BentoCardCoralText,
    secondaryContainer = BentoCardSlate,
    onSecondaryContainer = BentoTextPrimary,
    tertiary = BentoPulseGreen,
    background = BentoBg,
    onBackground = BentoTextPrimary,
    surface = BentoSurface,
    onSurface = BentoTextPrimary,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoTextSecondary,
    outline = BentoBorder
)

private val BentoLightColorScheme = lightColorScheme(
    primary = BentoCardPurple,
    onPrimary = Color.White,
    primaryContainer = BentoLilac,
    onPrimaryContainer = BentoLilacDark,
    secondary = BentoCardCoralText,
    onSecondary = Color.White,
    secondaryContainer = BentoCardCoral,
    onSecondaryContainer = BentoCardCoralText,
    tertiary = Color(0xFF00897B),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

enum class AppThemeMode {
    BENTO_GRID,
    DARK_AMOLED,
    MIDNIGHT_BLUE,
    CLEAN_LIGHT,
    SYSTEM
}

@Composable
fun ChronoTaskTheme(
    themeMode: AppThemeMode = AppThemeMode.BENTO_GRID,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.BENTO_GRID, AppThemeMode.DARK_AMOLED, AppThemeMode.MIDNIGHT_BLUE -> true
        AppThemeMode.CLEAN_LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) BentoDarkColorScheme else BentoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
