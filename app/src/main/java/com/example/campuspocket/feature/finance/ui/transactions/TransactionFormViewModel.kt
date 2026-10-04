package com.example.campuspocket.feature.finance.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.FinanceValidators
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
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
import java.time.LocalDate
import javax.inject.Inject

data class TransactionFormUiState(
    val transactionId: Long? = null,
    val type: TransactionType,
    /** Monto acumulado en centavos (entrada estilo cajero: solo dígitos). */
    val amountCents: Long = 0,
    val accountId: Long? = null,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val refundOfId: Long? = null,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val error: TransactionError? = null,
    val isLoaded: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val notFound: Boolean = false
) {
    val isEditing: Boolean get() = transactionId != null

    /** Texto visible del monto: 0 -> "0,00"; el usuario nunca escribe separadores. */
    val amountDisplay: String get() = Money.toLocalDecimalString(amountCents)
}

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val rawType: String =
        savedStateHandle.get<String>(FinanceDestinations.TRANSACTION_TYPE_ARG) ?: TransactionType.EXPENSE.name

    private val editingId: Long? =
        savedStateHandle.get<Long>(FinanceDestinations.TRANSACTION_ID_ARG)?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(type = runCatching { TransactionType.valueOf(rawType) }.getOrDefault(TransactionType.EXPENSE))
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = financeRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> = financeRepository.observeActiveCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Categorías válidas según el tipo: gasto/reembolso -> EXPENSE; ingreso -> INCOME. */
    val validCategories: StateFlow<List<Category>> = combine(categories, _uiState) { cats, state ->
        when (state.type) {
            TransactionType.EXPENSE, TransactionType.REFUND -> cats.filter { it.kind == CategoryKind.EXPENSE }
            TransactionType.INCOME -> cats.filter { it.kind == CategoryKind.INCOME }
            TransactionType.TRANSFER -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Gastos de la cuenta elegida, para vincular el reembolso (opcional). */
    val refundableExpenses: StateFlow<List<Transaction>> = combine(
        financeRepository.observeTransactions(), _uiState
    ) { transactions, state ->
        if (state.type != TransactionType.REFUND || state.accountId == null) emptyList()
        else transactions.filter {
            it.type == TransactionType.EXPENSE && it.accountId == state.accountId
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val tx = financeRepository.getTransaction(editingId)
                _uiState.value = if (tx == null) {
                    _uiState.value.copy(notFound = true, isLoaded = true)
                } else {
                    _uiState.value.copy(
                        transactionId = tx.id,
                        type = tx.type,
                        amountCents = tx.amountCents,
                        accountId = tx.accountId,
                        targetAccountId = tx.targetAccountId,
                        categoryId = tx.categoryId,
                        refundOfId = tx.refundOfId,
                        description = tx.description.orEmpty(),
                        date = tx.date,
                        isLoaded = true
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    /**
     * Entrada estilo cajero: el usuario solo teclea dígitos; cada dígito entra por la
     * derecha del monto (5 -> 0,05; luego 4 -> 0,54). Letras y separadores se ignoran.
     * Tope: 9 dígitos en total (máximo 9.999.999,99).
     */
    fun onAmountChange(value: String) {
        val digits = value.filter(Char::isDigit).take(9)
        val cents = digits.toLongOrNull() ?: 0L
        _uiState.value = _uiState.value.copy(amountCents = cents, error = null)
    }

    fun onAccountChange(accountId: Long) {
        _uiState.value = _uiState.value.copy(accountId = accountId, refundOfId = null, error = null)
    }

    fun onTargetAccountChange(accountId: Long) {
        _uiState.value = _uiState.value.copy(targetAccountId = accountId, error = null)
    }

    fun onCategoryChange(categoryId: Long) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId, error = null)
    }

    fun onRefundOfChange(transactionId: Long?) {
        _uiState.value = _uiState.value.copy(refundOfId = transactionId)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun save() {
        val state = _uiState.value
        if (state.amountCents <= 0) {
            _uiState.value = state.copy(error = TransactionError.AMOUNT_NOT_POSITIVE)
            return
        }
        val candidate = Transaction(
            id = state.transactionId,
            type = state.type,
            amountCents = state.amountCents,
            date = state.date,
            accountId = state.accountId ?: 0L,
            targetAccountId = state.targetAccountId,
            categoryId = if (state.type == TransactionType.TRANSFER) null else state.categoryId,
            description = state.description.trim().ifEmpty { null },
            refundOfId = if (state.type == TransactionType.REFUND) state.refundOfId else null
        )
        viewModelScope.launch {
            val error = FinanceValidators.validate(candidate, categories.value)
            if (error != null) {
                _uiState.value = state.copy(error = error)
                return@launch
            }
            if (state.isEditing) financeRepository.updateTransaction(candidate)
            else financeRepository.insertTransaction(candidate)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun delete() {
        val id = _uiState.value.transactionId ?: return
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }
}
