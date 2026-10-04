package com.example.campuspocket.feature.academic.ui

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Texto de horas en formato 24 h "H:mm" (acepta "7:00" y "07:00"). Sin dependencias de Android. */
object TimeText {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

    /** Devuelve null si el texto no es una hora válida. */
    fun parse(text: String): LocalTime? =
        try {
            LocalTime.parse(text.trim(), FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }

    fun format(time: LocalTime): String = time.format(FORMATTER)
}
