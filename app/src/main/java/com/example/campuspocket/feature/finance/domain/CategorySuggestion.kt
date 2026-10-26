package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Transaction
import java.time.Instant
import java.time.ZonedDateTime

/** Franjas horarias usadas para aprender la categoría habitual (v6). */
enum class DaySlot { MORNING, AFTERNOON, NIGHT }

/** Hábitos de categoría calculados a partir del historial reciente. */
data class CategoryHabits(
    /** La más frecuente en la franja actual; null si no hay historial. */
    val suggestedCategoryId: Long? = null,
    /** Las más usadas de los últimos 60 días, de mayor a menor. */
    val mostUsedCategoryIds: List<Long> = emptyList(),
    /** Categorías ordenadas por uso más reciente. */
    val recentCategoryIds: List<Long> = emptyList()
)

/**
 * Aprende de las transacciones existentes sin cambiar el esquema (v6, Parte 2):
 * - sugerencia = categoría más frecuente en la franja horaria actual (mañana,
 *   tarde o noche) durante los últimos 60 días;
 * - chips = categorías más usadas del mismo periodo;
 * - hoja = orden por uso reciente.
 *
 * Los reembolsos aprenden del historial de gastos porque comparten categorías.
 */
object CategorySuggestion {

    const val HISTORY_DAYS = 60L

    fun slotOf(hour: Int): DaySlot = when (hour) {
        in 6..11 -> DaySlot.MORNING
        in 12..17 -> DaySlot.AFTERNOON
        else -> DaySlot.NIGHT
    }

    fun analyze(
        transactions: List<Transaction>,
        type: TransactionType,
        now: ZonedDateTime
    ): CategoryHabits {
        val historyType = when (type) {
            TransactionType.EXPENSE, TransactionType.REFUND -> TransactionType.EXPENSE
            TransactionType.INCOME -> TransactionType.INCOME
            TransactionType.TRANSFER -> return CategoryHabits()
        }
        val today = now.toLocalDate()
        val since = today.minusDays(HISTORY_DAYS)
        val candidates = transactions.filter { tx ->
            tx.type == historyType &&
                tx.categoryId != null &&
                tx.date >= since &&
                tx.date <= today
        }
        if (candidates.isEmpty()) return CategoryHabits()

        val recent = candidates
            .sortedByDescending { it.createdAt }
            .mapNotNull { it.categoryId }
            .distinct()

        val usage = candidates
            .groupBy { it.categoryId!! }
            .map { (categoryId, rows) ->
                categoryId to rows
            }
            .sortedWith(
                compareByDescending<Pair<Long, List<Transaction>>> { it.second.size }
                    .thenByDescending { it.second.maxOfOrNull { row -> row.createdAt } ?: 0L }
            )
            .map { it.first }

        val currentSlot = slotOf(now.hour)
        val suggested = candidates
            .filter { tx ->
                slotOf(
                    Instant.ofEpochMilli(tx.createdAt).atZone(now.zone).hour
                ) == currentSlot
            }
            .groupBy { it.categoryId!! }
            .maxWithOrNull(
                compareBy<Map.Entry<Long, List<Transaction>>> { it.value.size }
                    .thenBy { entry -> entry.value.maxOfOrNull { it.createdAt } ?: 0L }
            )
            ?.key

        return CategoryHabits(
            suggestedCategoryId = suggested,
            mostUsedCategoryIds = usage.take(5),
            recentCategoryIds = recent
        )
    }
}
