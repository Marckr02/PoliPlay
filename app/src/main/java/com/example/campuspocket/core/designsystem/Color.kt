package com.example.campuspocket.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta primitiva de CampusPocket v2, derivada del ícono del búho.
 *
 * - Verde azulado (ink): hero, dock, selección y enlaces.
 * - Honey: resaltados sobre ink (pestaña activa, etiqueta AHORA, progreso).
 * - Amber: relleno de acciones principales y tarjeta de finanzas del Inicio.
 *
 * Los colores de datos (materias/cuentas) viven en ColorPalette.kt y no cambian
 * con el tema ni con los datos ya guardados.
 */
internal object CampusPalette {

    object Light {
        val Background = Color(0xFFF3F5F4)
        val Surface = Color(0xFFFFFFFF)
        val SurfaceSecondary = Color(0xFFE3E9E8)
        val OnBackground = Color(0xFF0A2229)
        val OnSurface = Color(0xFF0A2229)
        val TextSecondary = Color(0xFF4B5E63)
        val Primary = Color(0xFF044455)
        val OnPrimary = Color(0xFFFFFFFF)
        val PrimaryContainer = Color(0xFFD3E6EA)
        val OnPrimaryContainer = Color(0xFF04252E)
        val Sage = Color(0xFFD9F2E3)
        val OnSage = Color(0xFF14532D)
        val Warm = Color(0xFFFFE6D5)
        val OnWarm = Color(0xFF7A2E0E)
        val Border = Color(0xFFC9D3D5)
        val OutlineVariant = Color(0xFFE3E9E8)
        val Success = Color(0xFF17663F)
        val Warning = Color(0xFF8A4B12)
        val Error = Color(0xFFB4232C)
        val OnError = Color(0xFFFFFFFF)
        val ErrorContainer = Color(0xFFFFE0E1)
        val OnErrorContainer = Color(0xFF5F1215)
        val WarningContainer = Color(0xFFFFE6D5)
        val SuccessContainer = Color(0xFFD9F2E3)
        val HeroContainer = Color(0xFF044455)
        val OnHeroContainer = Color(0xFFFFFFFF)
        val SurfaceDim = Color(0xFFE3E9E8)
        val SurfaceBright = Color(0xFFFFFFFF)
        val SurfaceContainerLowest = Color(0xFFFFFFFF)
        val SurfaceContainerLow = Color(0xFFF7F8FA)
        val SurfaceContainer = Color(0xFFFFFFFF)
        val SurfaceContainerHigh = Color(0xFFEDEFF4)
        val SurfaceContainerHighest = Color(0xFFE3E9E8)
        val InverseSurface = Color(0xFF044455)
        val InverseOnSurface = Color(0xFFFFFFFF)
        val InversePrimary = Color(0xFFFBD76A)

        // Tokens extendidos.
        val Ink = Color(0xFF044455)
        val OnInk = Color(0xFFFFFFFF)
        val OnInkMuted = Color(0xFFB7D0D6)
        val InkMuted = Color(0xFFB7D0D6)
        val InkLine = Color(0xFF0E5A6C)
        val Lime = Color(0xFFFBD76A)
        val OnLime = Color(0xFF0A2229)
        val Accent = Color(0xFFF2C043)
        val OnAccent = Color(0xFF04252E)
        val AccentMuted = Color(0xFFF8D98A)
        val OnAccentMuted = Color(0xFF3A2600)
        val Selected = Color(0xFF044455)
        val OnSelected = Color(0xFFFFFFFF)
        val Danger = Color(0xFFB4232C)
        val OnDanger = Color(0xFFFFFFFF)
        val Track = Color(0xFFE3E9E8)
        val OnTrack = Color(0xFF0A2229)
        val Lavender = Color(0xFFFFF1CC)
        val OnLavender = Color(0xFF5A3F00)
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
        val Dock = Color(0xFF044455)
        val OnDock = Color(0xFFFFFFFF)
        val DockInactive = Color(0xFF8FB4BC)
    }

