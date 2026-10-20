package com.example.campuspocket.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Tipografía del sistema; escalable con el ajuste de fuente del dispositivo. */
val CampusFontFamily = FontFamily.Default

private val base = Typography()

/**
 * Jerarquía editorial del rediseño:
 * - Titulares fuertes pero no todos en negrita máxima (semibold).
 * - Cuerpo de texto normal, metadatos en bodySmall legibles.
 */
val CampusTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium)
)
