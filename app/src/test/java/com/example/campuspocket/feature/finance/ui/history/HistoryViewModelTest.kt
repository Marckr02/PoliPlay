package com.example.campuspocket.feature.finance.ui.history

import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.finance.testIncome
import com.example.campuspocket.feature.finance.testTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val oct5 = LocalDate.of(2026, 10, 5)
    private val oct10 = LocalDate.of(2026, 10, 10)

    @Test
    fun `sin filtros lista todo ordenado por fecha desc`() = runTest(testDispatcher) {
        val all = listOf(
            testExpense(id = 1, accountId = 1, categoryId = 1, amountCents = 500,
                description = "almuerzo", date = oct5),
            testIncome(id = 2, accountId = 1, categoryId = 2, amountCents = 2_000, date = oct10),
            testTransfer(id = 3, accountId = 1, targetAccountId = 2, amountCents = 100, date = oct5)
        )

        val vm = HistoryViewModel(newRepo(all))
        advanceUntilIdle()

        assertEquals(listOf(2L, 1L, 3L), vm.uiState.value.transactions.map { it.id })
    }

    @Test
    fun `applyFilters combina todos los criterios`() {
        val all = listOf(
            testExpense(id = 1, accountId = 1, categoryId = 1, description = "almuerzo", date = oct5),
            testExpense(id = 2, accountId = 1, categoryId = 1, description = "taxi", date = oct10),
            testIncome(id = 3, accountId = 2, categoryId = 2, date = oct10)
        )

        val filtered = HistoryViewModel.applyFilters(
            all,
            HistoryFilters(type = TransactionType.EXPENSE, accountId = 1, categoryId = 1, query = "alm")
        )

        assertEquals(listOf(1L), filtered.map { it.id })
    }

    @Test
    fun `el filtro de cuenta tambien atrapa transferencias entrantes`() {
        val all = listOf(
            testTransfer(id = 1, accountId = 1, targetAccountId = 2, amountCents = 100)
        )

        val fromTarget = HistoryViewModel.applyFilters(all, HistoryFilters(accountId = 2))
        val fromOrigin = HistoryViewModel.applyFilters(all, HistoryFilters(accountId = 1))

        assertEquals(listOf(1L), fromTarget.map { it.id })
        assertEquals(listOf(1L), fromOrigin.map { it.id })
    }

    @Test
    fun `rango de fechas deja fuera lo que no toca`() = runTest(testDispatcher) {
        val all = listOf(
            testExpense(id = 1, date = oct5),
            testExpense(id = 2, date = oct10)
        )
        val vm = HistoryViewModel(newRepo(all))
        advanceUntilIdle()

        vm.setDateRange(oct10, oct10)
        advanceUntilIdle()

        assertEquals(listOf(2L), vm.uiState.value.transactions.map { it.id })
    }

    private fun newRepo(transactions: List<Transaction>) = FakeFinanceRepository(
        accounts = listOf(testAccount(id = 1, name = "Efectivo"), testAccount(id = 2, name = "Banco")),
        categories = listOf(
            testCategory(id = 1, name = "Comida"),
            testCategory(id = 2, name = "Mesada", kind = com.example.campuspocket.feature.finance.data.CategoryKind.INCOME)
        ),
        transactions = transactions
    )
}
