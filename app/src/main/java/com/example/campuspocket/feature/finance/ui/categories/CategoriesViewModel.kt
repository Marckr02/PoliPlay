package com.example.campuspocket.feature.finance.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = financeRepository.observeActiveCategories()
        .map { list ->
            CategoriesUiState(
                expense = list.filter { it.kind == CategoryKind.EXPENSE },
                income = list.filter { it.kind == CategoryKind.INCOME },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CategoriesUiState())

    fun archive(categoryId: Long) {
        viewModelScope.launch { financeRepository.archiveCategory(categoryId) }
    }
}

data class CategoryFormUiState(
    val categoryId: Long? = null,
    val name: String = "",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val colorArgb: Int = 0xFF4C8DFF.toInt(),
    val nameError: Boolean = false,
    val isLoaded: Boolean = false,
    val saved: Boolean = false
) {
    val isEditing: Boolean get() = categoryId != null
}

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val editingId: Long? =
        savedStateHandle.get<Long>("categoryId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val category = financeRepository.getCategory(editingId)
                _uiState.value = if (category == null) {
                    _uiState.value.copy(isLoaded = true)
                } else {
                    _uiState.value.copy(
                        categoryId = category.id,
                        name = category.name,
                        kind = category.kind,
                        colorArgb = category.colorArgb,
                        isLoaded = true
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = false)
    }

    fun onKindChange(value: CategoryKind) {
        _uiState.value = _uiState.value.copy(kind = value)
    }

    fun onColorChange(value: Int) {
        _uiState.value = _uiState.value.copy(colorArgb = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = true)
            return
        }
        viewModelScope.launch {
            val category = Category(
                id = state.categoryId,
                name = state.name.trim(),
                kind = state.kind,
                colorArgb = state.colorArgb
            )
            if (state.isEditing) financeRepository.updateCategory(category)
            else financeRepository.insertCategory(category)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun archive() {
        val id = _uiState.value.categoryId ?: return
        viewModelScope.launch { financeRepository.archiveCategory(id) }
    }
}
