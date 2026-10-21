package com.example.campuspocket.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Escala de formas del rediseño:
 * - Hero: 32 dp; tarjetas: 28 dp; tarjetas pequeñas: 24 dp; notas: 26 dp.
 * - Campos y botones: 16 dp.
 * - Chips, botones circulares y barra flotante: píldora.
 * - Sin bordes ni elevación: la jerarquía viene del color y el tamaño.
 */
val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Radio máximo: píldoras de cualquier altura. */
val PillShape = RoundedCornerShape(percent = 50)

/** Radio específico de las tarjetas de nota (26 dp). */
val NoteShape = RoundedCornerShape(26.dp)

/** Radio de las tarjetas pequeñas tintadas (24 dp). */
val SmallCardShape = RoundedCornerShape(24.dp)
