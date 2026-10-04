package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.testAccount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del cálculo puro de saldos (spec 7.1): la combinación de las consultas del DAO.
 * El SQL real se prueba aparte en el instrumentado (FinanceDaoTest).
 */
class ComputeAccountBalancesTest {

    @Test
    fun `saldo = inicial + ingresos - gastos - transferencias salientes + entrantes`() {
        val accounts = listOf(
            testAccount(id = 1, initialBalanceCents = 10_000), // efectivo
            testAccount(id = 2, type = AccountType.DEBIT, initialBalanceCents = 20_000)
        )
        val deltas = listOf(
            TransactionDao.AccountDelta(1, 3_000),   // +2 ingresos, -2 gasto... neto del SQL
            TransactionDao.AccountDelta(2, -5_000)
        )
        val transferIns = listOf(TransactionDao.AccountDelta(1, 1_500))

        val result = ComputeAccountBalances.compute(accounts, deltas, transferIns)

        assertEquals(14_500L, result.first { it.account.id == 1L }.balanceCents)
        assertEquals(15_000L, result.first { it.account.id == 2L }.balanceCents)
    }

    @Test
    fun `la tarjeta de credito puede quedar en negativo (deuda)`() {
        val accounts = listOf(
            testAccount(id = 1, type = AccountType.CREDIT_CARD, initialBalanceCents = 0)
        )
        val deltas = listOf(TransactionDao.AccountDelta(1, -8_000))

        val result = ComputeAccountBalances.compute(accounts, deltas, emptyList())

        assertEquals(-8_000L, result.single().balanceCents)
    }

    @Test
    fun `sin movimientos el saldo es el inicial`() {
        val accounts = listOf(testAccount(id = 1, initialBalanceCents = 7_777))

        val result = ComputeAccountBalances.compute(accounts, emptyList(), emptyList())

        assertEquals(7_777L, result.single().balanceCents)
    }

    @Test
    fun `transferencia entre cuentas mueve el saldo sin duplicarlo`() {
        val accounts = listOf(
            testAccount(id = 1, initialBalanceCents = 1_000),
            testAccount(id = 2, initialBalanceCents = 500)
        )
        // El SQL de deltas cuenta la transferencia como salida (-) en el origen...
        val deltas = listOf(TransactionDao.AccountDelta(1, -400))
        // ...y observeTransferInDeltas la cuenta como entrada (+) en el destino.
        val transferIns = listOf(TransactionDao.AccountDelta(2, 400))

        val result = ComputeAccountBalances.compute(accounts, deltas, transferIns)

        assertEquals(600L, result.first { it.account.id == 1L }.balanceCents)
        assertEquals(900L, result.first { it.account.id == 2L }.balanceCents)
    }

    @Test
    fun `reembolso suma al saldo de su cuenta`() {
        val accounts = listOf(testAccount(id = 1, initialBalanceCents = 1_000))
        // Delta neto de la cuenta tras gasto de 500 y reembolso de 200: -300.
        val deltas = listOf(TransactionDao.AccountDelta(1, -300))

        val result = ComputeAccountBalances.compute(accounts, deltas, emptyList())

        assertEquals(700L, result.single().balanceCents)
    }
}
