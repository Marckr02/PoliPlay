package com.example.campuspocket.feature.academic.domain.model

import java.time.DayOfWeek

/**
 * Días lectivos de esta universidad: lunes a viernes; el sábado solo en algunos horarios.
 * El domingo nunca tiene clases (no se ofrece al crear sesiones ni en la vista semanal).
 */
object SchoolDays {
    val weekdays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val withSaturday: List<DayOfWeek> = weekdays + DayOfWeek.SATURDAY
}
