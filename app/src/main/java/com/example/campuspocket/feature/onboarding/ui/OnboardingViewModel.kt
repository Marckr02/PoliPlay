package com.example.campuspocket.feature.onboarding.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.core.preferences.CampusModule
import com.example.campuspocket.core.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Paso 0: nombre opcional. Paso 1: selección de módulos. Paso 2: horario y avisos. */
data class OnboardingUiState(
    val step: Int = 0,
    val name: String = "",
    val academicSelected: Boolean = true,
    val financeSelected: Boolean = true,
    val notesSelected: Boolean = true,
    val taskRemindersEnabled: Boolean = false,
    val exactAlarmsEnabled: Boolean = false,
    val saving: Boolean = false,
    val permissionErrorRes: Int = 0
) {
    fun isSelected(module: CampusModule): Boolean = when (module) {
        CampusModule.ACADEMIC -> academicSelected
        CampusModule.FINANCE -> financeSelected
        CampusModule.NOTES -> notesSelected
    }
    
    /** Total de pasos: 3 si académico está activo, 2 en caso contrario. */
    fun totalSteps(academicEnabled: Boolean): Int = if (academicEnabled) 3 else 2
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun goToModules() = _uiState.update { it.copy(step = 1, permissionErrorRes = 0) }

    fun goBackToName() = _uiState.update { it.copy(step = 0, permissionErrorRes = 0) }

    fun goToSchedule() = _uiState.update { it.copy(step = 2, permissionErrorRes = 0) }

    fun goBackToModules() = _uiState.update { it.copy(step = 1, permissionErrorRes = 0) }

    fun toggleModule(module: CampusModule, selected: Boolean) = _uiState.update { state ->
        when (module) {
            CampusModule.ACADEMIC -> state.copy(academicSelected = selected)
            CampusModule.FINANCE -> state.copy(financeSelected = selected)
            CampusModule.NOTES -> state.copy(notesSelected = selected)
        }
    }

    fun setTaskReminders(enabled: Boolean) = _uiState.update { it.copy(taskRemindersEnabled = enabled, permissionErrorRes = 0) }

    fun setExactAlarms(enabled: Boolean) = _uiState.update { it.copy(exactAlarmsEnabled = enabled, permissionErrorRes = 0) }

    fun setPermissionErrorRes(resId: Int) = _uiState.update { it.copy(permissionErrorRes = resId) }

    /** Lanza el selector de PDF para importar horario. */
    fun launchPdfImport(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(intent)
    }

    /** Maneja el resultado del selector de PDF. */
    fun handlePdfResult(uri: Uri?) {
        uri?.let { pdfUri ->
            // TODO: Launch ImportScheduleScreen with the PDF URI
            // For now, just acknowledge the selection
        }
    }

    /** Solicita permiso de notificaciones (Android 13+). */
    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Solicita permiso de alarmas exactas. */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }

    /** Guarda nombre (recortado), módulos y finalización en una sola escritura. */
    fun finish() {
        val state = _uiState.value
        if (state.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            userPreferencesRepository.completeOnboarding(
                displayName = state.name,
                academicEnabled = state.academicSelected,
                financeEnabled = state.financeSelected,
                notesEnabled = state.notesSelected
            )
            // TODO: Guardar preferencias de notificaciones y alarmas exactas
            // Si la escritura falló, la preferencia no cambia y el usuario puede reintentar.
            _uiState.update { it.copy(saving = false) }
        }
    }
}
