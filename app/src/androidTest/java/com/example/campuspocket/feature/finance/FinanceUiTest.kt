@file:SuppressLint("ViewModelConstructorInComposable")

package com.example.campuspocket.feature.finance

import android.annotation.SuppressLint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.util.Money
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.domain.repository.FinanceRepository
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import com.example.campuspocket.feature.finance.ui.accounts.AccountsScreen
import com.example.campuspocket.feature.finance.ui.accounts.AccountsViewModel
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormScreen
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI (Compose, dispositivo conectado): crear un gasto y que el saldo de la cuenta cambie.
 * Pantallas con sus ViewModels reales sobre un repositorio falso en memoria (sin Hilt).
 */
class FinanceUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val account = Account(id = 1, name = "Efectivo", type = AccountType.CASH, initialBalanceCents = 10_000)
    private val category = Category(id = 1, name = "Comida", kind = com.example.campuspocket.feature.finance.data.CategoryKind.EXPENSE)

    @Test
    fun crearGastoGuardaEnElRepositorio() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        var done = false

        composeRule.setContent {
            CampusTheme {
                TransactionFormScreen(
                    onDone = { done = true },
                    viewModel = TransactionFormViewModel(
                        SavedStateHandle(
                            mapOf(
                                FinanceDestinations.TRANSACTION_TYPE_ARG to TransactionType.EXPENSE.name,
                                FinanceDestinations.TRANSACTION_ID_ARG to -1L
                            )
                        ),
                        repo
                    )
                )
            }
        }

        // Monto estilo cajero: se reemplaza el "0,00" inicial por dígitos puros
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("450")
        // Cuenta
        composeRule.onNodeWithText("Cuenta").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Efectivo").performClick()
        // Categoría
        composeRule.onNodeWithText("Categoría").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Comida").performClick()
        // Guardar
        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.waitUntil(5_000) { done }
        val saved = repo.insertedTransactions.single()
        assertEquals(450L, saved.amountCents)
        assertEquals(1L, saved.accountId)
        assertEquals(TransactionType.EXPENSE, saved.type)
    }

    @Test
    fun laCuentaReflejaElGastoEnSuSaldo() {
        val gasto = Transaction(
            id = 1, type = TransactionType.EXPENSE, amountCents = 450,
            date = LocalDate.of(2026, 10, 4), accountId = 1, categoryId = 1
        )
        val repo = FakeFinanceRepository(listOf(account), listOf(category), listOf(gasto))

        composeRule.setContent {
            CampusTheme {
                AccountsScreen(
                    onOpenAccount = {},
                    onAddAccount = {},
                    onManageCategories = {},
                    viewModel = AccountsViewModel(ComputeAccountBalances(repo))
                )
            }
        }

        // Saldo: 100.00 - 4.50 = 95.50 (texto esperado tal como lo formatea Money)
        val expected = Money.format(9_550L)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText(expected)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("Efectivo")).fetchSemanticsNodes()
            .also { assertTrue(it.isNotEmpty()) }
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    // ---- Fake mínimo local (lectura/escritura en memoria) -------------------

    private class FakeFinanceRepository(
        initialAccounts: List<Account>,
        initialCategories: List<Category>,
        initialTransactions: List<Transaction> = emptyList()
    ) : FinanceRepository {

        private var nextTxId = 0L
        val accountsFlow = MutableStateFlow(initialAccounts)
        val categoriesFlow = MutableStateFlow(initialCategories)
        val transactionsFlow = MutableStateFlow(initialTransactions)
        val insertedTransactions = mutableListOf<Transaction>()

        override fun observeActiveAccounts(): Flow<List<Account>> = accountsFlow
        override fun observeAccount(accountId: Long): Flow<Account?> =
            accountsFlow.map { list -> list.firstOrNull { it.id == accountId } }
        override suspend fun getAccount(accountId: Long): Account? =
            accountsFlow.value.firstOrNull { it.id == accountId }
        override suspend fun insertAccount(account: Account): Long = 0L
        override suspend fun updateAccount(account: Account) = Unit
        override suspend fun archiveAccount(accountId: Long) = Unit

        override fun observeActiveCategories(): Flow<List<Category>> = categoriesFlow
        override suspend fun getCategory(categoryId: Long): Category? =
            categoriesFlow.value.firstOrNull { it.id == categoryId }
        override suspend fun insertCategory(category: Category): Long = 0L
        override suspend fun updateCategory(category: Category) = Unit
        override suspend fun archiveCategory(categoryId: Long) = Unit

        override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow
        override fun observeTransactionsByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
            transactionsFlow
        override fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
            transactionsFlow.map { list -> list.filter { it.accountId == accountId || it.targetAccountId == accountId } }
        override suspend fun getTransaction(id: Long): Transaction? =
            transactionsFlow.value.firstOrNull { it.id == id }
        override suspend fun insertTransaction(transaction: Transaction): Long {
            insertedTransactions += transaction
            val id = ++nextTxId
            transactionsFlow.value = transactionsFlow.value + transaction.copy(id = id)
            return id
        }
        override suspend fun updateTransaction(transaction: Transaction) = Unit
        override suspend fun deleteTransaction(transactionId: Long) = Unit

        override fun observeAccountDeltas() = transactionsFlow.map { list ->
            list.groupBy { it.accountId }.map { (accountId, txs) ->
                com.example.campuspocket.feature.finance.data.TransactionDao.AccountDelta(
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
        override fun observeTransferInDeltas() = transactionsFlow.map { list ->
            list.filter { it.type == TransactionType.TRANSFER && it.targetAccountId != null }
                .groupBy { it.targetAccountId!! }
                .map { (target, txs) ->
                    com.example.campuspocket.feature.finance.data.TransactionDao.AccountDelta(
                        target, txs.sumOf { it.amountCents }
                    )
                }
        }
        override fun observeNetSpentByCategory(
            start: LocalDate,
            end: LocalDate
        ): Flow<List<com.example.campuspocket.feature.finance.data.TransactionDao.CategorySpent>> =
            transactionsFlow.map { emptyList() }

        override fun observeAllBudgets() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.campuspocket.feature.finance.data.BudgetEntity>())
        override fun observeBudgetsForMonth(yearMonth: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.campuspocket.feature.finance.data.BudgetEntity>())
        override suspend fun getBudgetsForMonth(yearMonth: String) = emptyList<com.example.campuspocket.feature.finance.data.BudgetEntity>()
        override suspend fun getBudget(yearMonth: String, categoryId: Long) = null
        override suspend fun upsertBudget(yearMonth: String, categoryId: Long, amountCents: Long) = Unit
        override suspend fun deleteBudget(yearMonth: String, categoryId: Long) = Unit
        // ---- Pagos programados (Fase 5B) ----
        override fun observeActiveScheduledPayments() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>())
        override fun observeAllScheduledPayments() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>())
        override suspend fun getScheduledPayment(id: Long) = null
        override suspend fun getPaymentsDueUntil(date: LocalDate) = emptyList<com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity>()
        override suspend fun insertScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) = 0L
        override suspend fun updateScheduledPayment(payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity) = Unit
        override suspend fun deleteScheduledPayment(id: Long) = Unit
        override suspend fun markScheduledPaymentPaid(
            payment: com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity,
            amountCents: Long,
            date: LocalDate
        ) = Unit
    }
}
