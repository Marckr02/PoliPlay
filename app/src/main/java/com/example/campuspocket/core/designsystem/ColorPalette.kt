package com.example.campuspocket.core.designsystem

/** Paleta fija y distinguible para colorear las materias importadas (rota por orden de aparición). */
object ColorPalette {
    private val colors = listOf(
        0xFF4C8DFF, 0xFF2EB67D, 0xFFF5A623, 0xFF9B59B6, 0xFF1ABC9C,
        0xFFE67E22, 0xFFE5484D, 0xFF5B6B7F, 0xFF00897B, 0xFF7E57C2
    ).map { it.toInt() }

    operator fun get(index: Int): Int = colors[index % colors.size]
}
