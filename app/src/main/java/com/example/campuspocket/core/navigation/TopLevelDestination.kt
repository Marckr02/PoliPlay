package com.example.campuspocket.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.campuspocket.R
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferences

/**
 * Destinos de la barra inferior. Inicio y Ajustes están siempre disponibles;
 * los módulos aparecen según la selección persistida en DataStore.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    /** Módulo que debe estar activo para mostrarlo; null = siempre visible. */
    val module: CampusModule? = null
) {
    HOME("home", R.string.nav_home, R.drawable.ic_home),
    ACADEMIC("academic", R.string.nav_academic, R.drawable.ic_schedule, CampusModule.ACADEMIC),
    FINANCE("finance", R.string.nav_finance, R.drawable.ic_wallet, CampusModule.FINANCE),
    NOTES("notes", R.string.nav_notes, R.drawable.ic_notes, CampusModule.NOTES),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_settings);

    companion object {
        val start = HOME

        /** Opciones de navegación visibles según los módulos activos. */
        fun visibleFor(preferences: UserPreferences): List<TopLevelDestination> =
            entries.filter { it.module == null || preferences.isModuleEnabled(it.module) }
    }
}
