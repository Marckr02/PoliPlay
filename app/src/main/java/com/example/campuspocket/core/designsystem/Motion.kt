package com.example.campuspocket.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween as coreTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object MotionTokens {
    const val Short = 120
    const val Medium = 220
    const val Long = 360
    const val ExtraLong = 600
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

@Immutable
class MotionSpec(val enabled: Boolean) {
    /** Duración efectiva: 0 con "reducir animaciones". */
    fun ms(base: Int): Int = if (enabled) base else 0

    /** Genérica: se infiere AnimationSpec<Float>, <Color>, <Dp>, <IntOffset>… */
    fun <T> tween(
        base: Int,
        easing: Easing = MotionTokens.Standard,
        delay: Int = 0
    ): TweenSpec<T> = coreTween(
        durationMillis = ms(base),
        delayMillis = if (enabled) delay else 0,
        easing = easing
    )
}

val LocalMotion = compositionLocalOf { MotionSpec(enabled = true) }

@Composable
fun rememberMotionSpec(): MotionSpec {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        MotionSpec(enabled = scale != 0f)
    }
}