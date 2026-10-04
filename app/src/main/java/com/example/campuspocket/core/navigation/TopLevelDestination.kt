package com.example.campuspocket.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.campuspocket.R

/** Destinos de la barra inferior. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    ACADEMIC("academic", R.string.nav_academic, R.drawable.ic_schedule),
    FINANCE("finance", R.string.nav_finance, R.drawable.ic_wallet),
    NOTES("notes", R.string.nav_notes, R.drawable.ic_notes),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_settings);

    companion object {
        val start = ACADEMIC
    }
}
