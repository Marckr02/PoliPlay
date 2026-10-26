package com.example.campuspocket.feature.finance.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.core.designsystem.ColorPalette
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionSubtype
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.AccountBalance
import com.example.campuspocket.feature.finance.domain.BudgetPace
import com.example.campuspocket.feature.finance.domain.CategoryHabits
import com.example.campuspocket.feature.finance.domain.CategorySuggestion
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.FinanceValidators
import com.example.campuspocket.feature.finance.domain.MonthPace
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.model.TransactionTemplate
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.inject.Inject

data class TransactionFormUiState(
    val transactionId: Long? = null,
    val type: TransactionType,
    /** Subtipo (p. ej. retiro en cajero). Se conserva al editar; no se elige aquí. */
    val subtype: TransactionSubtype? = null,
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
    val notFound: Boolean = false,
    /** La cuenta/categoría vienen del último movimiento (etiqueta «Última usada»). */
    val usesLastAccount: Boolean = false,
    val usesLastCategory: Boolean = false,
    /** Feedback de plantillas; el id evita repetir el snackbar al recomponer. */
    val templateFeedback: TemplateFeedback? = null,
    val templateFeedbackId: Long = 0L,
    /** v6: muestra el campo de descripción (expandible con chip + Nota). */
    val showDescription: Boolean = false,
    /** v6: modo "Seguir añadiendo" - limpia monto pero conserva cuenta/categoría/fecha. */
    val keepAdding: Boolean = false,
    /** Contador de guardados en modo "Seguir añadiendo" para disparar snackbar. */
    val savedCount: Int = 0,
    /** ID de la última transacción insertada (para deshacer en modo "Seguir añadiendo"). */
    val lastInsertedTransactionId: Long? = null
) {
    val isEditing: Boolean get() = transactionId != null

    /** Texto visible del monto: 0 -> "0,00"; el usuario nunca escribe separadores. */
    val amountDisplay: String get() = Money.toLocalDecimalString(amountCents)
}

enum class TemplateFeedback { SAVED, DELETED }

