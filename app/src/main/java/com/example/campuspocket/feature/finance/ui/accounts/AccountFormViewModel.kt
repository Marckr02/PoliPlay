package com.example.campuspocket.feature.finance.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountFormUiState(
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    /** Monto inicial en centavos, siempre en positivo (entrada estilo cajero). */
    val initialAmountCents: Long = 0,
    val includeInTotal: Boolean = true,
    val colorArgb: Int,
    val nameError: Boolean = false,
    val isLoaded: Boolean = false,
    val saved: Boolean = false,
    val archivedDone: Boolean = false,
    val notFound: Boolean = false
) {
    val isEditing: Boolean get() = accountId != null

    /** Texto visible del monto: el usuario nunca escribe separadores. */
    val initialAmountDisplay: String get() = Money.toLocalDecimalString(initialAmountCents)
}

@HiltViewModel
class AccountFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val editingId: Long? =
        savedStateHandle.get<Long>(FinanceDestinations.ACCOUNT_ID_ARG)?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(
        AccountFormUiState(colorArgb = 0xFF4C8DFF.toInt())
    )
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val account = financeRepository.getAccount(editingId)
                _uiState.value = if (account == null) {
                    _uiState.value.copy(notFound = true, isLoaded = true)
                } else {
                    // La deuda se guarda negativa; en pantalla se edita en positivo.
                    _uiState.value.copy(
                        accountId = account.id,
                        name = account.name,
                        type = account.type,
                        initialAmountCents = kotlin.math.abs(account.initialBalanceCents),
                        includeInTotal = account.includeInTotal,
                        colorArgb = account.colorArgb,
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

    fun onTypeChange(value: AccountType) {
        _uiState.value = _uiState.value.copy(type = value)
    }

    /** Entrada estilo cajero: solo dígitos; cada dígito entra por la derecha del monto. */
    fun onInitialAmountChange(value: String) {
        val digits = value.filter(Char::isDigit).take(9)
        _uiState.value = _uiState.value.copy(initialAmountCents = digits.toLongOrNull() ?: 0L)
    }

    fun onIncludeInTotalChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(includeInTotal = value)
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
        // Tarjeta de crédito: la deuda ingresada en positivo se guarda negativa.
        val initialCents = if (state.type == AccountType.CREDIT_CARD) {
            -state.initialAmountCents
        } else {
            state.initialAmountCents
        }
        viewModelScope.launch {
            val account = Account(
                id = state.accountId,
                name = state.name.trim(),
                type = state.type,
                initialBalanceCents = initialCents,
                colorArgb = state.colorArgb,
                includeInTotal = state.includeInTotal
            )
            if (state.isEditing) financeRepository.updateAccount(account)
            else financeRepository.insertAccount(account)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun archive() {
        val id = _uiState.value.accountId ?: return
        viewModelScope.launch {
            financeRepository.archiveAccount(id)
            _uiState.value = _uiState.value.copy(archivedDone = true)
        }
    }
}
