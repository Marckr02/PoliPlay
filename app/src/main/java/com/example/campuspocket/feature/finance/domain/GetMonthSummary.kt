package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.finance.domain.model.Category
import com.example.campuspocket.feature.finance.domain.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lógica pura del resumen mensual (spec 7.3).
 * Todo en centavos, sin Android: 100 % testeable en JVM.
 */
object GetMonthSummary {

    data class CategoryRow(
        val categoryId: Long,
        val name: String,
        val colorArgb: Int,
        val netSpentCents: Long,
        val transactionCount: Int,
        val budgetCents: Long?,          // null = sin presupuesto definido
        val remainingCents: Long?        // presupuesto - gastado; <0 = excedido
    ) {
        val percent: Float? get() =
            budgetCents?.takeIf { it > 0 }?.let { netSpentCents.toFloat() / it.toFloat() }
    }

    /** Resultado del mes: lo que gasta cada categoría y su presupuesto. */
    data class MonthSummary(
        val yearMonth: YearMonth,
        val rows: List<CategoryRow>,        // ordenado por gastado neto desc (top primero)
        val totalSpentCents: Long,           // solo gasto neto de TODAS las categorías con gasto
        val totalBudgetCents: Long,          // suma de presupuestos definidos
        val availableCents: Long             // totalBudget - gasto neto de categorías CON presupuesto
    )

    /**
     * Combina transacciones del mes (ya filtradas por rango de fechas) + presupuestos +
     * categorías activas.
     *
     * Reglas:
     * - gastado neto por categoría = Σ gastos - Σ reembolsos; transferencias NO cuentan.
     * - el "disponible" solo resta lo gastado en categorías CON presupuesto.
     * - las categorías sin presupuesto salen en el top pero no influyen en disponible.
     */
    fun compute(
        yearMonth: YearMonth,
        transactions: List<Transaction>,
        budgets: Map<Long, Long>,              // categoryId -> amountCents
        categories: Map<Long, Category>,       // id -> Category
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): MonthSummary {
        val inMonth = transactions.filter { it.date >= monthStart && it.date <= monthEnd }

        // Neto por categoría (sin transferencias; REFUND resta).
        val spentByCategory = inMonth
            .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND }
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, txs) ->
                categoryId ?: return@mapNotNull null
                val net = txs.sumOf { if (it.type == TransactionType.EXPENSE) it.amountCents else -it.amountCents }
                net.takeIf { it != 0L }?.let { categoryId to it }
            }
            .toMap()

        val countByCategory = inMonth
            .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.REFUND }
            .groupBy { it.categoryId }
            .mapNotNull { (k, v) -> k?.let { it to v.size } }
            .toMap()

        val allCategoryIds = (spentByCategory.keys + budgets.keys).mapNotNull { it }.toSet()
        val rows = allCategoryIds.mapNotNull { cid ->
            val category = categories[cid] ?: return@mapNotNull null
            val budget = budgets[cid]
            val spent = spentByCategory[cid] ?: 0L
            if (spent == 0L && budget == null) return@mapNotNull null
            CategoryRow(
                categoryId = cid,
                name = category.name,
                colorArgb = category.colorArgb,
                netSpentCents = spent,
                transactionCount = countByCategory[cid] ?: 0,
                budgetCents = budget,
                remainingCents = budget?.let { it - spent }
            )
        }.sortedByDescending { it.netSpentCents }

        val totalBudget = budgets.values.sum()
        val available = totalBudget - rows
            .filter { it.budgetCents != null }
            .sumOf { it.netSpentCents }

        return MonthSummary(
            yearMonth = yearMonth,
            rows = rows,
            totalSpentCents = rows.sumOf { it.netSpentCents },
            totalBudgetCents = totalBudget,
            availableCents = available
        )
    }

    /**
     * Resumen del SEMESTRE: suma de los meses del semestre activo.
     * Si el semestre no tiene fechas (en realidad siempre las tiene), se usan los últimos 6 meses.
     */
    fun semesterMonths(currentMonth: YearMonth, semesterRange: Pair<LocalDate, LocalDate>?): List<YearMonth> =
        if (semesterRange != null) {
            val start = YearMonth.from(semesterRange.first)
            val end = YearMonth.from(semesterRange.second)
            generateSequence(start) { it.plusMonths(1) }
                .takeWhile { it <= end }
                .toList()
                .filter { it <= currentMonth }   // sin meses futuros
                .ifEmpty { fallbackSixMonths(currentMonth) }
        } else {
            fallbackSixMonths(currentMonth)
        }

    private fun fallbackSixMonths(currentMonth: YearMonth): List<YearMonth> =
        (0..5).reversed().map { currentMonth.minusMonths(it.toLong()) }

    /** Porcentaje con topes para la barra: null si no hay presupuesto. */
    fun percentOf(budgetCents: Long?, spentCents: Long): Float? =
        budgetCents?.takeIf { it > 0 }?.let { (spentCents.toFloat() / it.toFloat()).coerceAtLeast(0f) }
}


