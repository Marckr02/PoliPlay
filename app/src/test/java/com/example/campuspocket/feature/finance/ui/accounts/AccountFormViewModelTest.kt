package com.example.campuspocket.feature.finance.ui.accounts

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.testAccount
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        id: Long? = null,
        repository: FakeFinanceRepository = FakeFinanceRepository()
    ) = AccountFormViewModel(
        SavedStateHandle(mapOf(FinanceDestinations.ACCOUNT_ID_ARG to (id ?: -1L))),
        repository
    )

    @Test
    fun `entrada estilo cajero acumula digitos por la derecha`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onInitialAmountChange("5")   // 0,05
        assertEquals(5L, vm.uiState.value.initialAmountCents)
        vm.onInitialAmountChange("0,054") // teclear 4 tras el 5 (el campo muestra "0,05")
        assertEquals(54L, vm.uiState.value.initialAmountCents)
        vm.onInitialAmountChange("0,540")  // luego 0
        assertEquals(540L, vm.uiState.value.initialAmountCents)
    }

    @Test
    fun `deuda inicial de tarjeta se guarda en negativo`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val vm = viewModel(repository = repo)
        advanceUntilIdle()

        vm.onNameChange("Visa")
        vm.onTypeChange(AccountType.CREDIT_CARD)
        vm.onInitialAmountChange("20000") // $200,00 de deuda
        vm.save()
        advanceUntilIdle()

        val saved = repo.insertedAccounts.single()
        assertEquals(AccountType.CREDIT_CARD, saved.type)
        assertEquals(-20_000L, saved.initialBalanceCents)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `editar tarjeta carga la deuda en positivo`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(
                testAccount(id = 3, name = "Visa", type = AccountType.CREDIT_CARD, initialBalanceCents = -20_000)
            )
        )
        val vm = viewModel(id = 3, repository = repo)
        advanceUntilIdle()

        assertEquals(20_000L, vm.uiState.value.initialAmountCents)
        assertEquals(AccountType.CREDIT_CARD, vm.uiState.value.type)
    }
}
