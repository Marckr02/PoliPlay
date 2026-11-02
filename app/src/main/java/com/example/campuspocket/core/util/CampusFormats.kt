package com.example.campuspocket.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formateador unificado de fechas, horas, moneda y porcentajes (v10 D5)
 * utilizando el locale español de Ecuador (es-EC).
 */
object CampusFormats {
    val LOCALE_EC: Locale = Locale.Builder().setLanguage("es").setRegion("EC").build()

    fun dayLong(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LOCALE_EC))

    fun dayShort(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE d MMM", LOCALE_EC))

    fun monthYear(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_EC))

    fun time(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm", LOCALE_EC))

    fun dateTime(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", LOCALE_EC))

    fun money(cents: Long): String =
        Money.format(cents, locale = LOCALE_EC)

    fun percent(fraction: Float): String =
        String.format(LOCALE_EC, "%.1f", fraction * 100f).replace('.', ',')

    /** Recibe un porcentaje ya multiplicado por 100 (p. ej. 4.3 = "4,3"). */
    fun percentValue(percent: java.math.BigDecimal): String =
        String.format(LOCALE_EC, "%.1f", percent.setScale(1, java.math.RoundingMode.HALF_UP))
            .replace('.', ',')
}
