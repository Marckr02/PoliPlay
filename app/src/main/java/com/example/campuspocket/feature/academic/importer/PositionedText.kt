package com.example.campuspocket.feature.academic.importer

/** Un fragmento de texto con su posición en la página (y = hacia abajo desde arriba). */
data class PositionedText(
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val text: String
)
