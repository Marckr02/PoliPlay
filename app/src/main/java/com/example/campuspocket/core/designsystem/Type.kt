package com.example.campuspocket.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val CampusFontFamily = FontFamily.Default

private val base = Typography()

/** Escala tipográfica de Material 3 con la fuente de la app; títulos en negrita, cuerpo normal. */
val CampusTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = CampusFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = CampusFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = CampusFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = CampusFontFamily, fontWeight = FontWeight.Medium)
)
