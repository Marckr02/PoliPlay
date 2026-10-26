package com.example.campuspocket.feature.settings.ui

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.BuildConfig
import com.example.campuspocket.core.backup.BackupImportException
import com.example.campuspocket.core.backup.BackupImportIssue
import com.example.campuspocket.core.backup.BackupManager
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.PreferenceKeys
import com.example.campuspocket.core.preferences.ThemeMode
import com.example.campuspocket.core.preferences.UserPreferences
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import com.example.campuspocket.feature.academic.data.SemesterDao
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.finance.notifications.DailyPaymentReviewWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named

data class SettingsPrefs(
    val channelTasks: Boolean = true,
    val channelPayments: Boolean = true,
    val channelBudgets: Boolean = true,
    val lockEnabled: Boolean = false,
    val lockTimeoutSeconds: Int = 0,
    val lastBackupBannerDismissedAt: Long = 0
)

data class SettingsUiState(
    val busy: Boolean = false,
    val pendingImport: Uri? = null,
    val importError: BackupImportIssue? = null,
    val exportDone: Boolean = false,
    val importDone: Boolean = false,
    val showWhatsNew: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val dataStore: DataStore<Preferences>,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val semesterDao: SemesterDao,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // ---- Novedades (What's new) ----
    private val CURRENT_VERSION = BuildConfig.VERSION_NAME

    private val lastSeenVersion: StateFlow<String?> = dataStore.data
        .map { it[PreferenceKeys.LAST_SEEN_VERSION] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val shouldShowWhatsNew: StateFlow<Boolean> = lastSeenVersion
        .map { lastSeen ->
            // Mostrar si es la primera vez (null) o si la versión cambió
            lastSeen == null || lastSeen != CURRENT_VERSION
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ---- Respaldo ----

    val lastBackupAt: StateFlow<Long?> = dataStore.data
        .map { it[PreferenceKeys.LAST_BACKUP] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    // Observar shouldShowWhatsNew y actualizar _showWhatsNew
    init {
        viewModelScope.launch {
            shouldShowWhatsNew.collect { show ->
                if (show) _showWhatsNew.value = true
            }
        }
    }

    fun markWhatsNewSeen() {
        _showWhatsNew.value = false
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.LAST_SEEN_VERSION] = CURRENT_VERSION }
        }
    }

    fun suggestedFileName(): String = "campuspocket-${LocalDate.now()}.json"

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            try {
                withContext(ioDispatcher) {
                    context.contentResolver.openOutputStream(uri, "w")!!.use { out -> backupManager.exportBackup(out) }
                }
                dataStore.edit { it[PreferenceKeys.LAST_BACKUP] = System.currentTimeMillis() }
                _uiState.value = _uiState.value.copy(busy = false, exportDone = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, importError = BackupImportIssue.NOT_JSON)
            }
        }
    }

    fun prepareImport(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, importError = null)
            val ok = try {
                withContext(ioDispatcher) {
                    context.contentResolver.openInputStream(uri)!!.use { backupManager.readAndValidate(it) }
                }
                true
            } catch (e: BackupImportException) {
                _uiState.value = _uiState.value.copy(busy = false, importError = e.issue)
                false
            }
            if (ok) _uiState.value = _uiState.value.copy(busy = false, pendingImport = uri)
        }
    }

    fun confirmImport() {
        val uri = _uiState.value.pendingImport ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, pendingImport = null)
            val file = withContext(ioDispatcher) {
                context.contentResolver.openInputStream(uri)!!.use { backupManager.readAndValidate(it) }
            }
            try {
                backupManager.importBackup(file)
                dataStore.edit { it[PreferenceKeys.LAST_BACKUP] = System.currentTimeMillis() }
                _uiState.value = _uiState.value.copy(busy = false, importDone = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, importError = BackupImportIssue.NOT_JSON)
            }
        }
    }

    fun dismissImport() { _uiState.value = _uiState.value.copy(pendingImport = null, importError = null) }
    fun dismissExportDone() { _uiState.value = _uiState.value.copy(exportDone = false) }

    // ---- Banner de respaldo (>30 días o nunca, descartable 7 días) ----
    // la lógica vive en el companion como Companion.shouldShowBackupBanner (pura, probeable)
    fun delegateShouldShowBanner(lastBackupAt: Long?, dismissedAt: Long): Boolean =
        Companion.shouldShowBackupBanner(lastBackupAt, dismissedAt, System.currentTimeMillis())

    fun dismissBackupBanner() {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.BACKUP_BANNER_DISMISSED] = System.currentTimeMillis() }
        }
    }

    // ---- Preferencias ----

    /** Preferencias de personalización centralizadas (perfil, módulos, tema, onboarding). */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val prefs: StateFlow<SettingsPrefs> = dataStore.data.map { p ->
        SettingsPrefs(
            channelTasks = p[PreferenceKeys.CHANNEL_TASKS] ?: true,
            channelPayments = p[PreferenceKeys.CHANNEL_PAYMENTS] ?: true,
            channelBudgets = p[PreferenceKeys.CHANNEL_BUDGETS] ?: true,
            lockEnabled = p[PreferenceKeys.LOCK_ENABLED] ?: false,
            lockTimeoutSeconds = p[PreferenceKeys.LOCK_TIMEOUT_SECONDS] ?: 0,
            lastBackupBannerDismissedAt = p[PreferenceKeys.BACKUP_BANNER_DISMISSED] ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPrefs())

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) } }

    fun setDisplayName(name: String) { viewModelScope.launch { userPreferencesRepository.setDisplayName(name) } }

    fun clearDisplayName() { viewModelScope.launch { userPreferencesRepository.clearDisplayName() } }

    fun setModuleEnabled(module: CampusModule, enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setModuleEnabled(module, enabled) }
    }

    fun setChannelEnabled(channel: String, enabled: Boolean) {
        viewModelScope.launch {
            when (channel) {
                "tasks" -> dataStore.edit { it[PreferenceKeys.CHANNEL_TASKS] = enabled }
                "payments" -> dataStore.edit { it[PreferenceKeys.CHANNEL_PAYMENTS] = enabled }
                "budgets" -> dataStore.edit { it[PreferenceKeys.CHANNEL_BUDGETS] = enabled }
            }
        }
    }

    // ---- Semestre activo ----

    val semesters: StateFlow<List<SemesterEntity>> = semesterDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSemesterName: StateFlow<String?> = semesters
        .map { list -> list.firstOrNull { it.isActive }?.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setActiveSemester(id: Long) { viewModelScope.launch { semesterDao.setActive(id) } }

    // ---- Bloqueo ----

    fun enableLock() { viewModelScope.launch { dataStore.edit { it[PreferenceKeys.LOCK_ENABLED] = true } } }
    fun disableLock() { viewModelScope.launch { dataStore.edit { it[PreferenceKeys.LOCK_ENABLED] = false } } }
    fun setLockTimeout(seconds: Int) { viewModelScope.launch { dataStore.edit { it[PreferenceKeys.LOCK_TIMEOUT_SECONDS] = seconds } } }

    /** ¿El dispositivo tiene credencial (biométrica fuerte o credencial de pantalla)? */
    fun lockSupported(context: Context): Boolean {
        val bioAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bm = context.getSystemService(BiometricManager::class.java)
            bm?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } else false
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return bioAvailable || keyguard.isDeviceSecure
    }

    /** Tiempo de posado en background antes de pedir credencial otra vez. */
    fun lockExpiresAt(prefs: SettingsPrefs, wentBackgroundAt: Long): Long {
        val graceMillis = when (prefs.lockTimeoutSeconds) {
            0 -> 0L
            else -> prefs.lockTimeoutSeconds * 1000L
        }
        return wentBackgroundAt + graceMillis
    }

    fun reviewNow(context: Context) { DailyPaymentReviewWorker.enqueueNow(context) }

    companion object {
        /** Lógica pura del banner: prunedoble respaldo / ausencia / antigüedad de despacho. */
        fun shouldShowBackupBanner(lastBackupAt: Long?, dismissedAt: Long, nowMillis: Long): Boolean {
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val sevenDays = 7L * 24 * 60 * 60 * 1000
            if (dismissedAt > 0 && nowMillis - dismissedAt <= sevenDays) return false
            return lastBackupAt == null || nowMillis - lastBackupAt > thirtyDays
        }
    }
}