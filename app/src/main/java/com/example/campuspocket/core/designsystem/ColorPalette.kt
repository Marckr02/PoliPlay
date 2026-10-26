package com.example.campuspocket.core.designsystem

import androidx.annotation.StringRes
import com.example.campuspocket.R

/** Paleta limpia y distinguible para acentos de materias y cuentas. */
object ColorPalette {
    private val colors = listOf(
        0xFF2563EB, // Blue
        0xFF047857, // Emerald (WCAG AA sobre blanco y tinta oscura)
        0xFFD97706, // Amber
        0xFFDC2626, // Crimson
        0xFF7C3AED, // Violet
        0xFF0891B2, // Cyan
        0xFFEA580C, // Orange
        0xFFDB2777  // Pink
    ).map { it.toInt() }

    private val names = listOf(
        R.string.color_blue,
        R.string.color_emerald,
        R.string.color_amber,
        R.string.color_crimson,
        R.string.color_violet,
        R.string.color_cyan,
        R.string.color_orange,
        R.string.color_pink
    )

    operator fun get(index: Int): Int = colors[index % colors.size]

    @StringRes
    fun nameRes(index: Int): Int = names[index % names.size]
}
