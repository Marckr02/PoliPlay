package com.example.campuspocket.feature.finance.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.PaymentDomain
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SummaryUiState(
    val availableCents: Long = 0,
    val totalBalanceCents: Long = 0,
    val projectedBalanceCents: Long = 0,
    val projectedSpentCents: Long = 0,
    val topCategories: List<GetMonthSummary.CategoryRow> = emptyList(),
    val donutSlices: List<Pair<Int, Long>> = emptyList(),
    val upcomingPayments: List<ScheduledPaymentEntity> = emptyList(),
    val isLoading: Boolean = true
)

/** Resumen del mes actual + proyección con pagos pendientes (spec 7.3 y 5B). */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    computeAccountBalances: ComputeAccountBalances,
    financeRepository: FinanceRepository
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SummaryUiState> = combine(
        computeAccountBalances.observe(),
        financeRepository.observeActiveCategories(),
        financeRepository.observeTransactions(),
        financeRepository.observeAllBudgets(),
        financeRepository.observeAllScheduledPayments()
    ) { values ->
        val balances = values[0] as List<com.example.campuspocket.feature.finance.domain.AccountBalance>
        val categories = values[1] as List<com.example.campuspocket.feature.finance.domain.model.Category>
        val transactions = values[2] as List<com.example.campuspocket.feature.finance.domain.model.Transaction>
        val budgets = values[3] as List<com.example.campuspocket.feature.finance.data.BudgetEntity>
        val payments = values[4] as List<ScheduledPaymentEntity>

        val month = YearMonth.now()
        val today = LocalDate.now()

        val expenseCategories = categories
            .filter { it.kind == CategoryKind.EXPENSE }
            .mapNotNull { c -> c.id?.let { id -> id to c } }
            .toMap()
        val budgetMap = budgets
            .filter { it.yearMonth == month.toString() }
            .associate { it.categoryId to it.amountCents }

        val summary = GetMonthSummary.compute(
            yearMonth = month,
            transactions = transactions,
            budgets = budgetMap,
            categories = expenseCategories,
            monthStart = month.atDay(1),
            monthEnd = month.atEndOfMonth()
        )

        val activePayments = payments.filter { it.active }
        val totalBalance = balances.filter { it.account.includeInTotal }.sumOf { it.balanceCents }

        SummaryUiState(
            availableCents = summary.availableCents,
            totalBalanceCents = totalBalance,
            projectedBalanceCents = PaymentDomain.projectedBalance(totalBalance, activePayments, month.atEndOfMonth()),
            projectedSpentCents = PaymentDomain.projectedSpent(summary.totalSpentCents, activePayments, month),
            topCategories = summary.rows.filter { it.netSpentCents > 0 }.take(5),
            donutSlices = summary.rows.filter { it.netSpentCents > 0 }.take(5)
                .map { it.colorArgb to it.netSpentCents },
            upcomingPayments = PaymentDomain.upcomingPayments(payments, today),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SummaryUiState())
}
