package com.example.campuspocket.feature.finance.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BudgetsUiState(
    val yearMonth: YearMonth,
    val rows: List<GetMonthSummary.CategoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val semesterMode: Boolean = false
) {
    val totalBudgetCents: Long get() = rows.sumOf { it.budgetCents ?: 0L }
    val totalSpentCents: Long get() = rows.sumOf { it.netSpentCents }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    val yearMonth: StateFlow<YearMonth> = _yearMonth

    private val _semesterMode = MutableStateFlow(false)
    val semesterMode: StateFlow<Boolean> = _semesterMode

    val uiState: StateFlow<BudgetsUiState> = _yearMonth
        .flatMapLatest { month ->
            combine(
                financeRepository.observeActiveCategories(),
                financeRepository.observeTransactions(),
                financeRepository.observeAllBudgets(),
                _semesterMode
            ) { categories, transactions, allBudgets, semester ->
                val expenseCategories = categories
                    .filter { it.kind == CategoryKind.EXPENSE }
                    .mapNotNull { c -> c.id?.let { it to c } }
                    .toMap()

                if (!semester) {
                    val bMap = allBudgets
                        .filter { it.yearMonth == month.toString() }
                        .associate { it.categoryId to it.amountCents }
                    val summary = GetMonthSummary.compute(
                        yearMonth = month,
                        transactions = transactions,
                        budgets = bMap,
                        categories = expenseCategories,
                        monthStart = month.atDay(1),
                        monthEnd = month.atEndOfMonth()
                    )
                    BudgetsUiState(month, summary.rows, isLoading = false, semesterMode = false)
                } else {
                    val months = GetMonthSummary.semesterMonths(month, null)
                    val monthKeys = months.map { it.toString() }.toSet()
                    val aggregated = mutableMapOf<Long, Long>()
                    allBudgets
                        .filter { it.yearMonth in monthKeys }
                        .forEach { aggregated[it.categoryId] = (aggregated[it.categoryId] ?: 0L) + it.amountCents }
                    val start = months.first().atDay(1)
                    val end = months.last().atEndOfMonth()
                    val summary = GetMonthSummary.compute(
                        yearMonth = month,
                        transactions = transactions.filter { it.date >= start && it.date <= end },
                        budgets = aggregated,
                        categories = expenseCategories,
                        monthStart = start,
                        monthEnd = end
                    )
                    BudgetsUiState(month, summary.rows, isLoading = false, semesterMode = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BudgetsUiState(YearMonth.now()))

    fun previousMonth() { _yearMonth.value = _yearMonth.value.minusMonths(1) }
    fun nextMonth() { _yearMonth.value = _yearMonth.value.plusMonths(1) }
    fun toggleSemesterMode() { _semesterMode.value = !_semesterMode.value }

    fun saveBudget(categoryId: Long, amountCents: Long) {
        viewModelScope.launch {
            financeRepository.upsertBudget(_yearMonth.value.toString(), categoryId, amountCents)
        }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch {
            financeRepository.deleteBudget(_yearMonth.value.toString(), categoryId)
        }
    }

    /** Copia del mes anterior solo las categorías sin presupuesto este mes (no pisa nada). */
    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val current = _yearMonth.value
            val previous = current.minusMonths(1)
            val existing = financeRepository.getBudgetsForMonth(current.toString()).map { it.categoryId }.toSet()
            financeRepository.getBudgetsForMonth(previous.toString())
                .filter { it.categoryId !in existing }
                .forEach { financeRepository.upsertBudget(current.toString(), it.categoryId, it.amountCents) }
        }
    }
}
