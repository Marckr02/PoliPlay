package com.example.campuspocket.feature.finance

import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Fake en memoria de finanzas para las pruebas JVM. Registra operaciones recibidas. */
class FakeFinanceRepository(
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    transactions: List<Transaction> = emptyList()
) : FinanceRepository {

    private val accountsFlow = MutableStateFlow(accounts)
    private val categoriesFlow = MutableStateFlow(categories)
    private val transactionsFlow = MutableStateFlow(transactions)

    var nextAccountId = (accounts.mapNotNull { it.id }.maxOrNull() ?: 0L)
    var nextTransactionId = (transactions.mapNotNull { it.id }.maxOrNull() ?: 0L)
    var nextCategoryId = (categories.mapNotNull { it.id }.maxOrNull() ?: 0L)

    val insertedAccounts = mutableListOf<Account>()
    val updatedAccounts = mutableListOf<Account>()
    val archivedAccounts = mutableListOf<Long>()
    val archivedCategories = mutableListOf<Long>()
    val insertedTransactions = mutableListOf<Transaction>()
    val updatedTransactions = mutableListOf<Transaction>()
    val deletedTransactions = mutableListOf<Long>()

    // ---- Cuentas ----
    override fun observeActiveAccounts(): Flow<List<Account>> =
        accountsFlow.map { list -> list.filter { !it.archived } }

    override fun observeAccount(accountId: Long): Flow<Account?> =
        accountsFlow.map { list -> list.firstOrNull { it.id == accountId } }

    override suspend fun getAccount(accountId: Long): Account? =
        accountsFlow.value.firstOrNull { it.id == accountId }

    override suspend fun insertAccount(account: Account): Long {
        insertedAccounts += account
        val id = ++nextAccountId
        accountsFlow.value = accountsFlow.value + account.copy(id = id)
        return id
    }

    override suspend fun updateAccount(account: Account) {
        updatedAccounts += account
        accountsFlow.value = accountsFlow.value.map { if (it.id == account.id) account else it }
    }

    override suspend fun archiveAccount(accountId: Long) {
        archivedAccounts += accountId
        accountsFlow.value = accountsFlow.value.map {
            if (it.id == accountId) it.copy(archived = true) else it
        }
    }

    // ---- Categorías ----
    override fun observeActiveCategories(): Flow<List<Category>> =
        categoriesFlow.map { list -> list.filter { !it.archived } }

    override suspend fun getCategory(categoryId: Long): Category? =
        categoriesFlow.value.firstOrNull { it.id == categoryId }

    override suspend fun insertCategory(category: Category): Long {
        val id = ++nextCategoryId
        categoriesFlow.value = categoriesFlow.value + category.copy(id = id)
        return id
    }

    override suspend fun updateCategory(category: Category) {
        categoriesFlow.value = categoriesFlow.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun archiveCategory(categoryId: Long) {
        archivedCategories += categoryId
        categoriesFlow.value = categoriesFlow.value.map {
            if (it.id == categoryId) it.copy(archived = true) else it
        }
    }

    // ---- Transacciones ----
    override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow

    override fun observeTransactionsByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        transactionsFlow.map { list -> list.filter { it.date >= start && it.date <= end } }

    override fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionsFlow.map { list ->
            list.filter { it.accountId == accountId || it.targetAccountId == accountId }
        }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionsFlow.value.firstOrNull { it.id == id }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        insertedTransactions += transaction
        val id = ++nextTransactionId
        transactionsFlow.value = transactionsFlow.value + transaction.copy(id = id)
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        updatedTransactions += transaction
        transactionsFlow.value = transactionsFlow.value.map {
            if (it.id == transaction.id) transaction else it
        }
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        deletedTransactions += transactionId
        transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transactionId }
    }

    // ---- Agregados: misma lógica que el SQL del DAO (spec 7.1) ----
    override fun observeAccountDeltas(): Flow<List<TransactionDao.AccountDelta>> =
        transactionsFlow.map { list ->
            list.groupBy { it.accountId }.map { (accountId, txs) ->
                TransactionDao.AccountDelta(
                    accountId,
                    txs.sumOf {
                        when (it.type) {
                            TransactionType.INCOME, TransactionType.REFUND -> it.amountCents
                            else -> -it.amountCents
                        }
                    }
                )
            }
        }

    override fun observeTransferInDeltas(): Flow<List<TransactionDao.AccountDelta>> =
        transactionsFlow.map { list ->
            list.filter { it.type == TransactionType.TRANSFER && it.targetAccountId != null }
                .groupBy { it.targetAccountId!! }
                .map { (target, txs) ->
                    TransactionDao.AccountDelta(target, txs.sumOf { it.amountCents })
                }
        }

    override fun observeNetSpentByCategory(start: LocalDate, end: LocalDate): Flow<List<TransactionDao.CategorySpent>> =
        transactionsFlow.map { list ->
            list.filter { it.date >= start && it.date <= end && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .map { (categoryId, txs) ->
                    TransactionDao.CategorySpent(
                        categoryId,
                        txs.sumOf {
                            when (it.type) {
                                TransactionType.EXPENSE -> it.amountCents
                                TransactionType.REFUND -> -it.amountCents
                                else -> 0L
                            }
                        }
                    )
                }
        }

    // ---- Pagos programados (Fase 5B) ----
    private val paymentsFlow = MutableStateFlow<List<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>>(emptyList())
    val paidTransactions = mutableListOf<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>()

    override fun observeActiveScheduledPayments() = paymentsFlow.map { list -> list.filter { it.active } }
    override fun observeAllScheduledPayments() = paymentsFlow
    override suspend fun getScheduledPayment(id: Long) = paymentsFlow.value.firstOrNull { it.id == id }
    override suspend fun getPaymentsDueUntil(date: LocalDate) = paymentsFlow.value.filter { it.active && it.nextDueDate <= date }
    override suspend fun insertScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity): Long {
        val newId = (paymentsFlow.value.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
        paymentsFlow.value = paymentsFlow.value + payment.copy(id = newId)
        return newId
    }
    override suspend fun updateScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) {
        paymentsFlow.value = paymentsFlow.value.map { if (it.id == payment.id) payment else it }
    }
    override suspend fun deleteScheduledPayment(id: Long) {
        paymentsFlow.value = paymentsFlow.value.filterNot { it.id == id }
    }

    override suspend fun markScheduledPaymentPaid(
        payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity,
        amountCents: Long,
        date: LocalDate
    ) {
        // mismo contrato atómico: si algo falla no cambia nada
        paidTransactions += payment
        val next = com.example.campuspocket.feature.finance.domain.PaymentDomain.nextDueAfter(payment, date)
        paymentsFlow.value = paymentsFlow.value.map {
            if (it.id == payment.id) it.copy(nextDueDate = next) else it
        }
    }

    // ---- Presupuestos (Fase 5A) ----
    private val budgetsFlow = MutableStateFlow<List<com.example.campuspocket.feature.finance.data.BudgetEntity>>(emptyList())

    override fun observeAllBudgets() = budgetsFlow
    override fun observeBudgetsForMonth(yearMonth: String) =
        budgetsFlow.map { list -> list.filter { it.yearMonth == yearMonth } }
    override suspend fun getBudgetsForMonth(yearMonth: String) =
        budgetsFlow.value.filter { it.yearMonth == yearMonth }
    override suspend fun getBudget(yearMonth: String, categoryId: Long) =
        budgetsFlow.value.firstOrNull { it.yearMonth == yearMonth && it.categoryId == categoryId }
    override suspend fun upsertBudget(yearMonth: String, categoryId: Long, amountCents: Long) {
        val existing = budgetsFlow.value.firstOrNull { it.yearMonth == yearMonth && it.categoryId == categoryId }
        if (existing != null) {
            budgetsFlow.value = budgetsFlow.value.map { if (it === existing) it.copy(amountCents = amountCents) else it }
        } else {
            val nextId = (budgetsFlow.value.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
            budgetsFlow.value = budgetsFlow.value + com.example.campuspocket.feature.finance.data.BudgetEntity(
                id = nextId, yearMonth = yearMonth, categoryId = categoryId, amountCents = amountCents
            )
        }
    }
    override suspend fun deleteBudget(yearMonth: String, categoryId: Long) {
        budgetsFlow.value = budgetsFlow.value.filterNot { it.yearMonth == yearMonth && it.categoryId == categoryId }
    }
}

