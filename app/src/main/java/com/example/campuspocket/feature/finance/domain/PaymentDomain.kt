package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.Frequency
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.domain.model.Account
import com.example.campuspocket.feature.finance.domain.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

/**
 * Dominio puro de pagos programados (spec 7.4 + 7.6): recurrencias, proyecciones,
 * notificaciones y deriva de fin de mes. Todo en JVM (sin Android).
 */
object PaymentDomain {

    /** Día de mes anclado al crear (1-31); si 0, se usa el del nextDueDate. */
    fun anchorDay(payment: ScheduledPaymentEntity): Int =
        if (payment.anchorDayOfMonth > 0) payment.anchorDayOfMonth else payment.nextDueDate.dayOfMonth

    /**
     * Avanza la fecha de vencimiento manteniendo el día original (sin deriva de fin de mes).
     * Si el siguiente mes es más corto, se clampa al último día real de ese mes; al siguiente se recupera.
     */
    fun nextDueAfter(payment: ScheduledPaymentEntity, from: LocalDate): LocalDate {
        val freq = Frequency.valueOf(payment.frequency)
        // Semanal: no hay deriva de fin de mes, se avanza por semana simple.
        if (freq == Frequency.WEEKLY) return payment.nextDueDate.plusWeeks(1)
        val day = anchorDay(payment)
        val freqStep = if (freq == Frequency.MONTHLY) payment.nextDueDate.plusMonths(1) else payment.nextDueDate.plusYears(1)
        return clampDay(freqStep, day)
    }

    /** Las próximas recurrencias de `nextDueDate` dentro del rango [start, end] (inclusive). */
    fun occurrencesIn(
        payment: ScheduledPaymentEntity,
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {
        if (payment.nextDueDate > end || !payment.active) return emptyList()
        val result = mutableListOf<LocalDate>()
        var current = clampToWindowStart(payment, start)
        while (current <= end) {
            if (current >= start) result += current
            current = addFrequency(current, Frequency.valueOf(payment.frequency), anchorDay(payment))
        }
        return result
    }

    /**
     * balance proyectado = balance actual − pagos PENDIENTES (vencidos sin pagar cuentan como pendientes).
     */
    fun projectedBalance(balanceCents: Long, pendingPayments: List<ScheduledPaymentEntity>, at: LocalDate): Long =
        balanceCents - pendingPayments.filter { it.nextDueDate <= at }.sumOf { it.amountCents }

    /** gasto proyectado del mes = gasto neto del mes + pagos pendientes que vencen antes de fin de mes. */
    fun projectedSpent(
        netSpentCents: Long,
        pendingPayments: List<ScheduledPaymentEntity>,
        month: YearMonth
    ): Long {
        val end = month.atEndOfMonth()
        return netSpentCents + pendingPayments.filter { it.nextDueDate <= end }.sumOf { it.amountCents }
    }

    /** Los próximos 5 pagos por vencer (incluye vencidos sin pagar, primero). */
    fun upcomingPayments(
        payments: List<ScheduledPaymentEntity>,
        at: LocalDate,
        limit: Int = 5
    ): List<ScheduledPaymentEntity> = payments
        .filter { it.active }
        .sortedWith(compareBy({ it.nextDueDate }, { it.id }))
        .take(limit)

    /**
     * Qué pagos notificar hoy: vencen hoy o dentro de "recordar N días antes" (spec 9).
     * Devuelve cada pago como mucho una vez al día (la UI guarda la última fecha).
     */
    fun paymentsToNotify(
        payments: List<ScheduledPaymentEntity>,
        today: LocalDate
    ): List<ScheduledPaymentEntity> = payments
        .filter { it.active }
        .filter { it.nextDueDate <= today.plusDays(it.remindDaysBefore.toLong()) }
        .sortedBy { it.nextDueDate }

    /** Todos los pagos que vencen en un día concreto dentro de un mes (para el calendario). */
    fun paymentsDueOn(
        payments: List<ScheduledPaymentEntity>,
        month: YearMonth,
        day: LocalDate
    ): List<ScheduledPaymentEntity> = payments
        .filter { it.active }
        .filter { occurrencesIn(it, month.atDay(1), month.atEndOfMonth()).contains(day) }

    /**
     * Balance estimado al cierre de un día del mes:
     * - día <= hoy: balance REAL acumulado (aplicar transacciones reales a saldos iniciales).
     * - día > hoy: balance actual − pagos que vencen hasta ese día.
     */
    fun dailyBalances(
        accounts: List<com.example.campuspocket.feature.finance.domain.model.Account>,
        transactions: List<Transaction>,
        payments: List<ScheduledPaymentEntity>,
        month: YearMonth,
        today: LocalDate
    ): Map<LocalDate, Long> {
        val balances = mutableMapOf<Long, Long>()
        accounts.filter { it.includeInTotal }.forEach { balances[it.id ?: 0L] = it.initialBalanceCents }

        val dayCount = month.atEndOfMonth().dayOfMonth
        val result = LinkedHashMap<LocalDate, Long>(dayCount)

        (1..dayCount).forEach { day ->
            val date = month.atDay(day)
            // aplicar las transacciones reales de ese día
            transactions.filter { it.date == date }.forEach { tx ->
                val deltaOut = when (tx.type.name) {
                    "EXPENSE", "TRANSFER" -> -tx.amountCents
                    "INCOME", "REFUND" -> tx.amountCents
                    else -> 0L
                }
                if (tx.accountId in balances) {
                    balances[tx.accountId] = balances[tx.accountId]!! + deltaOut
                }
                // entrada de transferencia en destino
                if (tx.type.name == "TRANSFER" && tx.targetAccountId != null && tx.targetAccountId in balances) {
                    balances[tx.targetAccountId!!] = balances[tx.targetAccountId]!! + tx.amountCents
                }
            }
            if (date <= today) {
                result[date] = balances.values.sum()
            } else {
                // futuro: resto los pagos que vencen exactamente ese día
                val dueToday = payments.filter { it.active }.flatMap { p ->
                    occurrencesIn(p, month.atDay(1), month.atEndOfMonth()).filter { it == date }.map { p to it }
                }.sumOf { (p, _) -> p.amountCents }
                result[date] = (result[date.minusDays(1)] ?: balances.values.sum()) - dueToday
            }
        }
        return result
    }

    // ---- internos ----------------------------------------------------------

    private fun addFrequency(date: LocalDate, frequency: Frequency, anchorDay: Int? = null): LocalDate =
        when (frequency) {
            Frequency.WEEKLY -> {
                // Semanal: no hay deriva de fin de mes; se avanza por semana.
                date.plusWeeks(1)
            }
            Frequency.MONTHLY -> {
                val next = date.plusMonths(1)
                if (anchorDay != null) clampDay(next, anchorDay) else next
            }
            Frequency.YEARLY -> {
                val next = date.plusYears(1)
                if (anchorDay != null) clampDay(next, anchorDay) else next
            }
        }

    private fun clampDay(date: LocalDate, dayOfMonth: Int): LocalDate {
        val max = date.lengthOfMonth()
        return if (dayOfMonth > max) date.withDayOfMonth(max) else date.withDayOfMonth(dayOfMonth)
    }

    private fun clampToWindowStart(payment: ScheduledPaymentEntity, start: LocalDate): LocalDate {
        if (payment.nextDueDate >= start) return payment.nextDueDate
        // avanzar hasta entrar en la ventana
        var current = payment.nextDueDate
        while (current < start) {
            current = addFrequency(current, Frequency.valueOf(payment.frequency), anchorDay(payment))
        }
        return current
    }
}
