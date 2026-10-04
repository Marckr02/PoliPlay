package com.example.campuspocket.feature.finance.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AccountsUiState(
    val balances: List<AccountBalance> = emptyList(),
    val totalCents: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    computeAccountBalances: ComputeAccountBalances
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = computeAccountBalances.observe()
        .map { balances ->
            AccountsUiState(
                balances = balances.sortedWith(
                    compareBy({ it.account.type.ordinal }, { it.account.sortOrder }, { it.account.name })
                ),
                totalCents = balances.filter { it.account.includeInTotal }.sumOf { it.balanceCents },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountsUiState())
}
