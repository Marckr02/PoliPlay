package com.example.campuspocket.feature.onboarding.ui

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

/** Paso 0: nombre opcional. Paso 1: selección de módulos. */
data class OnboardingUiState(
    val step: Int = 0,
    val name: String = "",
    val academicSelected: Boolean = true,
    val financeSelected: Boolean = true,
    val notesSelected: Boolean = true,
    val saving: Boolean = false
) {
    fun isSelected(module: CampusModule): Boolean = when (module) {
        CampusModule.ACADEMIC -> academicSelected
        CampusModule.FINANCE -> financeSelected
        CampusModule.NOTES -> notesSelected
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun goToModules() = _uiState.update { it.copy(step = 1) }

    fun goBackToName() = _uiState.update { it.copy(step = 0) }

    fun toggleModule(module: CampusModule, selected: Boolean) = _uiState.update { state ->
        when (module) {
            CampusModule.ACADEMIC -> state.copy(academicSelected = selected)
            CampusModule.FINANCE -> state.copy(financeSelected = selected)
            CampusModule.NOTES -> state.copy(notesSelected = selected)
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
            // Si la escritura falló, la preferencia no cambia y el usuario puede reintentar.
            _uiState.update { it.copy(saving = false) }
        }
    }
}
