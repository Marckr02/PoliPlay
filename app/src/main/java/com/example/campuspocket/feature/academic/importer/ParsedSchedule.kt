package com.example.campuspocket.feature.academic.importer

import java.time.LocalDate

/** Una clase dentro de una celda de día: "7-9" en la columna Lunes -> dayOfWeek 1, 420..540. */
data class ParsedSlot(val dayOfWeek: Int, val startMinute: Int, val endMinute: Int)

/** Una fila numerada de la tabla del horario (puede traer varias sesiones, una por celda de día). */
data class ParsedScheduleRow(
    val nro: Int,
    val code: String,
    val name: String,
    val section: String,
    val room: String?,
    val credits: Int?,
    val teacher: String,
    val slots: List<ParsedSlot>
)

data class ParsedSchedule(
    val termLabel: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val rows: List<ParsedScheduleRow>,
    val warnings: List<ParseWarning>
)

/** Advertencias estructuradas: la pantalla las traduce a strings.xml. */
sealed interface ParseWarning {
    /** El PDF no tiene capa de texto (p. ej. capturado con "Microsoft Print to PDF" como imagen). */
    data object NoText : ParseWarning

    /** El PDF no es el horario del SAEw con "Guardar como PDF" (estructura no reconocida). */
    data object UnsupportedFormat : ParseWarning

    /** Fila numerada sin ningún rango horario. */
    data class RowWithoutSchedule(val nro: Int) : ParseWarning

    /** El código de materia no sigue el formato esperado (3-4 letras + 3 dígitos). */
    data class UnexpectedCode(val nro: Int, val code: String) : ParseWarning
}
