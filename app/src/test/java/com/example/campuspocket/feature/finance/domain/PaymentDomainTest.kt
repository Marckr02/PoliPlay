package com.example.campuspocket.feature.finance.domain

import com.example.campuspocket.feature.finance.data.Frequency
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pruebas puras del dominio de pagos: recurrencias, deriva de fin de mes,
 * proyecciones y qué notificar (spec 7.3, 7.4, 7.6).
 */
class PaymentDomainTest {

    private fun pay(
        id: Long = 1,
        name: String = "Pago",
        amountCents: Long = 1_000,
        frequency: String = Frequency.MONTHLY.name,
        nextDueDate: LocalDate,
        anchorDayOfMonth: Int = 0,
        remindDaysBefore: Int = 3,
        active: Boolean = true
    ) = ScheduledPaymentEntity(
        id = id,
        name = name,
        amountCents = amountCents,
        accountId = 1,
        categoryId = 1,
        frequency = frequency,
        nextDueDate = nextDueDate,
        anchorDayOfMonth = anchorDayOfMonth,
        remindDaysBefore = remindDaysBefore,
        active = active
    )

    @Test
    fun `semanal avanza por siete dias sin deriva`() {
        val p = pay(frequency = Frequency.WEEKLY.name, nextDueDate = LocalDate.of(2026, 1, 31))
        val next = PaymentDomain.nextDueAfter(p, p.nextDueDate)
        assertEquals(LocalDate.of(2026, 2, 7), next)
    }

    @Test
    fun `31 de enero mensual febrero cae en 28 y marzo vuelve al 31`() {
        val jan31 = pay(nextDueDate = LocalDate.of(2026, 1, 31), anchorDayOfMonth = 31)
        val feb = PaymentDomain.nextDueAfter(jan31, jan31.nextDueDate)
        assertEquals(LocalDate.of(2026, 2, 28), feb) // 2026 no es bisiesto
        // y el siguiente vuelve al 31
        val febEntity = jan31.copy(nextDueDate = feb)
        assertEquals(LocalDate.of(2026, 3, 31), PaymentDomain.nextDueAfter(febEntity, feb))
    }

    @Test
    fun `anio bisiesto 29 de febrero y no bisiesto cae al 28 y se recupera al 29`() {
        val feb29_2028 = pay(frequency = Frequency.YEARLY.name, nextDueDate = LocalDate.of(2028, 2, 29), anchorDayOfMonth = 29)
        val feb2029 = PaymentDomain.nextDueAfter(feb29_2028, feb29_2028.nextDueDate)
        assertEquals(LocalDate.of(2029, 2, 28), feb2029) // 2029 no bisiesto
        val febEntity = feb29_2028.copy(nextDueDate = feb2029)
        assertEquals(LocalDate.of(2030, 2, 28), PaymentDomain.nextDueAfter(febEntity, feb2029))
        // y recupera el 29 en el siguiente bisiesto
        val to2032 = feb29_2028.copy(nextDueDate = LocalDate.of(2031, 2, 28))
        assertEquals(LocalDate.of(2032, 2, 29), PaymentDomain.nextDueAfter(to2032, to2032.nextDueDate))
    }

    @Test
    fun `pagos vencidos cuentan como pendientes en balance proyectado`() {
        val endOfMonth = LocalDate.of(2026, 10, 31)
        val pending = listOf(
            pay(id = 1, nextDueDate = LocalDate.of(2026, 10, 2)), // vencido
            pay(id = 2, nextDueDate = LocalDate.of(2026, 10, 20)) // futuro (dentro del mes)
        )
        val projected = PaymentDomain.projectedBalance(100_000, pending, endOfMonth)
        // balance actual - (1000 + 1000) = 98_000
        assertEquals(98_000L, projected)
    }

    @Test
    fun `gasto proyectado suma los pendientes que vencen antes de fin de mes`() {
        val month = YearMonth.of(2026, 10)
        val pending = listOf(
            pay(id = 1, nextDueDate = LocalDate.of(2026, 10, 10)),
            pay(id = 2, nextDueDate = LocalDate.of(2026, 11, 1)) // fuera del mes
        )
        val projected = PaymentDomain.projectedSpent(5_000, pending, month)
        assertEquals(6_000, projected)
    }

    @Test
    fun `upcomingPayments ordena vencidos primero y limita a 5`() {
        val today = LocalDate.of(2026, 10, 4)
        val list = listOf(
            pay(id = 3, nextDueDate = LocalDate.of(2026, 10, 20)),
            pay(id = 1, nextDueDate = LocalDate.of(2026, 10, 2)), // vencido
            pay(id = 2, nextDueDate = LocalDate.of(2026, 10, 5)),
            pay(id = 4, nextDueDate = LocalDate.of(2026, 10, 19)),
            pay(id = 5, nextDueDate = LocalDate.of(2026, 10, 18)),
            pay(id = 6, nextDueDate = LocalDate.of(2026, 10, 17))
        )
        val upcoming = PaymentDomain.upcomingPayments(list, today)
        assertEquals(listOf(1L, 2L, 6L, 5L, 4L), upcoming.map { it.id })
    }

    @Test
    fun `paymentsToNotify solo dentro de la antelacion y activos`() {
        val today = LocalDate.of(2026, 10, 4)
        val list = listOf(
            pay(id = 1, nextDueDate = today.minusDays(1)),             // vencido
            pay(id = 2, nextDueDate = today),                          // hoy
            pay(id = 3, nextDueDate = today.plusDays(2)),              // dentro de 3 días
            pay(id = 4, nextDueDate = today.plusDays(5)),              // fuera
            pay(id = 5, nextDueDate = today, active = false)           // inactivo
        )
        val toNotify = PaymentDomain.paymentsToNotify(list, today)
        assertEquals(listOf(1L, 2L, 3L), toNotify.map { it.id })
    }
}
