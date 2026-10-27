package com.example.campuspocket.feature.academic.domain.model

import java.time.Instant

/**
 * Calificación de una evaluación dentro de una materia (sistema EPN).
 * 
 * Reglas EPN:
 * - Nota 1 (Primer Bimestre): sobre 20 puntos
 * - Nota 2 (Segundo Bimestre): sobre 20 puntos
 * - Suma = Nota_1 + Nota_2 (máx 40)
 * - Promedio = (Nota_1 + Nota_2) / 2
 * - Aprobado directo: Suma >= 28
 * - Supletorio: 18 <= Suma < 28
 * - Reprobado: Suma < 18
 * - Examen supletorio: sobre 40 puntos
 * - Nota final supletorio = (Nota_1 + Nota_2 + Nota_Suple) / 2
 * - Para aprobar supletorio: Nota final supletorio >= 24/40
 * - Nota requerida en supletorio = 48 - (Nota_1 + Nota_2)
 */
data class Grade(
    val id: Long? = null,
    val courseId: Long,
    val tipo: TipoNota,
    val score: Double,          // Nota obtenida
    val maxScore: Double = 20.0, // Nota máxima (20 para bimestres, 40 para supletorio)
    val date: Instant = Instant.now()
) {
    companion object {
        fun fromEntity(entity: com.example.campuspocket.feature.academic.data.GradeEntity): Grade {
            return Grade(
                id = if (entity.id == 0L) null else entity.id,
                courseId = entity.courseId,
                tipo = TipoNota.valueOf(entity.tipo),
                score = entity.score,
                maxScore = entity.maxScore,
                date = Instant.ofEpochMilli(entity.date)
            )
        }
    }

    fun toEntity(): com.example.campuspocket.feature.academic.data.GradeEntity {
        return com.example.campuspocket.feature.academic.data.GradeEntity(
            id = id ?: 0,
            courseId = courseId,
            tipo = tipo.name,
            score = score,
            maxScore = maxScore,
            date = date.toEpochMilli()
        )
    }
}

enum class TipoNota {
    BIMESTRE_1,     // Primer bimestre (sobre 20)
    BIMESTRE_2,     // Segundo bimestre (sobre 20)
    SUPLETORIO      // Examen supletorio (sobre 40)
}

/** Estado académico de una materia según reglas EPN. */
enum class EstadoAcademico {
    APROBADO_DIRECTO,   // Suma >= 28
    SUPLETORIO,         // 18 <= Suma < 28
    REPROBADO           // Suma < 18
}

/** Resultado del cálculo de estado y supletorio. */
data class CalificacionResultado(
    val nota1: Double? = null,           // Nota 1er bimestre (0-20)
    val nota2: Double? = null,           // Nota 2do bimestre (0-20)
    val suma: Double? = null,            // Nota_1 + Nota_2 (0-40)
    val promedio: Double? = null,        // (Nota_1 + Nota_2) / 2
    val estado: EstadoAcademico? = null, // Estado actual
    val notaSupletorioRequerida: Double? = null, // Puntos necesarios en supletorio (0-40)
    val aprobadoSupletorio: Boolean = false,      // Si ya tiene supletorio y aprobó
    val notaFinalSupletorio: Double? = null       // Nota final si hizo supletorio
)