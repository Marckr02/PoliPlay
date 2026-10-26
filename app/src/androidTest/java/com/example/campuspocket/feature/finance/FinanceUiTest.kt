@file:SuppressLint("ViewModelConstructorInComposable")

package com.example.campuspocket.feature.finance

import android.annotation.SuppressLint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.core.navigation.LocalSnackbarHost
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
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsScreen
import com.example.campuspocket.feature.finance.ui.budgets.BudgetsViewModel
import com.example.campuspocket.feature.finance.ui.summary.SummaryScreen
import com.example.campuspocket.feature.finance.ui.summary.SummaryViewModel
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormScreen
import com.example.campuspocket.feature.finance.ui.transactions.TransactionFormViewModel
import com.example.campuspocket.feature.finance.ui.withdrawal.WithdrawalScreen
import com.example.campuspocket.feature.finance.ui.withdrawal.WithdrawalViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

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
        val vm = formViewModel(repo)
        // Cuenta y categoría ya elegidas: este test se centra en el teclado y el guardado.
        vm.onAccountChange(1L)
        vm.onCategoryChange(1L)

        setFormContent(vm) { done = true }

        // Monto con el teclado en pantalla: 4,50 (450 centavos).
        // El teclado es de cajero: cada dígito entra por la derecha.
        listOf("cuatro", "cinco", "cero").forEach { key ->
            composeRule.onNodeWithContentDescription(key).performClick()
        }
        // Guardar
        composeRule.onNodeWithContentDescription("Guardar").performClick()

        composeRule.waitUntil(5_000) { done }
        val saved = repo.insertedTransactions.single()
        assertEquals(450L, saved.amountCents)
        assertEquals(1L, saved.accountId)
        assertEquals(TransactionType.EXPENSE, saved.type)
    }

    @Test
    fun elegirCategoriaConUnToqueLaSelecciona() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        val vm = formViewModel(repo)
        setFormContent(vm)

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Comida")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Comida").performClick()

        assertEquals(1L, vm.uiState.value.categoryId)
    }

    @Test
    fun elegirAyerActualizaLaFecha() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        val vm = formViewModel(repo)
        setFormContent(vm)

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Ayer")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ayer").performClick()

        assertEquals(LocalDate.now().minusDays(1), vm.uiState.value.date)
    }

    @Test
    fun laLineaDeImpactoApareceConPresupuesto() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        repo.monthlyBudgetsFlow.value = listOf(
            com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity(
                yearMonth = YearMonth.now().toString(),
                amountCents = 100_000
            )
        )
        val vm = formViewModel(repo)
        setFormContent(vm)

        listOf("uno", "cero").forEach { key ->
            composeRule.onNodeWithContentDescription(key).performClick()
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText("Tras este gasto", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sinPresupuestoNoHayLineaDeImpacto() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        val vm = formViewModel(repo)
        setFormContent(vm)

        listOf("uno", "cero").forEach { key ->
            composeRule.onNodeWithContentDescription(key).performClick()
        }

        assertTrue(
            composeRule.onAllNodes(
                hasText("Tras este gasto", substring = true)
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun seguirAnadiendoConservaCategoriaYCuenta() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        val vm = formViewModel(repo)
        vm.onAccountChange(1L)
        vm.onCategoryChange(1L)
        setFormContent(vm)

        composeRule.onNodeWithText("Seguir añadiendo").performClick()
        listOf("dos", "cero", "cero").forEach { key ->
            composeRule.onNodeWithContentDescription(key).performClick()
        }
        composeRule.onNodeWithContentDescription("Guardar").performClick()

        composeRule.waitUntil(5_000) { repo.insertedTransactions.size == 1 }
        composeRule.waitUntil(5_000) { vm.uiState.value.savedCount == 1 }
        assertEquals(200L, repo.insertedTransactions.single().amountCents)
        assertEquals(0L, vm.uiState.value.amountCents)
        assertEquals(1L, vm.uiState.value.categoryId)
        assertEquals(1L, vm.uiState.value.accountId)
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
        // El saldo aparece en la fila y en el total: basta con que al menos uno se vea.
        composeRule.onAllNodes(hasText(expected))[0].assertIsDisplayed()
    }

    @Test
    fun definirPresupuestoTotalDelMesLoGuarda() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))

        composeRule.setContent {
            CampusTheme {
                BudgetsScreen(viewModel = BudgetsViewModel(repo))
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Presupuesto total del mes"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Presupuesto total del mes").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("250000")
        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.waitUntil(5_000) {
            repo.monthlyBudgetsFlow.value.singleOrNull()?.amountCents == 250_000L
        }
    }

    @Test
    fun presupuestosMuestraElRitmoDelMes() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        repo.monthlyBudgetsFlow.value = listOf(
            com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity(
                yearMonth = YearMonth.now().toString(),
                amountCents = 100_000
            )
        )
        repo.transactionsFlow.value = listOf(
            Transaction(
                id = 99,
                type = TransactionType.EXPENSE,
                amountCents = 25_000,
                date = LocalDate.now(),
                accountId = 1,
                categoryId = 1
            )
        )

        composeRule.setContent {
            CampusTheme {
                BudgetsScreen(viewModel = BudgetsViewModel(repo))
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Esperado a hoy")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Esperado a hoy").assertIsDisplayed()
    }

    @Test
    fun retiroEnCajeroConChipGuardaLaTransferencia() {
        val cash = Account(id = 1, name = "Efectivo", type = AccountType.CASH, initialBalanceCents = 0)
        val savings = Account(id = 2, name = "Ahorros", type = AccountType.SAVINGS, initialBalanceCents = 20_000)
        val repo = FakeFinanceRepository(listOf(cash, savings), emptyList())
        var done = false

        composeRule.setContent {
            CampusTheme {
                WithdrawalScreen(
                    onDone = { done = true },
                    onCreateAccount = {},
                    viewModel = WithdrawalViewModel(ComputeAccountBalances(repo), repo)
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Ahorros")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("$10").performClick()
        composeRule.onNodeWithText("Confirmar retiro").performClick()

        composeRule.waitUntil(5_000) { repo.withdrawals.size == 1 }
        assertEquals(2L to 1_000L, repo.withdrawals.single())
        composeRule.onNodeWithText("Retiro registrado").assertIsDisplayed()

        composeRule.onNodeWithText("Aceptar").performClick()
        composeRule.waitUntil(5_000) { done }
    }

    @Test
    fun retiroConOtroMontoInvalidoMuestraErrorYNoGuarda() {
        val cash = Account(id = 1, name = "Efectivo", type = AccountType.CASH, initialBalanceCents = 0)
        val savings = Account(id = 2, name = "Ahorros", type = AccountType.SAVINGS, initialBalanceCents = 20_000)
        val repo = FakeFinanceRepository(listOf(cash, savings), emptyList())

        composeRule.setContent {
            CampusTheme {
                WithdrawalScreen(
                    onDone = {},
                    onCreateAccount = {},
                    viewModel = WithdrawalViewModel(ComputeAccountBalances(repo), repo)
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Ahorros")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Otro").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("1200")
        composeRule.onNodeWithText("Confirmar retiro").performClick()
        composeRule.onNodeWithText("El monto debe ser múltiplo de 5").assertIsDisplayed()
        assertTrue(repo.withdrawals.isEmpty())

        // Corrige a $25 y confirma.
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("2500")
        composeRule.onNodeWithText("Confirmar retiro").performClick()
        composeRule.waitUntil(5_000) { repo.withdrawals.singleOrNull() == (2L to 2_500L) }
    }

    @Test
    fun elRitmoDelMesCambiaAlRegistrarUnGasto() {
        val repo = FakeFinanceRepository(listOf(account), listOf(category))
        repo.monthlyBudgetsFlow.value = listOf(
            com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity(
                yearMonth = YearMonth.now().toString(),
                amountCents = 100_000
            )
        )

        composeRule.setContent {
            CampusTheme {
                SummaryScreen(
                    viewModel = SummaryViewModel(ComputeAccountBalances(repo), repo)
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Por debajo del ritmo")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("Puedes gastar", substring = true))
            .fetchSemanticsNodes()
            .also { assertTrue(it.isNotEmpty()) }

        // El gráfico nuevo antecede al hero del ritmo; desplázalo a la vista
        // antes de comprobar el cambio de estado.
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Por debajo del ritmo"))

        repo.transactionsFlow.value = repo.transactionsFlow.value + Transaction(
            id = 99,
            type = TransactionType.EXPENSE,
            amountCents = 150_000,
            date = LocalDate.now(),
            accountId = 1,
            categoryId = 1
        )

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText("Por encima del ritmo")
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun formViewModel(
        repo: FakeFinanceRepository,
        type: TransactionType = TransactionType.EXPENSE
    ): TransactionFormViewModel = TransactionFormViewModel(
        SavedStateHandle(
            mapOf(
                FinanceDestinations.TRANSACTION_TYPE_ARG to type.name,
                FinanceDestinations.TRANSACTION_ID_ARG to -1L
            )
        ),
        repo,
        ComputeAccountBalances(repo),
        Clock.systemDefaultZone()
    )

    private fun setFormContent(
        viewModel: TransactionFormViewModel,
        onDone: () -> Unit = {}
    ) {
        composeRule.setContent {
            CampusTheme {
                CompositionLocalProvider(LocalSnackbarHost provides SnackbarHostState()) {
                    TransactionFormScreen(
                        onDone = onDone,
                        viewModel = viewModel
                    )
                }
            }
        }
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
        val monthlyBudgetsFlow = MutableStateFlow(
            emptyList<com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity>()
        )
        val insertedTransactions = mutableListOf<Transaction>()
        val withdrawals = mutableListOf<Pair<Long, Long>>()

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

        override fun observeMonthlyBudgets() = monthlyBudgetsFlow
        override suspend fun getMonthlyBudget(yearMonth: String) =
            monthlyBudgetsFlow.value.firstOrNull { it.yearMonth == yearMonth }
        override suspend fun upsertMonthlyBudget(yearMonth: String, amountCents: Long) {
            monthlyBudgetsFlow.value = monthlyBudgetsFlow.value
                .filterNot { it.yearMonth == yearMonth } +
                com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity(
                    yearMonth = yearMonth,
                    amountCents = amountCents
                )
        }
        override suspend fun deleteMonthlyBudget(yearMonth: String) {
            monthlyBudgetsFlow.value = monthlyBudgetsFlow.value
                .filterNot { it.yearMonth == yearMonth }
        }
        override suspend fun withdrawToCash(
            savingsAccountId: Long,
            amountCents: Long,
            date: LocalDate
        ): com.example.campuspocket.feature.finance.domain.WithdrawalResult {
            withdrawals += savingsAccountId to amountCents
            return com.example.campuspocket.feature.finance.domain.WithdrawalResult.Success(++nextTxId)
        }
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
