package com.example.campuspocket.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CampusPrimary,
    primaryContainer = CampusPrimaryContainer,
    background = CampusBackground,
    surface = CampusSurface,
    surfaceVariant = CampusSurfaceVariant,
    onPrimary = CampusOnPrimary,
    onPrimaryContainer = CampusOnPrimaryContainer,
    onBackground = CampusOnBackground,
    onSurface = CampusOnSurface,
    onSurfaceVariant = CampusOnSurfaceVariant,
    outline = CampusOutline,
    error = CampusError,
    onError = CampusOnError,
    secondary = CampusPrimary.copy(alpha = 0.8f),
    secondaryContainer = CampusPrimaryContainer.copy(alpha = 0.5f),
    onSecondary = CampusOnPrimary,
    onSecondaryContainer = CampusOnPrimaryContainer,
    tertiary = CampusSuccess,
    tertiaryContainer = CampusSuccess.copy(alpha = 0.15f),
    onTertiary = CampusOnPrimary,
    onTertiaryContainer = CampusSuccess,
)

private val DarkColorScheme = darkColorScheme(
    primary = CampusPrimary.copy(alpha = 0.9f),
    primaryContainer = CampusPrimary.copy(alpha = 0.3f),
    background = Color(0xFF121820),
    surface = Color(0xFF1A2230),
    surfaceVariant = Color(0xFF252E3E),
    onPrimary = CampusOnPrimary,
    onPrimaryContainer = CampusOnPrimary,
    onBackground = Color(0xFFE8EDF3),
    onSurface = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF9BA8BB),
    outline = Color(0xFF3A4758),
    error = CampusError.copy(alpha = 0.9f),
    onError = CampusOnError,
    secondary = CampusPrimary.copy(alpha = 0.7f),
    secondaryContainer = CampusPrimary.copy(alpha = 0.2f),
    onSecondary = CampusOnPrimary,
    onSecondaryContainer = CampusOnPrimary,
    tertiary = CampusSuccess.copy(alpha = 0.9f),
    tertiaryContainer = CampusSuccess.copy(alpha = 0.2f),
    onTertiary = CampusOnPrimary,
    onTertiaryContainer = CampusSuccess,
)

@Composable
fun CampusTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CampusTypography,
        shapes = CampusShapes,
        content = content
    )
}
