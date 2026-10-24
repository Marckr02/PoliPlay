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

    /** Días que quedan del mes, incluido hoy (v5 §2.3). */
    val daysRemaining: Int
        get() = daysInMonth - day + 1

    /** Presupuesto restante repartido entre los días que quedan; 0 si ya se agotó. */
    val perDayCents: Long
        get() = BudgetPace.perDayCents(budgetCents, spentCents, daysRemaining)

    /**
     * Cierre del mes al ritmo actual. Antes del día 3 es una extrapolación poco
     * fiable y la UI muestra "Aún faltan datos" (v5 §2.3).
     */
    val estimatedClosingCents: Long
        get() = BudgetPace.estimatedClosingCents(spentCents, day, daysInMonth)

    /** El cierre estimado solo se considera fiable a partir del día 3. */
    val hasEnoughDataForClosing: Boolean
        get() = day >= MIN_DAYS_FOR_CLOSING

    /** Ya se gastó el presupuesto completo (o más). */
    val isBudgetExhausted: Boolean
        get() = spentCents >= budgetCents

    /** El cierre estimado terminaría por encima del presupuesto. */
    val isClosingOverBudget: Boolean
        get() = estimatedClosingCents > budgetCents

    companion object {
        const val MIN_DAYS_FOR_CLOSING = 3
    }
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

    /**
     * Presupuesto restante por día, redondeado a centavos con HALF_UP.
     * Incluye hoy entre los días restantes. Nunca divide entre cero y devuelve
     * 0 cuando el presupuesto ya se agotó.
     */
    fun perDayCents(budgetCents: Long, spentCents: Long, daysRemaining: Int): Long {
        if (daysRemaining <= 0) return 0L
        val remaining = (budgetCents - spentCents).coerceAtLeast(0L)
        return BigDecimal.valueOf(remaining)
            .divide(BigDecimal.valueOf(daysRemaining.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Cierre estimado = gastado / día actual × días del mes, redondeado a
     * centavos con HALF_UP (v5 §2.3).
     */
    fun estimatedClosingCents(spentCents: Long, day: Int, daysInMonth: Int): Long {
        if (day <= 0) return 0L
        return BigDecimal.valueOf(spentCents.coerceAtLeast(0L))
            .multiply(BigDecimal.valueOf(daysInMonth.toLong()))
            .divide(BigDecimal.valueOf(day.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /** Porcentaje con 1 decimal, half-up. */
    fun percentOf(part: Long, total: Long): BigDecimal =
        BigDecimal.valueOf(part)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)

    /** Mes actual en la zona del dispositivo (helper para la UI). */
    fun currentMonth(today: LocalDate): YearMonth = YearMonth.from(today)
}
