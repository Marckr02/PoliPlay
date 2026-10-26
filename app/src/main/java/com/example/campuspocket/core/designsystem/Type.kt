package com.example.campuspocket.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.campuspocket.R

/** Plus Jakarta Sans (OFL) empaquetada en res/font; escala con el ajuste del sistema. */
val CampusFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Float = 0f
) = TextStyle(
    fontFamily = CampusFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

/**
 * Jerarquía editorial del rediseño:
 * - Títulos grandes en ExtraBold con tracking negativo.
 * - Cifras hero ExtraBold; cuerpo Medium (500) para lectura cómoda.
 * - Etiquetas pequeñas Semibold/Bold en mayúsculas solo donde la spec lo pide.
 */
val CampusTypography = Typography(
    displayLarge = style(44, 50, FontWeight.ExtraBold, -1.5f),
    displayMedium = style(40, 46, FontWeight.ExtraBold, -1.2f),
    displaySmall = style(34, 40, FontWeight.ExtraBold, -1f),
    headlineLarge = style(34, 40, FontWeight.ExtraBold, -1f),
    headlineMedium = style(26, 32, FontWeight.ExtraBold, -0.6f),
    headlineSmall = style(22, 28, FontWeight.ExtraBold, -0.4f),
    titleLarge = style(20, 26, FontWeight.ExtraBold, -0.2f),
    titleMedium = style(16, 22, FontWeight.Bold),
    titleSmall = style(14, 20, FontWeight.Bold),
    bodyLarge = style(15, 22, FontWeight.Medium),
    bodyMedium = style(14, 20, FontWeight.Medium),
    bodySmall = style(13, 18, FontWeight.Medium),
    labelLarge = style(14, 18, FontWeight.Bold),
    labelMedium = style(12, 16, FontWeight.Bold, 0.2f),
    labelSmall = style(12, 16, FontWeight.SemiBold, 0.4f)
)

/**
 * Estilos especiales fuera de la escala Material: cifras hero, numerales de
 * tarjeta, sobrelíneas y etiquetas del teclado. Evitan tamaños y alturas de
 * línea sueltos en las pantallas y escalan con el ajuste de fuente del sistema.
 */
val CampusNumeralHero = style(52, 58, FontWeight.ExtraBold, -2f)
val CampusNumeral = style(32, 38, FontWeight.ExtraBold, -0.8f)
val CampusNumeralSmall = style(24, 30, FontWeight.ExtraBold, -0.4f)
val CampusOverline = style(11, 16, FontWeight.Bold, 0.8f)
val CampusKeypadLabel = style(20, 22, FontWeight.ExtraBold, -0.2f)
