package com.example.campuspocket.core.designsystem

import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Movimiento global (v5 §10.1). Se lee **una sola vez** al iniciar la Activity
 * y se vuelve a leer en `onResume` para captar cambios del usuario en
 * "Reducir animaciones" (Ajustes > Accesibilidad).
 */
data class MotionSpec(val enabled: Boolean) {
    /** Duración real según la preferencia del usuario. */
    fun ms(base: Int) = if (enabled) base else 0
}

val LocalMotion = staticCompositionLocalOf { MotionSpec(true) }

/** Tokens de duración (ms). Nada supera 360 ms salvo visualizaciones puntuales. */
object MotionTokens {
    const val Short = 120
    const val Medium = 220
    const val Long = 360
    const val InsightBar = 600   // tarjeta ritmo (v5 §10.2 #1)
}

/** Easings estandarizados (v5 §10.1). */
object MotionEasing {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/**
 * Envuelve el tema y provee `LocalMotion` leyendo la escala global del sistema.
 * Se recompuesta cuando cambia `motionKey` (p. ej. en `onResume` de MainActivity).
 */
@Composable
fun ProvideMotion(
    motionKey: Any = Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val enabled = remember(motionKey) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
    CompositionLocalProvider(LocalMotion provides MotionSpec(enabled)) {
        content()
    }
}

/** Helper para que MainActivity fuerce la relectura en onResume. */
class MotionKeyHolder {
    var key: Any = Unit
        private set
    fun refresh() { key = Object() }
}

/**
 * Vibración táctil unificada (v5 §10.3).
 * Usa `HapticFeedbackType` si está disponible en Compose ≥ 1.6; fallback a View.
 */
@Composable
fun performHaptic(type: HapticType) {
    val view = LocalContext.current as? View
    LaunchedEffect(type) {
        view?.let {
            when (type) {
                HapticType.Confirm -> it.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                HapticType.Reject -> it.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                HapticType.KeyPress -> it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }
    }
}

enum class HapticType { Confirm, Reject, KeyPress }