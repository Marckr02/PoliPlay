package com.example.campuspocket.feature.finance.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ritmo del presupuesto TOTAL del mes (Fase 8B). Lógica pura, sin Android.
 *
 * - esperadoPct = día / díasDelMes
 * - esperadoMonto = presupuesto × día / díasDelMes (half-up a centavos)
 * - realPct = gastado / presupuesto
 * - "de más" = gastado − esperadoMonto (solo si va por encima)
 *
 * Las comparaciones usan los valores SIN redondear; el redondeo es solo de presentación.
 */
data class MonthPace(
    val date: LocalDate,
    val budgetCents: Long,
    val spentCents: Long,
    val expectedCents: Long,
    /** Porcentaje esperado con 1 decimal (p. ej. 64.5). */
    val expectedPct: BigDecimal,
    /** Porcentaje real con 1 decimal (p. ej. 53.0); puede pasar de 100. */
    val realPct: BigDecimal,
    val isOver: Boolean,
    /** Monto de más respecto al ritmo esperado; 0 si va por debajo. */
    val overCents: Long
) {
    val day: Int get() = date.dayOfMonth
    val daysInMonth: Int get() = date.lengthOfMonth()

    /** Fracción para la barra: se limita a 1 aunque el gasto real supere el 100 %. */
    val barProgress: Float
        get() = (spentCents.toDouble() / budgetCents.toDouble()).coerceIn(0.0, 1.0).toFloat()

    /** Posición de la marca vertical del esperado (0..1). */
    val expectedFraction: Float
        get() = (day.toDouble() / daysInMonth.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

object BudgetPace {

    private val HUNDRED = BigDecimal.valueOf(100)

    /**
     * Devuelve null si no hay presupuesto total (ausente o <= 0): la UI oculta el
     * indicador y sugiere crearlo. Nunca divide entre cero.
     */
    fun compute(
        today: LocalDate,
        budgetCents: Long?,
        spentCents: Long
    ): MonthPace? {
        if (budgetCents == null || budgetCents <= 0L) return null

        val days = today.lengthOfMonth()
        val day = today.dayOfMonth
        val spent = spentCents.coerceAtLeast(0L)

        val expectedCents = BigDecimal.valueOf(budgetCents)
            .multiply(BigDecimal.valueOf(day.toLong()))
            .divide(BigDecimal.valueOf(days.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()

        // Comparación exacta, sin pasar por Float/Double.
        val isOver = BigDecimal.valueOf(spent)
            .multiply(BigDecimal.valueOf(days.toLong()))
            .compareTo(
                BigDecimal.valueOf(budgetCents).multiply(BigDecimal.valueOf(day.toLong()))
            ) > 0

        return MonthPace(
            date = today,
            budgetCents = budgetCents,
            spentCents = spent,
            expectedCents = expectedCents,
            expectedPct = percentOf(day.toLong(), days.toLong()),
            realPct = percentOf(spent, budgetCents),
            isOver = isOver,
            overCents = if (isOver) spent - expectedCents else 0L
        )
    }

    /** Porcentaje con 1 decimal, half-up. */
    fun percentOf(part: Long, total: Long): BigDecimal =
        BigDecimal.valueOf(part)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)

    /** Mes actual en la zona del dispositivo (helper para la UI). */
    fun currentMonth(today: LocalDate): YearMonth = YearMonth.from(today)
}
