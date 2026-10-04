package com.example.campuspocket.feature.finance.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState
    data object NotFound : AccountDetailUiState
    data class Content(val account: Account, val transactions: List<Transaction>) : AccountDetailUiState
}

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val accountId: Long =
        savedStateHandle.get<Long>(FinanceDestinations.ACCOUNT_ID_ARG) ?: -1L

    val uiState: StateFlow<AccountDetailUiState> = combine(
        financeRepository.observeAccount(accountId),
        financeRepository.observeTransactionsByAccount(accountId)
    ) { account, transactions ->
        when (account) {
            null -> AccountDetailUiState.NotFound
            else -> AccountDetailUiState.Content(account, transactions)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountDetailUiState.Loading)

    private val _archived = MutableStateFlow(false)
    val archived: StateFlow<Boolean> = _archived.asStateFlow()

    fun archive() {
        if (_archived.value) return
        viewModelScope.launch {
            financeRepository.archiveAccount(accountId)
            _archived.value = true
        }
    }
}