fun testAccount(
    id: Long? = null,
    name: String = "Efectivo",
    type: com.example.campuspocket.feature.finance.data.AccountType =
        com.example.campuspocket.feature.finance.data.AccountType.CASH,
    initialBalanceCents: Long = 0,
    includeInTotal: Boolean = true,
    archived: Boolean = false
): Account = Account(
    id = id,
    name = name,
    type = type,
    initialBalanceCents = initialBalanceCents,
    includeInTotal = includeInTotal,
    archived = archived
)

fun testCategory(
    id: Long? = null,
    name: String = "Alimentación",
    kind: CategoryKind = CategoryKind.EXPENSE
): Category = Category(id = id, name = name, kind = kind)

fun testExpense(
    id: Long? = null,
    accountId: Long = 1,
    categoryId: Long? = 1,
    amountCents: Long = 1_000,
    description: String? = null,
    date: LocalDate = LocalDate.of(2026, 10, 1)
): Transaction = Transaction(
    id = id,
    type = TransactionType.EXPENSE,
    amountCents = amountCents,
    date = date,
    accountId = accountId,
    categoryId = categoryId,
    description = description
)

fun testIncome(
    id: Long? = null,
    accountId: Long = 1,
    categoryId: Long? = 2,
    amountCents: Long = 2_000,
    date: LocalDate = LocalDate.of(2026, 10, 1)
): Transaction = Transaction(
    id = id,
    type = TransactionType.INCOME,
    amountCents = amountCents,
    date = date,
    accountId = accountId,
    categoryId = categoryId
)

fun testTransfer(
    id: Long? = null,
    accountId: Long = 1,
    targetAccountId: Long = 2,
    amountCents: Long = 500,
    date: LocalDate = LocalDate.of(2026, 10, 1)
): Transaction = Transaction(
    id = id,
    type = TransactionType.TRANSFER,
    amountCents = amountCents,
    date = date,
    accountId = accountId,
    targetAccountId = targetAccountId
)

fun testRefund(
    id: Long? = null,
    accountId: Long = 1,
    categoryId: Long? = 1,
    amountCents: Long = 300,
    refundOfId: Long? = null,
    date: LocalDate = LocalDate.of(2026, 10, 1)
): Transaction = Transaction(
    id = id,
    type = TransactionType.REFUND,
    amountCents = amountCents,
    date = date,
    accountId = accountId,
    categoryId = categoryId,
    refundOfId = refundOfId
)
