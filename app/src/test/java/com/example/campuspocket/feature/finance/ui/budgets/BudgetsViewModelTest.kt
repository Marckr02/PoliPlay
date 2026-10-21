package com.example.campuspocket.feature.finance.ui.budgets

import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
}
