package com.example.campuspocket.feature.finance.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.finance.data.TransactionSubtype
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
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
import javax.inject.Inject

/** Filtros del historial (Fase 4). Todo vive en el estado y se reaplica sobre los Flow. */
data class HistoryFilters(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val type: TransactionType? = null,
    val subtype: TransactionSubtype? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val query: String = ""
)

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: Map<Long, Account> = emptyMap(),
    val categories: Map<Long, Category> = emptyMap(),
    val filters: HistoryFilters = HistoryFilters(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val filters = MutableStateFlow(HistoryFilters())

    /**
     * Ventana de carga (v5): meses hacia atrás que se consultan además del mes
     * actual. Evita traer todo el historial en cada emisión.
     */
    private val loadedMonths = MutableStateFlow(1)

    private val transactionsFlow = combine(filters, loadedMonths) { f, months ->
        val today = LocalDate.now()
        val end = f.endDate ?: today.withDayOfMonth(today.lengthOfMonth())
        val start = f.startDate
            ?: today.withDayOfMonth(1).minusMonths((months - 1).toLong())
        start to end
    }.flatMapLatest { (start, end) ->
        financeRepository.observeTransactionsByDateRange(start, end)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionsFlow,
        financeRepository.observeActiveAccounts(),
        financeRepository.observeActiveCategories(),
        filters
    ) { transactions, accounts, categories, f ->
        HistoryUiState(
            transactions = applyFilters(transactions, f),
            accounts = accounts.mapNotNull { a -> a.id?.let { it to a } }.toMap(),
            categories = categories.mapNotNull { c -> c.id?.let { it to c } }.toMap(),
            filters = f,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        filters.value = filters.value.copy(startDate = start, endDate = end)
    }

    /** Amplía la ventana de consulta un mes hacia atrás (v5). */
    fun loadPreviousMonth() {
        loadedMonths.value += 1
    }

    fun setType(type: TransactionType?) {
        filters.value = filters.value.copy(type = type)
    }

    fun setSubtype(subtype: TransactionSubtype?) {
        filters.value = filters.value.copy(subtype = subtype)
    }

    fun setAccount(accountId: Long?) {
        filters.value = filters.value.copy(accountId = accountId)
    }

    fun setCategory(categoryId: Long?) {
        filters.value = filters.value.copy(categoryId = categoryId)
    }

    fun setQuery(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    companion object {
        /** Filtrado puro (probado en JVM). */
        fun applyFilters(transactions: List<Transaction>, f: HistoryFilters): List<Transaction> =
            transactions
                .filter { f.type == null || it.type == f.type }
                .filter { f.subtype == null || it.subtype == f.subtype }
                .filter {
                    f.accountId == null || it.accountId == f.accountId || it.targetAccountId == f.accountId
                }
                .filter { f.categoryId == null || it.categoryId == f.categoryId }
                .filter {
                    f.query.isBlank() ||
                        it.description?.contains(f.query.trim(), ignoreCase = true) == true
                }
                .sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.id ?: 0L })
    }
}
