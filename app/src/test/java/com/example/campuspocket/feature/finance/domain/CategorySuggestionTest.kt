package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggestionTest {

    private val now = ZonedDateTime.of(2026, 10, 26, 10, 0, 0, 0, ZoneOffset.UTC)

    private fun expense(
        id: Long,
        categoryId: Long,
        hourUtc: Int,
        day: LocalDate = LocalDate.of(2026, 10, 20)
    ): Transaction = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amountCents = 1_000,
        date = day,
        accountId = 1,
        categoryId = categoryId,
        createdAt = day.atTime(hourUtc, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    )

    @Test
    fun `sugiere la categoria mas frecuente en la franja actual`() {
        val txs = listOf(
            expense(1, categoryId = 10, hourUtc = 9),
            expense(2, categoryId = 10, hourUtc = 10),
            expense(3, categoryId = 20, hourUtc = 8),
            extra(4, categoryId = 30, hourUtc = 15),
            extra(5, categoryId = 30, hourUtc = 16),
            extra(6, categoryId = 30, hourUtc = 17),
            extra(7, categoryId = 40, hourUtc = 19)
        )

        val habits = CategorySuggestion.analyze(txs, TransactionType.EXPENSE, now)

        assertEquals(10L, habits.suggestedCategoryId)
        assertEquals(listOf(30L, 10L, 40L, 20L), habits.mostUsedCategoryIds)
        assertEquals(listOf(40L, 30L, 10L, 20L), habits.recentCategoryIds)
    }

    @Test
    fun `sin historial en la franja no sugiere y cae al historial general`() {
        val txs = listOf(extra(1, categoryId = 10, hourUtc = 18))

        val habits = CategorySuggestion.analyze(txs, TransactionType.EXPENSE, now)

        assertNull(habits.suggestedCategoryId)
        assertEquals(listOf(10L), habits.mostUsedCategoryIds)
    }

    @Test
    fun `ignora movimientos de mas de 60 dias y de otro tipo`() {
        val old = expense(1, categoryId = 10, hourUtc = 9, day = LocalDate.of(2026, 7, 1))
        val income = extra(2, categoryId = 20, hourUtc = 9)
            .copy(type = TransactionType.INCOME)
        val recent = expense(3, categoryId = 30, hourUtc = 9)

        val habits = CategorySuggestion.analyze(listOf(old, income, recent), TransactionType.EXPENSE, now)

        assertEquals(30L, habits.suggestedCategoryId)
        assertEquals(listOf(30L), habits.mostUsedCategoryIds)
    }

    @Test
    fun `una transferencia no sugiere categoria`() {
        val habits = CategorySuggestion.analyze(
            listOf(expense(1, categoryId = 10, hourUtc = 9)),
            TransactionType.TRANSFER,
            now
        )

        assertNull(habits.suggestedCategoryId)
        assertEquals(emptyList<Long>(), habits.mostUsedCategoryIds)
    }

    @Test
    fun `las franjas respetan manana tarde y noche`() {
        assertEquals(DaySlot.MORNING, CategorySuggestion.slotOf(6))
        assertEquals(DaySlot.MORNING, CategorySuggestion.slotOf(11))
        assertEquals(DaySlot.AFTERNOON, CategorySuggestion.slotOf(12))
        assertEquals(DaySlot.AFTERNOON, CategorySuggestion.slotOf(17))
        assertEquals(DaySlot.NIGHT, CategorySuggestion.slotOf(18))
        assertEquals(DaySlot.NIGHT, CategorySuggestion.slotOf(5))
    }

    private fun extra(id: Long, categoryId: Long, hourUtc: Int): Transaction =
        expense(id, categoryId, hourUtc).copy(
            createdAt = Instant.parse("2026-10-21T${hourUtc.toString().padStart(2, '0')}:00:00Z")
                .toEpochMilli()
        )
}
