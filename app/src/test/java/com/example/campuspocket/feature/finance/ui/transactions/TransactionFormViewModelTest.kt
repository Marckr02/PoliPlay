package com.example.campuspocket.feature.finance.ui.transactions

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.TransactionError
import com.example.campuspocket.feature.finance.testAccount
import com.example.campuspocket.feature.finance.testCategory
import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.finance.ui.FinanceDestinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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
        repository: FakeFinanceRepository = FakeFinanceRepository(accounts, categories)
    ) = TransactionFormViewModel(
        SavedStateHandle(
            mapOf(
                FinanceDestinations.TRANSACTION_TYPE_ARG to type.name,
                FinanceDestinations.TRANSACTION_ID_ARG to (id ?: -1L)
            )
        ),
        repository
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
}
