package com.example.campuspocket.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta primitiva de CampusPocket (tokens de color).
 *
 * Fuente: especificación de rediseño "híbrido profesional".
 * - Tema claro: fondo cálido (#F5F1E8), tarjetas blancas, azul de marca #365CF5.
 * - Tema oscuro: azul carbón (#111A26), superficies elevadas por tono, azul claro #91A8FF.
 *
 * Los colores de datos (materias, cuentas, categorías) viven en ColorPalette.kt y
 * NO deben confundirse con estos tokens: cambiar el tema no altera datos guardados.
 */
internal object CampusPalette {

    object Light {
        val Background = Color(0xFFF5F1E8)
        val Surface = Color(0xFFFFFFFF)
        val SurfaceSecondary = Color(0xFFEDE8DE)
        val OnBackground = Color(0xFF18283B)
        val OnSurface = Color(0xFF18283B)
        val TextSecondary = Color(0xFF526071)
        val Primary = Color(0xFF365CF5)
        val OnPrimary = Color(0xFFFFFFFF)
        val PrimaryContainer = Color(0xFFE7ECFF)
        val OnPrimaryContainer = Color(0xFF18283B)
        val Sage = Color(0xFFD8E8D7)
        val OnSage = Color(0xFF1F3D2B)
        val Warm = Color(0xFFE9B96E)
        val OnWarm = Color(0xFF4A3300)
        val Border = Color(0xFFD8DDE4)
        val OutlineVariant = Color(0xFFE8E4DB)
        val Success = Color(0xFF247A52)
        val Warning = Color(0xFF8A5700)
        val Error = Color(0xFFB4232C)
        val OnError = Color(0xFFFFFFFF)
        val ErrorContainer = Color(0xFFFBDBD9)
        val OnErrorContainer = Color(0xFF5F1215)
        val WarningContainer = Color(0xFFF9E7C8)
        val SuccessContainer = Color(0xFFD8E8D7)
        val HeroContainer = Color(0xFF18283B)
        val OnHeroContainer = Color(0xFFF5F1E8)
        val SurfaceDim = Color(0xFFE8E2D4)
        val SurfaceBright = Color(0xFFFFFCF5)
        val SurfaceContainerLowest = Color(0xFFFFFFFF)
        val SurfaceContainerLow = Color(0xFFF8F5EC)
        val SurfaceContainer = Color(0xFFEDE8DE)
        val SurfaceContainerHigh = Color(0xFFE8E2D6)
        val SurfaceContainerHighest = Color(0xFFE2DCCF)
        val InverseSurface = Color(0xFF2C3C52)
        val InverseOnSurface = Color(0xFFF5F1E8)
        val InversePrimary = Color(0xFF91A8FF)
    }

    object Dark {
        val Background = Color(0xFF111A26)
        val Surface = Color(0xFF1B2737)
        val SurfaceSecondary = Color(0xFF243246)
        val OnBackground = Color(0xFFF1F4F8)
        val OnSurface = Color(0xFFF1F4F8)
        val TextSecondary = Color(0xFFB2BDCC)
        val Primary = Color(0xFF91A8FF)
        val OnPrimary = Color(0xFF111A26)
        val PrimaryContainer = Color(0xFF29385C)
        val OnPrimaryContainer = Color(0xFFE3E9FF)
        val Sage = Color(0xFFA7C9AC)
        val OnSage = Color(0xFF10281A)
        val Warm = Color(0xFFF0C77D)
        val OnWarm = Color(0xFF392C0A)
        val Border = Color(0xFF39485B)
        val OutlineVariant = Color(0xFF2A3748)
        val Success = Color(0xFF8AD5A7)
        val Warning = Color(0xFFF0C77D)
        val Error = Color(0xFFFF9A9E)
        val OnError = Color(0xFF490C12)
        val ErrorContainer = Color(0xFF7F1D24)
        val OnErrorContainer = Color(0xFFFFDAD9)
        val WarningContainer = Color(0xFF553F17)
        val SuccessContainer = Color(0xFF1E3D2B)
        val HeroContainer = Color(0xFF243246)
        val OnHeroContainer = Color(0xFFF1F4F8)
        val SurfaceDim = Color(0xFF111A26)
        val SurfaceBright = Color(0xFF384B63)
        val SurfaceContainerLowest = Color(0xFF0D141E)
        val SurfaceContainerLow = Color(0xFF161F2C)
        val SurfaceContainer = Color(0xFF1B2737)
        val SurfaceContainerHigh = Color(0xFF243246)
        val SurfaceContainerHighest = Color(0xFF2C3C52)
        val InverseSurface = Color(0xFFF1F4F8)
        val InverseOnSurface = Color(0xFF18283B)
        val InversePrimary = Color(0xFF365CF5)
    }
}

/**
 * Tokens semánticos que Material 3 no cubre de serie: éxito, advertencia,
 * acento cálido y superficies de "tarjeta destacada" (hero).
 * Se exponen por composición para que los componentes no fijen colores a mano.
 */
@Immutable
data class CampusExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val warningContainer: Color,
    val successContainer: Color,
    val sage: Color,
    val onSage: Color,
    val warm: Color,
    val onWarm: Color,
    val heroContainer: Color,
    val onHeroContainer: Color
)

internal val LightExtendedColors = CampusExtendedColors(
    success = CampusPalette.Light.Success,
    onSuccess = CampusPalette.Light.OnPrimary,
    warning = CampusPalette.Light.Warning,
    onWarning = CampusPalette.Light.OnPrimary,
    errorContainer = CampusPalette.Light.ErrorContainer,
    onErrorContainer = CampusPalette.Light.OnErrorContainer,
    warningContainer = CampusPalette.Light.WarningContainer,
    successContainer = CampusPalette.Light.SuccessContainer,
    sage = CampusPalette.Light.Sage,
    onSage = CampusPalette.Light.OnSage,
    warm = CampusPalette.Light.Warm,
    onWarm = CampusPalette.Light.OnWarm,
    heroContainer = CampusPalette.Light.HeroContainer,
    onHeroContainer = CampusPalette.Light.OnHeroContainer
)

internal val DarkExtendedColors = CampusExtendedColors(
    success = CampusPalette.Dark.Success,
    onSuccess = CampusPalette.Dark.OnPrimary,
    warning = CampusPalette.Dark.Warning,
    onWarning = CampusPalette.Dark.OnPrimary,
    errorContainer = CampusPalette.Dark.ErrorContainer,
    onErrorContainer = CampusPalette.Dark.OnErrorContainer,
    warningContainer = CampusPalette.Dark.WarningContainer,
    successContainer = CampusPalette.Dark.SuccessContainer,
    sage = CampusPalette.Dark.Sage,
    onSage = CampusPalette.Dark.OnSage,
    warm = CampusPalette.Dark.Warm,
    onWarm = CampusPalette.Dark.OnWarm,
    heroContainer = CampusPalette.Dark.HeroContainer,
    onHeroContainer = CampusPalette.Dark.OnHeroContainer
)

internal val LocalCampusExtendedColors = compositionLocalOf { LightExtendedColors }

/** Acceso ergonómico a los tokens semánticos extendidos del tema actual. */
val MaterialTheme.extended: CampusExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCampusExtendedColors.current
