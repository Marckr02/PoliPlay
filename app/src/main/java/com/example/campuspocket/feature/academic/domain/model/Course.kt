package com.example.campuspocket.feature.academic.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Materia de un semestre. `id == null` significa "aún no guardada".
 * Las sesiones se editan junto con la materia (horario manual).
 */
data class Course(
    val id: Long? = null,
    val code: String,
    val name: String,
    val section: String,
    val teacher: String? = null,
    val colorArgb: Int = DEFAULT_COLOR,
    val sessions: List<ClassSession> = emptyList()
) {
    companion object {
        const val DEFAULT_COLOR = 0xFF4C8DFF.toInt()
    }
}

/** Bloque de clase semanal de una materia. Los tiempos son por día y hora (sin fecha). */
data class ClassSession(
    val id: Long? = null,
    val courseId: Long? = null,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null
)
