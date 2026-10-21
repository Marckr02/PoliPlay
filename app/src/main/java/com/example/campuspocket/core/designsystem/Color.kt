package com.example.campuspocket.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta primitiva de CampusPocket (tokens de color).
 *
 * Fuente: especificación "bento + barra flotante".
 * - Claro: fondo neutro frío (#F2F3F7), tarjetas blancas, tinta #0E1116 y acento índigo.
 * - Oscuro: fondo #0B0E13, tarjetas #171B23, hero/barra #232A36 y acento #8B87FF.
 *
 * Los colores de datos (materias, cuentas, categorías) viven en ColorPalette.kt y
 * NO deben confundirse con estos tokens: cambiar el tema no altera datos guardados.
 */
internal object CampusPalette {

    object Light {
        val Background = Color(0xFFF2F3F7)
        val Surface = Color(0xFFFFFFFF)
        val SurfaceSecondary = Color(0xFFE4E7EE)
        val OnBackground = Color(0xFF0E1116)
        val OnSurface = Color(0xFF0E1116)
        val TextSecondary = Color(0xFF565D6B)
        val Primary = Color(0xFF4F46E5)
        val OnPrimary = Color(0xFFFFFFFF)
        val PrimaryContainer = Color(0xFFE4E3FF)
        val OnPrimaryContainer = Color(0xFF3730A3)
        val Sage = Color(0xFFD9F2E3)
        val OnSage = Color(0xFF14532D)
        val Warm = Color(0xFFFFE6D5)
        val OnWarm = Color(0xFF7A2E0E)
        val Border = Color(0xFFE4E7EE)
        val OutlineVariant = Color(0xFFEDEFF4)
        val Success = Color(0xFF17663F)
        val Warning = Color(0xFF8A4B12)
        val Error = Color(0xFFB4232C)
        val OnError = Color(0xFFFFFFFF)
        val ErrorContainer = Color(0xFFFFE0E1)
        val OnErrorContainer = Color(0xFF5F1215)
        val WarningContainer = Color(0xFFFFE6D5)
        val SuccessContainer = Color(0xFFD9F2E3)
        val HeroContainer = Color(0xFF0E1116)
        val OnHeroContainer = Color(0xFFF1F4F8)
        val SurfaceDim = Color(0xFFE4E7EE)
        val SurfaceBright = Color(0xFFFFFFFF)
        val SurfaceContainerLowest = Color(0xFFFFFFFF)
        val SurfaceContainerLow = Color(0xFFF7F8FA)
        val SurfaceContainer = Color(0xFFFFFFFF)
        val SurfaceContainerHigh = Color(0xFFEDEFF4)
        val SurfaceContainerHighest = Color(0xFFE4E7EE)
        val InverseSurface = Color(0xFF0E1116)
        val InverseOnSurface = Color(0xFFF1F4F8)
        val InversePrimary = Color(0xFFC6F45A)

        // Tokens extendidos del rediseño.
        val Ink = Color(0xFF0E1116)
        val OnInk = Color(0xFFF1F4F8)
        val OnInkMuted = Color(0xFFB4BBC8)
        val Lime = Color(0xFFC6F45A)
        val OnLime = Color(0xFF0E1116)
        val Track = Color(0xFFE4E7EE)
        val OnTrack = Color(0xFF0E1116)
        val Lavender = Color(0xFFE4E3FF)
        val OnLavender = Color(0xFF3730A3)
        val Peach = Color(0xFFFFE6D5)
        val OnPeach = Color(0xFF7A2E0E)
        val Mint = Color(0xFFD9F2E3)
        val OnMint = Color(0xFF14532D)
        val Sky = Color(0xFFDDF0F5)
        val OnSky = Color(0xFF1F4B5A)
        val LimeSoft = Color(0xFFE7F5C2)
        val OnLimeSoft = Color(0xFF2F3A0B)
        val Violet = Color(0xFFE9E2FF)
        val OnViolet = Color(0xFF3B2A7A)
        val Dock = Color(0xFF0E1116)
        val OnDock = Color(0xFFF1F4F8)
        val DockInactive = Color(0xFFA3AAB8)
    }

    object Dark {
        val Background = Color(0xFF0B0E13)
        val Surface = Color(0xFF171B23)
        val SurfaceSecondary = Color(0xFF232A36)
        val OnBackground = Color(0xFFF1F4F8)
        val OnSurface = Color(0xFFF1F4F8)
        val TextSecondary = Color(0xFFAAB2C2)
        val Primary = Color(0xFF8B87FF)
        val OnPrimary = Color(0xFF0B0E13)
        val PrimaryContainer = Color(0xFF2B2854)
        val OnPrimaryContainer = Color(0xFFE4E3FF)
        val Sage = Color(0xFF193B2A)
        val OnSage = Color(0xFFB7F0CB)
        val Warm = Color(0xFF3A2519)
        val OnWarm = Color(0xFFFFD9BF)
        val Border = Color(0xFF2A303B)
        val OutlineVariant = Color(0xFF222833)
        val Success = Color(0xFF7EE2A8)
        val Warning = Color(0xFFFFC46B)
        val Error = Color(0xFFFF9A9E)
        val OnError = Color(0xFF490C12)
        val ErrorContainer = Color(0xFF5C1A1F)
        val OnErrorContainer = Color(0xFFFFDAD9)
        val WarningContainer = Color(0xFF3A2519)
        val SuccessContainer = Color(0xFF193B2A)
        val HeroContainer = Color(0xFF232A36)
        val OnHeroContainer = Color(0xFFF1F4F8)
        val SurfaceDim = Color(0xFF0B0E13)
        val SurfaceBright = Color(0xFF2A303B)
        val SurfaceContainerLowest = Color(0xFF0B0E13)
        val SurfaceContainerLow = Color(0xFF12161D)
        val SurfaceContainer = Color(0xFF171B23)
        val SurfaceContainerHigh = Color(0xFF1E242E)
        val SurfaceContainerHighest = Color(0xFF232A36)
        val InverseSurface = Color(0xFFF1F4F8)
        val InverseOnSurface = Color(0xFF0E1116)
        val InversePrimary = Color(0xFF4F46E5)

        // Tokens extendidos del rediseño.
        val Ink = Color(0xFF232A36)
        val OnInk = Color(0xFFF1F4F8)
        val OnInkMuted = Color(0xFFB4BBC8)
        val Lime = Color(0xFFC6F45A)
        val OnLime = Color(0xFF0E1116)
        val Track = Color(0xFF232A36)
        val OnTrack = Color(0xFFF1F4F8)
        val Lavender = Color(0xFF2E2A52)
        val OnLavender = Color(0xFFD9D6FF)
        val Peach = Color(0xFF3A2519)
        val OnPeach = Color(0xFFFFD9BF)
        val Mint = Color(0xFF193B2A)
        val OnMint = Color(0xFFB7F0CB)
        val Sky = Color(0xFF16333C)
        val OnSky = Color(0xFFB9E6F2)
        val LimeSoft = Color(0xFF2E3A12)
        val OnLimeSoft = Color(0xFFE7F5C2)
        val Violet = Color(0xFF2B2347)
        val OnViolet = Color(0xFFDCD2FF)
        val Dock = Color(0xFF232A36)
        val OnDock = Color(0xFFF1F4F8)
        val DockInactive = Color(0xFF9AA3B2)
    }
}

/**
 * Tokens semánticos que Material 3 no cubre de serie: éxito, advertencia,
 * acento lima, pista de controles, barra flotante y tintes pastel.
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
    val onHeroContainer: Color,
    val ink: Color,
    val onInk: Color,
    val onInkMuted: Color,
    val lime: Color,
    val onLime: Color,
    val track: Color,
    val onTrack: Color,
    val lavender: Color,
    val onLavender: Color,
    val peach: Color,
    val onPeach: Color,
    val mint: Color,
    val onMint: Color,
    val sky: Color,
    val onSky: Color,
    val limeSoft: Color,
    val onLimeSoft: Color,
    val violet: Color,
    val onViolet: Color,
    val dock: Color,
    val onDock: Color,
    val dockInactive: Color
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
    onHeroContainer = CampusPalette.Light.OnHeroContainer,
    ink = CampusPalette.Light.Ink,
    onInk = CampusPalette.Light.OnInk,
    onInkMuted = CampusPalette.Light.OnInkMuted,
    lime = CampusPalette.Light.Lime,
    onLime = CampusPalette.Light.OnLime,
    track = CampusPalette.Light.Track,
    onTrack = CampusPalette.Light.OnTrack,
    lavender = CampusPalette.Light.Lavender,
    onLavender = CampusPalette.Light.OnLavender,
    peach = CampusPalette.Light.Peach,
    onPeach = CampusPalette.Light.OnPeach,
    mint = CampusPalette.Light.Mint,
    onMint = CampusPalette.Light.OnMint,
    sky = CampusPalette.Light.Sky,
    onSky = CampusPalette.Light.OnSky,
    limeSoft = CampusPalette.Light.LimeSoft,
    onLimeSoft = CampusPalette.Light.OnLimeSoft,
    violet = CampusPalette.Light.Violet,
    onViolet = CampusPalette.Light.OnViolet,
    dock = CampusPalette.Light.Dock,
    onDock = CampusPalette.Light.OnDock,
    dockInactive = CampusPalette.Light.DockInactive
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
    onHeroContainer = CampusPalette.Dark.OnHeroContainer,
    ink = CampusPalette.Dark.Ink,
    onInk = CampusPalette.Dark.OnInk,
    onInkMuted = CampusPalette.Dark.OnInkMuted,
    lime = CampusPalette.Dark.Lime,
    onLime = CampusPalette.Dark.OnLime,
    track = CampusPalette.Dark.Track,
    onTrack = CampusPalette.Dark.OnTrack,
    lavender = CampusPalette.Dark.Lavender,
    onLavender = CampusPalette.Dark.OnLavender,
    peach = CampusPalette.Dark.Peach,
    onPeach = CampusPalette.Dark.OnPeach,
    mint = CampusPalette.Dark.Mint,
    onMint = CampusPalette.Dark.OnMint,
    sky = CampusPalette.Dark.Sky,
    onSky = CampusPalette.Dark.OnSky,
    limeSoft = CampusPalette.Dark.LimeSoft,
    onLimeSoft = CampusPalette.Dark.OnLimeSoft,
    violet = CampusPalette.Dark.Violet,
    onViolet = CampusPalette.Dark.OnViolet,
    dock = CampusPalette.Dark.Dock,
    onDock = CampusPalette.Dark.OnDock,
    dockInactive = CampusPalette.Dark.DockInactive
)

internal val LocalCampusExtendedColors = compositionLocalOf { LightExtendedColors }

/** Acceso ergonómico a los tokens semánticos extendidos del tema actual. */
val MaterialTheme.extended: CampusExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCampusExtendedColors.current
