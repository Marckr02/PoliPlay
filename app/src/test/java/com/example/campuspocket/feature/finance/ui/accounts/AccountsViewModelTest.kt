package com.example.campuspocket.feature.finance.ui.accounts

import com.example.campuspocket.feature.finance.FakeFinanceRepository
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.domain.ComputeAccountBalances
import com.example.campuspocket.feature.finance.testAccount
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lista saldos con total incluyendo solo las marcadas`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository(
            accounts = listOf(
                testAccount(id = 1, name = "Efectivo", type = AccountType.CASH, initialBalanceCents = 1_000),
                testAccount(id = 2, name = "Banco", type = AccountType.DEBIT, initialBalanceCents = 2_000),
                testAccount(
                    id = 3,
                    name = "Tarjeta",
                    type = AccountType.CREDIT_CARD,
                    initialBalanceCents = -5_000,
                    includeInTotal = false
                )
            )
        )
        val vm = AccountsViewModel(ComputeAccountBalances(repo))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Efectivo", "Banco", "Tarjeta"), state.balances.map { it.account.name })
        assertEquals(listOf(1_000L, 2_000L, -5_000L), state.balances.map { it.balanceCents })
        // La tarjeta excluida del total no suma.
        assertEquals(3_000L, state.totalCents)
    }
}
