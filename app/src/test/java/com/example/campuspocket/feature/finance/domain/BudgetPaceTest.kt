package com.example.campuspocket.feature.finance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Pruebas JVM del ritmo del presupuesto total (Fase 8B). */
class BudgetPaceTest {

    private fun day(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun `20 de 31 dias da esperado 64 punto 5 por ciento`() {
        val pace = BudgetPace.compute(
            today = day(2026, 10, 20),
            budgetCents = 100_000,
            spentCents = 53_000
        )!!

        assertEquals(BigDecimal("64.5"), pace.expectedPct)
        assertEquals(BigDecimal("53.0"), pace.realPct)
        assertEquals(64_516L, pace.expectedCents) // 100000 * 20 / 31, half-up
        assertFalse(pace.isOver)
        assertEquals(0L, pace.overCents)
        assertEquals(20, pace.day)
        assertEquals(31, pace.daysInMonth)
    }

    @Test
    fun `el ultimo dia de meses de 28 29 30 y 31 dias da 100 por ciento`() {
        listOf(
            day(2026, 2, 28),
            day(2028, 2, 29),
            day(2026, 4, 30),
            day(2026, 5, 31)
        ).forEach { lastDay ->
            val pace = BudgetPace.compute(lastDay, 90_000, 45_000)!!
            assertEquals(
                "Esperado incorrecto para ${lastDay.monthValue}/${lastDay.lengthOfMonth()}",
                BigDecimal("100.0"),
                pace.expectedPct
            )
            assertEquals(90_000L, pace.expectedCents)
        }
    }

    @Test
    fun `el dia 1 usa un dia de 31`() {
        val pace = BudgetPace.compute(day(2026, 10, 1), 310_000, 0)!!

        assertEquals(BigDecimal("3.2"), pace.expectedPct)
        assertEquals(10_000L, pace.expectedCents)
        assertEquals(BigDecimal("0.0"), pace.realPct)
    }

    @Test
    fun `por encima del ritmo calcula el monto de mas`() {
        val pace = BudgetPace.compute(day(2026, 10, 20), 100_000, 65_000)!!

        assertTrue(pace.isOver)
        assertEquals(484L, pace.overCents) // 65000 - 64516
        assertEquals(BigDecimal("65.0"), pace.realPct)
    }

    @Test
    fun `presupuesto ausente o cero no produce ritmo`() {
        assertNull(BudgetPace.compute(day(2026, 10, 20), null, 10_000))
        assertNull(BudgetPace.compute(day(2026, 10, 20), 0, 10_000))
        assertNull(BudgetPace.compute(day(2026, 10, 20), -1, 10_000))
    }

    @Test
    fun `mas de 100 por ciento muestra el real completo pero la barra se limita`() {
        val pace = BudgetPace.compute(day(2026, 10, 20), 100_000, 150_000)!!

        assertEquals(BigDecimal("150.0"), pace.realPct)
        assertEquals(1f, pace.barProgress)
        assertTrue(pace.isOver)
    }

    @Test
    fun `la marca esperada usa la fraccion del dia`() {
        val pace = BudgetPace.compute(day(2026, 10, 20), 100_000, 0)!!
        assertEquals(20f / 31f, pace.expectedFraction, 0.0001f)

        val lastDay = BudgetPace.compute(day(2026, 10, 31), 100_000, 0)!!
        assertEquals(1f, lastDay.expectedFraction)
    }

    @Test
    fun `porcentajes redondean a un decimal half-up`() {
        val pace = BudgetPace.compute(day(2026, 10, 10), 3_000, 1_000)!!

        assertEquals(BigDecimal("33.3"), pace.realPct)
        assertEquals(BigDecimal("32.3"), pace.expectedPct) // 10/31 = 32.258...
    }

    @Test
    fun `gasto negativo se normaliza a cero`() {
        val pace = BudgetPace.compute(day(2026, 10, 20), 100_000, -5_000)!!
        assertEquals(0L, pace.spentCents)
        assertEquals(BigDecimal("0.0"), pace.realPct)
    }
}
