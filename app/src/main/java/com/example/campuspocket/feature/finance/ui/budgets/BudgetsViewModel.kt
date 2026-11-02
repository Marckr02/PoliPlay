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
import kotlinx.coroutines.flow.first
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
    val pace: MonthPace? = null,
    /** true si el mes visible es el actual. */
    val isCurrentMonth: Boolean = true
) {
    val totalBudgetCents: Long get() = rows.sumOf { it.budgetCents ?: 0L }
    val totalSpentCents: Long get() = rows.sumOf { it.netSpentCents }
}

/** Resultado de copiar presupuestos de un mes anterior al mes visible. */
data class CopyBudgetsResult(
    /** Mes de donde se copió; null si no hay presupuestos en los últimos 12 meses. */
    val sourceMonth: YearMonth?,
    val categoriesCopied: Int,
    val totalCopied: Boolean,
    val targetMonth: YearMonth,
    val copiedCategoryIds: List<Long> = emptyList()
)

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
                        monthEnd = month.atEndOfMonth(),
                        includeEmptyCategories = true
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
                        pace = pace,
                        isCurrentMonth = month == currentMonth
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
    fun goToCurrentMonth() { _yearMonth.value = YearMonth.from(LocalDate.now(clock)) }
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

    fun restoreBudget(yearMonth: YearMonth, categoryId: Long, amountCents: Long) {
        viewModelScope.launch {
            financeRepository.upsertBudget(yearMonth.toString(), categoryId, amountCents)
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

    fun restoreMonthlyBudget(yearMonth: YearMonth, amountCents: Long) {
        viewModelScope.launch {
            financeRepository.upsertMonthlyBudget(yearMonth.toString(), amountCents)
        }
    }

    /**
     * Copia al mes visible los presupuestos del mes anterior más reciente que tenga datos
     * (hasta 12 meses atrás). No pisa nada: solo copia categorías de gasto activas que aún
     * no tienen presupuesto este mes y el presupuesto total si este mes no tiene.
     */
    suspend fun copyFromPreviousMonth(): CopyBudgetsResult {
        val current = _yearMonth.value
        val currentKey = current.toString()

        val categoryBudgetMonths = financeRepository.observeAllBudgets().first()
            .map { it.yearMonth }.toSet()
        val totalBudgetMonths = financeRepository.observeMonthlyBudgets().first()
            .map { it.yearMonth }.toSet()

        val source = generateSequence(current.minusMonths(1)) { it.minusMonths(1) }
            .take(12)
            .firstOrNull { it.toString() in categoryBudgetMonths || it.toString() in totalBudgetMonths }
            ?: return CopyBudgetsResult(
                sourceMonth = null,
                categoriesCopied = 0,
                totalCopied = false,
                targetMonth = current
            )
        val sourceKey = source.toString()

        val activeExpenseIds = financeRepository.observeActiveCategories().first()
            .filter { it.kind == CategoryKind.EXPENSE }
            .mapNotNull { it.id }
            .toSet()
        val alreadyDefined = financeRepository.getBudgetsForMonth(currentKey).map { it.categoryId }.toSet()
        val toCopy = financeRepository.getBudgetsForMonth(sourceKey)
            .filter { it.categoryId in activeExpenseIds && it.categoryId !in alreadyDefined }
        toCopy.forEach { financeRepository.upsertBudget(currentKey, it.categoryId, it.amountCents) }

        val sourceTotal = financeRepository.getMonthlyBudget(sourceKey)
        val currentTotal = financeRepository.getMonthlyBudget(currentKey)
        var totalCopied = false
        if (sourceTotal != null && currentTotal == null) {
            financeRepository.upsertMonthlyBudget(currentKey, sourceTotal.amountCents)
            totalCopied = true
        }
        return CopyBudgetsResult(
            sourceMonth = source,
            categoriesCopied = toCopy.size,
            totalCopied = totalCopied,
            targetMonth = current,
            copiedCategoryIds = toCopy.map { it.categoryId }
        )
    }

    /** Revierte exactamente lo que copió [copyFromPreviousMonth]. */
    suspend fun undoCopy(result: CopyBudgetsResult) {
        val key = result.targetMonth.toString()
        result.copiedCategoryIds.forEach { financeRepository.deleteBudget(key, it) }
        if (result.totalCopied) financeRepository.deleteMonthlyBudget(key)
    }
}
