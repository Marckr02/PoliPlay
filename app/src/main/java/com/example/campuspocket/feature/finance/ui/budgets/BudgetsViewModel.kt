package com.example.campuspocket.feature.finance.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.domain.BudgetPace
import com.example.campuspocket.feature.finance.domain.GetMonthSummary
import com.example.campuspocket.feature.finance.domain.MonthPace
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
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class BudgetsUiState(
    val yearMonth: YearMonth,
    val rows: List<GetMonthSummary.CategoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val semesterMode: Boolean = false,
    /** Presupuesto total independiente del mes; null si no está definido. */
    val monthlyBudgetCents: Long? = null,
    /** Ritmo del presupuesto total; null si no hay presupuesto o el mes es futuro. */
    val pace: MonthPace? = null
) {
    val totalBudgetCents: Long get() = rows.sumOf { it.budgetCents ?: 0L }
    val totalSpentCents: Long get() = rows.sumOf { it.netSpentCents }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.from(LocalDate.now(clock)))
    val yearMonth: StateFlow<YearMonth> = _yearMonth

    private val _semesterMode = MutableStateFlow(false)
    val semesterMode: StateFlow<Boolean> = _semesterMode

    val uiState: StateFlow<BudgetsUiState> = _yearMonth
        .flatMapLatest { month ->
            combine(
                financeRepository.observeActiveCategories(),
                financeRepository.observeTransactions(),
                financeRepository.observeAllBudgets(),
                _semesterMode,
                financeRepository.observeMonthlyBudgets()
            ) { categories, transactions, allBudgets, semester, monthlyBudgets ->
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
                    val monthlyBudget = monthlyBudgets
                        .firstOrNull { it.yearMonth == month.toString() }
                        ?.amountCents
                    val today = LocalDate.now(clock)
                    val currentMonth = YearMonth.from(today)
                    val paceDate = when {
                        month == currentMonth -> today
                        month < currentMonth -> month.atEndOfMonth()
                        else -> null
                    }
                    val pace = paceDate?.let {
                        BudgetPace.compute(
                            today = it,
                            budgetCents = monthlyBudget,
                            spentCents = summary.totalSpentCents
                        )
                    }
                    BudgetsUiState(
                        yearMonth = month,
                        rows = summary.rows,
                        isLoading = false,
                        semesterMode = false,
                        monthlyBudgetCents = monthlyBudget,
                        pace = pace
                    )
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
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            BudgetsUiState(YearMonth.from(LocalDate.now(clock)))
        )

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

    /** Presupuesto total del mes (independiente de las categorías, sin arrastre). */
    fun saveMonthlyBudget(amountCents: Long) {
        if (amountCents <= 0L) return
        viewModelScope.launch {
            financeRepository.upsertMonthlyBudget(_yearMonth.value.toString(), amountCents)
        }
    }

    fun deleteMonthlyBudget() {
        viewModelScope.launch {
            financeRepository.deleteMonthlyBudget(_yearMonth.value.toString())
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
