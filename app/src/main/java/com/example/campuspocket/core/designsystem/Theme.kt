package com.example.campuspocket.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Esquemas Material 3 completos derivados de la paleta de la especificación.
 *
 * Reglas aplicadas:
 * - El azul de marca (primary) señala acciones y selección; no pinta grandes áreas.
 * - El fondo claro es cálido (#F5F1E8); las tarjetas principales son blancas.
 * - En oscuro no hay negro absoluto: azul carbón (#111A26) con superficies por tono.
 * - Todas las combinaciones texto/fondo cumplen contraste AA (>= 4.5:1).
 */
private val LightColorScheme = lightColorScheme(
    primary = CampusPalette.Light.Primary,
    onPrimary = CampusPalette.Light.OnPrimary,
    primaryContainer = CampusPalette.Light.PrimaryContainer,
    onPrimaryContainer = CampusPalette.Light.OnPrimaryContainer,
    inversePrimary = CampusPalette.Light.InversePrimary,

    secondary = CampusPalette.Light.TextSecondary,
    onSecondary = CampusPalette.Light.OnPrimary,
    secondaryContainer = CampusPalette.Light.Sage,
    onSecondaryContainer = CampusPalette.Light.OnSage,

    tertiary = CampusPalette.Light.Warm,
    onTertiary = CampusPalette.Light.OnWarm,
    tertiaryContainer = CampusPalette.Light.WarningContainer,
    onTertiaryContainer = CampusPalette.Light.OnWarm,

    background = CampusPalette.Light.Background,
    onBackground = CampusPalette.Light.OnBackground,
    surface = CampusPalette.Light.Surface,
    onSurface = CampusPalette.Light.OnSurface,
    surfaceVariant = CampusPalette.Light.SurfaceSecondary,
    onSurfaceVariant = CampusPalette.Light.TextSecondary,
    surfaceDim = CampusPalette.Light.SurfaceDim,
    surfaceBright = CampusPalette.Light.SurfaceBright,
    surfaceContainerLowest = CampusPalette.Light.SurfaceContainerLowest,
    surfaceContainerLow = CampusPalette.Light.SurfaceContainerLow,
    surfaceContainer = CampusPalette.Light.SurfaceContainer,
    surfaceContainerHigh = CampusPalette.Light.SurfaceContainerHigh,
    surfaceContainerHighest = CampusPalette.Light.SurfaceContainerHighest,

    inverseSurface = CampusPalette.Light.InverseSurface,
    inverseOnSurface = CampusPalette.Light.InverseOnSurface,

    outline = CampusPalette.Light.Border,
    outlineVariant = CampusPalette.Light.OutlineVariant,

    error = CampusPalette.Light.Error,
    onError = CampusPalette.Light.OnError,
    errorContainer = CampusPalette.Light.ErrorContainer,
    onErrorContainer = CampusPalette.Light.OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = CampusPalette.Dark.Primary,
    onPrimary = CampusPalette.Dark.OnPrimary,
    primaryContainer = CampusPalette.Dark.PrimaryContainer,
    onPrimaryContainer = CampusPalette.Dark.OnPrimaryContainer,
    inversePrimary = CampusPalette.Dark.InversePrimary,

    secondary = CampusPalette.Dark.TextSecondary,
    onSecondary = CampusPalette.Dark.OnPrimary,
    secondaryContainer = CampusPalette.Dark.Sage,
    onSecondaryContainer = CampusPalette.Dark.OnSage,

    tertiary = CampusPalette.Dark.Warm,
    onTertiary = CampusPalette.Dark.OnWarm,
    tertiaryContainer = CampusPalette.Dark.WarningContainer,
    onTertiaryContainer = CampusPalette.Dark.OnWarm,

    background = CampusPalette.Dark.Background,
    onBackground = CampusPalette.Dark.OnBackground,
    surface = CampusPalette.Dark.Surface,
    onSurface = CampusPalette.Dark.OnSurface,
    surfaceVariant = CampusPalette.Dark.SurfaceSecondary,
    onSurfaceVariant = CampusPalette.Dark.TextSecondary,
    surfaceDim = CampusPalette.Dark.SurfaceDim,
    surfaceBright = CampusPalette.Dark.SurfaceBright,
    surfaceContainerLowest = CampusPalette.Dark.SurfaceContainerLowest,
    surfaceContainerLow = CampusPalette.Dark.SurfaceContainerLow,
    surfaceContainer = CampusPalette.Dark.SurfaceContainer,
    surfaceContainerHigh = CampusPalette.Dark.SurfaceContainerHigh,
    surfaceContainerHighest = CampusPalette.Dark.SurfaceContainerHighest,

    inverseSurface = CampusPalette.Dark.InverseSurface,
    inverseOnSurface = CampusPalette.Dark.InverseOnSurface,

    outline = CampusPalette.Dark.Border,
    outlineVariant = CampusPalette.Dark.OutlineVariant,

    error = CampusPalette.Dark.Error,
    onError = CampusPalette.Dark.OnError,
    errorContainer = CampusPalette.Dark.ErrorContainer,
    onErrorContainer = CampusPalette.Dark.OnErrorContainer
)

/**
 * Tema único de la app. `darkTheme` lo decide MainActivity a partir de la
 * preferencia persistida (Sistema/Claro/Oscuro) en DataStore.
 */
@Composable
fun CampusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalCampusExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}
