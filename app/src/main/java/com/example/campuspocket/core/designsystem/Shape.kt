package com.example.campuspocket.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Escala de formas del rediseño:
 * - Tarjetas principales: 16–20 dp.
 * - Campos y botones: 12–16 dp.
 * - Chips e indicadores: formas compactas (8 dp).
 * - Hojas y diálogos: 28 dp, sin exagerar el resto de elementos.
 */
val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
