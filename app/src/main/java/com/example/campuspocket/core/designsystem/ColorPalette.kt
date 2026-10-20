package com.example.campuspocket.core.designsystem

/** Paleta limpia y distinguible para acentos de materias y cuentas. */
object ColorPalette {
    private val colors = listOf(
        0xFF2563EB, // Blue
        0xFF059669, // Emerald
        0xFFD97706, // Amber
        0xFFDC2626, // Crimson
        0xFF7C3AED, // Violet
        0xFF0891B2, // Cyan
        0xFFEA580C, // Orange
        0xFFDB2777  // Pink
    ).map { it.toInt() }

    operator fun get(index: Int): Int = colors[index % colors.size]
}