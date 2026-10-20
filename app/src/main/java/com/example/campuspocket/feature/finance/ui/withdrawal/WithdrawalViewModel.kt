package com.example.campuspocket.feature.finance.ui.withdrawal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.finance.domain.CashAccountRules
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.WithdrawalError
import com.example.campuspocket.feature.finance.domain.WithdrawalResult
import com.example.campuspocket.feature.finance.domain.WithdrawalRules
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WithdrawalUiState(
    val savingsAccounts: List<AccountBalance> = emptyList(),
    val cashAccount: Account? = null,
    val selectedAccountId: Long? = null,
    /** true cuando el usuario eligió el chip "Otro". */
    val isCustomAmount: Boolean = false,
    /** 5, 10 o 15 cuando hay un chip predefinido seleccionado. */
    val presetDollars: Int? = null,
    val amountCents: Long = 0,
    val error: WithdrawalError? = null,
    val savedAmountCents: Long? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true
) {
    val selectedAccount: AccountBalance?
        get() = savingsAccounts.firstOrNull { it.account.id == selectedAccountId }
}

private data class WithdrawalFormState(
    val accountId: Long? = null,
    val isCustomAmount: Boolean = false,
    val presetDollars: Int? = null,
    val amountCents: Long = 0,
    val error: WithdrawalError? = null,
    val savedAmountCents: Long? = null,
    val isSaving: Boolean = false
)

/**
 * Retiro en cajero (Fase 8A): transferencia desde una cuenta de ahorros hacia Efectivo.
 * No es un gasto; el repositorio lo inserta en una sola transacción y valida el saldo allí.
 */
@HiltViewModel
class WithdrawalViewModel @Inject constructor(
    computeAccountBalances: ComputeAccountBalances,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val form = MutableStateFlow(WithdrawalFormState())

    val uiState: StateFlow<WithdrawalUiState> = combine(
        computeAccountBalances.observe(),
        form
    ) { balances, current ->
        val savings = balances.filter { it.account.type == AccountType.SAVINGS }
        val cash = CashAccountRules.pick(balances.map { it.account })
        val selectedId = current.accountId
            ?.takeIf { id -> savings.any { it.account.id == id } }
            ?: savings.firstOrNull()?.account?.id

        WithdrawalUiState(
            savingsAccounts = savings,
            cashAccount = cash,
            selectedAccountId = selectedId,
            isCustomAmount = current.isCustomAmount,
            presetDollars = current.presetDollars,
            amountCents = current.amountCents,
            error = current.error,
            savedAmountCents = current.savedAmountCents,
            isSaving = current.isSaving,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WithdrawalUiState())

    fun selectAccount(accountId: Long) {
        form.value = form.value.copy(accountId = accountId, error = null)
    }

    fun selectPreset(dollars: Int) {
        form.value = form.value.copy(
            isCustomAmount = false,
            presetDollars = dollars,
            amountCents = dollars * 100L,
            error = null
        )
    }

    fun selectOtherAmount() {
        form.value = form.value.copy(
            isCustomAmount = true,
            presetDollars = null,
            amountCents = 0L,
            error = null
        )
    }

    /** Entrada estilo cajero: solo dígitos, cada uno entra por la derecha. */
    fun onAmountChange(value: String) {
        val digits = value.filter(Char::isDigit).take(9)
        form.value = form.value.copy(
            isCustomAmount = true,
            presetDollars = null,
            amountCents = digits.toLongOrNull() ?: 0L,
            error = null
        )
    }

    fun save() {
        val state = uiState.value
        if (state.savedAmountCents != null) return
        val source = state.selectedAccount
        val error = WithdrawalRules.validate(
            amountCents = state.amountCents,
            sourceBalanceCents = source?.balanceCents,
            sourceName = source?.account?.name,
            hasCashAccount = state.cashAccount != null
        )
        if (error != null) {
            form.value = form.value.copy(error = error)
            return
        }

        val sourceId = source?.account?.id ?: return
        val amount = state.amountCents
        form.value = form.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            when (val result = financeRepository.withdrawToCash(sourceId, amount, LocalDate.now())) {
                is WithdrawalResult.Success ->
                    form.value = form.value.copy(
                        savedAmountCents = amount,
                        isSaving = false,
                        error = null
                    )
                is WithdrawalResult.Failure ->
                    form.value = form.value.copy(error = result.error, isSaving = false)
            }
        }
    }
}
