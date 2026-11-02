package com.example.campuspocket.feature.finance.ui.budgets

import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-10-21T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `expone el ritmo del presupuesto mensual en el estado`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE)),
            transactions = listOf(
                testExpense(
                    id = 1,
                    accountId = 1,
                    categoryId = 1,
                    amountCents = 5_000,
                    date = LocalDate.of(2026, 10, 10)
                )
            )
        )
        repo.upsertMonthlyBudget("2026-10", 16_000)

        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(16_000L, state.monthlyBudgetCents)
        assertEquals(5_000L, state.totalSpentCents)
        val pace = state.pace
        assertNotNull(pace)
        assertEquals(16_000L, pace!!.budgetCents)
        assertEquals(5_000L, pace.spentCents)
    }

    @Test
    fun `mes sin movimientos lista las categorias de gasto sin presupuesto`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(
                testCategory(id = 1, kind = CategoryKind.EXPENSE),
                testCategory(id = 2, kind = CategoryKind.EXPENSE),
                testCategory(id = 3, kind = CategoryKind.INCOME)
            )
        )
        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()
        viewModel.nextMonth()
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(setOf(1L, 2L), rows.map { it.categoryId }.toSet())
        assertTrue(rows.all { it.budgetCents == null })
    }

    @Test
    fun `copiar trae categorias y total del mes anterior sin pisar lo existente`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(
                testCategory(id = 1, kind = CategoryKind.EXPENSE),
                testCategory(id = 2, kind = CategoryKind.EXPENSE)
            )
        )
        repo.upsertBudget("2026-09", 1, 20_000)
        repo.upsertBudget("2026-09", 2, 10_000)
        repo.upsertMonthlyBudget("2026-09", 100_000)
        repo.upsertBudget("2026-10", 2, 15_000)

        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()
        val result = viewModel.copyFromPreviousMonth()

        assertEquals(YearMonth.of(2026, 9), result.sourceMonth)
        assertEquals(1, result.categoriesCopied)
        assertTrue(result.totalCopied)
        assertEquals(20_000L, repo.getBudget("2026-10", 1)!!.amountCents)
        assertEquals(15_000L, repo.getBudget("2026-10", 2)!!.amountCents)
        assertEquals(100_000L, repo.getMonthlyBudget("2026-10")!!.amountCents)
    }

    @Test
    fun `copiar busca el ultimo mes con datos y avisa si no hay ninguno`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE))
        )
        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()
        assertNull(viewModel.copyFromPreviousMonth().sourceMonth)

        repo.upsertMonthlyBudget("2026-07", 50_000)
        val result = viewModel.copyFromPreviousMonth()
        assertEquals(YearMonth.of(2026, 7), result.sourceMonth)
        assertTrue(result.totalCopied)
    }

    @Test
    fun `ir al mes actual vuelve al mes del reloj`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE))
        )
        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCurrentMonth)

        viewModel.nextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.yearMonth)
        assertTrue(!viewModel.uiState.value.isCurrentMonth)

        viewModel.goToCurrentMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.yearMonth)
        assertTrue(viewModel.uiState.value.isCurrentMonth)
    }

    @Test
    fun `restaurar un presupuesto lo vuelve a dejar en el repositorio`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE))
        )
        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()

        viewModel.restoreBudget(YearMonth.of(2026, 10), 1, 5_000)
        advanceUntilIdle()

        assertEquals(5_000L, repo.getBudget("2026-10", 1)!!.amountCents)
    }

    @Test
    fun `restaurar el presupuesto total lo vuelve a dejar en el repositorio`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(testCategory(id = 1, kind = CategoryKind.EXPENSE))
        )
        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()

        viewModel.restoreMonthlyBudget(YearMonth.of(2026, 10), 75_000)
        advanceUntilIdle()

        assertEquals(75_000L, repo.getMonthlyBudget("2026-10")!!.amountCents)
    }

    @Test
    fun `deshacer la copia borra solo lo copiado`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(testAccount(id = 1)),
            categories = listOf(
                testCategory(id = 1, kind = CategoryKind.EXPENSE),
                testCategory(id = 2, kind = CategoryKind.EXPENSE)
            )
        )
        repo.upsertBudget("2026-09", 1, 20_000)
        repo.upsertBudget("2026-09", 2, 10_000)
        repo.upsertMonthlyBudget("2026-09", 100_000)
        repo.upsertBudget("2026-10", 2, 15_000)

        val viewModel = BudgetsViewModel(repo, fixedClock)
        advanceUntilIdle()
        val result = viewModel.copyFromPreviousMonth()
        assertEquals(listOf(1L), result.copiedCategoryIds)
        assertEquals(20_000L, repo.getBudget("2026-10", 1)!!.amountCents)

        viewModel.undoCopy(result)

        assertNull(repo.getBudget("2026-10", 1))
        assertEquals(15_000L, repo.getBudget("2026-10", 2)!!.amountCents)
        assertNull(repo.getMonthlyBudget("2026-10"))
    }
}
