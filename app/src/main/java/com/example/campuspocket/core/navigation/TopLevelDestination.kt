package com.example.campuspocket.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.campuspocket.R
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences

/**
 * Destinos de la barra flotante. Inicio y los módulos activos aparecen en el
 * dock; Ajustes se abre desde el avatar de Inicio, no ocupa una pestaña.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    /** Módulo que debe estar activo para mostrarlo; null = siempre visible. */
    val module: CampusModule? = null,
    /** Si forma parte de la barra flotante inferior. */
    val showInDock: Boolean = true
) {
    HOME("home", R.string.nav_home, R.drawable.ic_home),
    ACADEMIC("academic", R.string.nav_academic, R.drawable.ic_schedule, CampusModule.ACADEMIC),
    FINANCE("finance", R.string.nav_finance, R.drawable.ic_wallet, CampusModule.FINANCE),
    NOTES("notes", R.string.nav_notes, R.drawable.ic_notes, CampusModule.NOTES),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_settings, showInDock = false);

    companion object {
        val start = HOME

        /** Opciones de la barra flotante visibles según los módulos activos. */
        fun visibleFor(preferences: UserPreferences): List<TopLevelDestination> =
            entries.filter {
                it.showInDock && (it.module == null || preferences.isModuleEnabled(it.module))
            }
    }
}
