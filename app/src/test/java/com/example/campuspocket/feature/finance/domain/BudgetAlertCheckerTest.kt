package com.example.campuspocket.feature.finance.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Cruces de umbral del presupuesto una sola vez por umbral y mes (spec 7.3). */
class BudgetAlertCheckerTest {

    @Test
    fun `cruzar el 80 notifica una vez`() {
        // antes al 70 -> despues al 82: cruzó
        assertEquals(80, BudgetAlertChecker.crossedThreshold(70_000, 82_000, 100_000))
        // antes ya estaba en 81: no cruza de nuevo
        assertEquals(0, BudgetAlertChecker.crossedThreshold(81_000, 90_000, 100_000))
    }

    @Test
    fun `cruzar el 100 notifica una vez`() {
        assertEquals(100, BudgetAlertChecker.crossedThreshold(95_000, 101_000, 100_000))
        assertEquals(0, BudgetAlertChecker.crossedThreshold(101_000, 120_000, 100_000))
    }

    @Test
    fun `sin presupuesto no notifica`() {
        assertEquals(0, BudgetAlertChecker.crossedThreshold(0, 9_999, 0))
    }

    @Test
    fun `cruzar de abajo a encima en un solo salto notifica el 100 (el mayor)`() {
        assertEquals(100, BudgetAlertChecker.crossedThreshold(60_000, 150_000, 100_000))
    }
}
