package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.testExpense
import com.example.campuspocket.feature.finance.testIncome
import com.example.campuspocket.feature.finance.testTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas JVM de DayNet: el total del día no debe contar las transferencias como gasto.
 */
class DayNetTest {

    @Test fun `la transferencia no cuenta como gasto del dia`() {
        val transactions = listOf(
            testExpense(amountCents = 815),
            testTransfer(amountCents = 2_000)
        )
        assertEquals(-815L, DayNet.netCents(transactions))
    }

    @Test fun `un dia solo con transferencias no tiene total`() {
        val transactions = listOf(testTransfer(amountCents = 2_000))
        assertFalse(DayNet.hasTotal(transactions))
        assertEquals(0L, DayNet.netCents(transactions))
    }

    @Test fun `ingreso menos gasto`() {
        val transactions = listOf(
            testIncome(amountCents = 1_000),
            testExpense(amountCents = 300)
        )
        assertEquals(700L, DayNet.netCents(transactions))
        assertTrue(DayNet.hasTotal(transactions))
    }
}
