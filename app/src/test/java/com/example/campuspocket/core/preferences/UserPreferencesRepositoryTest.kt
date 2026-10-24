package com.example.campuspocket.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pruebas JVM del repositorio de preferencias con un DataStore real
 * (archivo temporal), no con simulaciones: misma ruta de código que la app.
 */
class UserPreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun newStore(scope: kotlinx.coroutines.CoroutineScope, name: String) =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(folder.root, "$name.preferences_pb") }
        )

    @Test
    fun `valores por defecto sin escrituras`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "defaults"))
        val prefs = repo.preferences.first()
        assertEquals("", prefs.displayName)
        assertTrue(prefs.academicEnabled)
        assertTrue(prefs.financeEnabled)
        assertTrue(prefs.notesEnabled)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertFalse(prefs.onboardingCompleted)
        assertFalse(prefs.hideAmounts)
    }

    @Test
    fun `modo privado se persiste y se recupera`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "privacy"))
        repo.setHideAmounts(true)
        assertTrue(repo.preferences.first().hideAmounts)

        repo.setHideAmounts(false)
        assertFalse(repo.preferences.first().hideAmounts)
    }

    @Test
    fun `nombre se recorta y se limita`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "name"))
        repo.setDisplayName("   Marco   ")
        assertEquals("Marco", repo.preferences.first().displayName)

        repo.setDisplayName("  ")
        assertEquals("", repo.preferences.first().displayName)

        val largo = "x".repeat(200)
        repo.setDisplayName(largo)
        assertEquals(UserPreferences.MAX_NAME_LENGTH, repo.preferences.first().displayName.length)
    }

    @Test
    fun `borrar nombre deja cadena vacia`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "clear"))
        repo.setDisplayName("Ana")
        repo.clearDisplayName()
        assertEquals("", repo.preferences.first().displayName)
    }

    @Test
    fun `modulos se persisten y se recuperan`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "modules"))
        repo.setModuleEnabled(CampusModule.FINANCE, false)
        repo.setModuleEnabled(CampusModule.NOTES, false)
        val prefs = repo.preferences.first()
        assertTrue(prefs.academicEnabled)
        assertFalse(prefs.financeEnabled)
        assertFalse(prefs.notesEnabled)
        assertFalse(prefs.isModuleEnabled(CampusModule.FINANCE))
        assertTrue(prefs.isModuleEnabled(CampusModule.ACADEMIC))

        repo.setModuleEnabled(CampusModule.FINANCE, true)
        assertTrue(repo.preferences.first().financeEnabled)
    }

    @Test
    fun `tema se persiste con los valores existentes`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "theme"))
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.preferences.first().themeMode)
        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.preferences.first().themeMode)
        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repo.preferences.first().themeMode)
    }

    @Test
    fun `completar onboarding guarda todo en una sola escritura`() = runTest {
        val repo = UserPreferencesRepository(newStore(backgroundScope, "onboarding"))
        repo.completeOnboarding(
            displayName = "  Marco  ",
            academicEnabled = true,
            financeEnabled = false,
            notesEnabled = true
        )
        val prefs = repo.preferences.first()
        assertTrue(prefs.onboardingCompleted)
        assertEquals("Marco", prefs.displayName)
        assertFalse(prefs.financeEnabled)
        assertTrue(prefs.notesEnabled)
    }

    @Test
    fun `mapeo puro respeta claves existentes del proyecto`() {
        val legacy = preferencesOf(
            PreferenceKeys.THEME to "dark",
            PreferenceKeys.ONBOARDING_COMPLETED to true
        )
        val mapped = UserPreferences.from(legacy)
        assertEquals(ThemeMode.DARK, mapped.themeMode)
        assertTrue(mapped.onboardingCompleted)
        assertTrue(mapped.academicEnabled)

        val vacio = UserPreferences.from(emptyPreferences())
        assertEquals(ThemeMode.SYSTEM, vacio.themeMode)
        assertFalse(vacio.onboardingCompleted)
    }

    @Test
    fun `themeMode tolera valores desconocidos`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("no-existe"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("system"))
    }
}
