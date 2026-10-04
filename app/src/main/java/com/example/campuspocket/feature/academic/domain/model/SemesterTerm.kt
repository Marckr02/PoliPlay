package com.example.campuspocket.feature.academic.domain.model

import java.time.LocalDate

/**
 * Derivación determinista del semestre "actual" a partir de una fecha:
 * enero-junio -> "AAAA-1" y julio-diciembre -> "AAAA-2".
 * Sirve para crear el semestre activo la primera vez que se registra una materia
 * (el horario manual no tiene gestión de semestres en esta fase).
 */
object SemesterTerm {

    data class Term(val name: String, val start: LocalDate, val end: LocalDate)

    fun forDate(date: LocalDate): Term {
        val year = date.year
        return if (date.monthValue <= 6) {
            Term(name = "$year-1", start = LocalDate.of(year, 1, 1), end = LocalDate.of(year, 6, 30))
        } else {
            Term(name = "$year-2", start = LocalDate.of(year, 7, 1), end = LocalDate.of(year, 12, 31))
        }
    }
}
