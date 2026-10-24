package com.example.campuspocket.core.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utilidad de vibración háptica (Sección 10.3 v5).
 *
 * Usa `HapticFeedbackConstants` estándar. Renombrado a `Haptics` para
 * no chocar con `androidx.compose.ui.hapticfeedback.HapticFeedback`.
 */
object Haptics {
    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm(view: View) {
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
        view.performHapticFeedback(c)
    }

    fun reject(view: View) {
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }

    fun contextClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun selection(view: View) {
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
        view.performHapticFeedback(c)
    }
}