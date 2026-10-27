package com.example.campuspocket.feature.finance.ui.transactions

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.finance.testIncome
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val accounts = listOf(testAccount(id = 1, name = "Efectivo"), testAccount(id = 2, name = "Banco"))
    private val categories = listOf(
        testCategory(id = 1, name = "Comida", kind = CategoryKind.EXPENSE),
        testCategory(id = 2, name = "Mesada", kind = CategoryKind.INCOME)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        type: TransactionType,
        id: Long? = null,
        repository: FakeFinanceRepository = FakeFinanceRepository(accounts, categories),
        clock: Clock = FIXED_CLOCK,
        repeatLast: Boolean = false
    ) = TransactionFormViewModel(
        SavedStateHandle(
            mapOf(
                FinanceDestinations.TRANSACTION_TYPE_ARG to type.name,
                FinanceDestinations.TRANSACTION_ID_ARG to (id ?: -1L),
                FinanceDestinations.REPEAT_LAST_ARG to repeatLast
            )
        ),
        repository,
        ComputeAccountBalances(repository),
        clock
    )

    @Test
    fun `gasto valido se guarda con categoria de gasto y monto en centavos`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.EXPENSE, repository = repo)
        advanceUntilIdle()

        vm.onAmountChange("1250") // estilo cajero: dígitos puros -> $12,50
        vm.onAccountChange(1)
        vm.onCategoryChange(1)
        vm.onDescriptionChange("almuerzo")
        vm.save()
        advanceUntilIdle()

        val saved = repo.insertedTransactions.single()
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(1_250L, saved.amountCents)
        assertEquals(1L, saved.accountId)
        assertEquals(1L, saved.categoryId)
        assertEquals("almuerzo", saved.description)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `monto invalido deja error y no guarda`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.EXPENSE, repository = repo)
        advanceUntilIdle()

        vm.onAmountChange("0")
        vm.onAccountChange(1)
        vm.onCategoryChange(1)
        vm.save()
        advanceUntilIdle()

        assertEquals(TransactionError.AMOUNT_NOT_POSITIVE, vm.uiState.value.error)
        assertTrue(repo.insertedTransactions.isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `gasto con categoria de ingreso no se guarda`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.EXPENSE, repository = repo)
        advanceUntilIdle()

        vm.onAmountChange("1000") // $10,00
        vm.onAccountChange(1)
        vm.onCategoryChange(2)
        vm.save()
        advanceUntilIdle()

        assertEquals(TransactionError.WRONG_CATEGORY_KIND, vm.uiState.value.error)
        assertTrue(repo.insertedTransactions.isEmpty())
    }

    @Test
    fun `transferencia a la misma cuenta no se guarda`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.TRANSFER, repository = repo)
        advanceUntilIdle()

        vm.onAmountChange("2000") // $20,00
        vm.onAccountChange(1)
        vm.onTargetAccountChange(1)
        vm.save()
        advanceUntilIdle()

        assertEquals(TransactionError.TRANSFER_SAME_ACCOUNT, vm.uiState.value.error)
        assertTrue(repo.insertedTransactions.isEmpty())
    }

    @Test
    fun `reembolso puede guardarse sin gasto original`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.REFUND, repository = repo)
        advanceUntilIdle()

        vm.onAmountChange("500") // $5,00
        vm.onAccountChange(1)
        vm.onCategoryChange(1)
        vm.save()
        advanceUntilIdle()

        val saved = repo.insertedTransactions.single()
        assertEquals(TransactionType.REFUND, saved.type)
        assertNull(saved.refundOfId)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `repetir ultimo gasto precarga monto nota cuenta y categoria`() = runTest(testDispatcher) {
        val expense = testExpense(
            id = 7,
            accountId = 2,
            categoryId = 1,
            amountCents = 3_450,
            description = "Café con pan",
            date = LocalDate.of(2026, 10, 20)
        )
        val income = testIncome(
            id = 8,
            accountId = 1,
            categoryId = 2,
            amountCents = 9_000,
            date = LocalDate.of(2026, 10, 21)
        )
        val repo = FakeFinanceRepository(accounts, categories, listOf(income, expense))

        val vm = viewModel(TransactionType.EXPENSE, repository = repo, repeatLast = true)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3_450L, state.amountCents)
        assertEquals("Café con pan", state.description)
        assertEquals(2L, state.accountId)
        assertEquals(1L, state.categoryId)
        assertTrue(state.showDescription)
        assertTrue(state.isLoaded)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `editar carga y actualiza conservando el id`() = runTest(testDispatcher) {
        val existing = testExpense(
            id = 9, accountId = 1, categoryId = 1, amountCents = 2_500,
            description = "viejo", date = LocalDate.of(2026, 10, 3)
        )
        val repo = FakeFinanceRepository(accounts, categories, listOf(existing))
        val vm = viewModel(TransactionType.EXPENSE, id = 9, repository = repo)
        advanceUntilIdle()

        assertEquals(2_500L, vm.uiState.value.amountCents)

        vm.onAmountChange("3000") // $30,00
        vm.onDescriptionChange("nuevo")
        vm.save()
        advanceUntilIdle()

        val updated = repo.updatedTransactions.single()
        assertEquals(9L, updated.id)
        assertEquals(3_000L, updated.amountCents)
        assertEquals("nuevo", updated.description)
    }

    @Test
    fun `sugiere la categoria habitual de la franja y no la ultima usada`() =
        runTest(testDispatcher) {
            val morning = Instant.parse("2026-10-20T09:00:00Z").toEpochMilli()
            val later = Instant.parse("2026-10-25T15:00:00Z").toEpochMilli()
            val expenseCategories = listOf(
                testCategory(id = 1, name = "Comida", kind = CategoryKind.EXPENSE),
                testCategory(id = 3, name = "Transporte", kind = CategoryKind.EXPENSE)
            )
            val history = listOf(
                testExpense(id = 1, categoryId = 1, date = LocalDate.of(2026, 10, 20))
                    .copy(createdAt = morning),
                testExpense(id = 2, categoryId = 1, date = LocalDate.of(2026, 10, 21))
                    .copy(createdAt = morning),
                testExpense(id = 3, categoryId = 3, date = LocalDate.of(2026, 10, 25))
                    .copy(createdAt = later)
            )
            val repo = FakeFinanceRepository(accounts, expenseCategories, history)

            val vm = viewModel(TransactionType.EXPENSE, repository = repo)
            advanceUntilIdle()

            assertEquals(1L, vm.uiState.value.categoryId)
            assertFalse(vm.uiState.value.usesLastCategory)
        }

    @Test
    fun `crear categoria desde el formulario la selecciona`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(accounts, categories)
        val vm = viewModel(TransactionType.EXPENSE, repository = repo)
        advanceUntilIdle()

        vm.createCategory("Café")
        advanceUntilIdle()

        val created = repo.observeActiveCategories().first().single { it.id == 3L }
        assertEquals("Café", created.name)
        assertEquals(CategoryKind.EXPENSE, created.kind)
        assertEquals(3L, vm.uiState.value.categoryId)
    }

    @Test
    fun `seguir anadiendo limpia el monto y deshacer borra el ultimo`() =
        runTest(testDispatcher) {
            val repo = FakeFinanceRepository(accounts, categories)
            val vm = viewModel(TransactionType.EXPENSE, repository = repo)
            advanceUntilIdle()

            vm.onToggleKeepAdding(true)
            vm.onAmountChange("1234")
            vm.onAccountChange(1)
            vm.onCategoryChange(1)
            vm.save()
            advanceUntilIdle()

            assertEquals(0L, vm.uiState.value.amountCents)
            assertFalse(vm.uiState.value.saved)
            assertEquals(1, vm.uiState.value.savedCount)
            val insertedId = vm.uiState.value.lastInsertedTransactionId
            assertEquals(1L, insertedId)
            assertEquals(1, repo.insertedTransactions.size)

            vm.deleteLastTransaction()
            advanceUntilIdle()

            assertEquals(listOf(insertedId), repo.deletedTransactions)
            assertEquals(1, vm.uiState.value.savedCount)
            assertNull(vm.uiState.value.lastInsertedTransactionId)
        }

    @Test
    fun `la linea de impacto suma el monto al gasto del mes`() = runTest(testDispatcher) {
        val existing = testExpense(
            id = 1,
            accountId = 1,
            categoryId = 1,
            amountCents = 25_000,
            date = LocalDate.of(2026, 10, 5)
        )
        val repo = FakeFinanceRepository(accounts, categories, listOf(existing))
        repo.upsertMonthlyBudget("2026-10", 100_000)
        val vm = viewModel(TransactionType.EXPENSE, repository = repo)
        advanceUntilIdle()

        vm.onAccountChange(1)
        vm.onAmountChange("10000") // $100,00
        advanceUntilIdle()

        val pace = vm.monthlyImpact.value.pace
        assertEquals(100_000L, pace?.budgetCents)
        assertEquals(35_000L, pace?.spentCents)
        assertEquals("Efectivo", vm.monthlyImpact.value.accountName)
        assertEquals(-35_000L, vm.monthlyImpact.value.accountAfterCents)
    }

    private companion object {
        val FIXED_CLOCK: Clock =
            Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC)
    }
}