    object Dark {
        val Background = Color(0xFF071A20)
        val Surface = Color(0xFF0E2A33)
        val SurfaceSecondary = Color(0xFF143540)
        val OnBackground = Color(0xFFEAF2F3)
        val OnSurface = Color(0xFFEAF2F3)
        val TextSecondary = Color(0xFF9DB8BE)
        val Primary = Color(0xFFFBD76A)
        val OnPrimary = Color(0xFF04252E)
        val PrimaryContainer = Color(0xFF0F3B48)
        val OnPrimaryContainer = Color(0xFFEAF2F3)
        val Sage = Color(0xFF1E3D2B)
        val OnSage = Color(0xFF8AD5A7)
        val Warm = Color(0xFF4A2A1A)
        val OnWarm = Color(0xFFFFD9C2)
        val Border = Color(0xFF2A4650)
        val OutlineVariant = Color(0xFF143540)
        val Success = Color(0xFF7EE2A8)
        val Warning = Color(0xFFFFC46B)
        val Error = Color(0xFFFF9A9E)
        val OnError = Color(0xFF490C12)
        val ErrorContainer = Color(0xFF5C1A1F)
        val OnErrorContainer = Color(0xFFFFDAD9)
        val WarningContainer = Color(0xFF4A2A1A)
        val SuccessContainer = Color(0xFF1E3D2B)
        val HeroContainer = Color(0xFF0F3B48)
        val OnHeroContainer = Color(0xFFEAF2F3)
        val SurfaceDim = Color(0xFF071A20)
        val SurfaceBright = Color(0xFF1A4550)
        val SurfaceContainerLowest = Color(0xFF071A20)
        val SurfaceContainerLow = Color(0xFF0B2028)
        val SurfaceContainer = Color(0xFF0E2A33)
        val SurfaceContainerHigh = Color(0xFF122F39)
        val SurfaceContainerHighest = Color(0xFF143540)
        val InverseSurface = Color(0xFFEAF2F3)
        val InverseOnSurface = Color(0xFF071A20)
        val InversePrimary = Color(0xFF044455)

        // Tokens extendidos.
        val Ink = Color(0xFF0F3B48)
        val OnInk = Color(0xFFEAF2F3)
        val OnInkMuted = Color(0xFFA9C4CA)
        val InkMuted = Color(0xFFA9C4CA)
        val InkLine = Color(0xFF1A5666)
        val Lime = Color(0xFFFBD76A)
        val OnLime = Color(0xFF0A2229)
        val Accent = Color(0xFFF2C043)
        val OnAccent = Color(0xFF04252E)
        val AccentMuted = Color(0xFFF8D98A)
        val OnAccentMuted = Color(0xFF3A2600)
        val Selected = Color(0xFFF3F5F4)
        val OnSelected = Color(0xFF04252E)
        val Danger = Color(0xFFFF9A9E)
        val OnDanger = Color(0xFF490C12)
        val Track = Color(0xFF143540)
        val OnTrack = Color(0xFFEAF2F3)
        val Lavender = Color(0xFF4A3A12)
        val OnLavender = Color(0xFFFFE3A0)
        val Peach = Color(0xFF4A2A1A)
        val OnPeach = Color(0xFFFFD9C2)
        val Mint = Color(0xFF1E3D2B)
        val OnMint = Color(0xFF8AD5A7)
        val Sky = Color(0xFF173A45)
        val OnSky = Color(0xFFCDE9F1)
        val LimeSoft = Color(0xFF2E3A12)
        val OnLimeSoft = Color(0xFFE7F5C2)
        val Violet = Color(0xFF2B2347)
        val OnViolet = Color(0xFFDCD2FF)
        val Dock = Color(0xFF0F3B48)
        val OnDock = Color(0xFFEAF2F3)
        val DockInactive = Color(0xFF8FB4BC)
    }
}

/**
 * Tokens semánticos que Material 3 no cubre de serie: éxito, advertencia,
 * acento ámbar, selección, barra flotante y tintes pastel.
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
    val inkMuted: Color,
    val inkLine: Color,
    val lime: Color,
    val onLime: Color,
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val onAccentMuted: Color,
    val selected: Color,
    val onSelected: Color,
    val danger: Color,
    val onDanger: Color,
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
    inkMuted = CampusPalette.Light.InkMuted,
    inkLine = CampusPalette.Light.InkLine,
    lime = CampusPalette.Light.Lime,
    onLime = CampusPalette.Light.OnLime,
    accent = CampusPalette.Light.Accent,
    onAccent = CampusPalette.Light.OnAccent,
    accentMuted = CampusPalette.Light.AccentMuted,
    onAccentMuted = CampusPalette.Light.OnAccentMuted,
    selected = CampusPalette.Light.Selected,
    onSelected = CampusPalette.Light.OnSelected,
    danger = CampusPalette.Light.Danger,
    onDanger = CampusPalette.Light.OnDanger,
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
    inkMuted = CampusPalette.Dark.InkMuted,
    inkLine = CampusPalette.Dark.InkLine,
    lime = CampusPalette.Dark.Lime,
    onLime = CampusPalette.Dark.OnLime,
    accent = CampusPalette.Dark.Accent,
    onAccent = CampusPalette.Dark.OnAccent,
    accentMuted = CampusPalette.Dark.AccentMuted,
    onAccentMuted = CampusPalette.Dark.OnAccentMuted,
    selected = CampusPalette.Dark.Selected,
    onSelected = CampusPalette.Dark.OnSelected,
    danger = CampusPalette.Dark.Danger,
    onDanger = CampusPalette.Dark.OnDanger,
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
