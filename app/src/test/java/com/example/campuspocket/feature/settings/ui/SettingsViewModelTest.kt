package com.example.campuspocket.feature.settings.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas puras del banner de respaldo: llaman a la función estática real del ViewModel. */
class SettingsViewModelTest {

    private val DAY = 24L * 60 * 60 * 1000

    @Test
    fun `sin respaldo previo sale el banner`() {
        assertTrue(SettingsViewModel.shouldShowBackupBanner(null, 0, System.currentTimeMillis()))
    }

    @Test
    fun `descartado hace menos de 7 dias lo oculta`() {
        val now = System.currentTimeMillis()
        assertFalse(SettingsViewModel.shouldShowBackupBanner(now - 2 * DAY, now - 2 * DAY, now))
    }

    @Test
    fun `descartado hace mas de 7 dias y respaldo viejo reaparece`() {
        val now = System.currentTimeMillis()
        assertTrue(SettingsViewModel.shouldShowBackupBanner(now - 40 * DAY, now - 10 * DAY, now))
    }

    @Test
    fun `respaldo reciente no muestra banner`() {
        val now = System.currentTimeMillis()
        assertFalse(SettingsViewModel.shouldShowBackupBanner(now - 3 * DAY, 0, now))
    }
}
