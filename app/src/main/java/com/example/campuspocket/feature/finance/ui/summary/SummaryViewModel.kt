package com.example.campuspocket.feature.finance.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.BudgetPace
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.MonthPace
import com.example.campuspocket.feature.finance.domain.PaymentDomain
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SummaryUiState(
    val availableCents: Long = 0,
    val totalBalanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val projectedBalanceCents: Long = 0,
    val projectedSpentCents: Long = 0,
    val topCategories: List<GetMonthSummary.CategoryRow> = emptyList(),
    val donutSlices: List<Pair<Int, Long>> = emptyList(),
    val upcomingPayments: List<ScheduledPaymentEntity> = emptyList(),
    val pace: MonthPace? = null,
    /** Gasto acumulado por día del mes (índice 0 = día 1), para el gráfico v5. */
    val dailySpentCents: List<Long> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Remanente del presupuesto total; null si no hay presupuesto mensual. */
    val monthlyRemainingCents: Long?
        get() = pace?.let { it.budgetCents - it.spentCents }
}

/** Resumen del mes actual + proyección con pagos pendientes (spec 7.3 y 5B). */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    computeAccountBalances: ComputeAccountBalances,
    financeRepository: FinanceRepository
) : ViewModel() {

    /** Emite la fecha actual y se vuelve a emitir al cambiar de día (revisa cada minuto). */
    private val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SummaryUiState> = combine(
        computeAccountBalances.observe(),
        financeRepository.observeActiveCategories(),
        financeRepository.observeTransactions(),
        financeRepository.observeAllBudgets(),
        financeRepository.observeAllScheduledPayments(),
        financeRepository.observeMonthlyBudgets(),
        todayFlow
    ) { values ->
        val balances = values[0] as List<com.example.campuspocket.feature.finance.domain.AccountBalance>
        val categories = values[1] as List<com.example.campuspocket.feature.finance.domain.model.Category>
        val transactions = values[2] as List<com.example.campuspocket.feature.finance.domain.model.Transaction>
        val budgets = values[3] as List<com.example.campuspocket.feature.finance.data.BudgetEntity>
        val payments = values[4] as List<ScheduledPaymentEntity>
        val monthlyBudgets = values[5] as List<com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity>
        val today = values[6] as LocalDate

        val month = YearMonth.from(today)

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

        val monthlyBudgetCents = monthlyBudgets
            .firstOrNull { it.yearMonth == month.toString() }
            ?.amountCents
        val pace = BudgetPace.compute(
            today = today,
            budgetCents = monthlyBudgetCents,
            spentCents = summary.totalSpentCents
        )

        // Serie acumulada para el gráfico: mismo criterio que GetMonthSummary
        // (gastos menos reembolsos, sin transferencias) y solo hasta hoy.
        val spentByDay = LongArray(month.lengthOfMonth())
        transactions.forEach { tx ->
            if (tx.date >= month.atDay(1) && tx.date <= today &&
                (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.REFUND)
            ) {
                val delta = if (tx.type == TransactionType.EXPENSE) tx.amountCents else -tx.amountCents
                spentByDay[tx.date.dayOfMonth - 1] += delta
            }
        }
        var running = 0L
        val dailySpent = spentByDay.map { day ->
            running += day
            running
        }
        val incomeCents = transactions
            .filter {
                it.type == com.example.campuspocket.feature.finance.data.TransactionType.INCOME &&
                    it.date >= month.atDay(1) && it.date <= month.atEndOfMonth()
            }
            .sumOf { it.amountCents }

        SummaryUiState(
            availableCents = summary.availableCents,
            totalBalanceCents = totalBalance,
            incomeCents = incomeCents,
            expenseCents = summary.totalSpentCents,
            projectedBalanceCents = PaymentDomain.projectedBalance(totalBalance, activePayments, month.atEndOfMonth()),
            projectedSpentCents = PaymentDomain.projectedSpent(summary.totalSpentCents, activePayments, month),
            topCategories = summary.rows.filter { it.netSpentCents > 0 }.take(5),
            donutSlices = summary.rows.filter { it.netSpentCents > 0 }.take(5)
                .map { it.colorArgb to it.netSpentCents },
            upcomingPayments = PaymentDomain.upcomingPayments(payments, today),
            pace = pace,
            dailySpentCents = dailySpent,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SummaryUiState())
}
