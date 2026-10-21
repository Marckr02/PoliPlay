package com.example.campuspocket.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Utilidades de contraste para colores de datos (materias, cuentas, categorías).
 * El color de texto se elige comparando el contraste real con blanco y con la
 * tinta oscura de la app, en lugar de asumir que todo fondo es oscuro.
 */
object ContrastUtil {

    val DarkInk = Color(0xFF0A2229)
    val LightInk = Color(0xFFFFFFFF)

    fun contrastRatio(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /** Devuelve el texto (claro u oscuro) que mejor contrasta con [background]. */
    fun onColorFor(background: Color): Color {
        val dark = contrastRatio(background, DarkInk)
        val light = contrastRatio(background, LightInk)
        return if (dark >= light) DarkInk else LightInk
    }

    /** Fondo seguro para un color de dato: el propio color o su tinte suave. */
    fun containerFor(color: Color): Color = color.copy(alpha = 0.16f)
}
