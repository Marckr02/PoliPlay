package com.example.campuspocket.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.campuspocket.core.designsystem.LocalMotion
import com.example.campuspocket.core.designsystem.MotionTokens
import com.example.campuspocket.core.designsystem.extended

/**
 * Esqueleto de carga (Sección 8 v5).
 *
 * Bloque `track` con una banda clara semitransparente que recorre el ancho
 * cada 1.2 s (lineal, infinito). Con `LocalMotion` desactivado, estático.
 *
 * Uso: Finanzas (hero, tarjetas, filas), Hoy (clase y tareas), Notas (cuadrícula)
 * mientras llega el primer valor del flujo (`isLoading`).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val motion = LocalMotion.current
    val base = MaterialTheme.extended.track
    val band = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f)
               else Color.White.copy(alpha = 0.45f)

    val progress: State<Float> = if (motion.enabled) {
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = -0.3f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerX"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier
            .clip(shape)
            .background(base)
            .drawBehind {
                if (motion.enabled) {            // con "reducir animaciones": bloque estático
                    val w = size.width
                    drawRect(
                        color = band,
                        topLeft = Offset(progress.value * w, 0f),
                        size = Size(w * 0.22f, size.height)
                    )
                }
            }
    )
}

/**
 * Variante conveniente para esqueletos de tarjeta completa.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    cardHeight: androidx.compose.ui.unit.Dp = 120.dp
) {
    ShimmerBox(
        modifier = modifier.fillMaxWidth().height(cardHeight)
    )
}

/**
 * Variante para esqueletos de fila/lista.
 */
@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier,
    rowHeight: androidx.compose.ui.unit.Dp = 72.dp
) {
    ShimmerBox(
        modifier = modifier.fillMaxWidth().height(rowHeight)
    )
}

/**
 * Variante para esqueletos de avatar/icono circular.
 */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    ShimmerBox(
        modifier = modifier.size(size),
        shape = androidx.compose.foundation.shape.CircleShape
    )
}