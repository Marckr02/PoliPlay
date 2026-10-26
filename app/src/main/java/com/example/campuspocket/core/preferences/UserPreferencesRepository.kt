package com.example.campuspocket.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.campuspocket.core.util.LogUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Claves únicas del DataStore de preferencias ("settings").
 * Regla: ninguna pantalla define sus propias claves; todo pasa por aquí
 * y por [UserPreferencesRepository].
 */
object PreferenceKeys {
    // Apariencia y personalización
    val THEME = stringPreferencesKey("theme")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val ACADEMIC_ENABLED = booleanPreferencesKey("module_academic_enabled")
    val FINANCE_ENABLED = booleanPreferencesKey("module_finance_enabled")
    val NOTES_ENABLED = booleanPreferencesKey("module_notes_enabled")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")

    // Ajustes existentes (respaldo, notificaciones, seguridad)
    val LAST_BACKUP = longPreferencesKey("last_backup_at")
    val BACKUP_BANNER_DISMISSED = longPreferencesKey("backup_banner_dismissed")
    val CHANNEL_TASKS = booleanPreferencesKey("channel_tasks")
    val CHANNEL_PAYMENTS = booleanPreferencesKey("channel_payments")
    val CHANNEL_BUDGETS = booleanPreferencesKey("channel_budgets")
    val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    val LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")

    // Avisos elegidos en la bienvenida
    val TASK_REMINDERS_ENABLED = booleanPreferencesKey("task_reminders_enabled")
    val EXACT_ALARMS_ENABLED = booleanPreferencesKey("exact_alarms_enabled")

    // Novedades: última versión vista por el usuario (string "0.8.0")
    val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
}

/** Módulos funcionales de la app; activables por separado sin borrar datos. */
enum class CampusModule {
    ACADEMIC,
    FINANCE,
    NOTES
}

/** Tema elegido por el usuario; persistido como cadena estable. */
enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/** Estado de personalización leído de DataStore. */
data class UserPreferences(
    val displayName: String = "",
    val academicEnabled: Boolean = true,
    val financeEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,
    /** Modo privado: oculta montos en Inicio y Finanzas (v5 §2.4). */
    val hideAmounts: Boolean = false
) {
    fun isModuleEnabled(module: CampusModule): Boolean = when (module) {
        CampusModule.ACADEMIC -> academicEnabled
        CampusModule.FINANCE -> financeEnabled
        CampusModule.NOTES -> notesEnabled
    }

    companion object {
        /** Longitud máxima razonable del nombre local (recorta, no rechaza). */
        const val MAX_NAME_LENGTH = 60

        fun sanitizeDisplayName(raw: String): String = raw.trim().take(MAX_NAME_LENGTH)

        /** Mapeo puro Preferences -> UserPreferences (probeable sin Android). */
        fun from(preferences: Preferences): UserPreferences = UserPreferences(
            displayName = preferences[PreferenceKeys.DISPLAY_NAME].orEmpty(),
            academicEnabled = preferences[PreferenceKeys.ACADEMIC_ENABLED] ?: true,
            financeEnabled = preferences[PreferenceKeys.FINANCE_ENABLED] ?: true,
            notesEnabled = preferences[PreferenceKeys.NOTES_ENABLED] ?: true,
            themeMode = ThemeMode.fromStorage(preferences[PreferenceKeys.THEME]),
            onboardingCompleted = preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false,
            hideAmounts = preferences[PreferenceKeys.HIDE_AMOUNTS] ?: false
        )
    }
}

/**
 * Único punto de lectura/escritura de las preferencias de personalización.
 * No crea otra instancia de DataStore: recibe la existente por Hilt.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val safeData: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) {
            LogUtil.w("DataStore ilegible; se usan preferencias por defecto", tag = "Prefs")
            emit(emptyPreferences())
        } else {
            throw e
        }
    }

    val preferences: Flow<UserPreferences> = safeData.map { UserPreferences.from(it) }

    /** Nombre opcional: se recorta y limita; vacío elimina la clave (nunca "null"). */
    suspend fun setDisplayName(name: String) = editOrLog {
        val clean = UserPreferences.sanitizeDisplayName(name)
        if (clean.isEmpty()) remove(PreferenceKeys.DISPLAY_NAME) else set(PreferenceKeys.DISPLAY_NAME, clean)
    }

    suspend fun clearDisplayName() = editOrLog { remove(PreferenceKeys.DISPLAY_NAME) }

    suspend fun setThemeMode(mode: ThemeMode) = editOrLog {
        set(PreferenceKeys.THEME, mode.storageValue)
    }

    /** Modo privado global; se recuerda entre reinicios. */
    suspend fun setHideAmounts(hidden: Boolean) = editOrLog {
        set(PreferenceKeys.HIDE_AMOUNTS, hidden)
    }

    suspend fun setModuleEnabled(module: CampusModule, enabled: Boolean) = editOrLog {
        when (module) {
            CampusModule.ACADEMIC -> set(PreferenceKeys.ACADEMIC_ENABLED, enabled)
            CampusModule.FINANCE -> set(PreferenceKeys.FINANCE_ENABLED, enabled)
            CampusModule.NOTES -> set(PreferenceKeys.NOTES_ENABLED, enabled)
        }
    }

    /**
     * Cierre del onboarding: guarda nombre (opcional), selección de módulos y
     * la marca de finalización en una sola escritura.
     */
    suspend fun completeOnboarding(
        displayName: String,
        academicEnabled: Boolean,
        financeEnabled: Boolean,
        notesEnabled: Boolean,
        taskRemindersEnabled: Boolean = false,
        exactAlarmsEnabled: Boolean = false
    ) = editOrLog {
        val clean = UserPreferences.sanitizeDisplayName(displayName)
        if (clean.isEmpty()) remove(PreferenceKeys.DISPLAY_NAME) else set(PreferenceKeys.DISPLAY_NAME, clean)
        set(PreferenceKeys.ACADEMIC_ENABLED, academicEnabled)
        set(PreferenceKeys.FINANCE_ENABLED, financeEnabled)
        set(PreferenceKeys.NOTES_ENABLED, notesEnabled)
        set(PreferenceKeys.TASK_REMINDERS_ENABLED, taskRemindersEnabled)
        set(PreferenceKeys.EXACT_ALARMS_ENABLED, exactAlarmsEnabled)
        set(PreferenceKeys.ONBOARDING_COMPLETED, true)
    }

    /** Escritura tolerante a fallos: no rompe la UI si el disco falla. */
    private suspend fun editOrLog(block: androidx.datastore.preferences.core.MutablePreferences.() -> Unit) {
        try {
            dataStore.edit(block)
        } catch (e: IOException) {
            LogUtil.e("No se pudieron guardar las preferencias", e, tag = "Prefs")
        }
    }
}
