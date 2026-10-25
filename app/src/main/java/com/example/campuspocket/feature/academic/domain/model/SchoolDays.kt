package com.example.campuspocket.feature.academic.domain.model

import java.time.DayOfWeek

/** Días de la semana para clases y eventos académicos. */
object SchoolDays {
    val weekdays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val withSaturday: List<DayOfWeek> = weekdays + DayOfWeek.SATURDAY

    val allDays: List<DayOfWeek> = withSaturday + DayOfWeek.SUNDAY
}