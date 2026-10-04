package com.example.campuspocket.feature.finance.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.domain.PaymentDomain
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DayCellState(
    val date: LocalDate,
    val balanceCents: Long,
    val transactionIds: List<Long> = emptyList(),
    val paymentIds: List<Long> = emptyList()
)

data class CalendarUiState(
    val month: YearMonth,
    val days: List<DayCellState> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<CalendarUiState> = _month
        .flatMapLatest { month ->
            combine(
                financeRepository.observeActiveAccounts(),
                financeRepository.observeTransactions(),
                financeRepository.observeAllScheduledPayments()
            ) { accounts, transactions, payments ->
                val today = LocalDate.now()
                val dailyBalances = PaymentDomain.dailyBalances(
                    accounts = accounts,
                    transactions = transactions,
                    payments = payments,
                    month = month,
                    today = today
                )
                val firstDow = month.atDay(1).dayOfWeek.value // 1=lunes
                val padding = (firstDow - 1) % 7
                CalendarUiState(
                    month = month,
                    days = List(month.atEndOfMonth().dayOfMonth + padding) { i ->
                        val dayIndex = i - padding
                        if (dayIndex < 0) {
                            DayCellState(month.atDay(1).minusDays(padding - i.toLong()), 0)
                        } else {
                            val date = month.atDay(dayIndex + 1)
                            DayCellState(
                                date = date,
                                balanceCents = dailyBalances[date] ?: 0,
                                transactionIds = transactions.filter { it.date == date }.mapNotNull { it.id },
                                paymentIds = payments
                                    .filter { it.active }
                                    .filter { PaymentDomain.paymentsDueOn(listOf(it), month, date).isNotEmpty() }
                                    .map { it.id }
                            )
                        }
                    },
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalendarUiState(YearMonth.now()))

    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
}