/** Datos para la línea de impacto bajo el monto (v6). */
data class TransactionImpact(
    /** Ritmo del mes con el monto en curso; null si no hay presupuesto o no aplica. */
    val pace: MonthPace? = null,
    /** Cuenta elegida y su saldo después del movimiento (ingresos y transferencias). */
    val accountName: String? = null,
    val accountAfterCents: Long? = null
)

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository,
    computeAccountBalances: ComputeAccountBalances,
    private val clock: Clock
) : ViewModel() {

    private val rawType: String =
        savedStateHandle.get<String>(FinanceDestinations.TRANSACTION_TYPE_ARG) ?: TransactionType.EXPENSE.name

    private val editingId: Long? =
        savedStateHandle.get<Long>(FinanceDestinations.TRANSACTION_ID_ARG)?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(
            type = runCatching { TransactionType.valueOf(rawType) }
                .getOrDefault(TransactionType.EXPENSE),
            date = LocalDate.now(clock)
        )
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()
    private var isSaving = false

    val accounts: StateFlow<List<Account>> = financeRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> = financeRepository.observeActiveCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Cuentas con su saldo real (para los chips y la línea de impacto). */
    val accountBalances: StateFlow<List<AccountBalance>> = computeAccountBalances.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val transactions: Flow<List<Transaction>> = financeRepository.observeTransactions()

    /** Categorías frecuentes, recientes y sugeridas según la franja horaria. */
    val categoryHabits: StateFlow<CategoryHabits> = combine(transactions, _uiState) { txs, state ->
        CategorySuggestion.analyze(txs, state.type, ZonedDateTime.now(clock))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CategoryHabits())

    /** Descripciones anteriores que coinciden con la nota escrita (v6). */
    val descriptionSuggestions: StateFlow<List<Transaction>> = combine(transactions, _uiState) { txs, state ->
        val query = state.description.trim()
        if (query.isEmpty() || state.type == TransactionType.TRANSFER) {
            emptyList()
        } else {
            txs.asSequence()
                .filter { it.type == state.type }
                .filter { tx ->
                    val description = tx.description?.trim().orEmpty()
                    description.isNotEmpty() &&
                        description.contains(query, ignoreCase = true) &&
                        !description.equals(query, ignoreCase = true)
                }
                .distinctBy { it.description!!.trim().lowercase() }
                .sortedByDescending { it.createdAt }
                .take(4)
                .toList()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Línea de impacto: ritmo del presupuesto total con el monto en curso
     * (solo gastos) o saldo de la cuenta tras el movimiento.
     */
    val monthlyImpact: StateFlow<TransactionImpact> = combine(
        transactions,
        financeRepository.observeMonthlyBudgets(),
        accountBalances,
        _uiState
    ) { txs, monthlyBudgets, balances, state ->
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val monthKey = month.toString()
        val monthSpent = txs
            .filter {
                it.id != state.transactionId &&
                    it.date >= month.atDay(1) && it.date <= month.atEndOfMonth() &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND)
            }
            .sumOf {
                if (it.type == TransactionType.EXPENSE) it.amountCents else -it.amountCents
            }
            .coerceAtLeast(0L)
        val monthlyBudget = monthlyBudgets
            .firstOrNull { it.yearMonth == monthKey }
            ?.amountCents
            ?.takeIf { it > 0L }

        // El gasto solo altera el ritmo si su fecha cae en el mes en curso; al editar,
        // el movimiento original ya se excluyó arriba para no contarlo dos veces.
        val appliesToCurrentMonth = YearMonth.from(state.date) == month
        val pace = when {
            state.type == TransactionType.EXPENSE && appliesToCurrentMonth ->
                BudgetPace.compute(today, monthlyBudget, monthSpent + state.amountCents)
            else -> null
        }

        // La vista previa del saldo solo aplica a movimientos nuevos: al editar, el
        // saldo actual ya incluye el movimiento original y la cifra sería ambigua.
        val selected = balances.firstOrNull { it.account.id == state.accountId }
        val after = selected
            ?.takeUnless { state.isEditing }
            ?.let { balance ->
                when (state.type) {
                    TransactionType.INCOME, TransactionType.REFUND ->
                        balance.balanceCents + state.amountCents
                    TransactionType.EXPENSE, TransactionType.TRANSFER ->
                        balance.balanceCents - state.amountCents
                }
            }
        TransactionImpact(
            pace = pace,
            accountName = selected?.account?.name,
            accountAfterCents = after
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionImpact())

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

    /** Plantillas recientes del mismo tipo; máximo 8 (v5 §4). */
    val templates: StateFlow<List<TransactionTemplate>> = combine(
        financeRepository.observeRecentTransactionTemplates(8), _uiState
    ) { list, state ->
        if (state.isEditing || state.type == TransactionType.TRANSFER) emptyList()
        else list.filter { it.type == state.type }
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
                        subtype = tx.subtype,
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
            viewModelScope.launch {
                // Captura rápida: cuenta del último movimiento y categoría sugerida
                // por la franja horaria; si no hay historial, la última usada (v6).
                val latest = runCatching { financeRepository.getLatestTransaction() }.getOrNull()
                val type = _uiState.value.type
                val accountId = latest?.accountId?.let { id ->
                    financeRepository.getAccount(id)?.takeIf { !it.archived }?.id
                }
                val expectedKind = when (type) {
                    TransactionType.INCOME -> CategoryKind.INCOME
                    TransactionType.EXPENSE, TransactionType.REFUND -> CategoryKind.EXPENSE
                    TransactionType.TRANSFER -> null
                }
                val history = if (expectedKind == null) {
                    CategoryHabits()
                } else {
                    CategorySuggestion.analyze(
                        transactions = runCatching { transactions.first() }.getOrDefault(emptyList()),
                        type = type,
                        now = ZonedDateTime.now(clock)
                    )
                }
                fun validCategory(category: Category?): Category? = category?.takeIf {
                    expectedKind != null && it.kind == expectedKind && !it.archived
                }
                val suggestedCategory = validCategory(
                    history.suggestedCategoryId?.let { id ->
                        runCatching { financeRepository.getCategory(id) }.getOrNull()
                    }
                )
                val latestCategory = validCategory(
                    latest?.categoryId?.let { id ->
                        runCatching { financeRepository.getCategory(id) }.getOrNull()
                    }
                )
                val category = suggestedCategory ?: latestCategory
                val current = _uiState.value
                _uiState.value = current.copy(
                    accountId = current.accountId ?: accountId,
                    categoryId = current.categoryId ?: category?.id,
                    usesLastAccount = current.accountId == null && accountId != null,
                    usesLastCategory = current.categoryId == null &&
                        suggestedCategory == null && latestCategory != null,
                    isLoaded = true
                )
            }
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
        _uiState.value = _uiState.value.copy(
            accountId = accountId,
            refundOfId = null,
            usesLastAccount = false,
            error = null
        )
    }

    fun onTargetAccountChange(accountId: Long) {
        _uiState.value = _uiState.value.copy(targetAccountId = accountId, error = null)
    }

    /** Cambia el tipo sin perder monto, cuenta, fecha ni nota. */
    fun onTypeChange(type: TransactionType) {
        val state = _uiState.value
        if (state.type == type) return
        val expectedKind = when (type) {
            TransactionType.INCOME -> CategoryKind.INCOME
            TransactionType.EXPENSE, TransactionType.REFUND -> CategoryKind.EXPENSE
            TransactionType.TRANSFER -> null
        }
        val categoryStillValid = state.categoryId?.takeIf { id ->
            expectedKind != null && categories.value.any { it.id == id && it.kind == expectedKind }
        }
        _uiState.value = state.copy(
            type = type,
            categoryId = if (type == TransactionType.TRANSFER) null else categoryStillValid,
            targetAccountId = if (type == TransactionType.TRANSFER) state.targetAccountId else null,
            refundOfId = if (type == TransactionType.REFUND) state.refundOfId else null,
            usesLastCategory = false,
            error = null
        )
    }

    /**
     * Crea una categoría desde la hoja sin salir del formulario. Si ya existe
     * una con ese nombre y tipo, la reutiliza (v6, hoja de categorías).
     */
    fun createCategory(name: String) {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return
        val type = _uiState.value.type
        if (type == TransactionType.TRANSFER) return
        val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        viewModelScope.launch {
            val existing = categories.value.firstOrNull {
                it.kind == kind && it.name.equals(clean, ignoreCase = true)
            }
            val id = existing?.id ?: financeRepository.insertCategory(
                Category(
                    name = clean,
                    kind = kind,
                    colorArgb = ColorPalette[categories.value.count { it.kind == kind }]
                )
            )
            _uiState.value = _uiState.value.copy(
                categoryId = id,
                usesLastCategory = false,
                error = null
            )
        }
    }

    fun onCategoryChange(categoryId: Long) {
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId,
            usesLastCategory = false,
            error = null
        )
    }

    /**
     * Aplica monto, cuenta y categoría de la plantilla; conserva el tipo actual.
     */
    fun applyTemplate(template: TransactionTemplate) {
        val state = _uiState.value
        if (state.type == TransactionType.TRANSFER || template.type != state.type) return
        val accountId = template.accountId
            ?.takeIf { id -> accounts.value.any { it.id == id } }
            ?: state.accountId
        val categoryId = template.categoryId
            ?.takeIf { id -> categories.value.any { it.id == id } }
            ?: state.categoryId
        _uiState.value = state.copy(
            amountCents = template.amountCents,
            accountId = accountId,
            categoryId = categoryId,
            refundOfId = null,
            usesLastAccount = false,
            usesLastCategory = false,
            error = null
        )
        template.id?.let { id ->
            viewModelScope.launch { financeRepository.touchTransactionTemplate(id) }
        }
    }

    /**
     * Guarda el movimiento editado como plantilla (v5 §4). No modifica ni
     * guarda el movimiento; solo crea la plantilla reutilizable.
     */
    fun saveAsTemplate(name: String) {
        val state = _uiState.value
        if (state.type == TransactionType.TRANSFER) return
        if (name.isBlank() || state.amountCents <= 0) return
        viewModelScope.launch {
            financeRepository.insertTransactionTemplate(
                TransactionTemplate(
                    name = name.trim().take(40),
                    amountCents = state.amountCents,
                    type = state.type,
                    accountId = state.accountId,
                    categoryId = state.categoryId,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(
                templateFeedback = TemplateFeedback.SAVED,
                templateFeedbackId = _uiState.value.templateFeedbackId + 1
            )
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        val id = template.id ?: return
        viewModelScope.launch {
            financeRepository.deleteTransactionTemplate(id)
            _uiState.value = _uiState.value.copy(
                templateFeedback = TemplateFeedback.DELETED,
                templateFeedbackId = _uiState.value.templateFeedbackId + 1
            )
        }
    }

    fun onRefundOfChange(transactionId: Long?) {
        _uiState.value = _uiState.value.copy(refundOfId = transactionId)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    /** Autocompleta nota, categoría y monto desde una descripción anterior (v6). */
    fun applyDescriptionSuggestion(suggestion: Transaction) {
        val state = _uiState.value
        val valid = validCategories.value
        val categoryId = suggestion.categoryId
            ?.takeIf { id -> valid.any { it.id == id } }
            ?: state.categoryId
        _uiState.value = state.copy(
            description = suggestion.description.orEmpty(),
            categoryId = categoryId,
            amountCents = suggestion.amountCents.takeIf { it > 0L } ?: state.amountCents,
            usesLastCategory = false,
            error = null
        )
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onToggleDescription() {
        _uiState.value = _uiState.value.copy(showDescription = !_uiState.value.showDescription)
    }

    fun onToggleKeepAdding(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(keepAdding = enabled)
    }

    fun save() {
        if (isSaving) return
        val state = _uiState.value
        if (state.saved || state.deleted) return
        if (state.amountCents <= 0) {
            _uiState.value = state.copy(error = TransactionError.AMOUNT_NOT_POSITIVE)
            return
        }
        isSaving = true
        val candidate = Transaction(
            id = state.transactionId,
            type = state.type,
            subtype = state.subtype,
            amountCents = state.amountCents,
            date = state.date,
            accountId = state.accountId ?: 0L,
            targetAccountId = state.targetAccountId,
            categoryId = if (state.type == TransactionType.TRANSFER) null else state.categoryId,
            description = state.description.trim().ifEmpty { null },
            refundOfId = if (state.type == TransactionType.REFUND) state.refundOfId else null
        )
        viewModelScope.launch {
            try {
                val error = FinanceValidators.validate(candidate, categories.value)
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = error)
                    return@launch
                }
                val insertedId = if (state.isEditing) {
                    financeRepository.updateTransaction(candidate)
                    state.transactionId
                } else {
                    financeRepository.insertTransaction(candidate)
                }

                // v6: modo "Seguir añadiendo" - limpia monto pero conserva cuenta/categoría/fecha
                if (state.keepAdding && !state.isEditing) {
                    _uiState.value = _uiState.value.copy(
                        amountCents = 0,
                        error = null,
                        savedCount = state.savedCount + 1,
                        lastInsertedTransactionId = insertedId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        saved = true,
                        lastInsertedTransactionId = insertedId
                    )
                }
            } finally {
                isSaving = false
            }
        }
    }

    fun delete() {
        val id = _uiState.value.transactionId ?: return
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }

    fun deleteLastTransaction() {
        val id = _uiState.value.lastInsertedTransactionId ?: return
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
            _uiState.value = _uiState.value.copy(lastInsertedTransactionId = null)
        }
    }
}
