package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pruebas JVM de GetMonthSummary (spec 7.3): todo puro, sin Room ni Android.
 */
class GetMonthSummaryTest {

    private val oct = YearMonth.of(2026, 10)
    private val start = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 10, 31)

    private fun tx(
        id: Long, type: TransactionType, categoryId: Long?, amountCents: Long, date: LocalDate = start
    ) = Transaction(
        id = id,
        type = type,
        amountCents = amountCents,
        date = date,
        accountId = 1,
        categoryId = categoryId
    )

    private val food = Category(1, "Comida", CategoryKind.EXPENSE, 0xFFE5484D.toInt())
    private val transport = Category(2, "Transporte", CategoryKind.EXPENSE, 0xFF4C8DFF.toInt())
    private val salary = Category(3, "Mesada", CategoryKind.INCOME, 0xFF2EB67D.toInt())
    private val catMap = listOf(food, transport, salary).mapNotNull { c -> c.id?.let { it to c } }.toMap()

    @Test
    fun `neto por categoria resta reembolsos y las transferencias no cuentan`() {
        val txs = listOf(
            tx(1, TransactionType.EXPENSE, 1, 10_000),
            tx(2, TransactionType.EXPENSE, 1, 5_000),
            tx(3, TransactionType.REFUND, 1, 2_000),       // resta
            tx(4, TransactionType.TRANSFER, null, 99_000), // ignora aunque tenga cuenta
            tx(5, TransactionType.EXPENSE, 2, 3_000)
        )
        val result = GetMonthSummary.compute(
            oct, txs, emptyMap(), catMap, start, end
        )

        assertEquals(13_000L, result.rows.first { it.categoryId == 1L }.netSpentCents)
        assertEquals(3_000L, result.rows.first { it.categoryId == 2L }.netSpentCents)
        assertEquals(3, result.rows.first { it.categoryId == 1L }.transactionCount)
        // Transferencia no aparece
        assertEquals(2, result.rows.size)
        assertEquals(16_000L, result.totalSpentCents)
    }

    @Test
    fun `disponible = presupuestos - gastado solo de categorias CON presupuesto`() {
        val budgets = mapOf(1L to 20_000L) // solo comida con presupuesto
        val txs = listOf(
            tx(1, TransactionType.EXPENSE, 1, 5_000),  // con presupuesto
            tx(2, TransactionType.EXPENSE, 2, 7_000)   // sin presupuesto: sale en el top, no en el disponible
        )

        val result = GetMonthSummary.compute(oct, txs, budgets, catMap, start, end)

        assertEquals(20_000L, result.totalBudgetCents)
        // disponible solo resta los 5000 de la categoría con presupuesto
        assertEquals(15_000L, result.availableCents)

        // transporte sale en el top sin presupuesto
        val transportRow = result.rows.first { it.categoryId == 2L }
        assertEquals(7_000L, transportRow.netSpentCents)
        assertEquals(null, transportRow.budgetCents)
        assertEquals(null, transportRow.remainingCents)
    }

    @Test
    fun `categoria excedida marca remaining negativo`() {
        val budgets = mapOf(1L to 1_000L)
        val txs = listOf(tx(1, TransactionType.EXPENSE, 1, 1_300))

        val result = GetMonthSummary.compute(oct, txs, budgets, catMap, start, end)
        val row = result.rows.single()

        assertEquals(-300L, row.remainingCents)
        assertEquals(1.3f, row.percent!!, 0.001f)
    }

    @Test
    fun `top ordenado por gastado neto descendente`() {
        val txs = listOf(
            tx(1, TransactionType.EXPENSE, 1, 100),
            tx(2, TransactionType.EXPENSE, 2, 5_000),
            tx(3, TransactionType.EXPENSE, 3, 800)
        )

        val result = GetMonthSummary.compute(oct, txs, emptyMap(), catMap, start, end)

        assertEquals(listOf(2L, 3L, 1L), result.rows.map { it.categoryId })
    }

    @Test
    fun `limites de mes dejan fuera lo de otro mes`() {
        val otherMonth = LocalDate.of(2026, 9, 30)
        val txs = listOf(
            tx(1, TransactionType.EXPENSE, 1, 999, date = otherMonth)
        )

        val result = GetMonthSummary.compute(oct, txs, emptyMap(), catMap, start, end)
        assertEquals(0, result.rows.size)
        assertEquals(0L, result.totalSpentCents)
    }

    @Test
    fun `semestre con fechas suma solo sus meses (sin futuros)`() {
        val thisMonth = YearMonth.of(2026, 10)
        val semesterRange = LocalDate.of(2026, 8, 1) to LocalDate.of(2027, 2, 28)

        val months = GetMonthSummary.semesterMonths(thisMonth, semesterRange)

        assertEquals(
            listOf(
                YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)
            ),
            months
        )
    }

    @Test
    fun `sin fechas de semestre usa los ultimos seis meses`() {
        val thisMonth = YearMonth.of(2026, 10)
        val months = GetMonthSummary.semesterMonths(thisMonth, null)

        assertEquals(6, months.size)
        assertEquals(YearMonth.of(2026, 5), months.first())
        assertEquals(thisMonth, months.last())
    }
}
