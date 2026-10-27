package com.example.campuspocket.feature.academic.domain

import com.example.campuspocket.feature.academic.domain.model.CalificacionResultado
import com.example.campuspocket.feature.academic.domain.model.EstadoAcademico
import com.example.campuspocket.feature.academic.domain.model.Grade
import com.example.campuspocket.feature.academic.domain.model.TipoNota
import com.example.campuspocket.feature.academic.domain.repository.GradeRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caso de uso para calcular el estado académico y simulador de supletorio (reglas EPN).
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
@Singleton
class FinalExamSimulator @Inject constructor(
    private val gradeRepository: GradeRepository
) {

    /**
     * Calcula el estado académico completo y la nota requerida en supletorio.
     *
     * @param courseId ID de la materia
     * @return Resultado con estado, notas y cálculo de supletorio
     */
    suspend fun calcularEstado(courseId: Long): CalificacionResultado {
        val grades = gradeRepository.getGradesByCourse(courseId)
        return calcularEstado(grades)
    }

    /**
     * Calcula el estado académico a partir de una lista de calificaciones.
     */
    fun calcularEstado(grades: List<Grade>): CalificacionResultado {
        val bimestre1 = grades.find { it.tipo == TipoNota.BIMESTRE_1 }
        val bimestre2 = grades.find { it.tipo == TipoNota.BIMESTRE_2 }
        val supletorio = grades.find { it.tipo == TipoNota.SUPLETORIO }

        val nota1 = bimestre1?.score
        val nota2 = bimestre2?.score

        // Si no tiene ambas notas bimestrales, no se puede calcular estado
        if (nota1 == null || nota2 == null) {
            return CalificacionResultado(
                nota1 = nota1,
                nota2 = nota2
            )
        }

        val suma = nota1 + nota2
        val promedio = suma / 2.0

        // Determinar estado según suma
        val estado = when {
            suma >= 28.0 -> EstadoAcademico.APROBADO_DIRECTO
            suma >= 18.0 -> EstadoAcademico.SUPLETORIO
            else -> EstadoAcademico.REPROBADO
        }

        // Calcular nota requerida en supletorio
        val notaSupletorioRequerida = if (estado == EstadoAcademico.SUPLETORIO) {
            48.0 - suma
        } else {
            null
        }

        // Si ya tiene nota de supletorio, calcular nota final
        var aprobadoSupletorio = false
        var notaFinalSupletorio: Double? = null

        if (supletorio != null) {
            notaFinalSupletorio = (suma + supletorio.score) / 2.0
            aprobadoSupletorio = (notaFinalSupletorio!! >= 24.0)
        }

        return CalificacionResultado(
            nota1 = nota1,
            nota2 = nota2,
            suma = suma,
            promedio = promedio,
            estado = estado,
            notaSupletorioRequerida = notaSupletorioRequerida,
            aprobadoSupletorio = aprobadoSupletorio,
            notaFinalSupletorio = notaFinalSupletorio
        )
    }

    /**
     * Formatea el resultado para mostrar al usuario.
     */
    fun formatearResultado(resultado: CalificacionResultado): String {
        val fmt = { d: Double -> String.format(Locale.US, "%.1f", d) }
        return when (resultado.estado) {
            EstadoAcademico.APROBADO_DIRECTO -> {
                "✅ Aprobado directo\n" +
                "Nota 1: ${fmt(resultado.nota1!!)}/20\n" +
                "Nota 2: ${fmt(resultado.nota2!!)}/20\n" +
                "Suma: ${fmt(resultado.suma!!)}/40\n" +
                "Promedio: ${fmt(resultado.promedio!!)}/20"
            }
            EstadoAcademico.SUPLETORIO -> {
                val base = "⚠️ Supletorio\n" +
                    "Nota 1: ${fmt(resultado.nota1!!)}/20\n" +
                    "Nota 2: ${fmt(resultado.nota2!!)}/20\n" +
                    "Suma: ${fmt(resultado.suma!!)}/40\n" +
                    "Promedio: ${fmt(resultado.promedio!!)}/20\n" +
                    "Necesitas en supletorio: ${fmt(resultado.notaSupletorioRequerida!!)}/40"
                
                if (resultado.aprobadoSupletorio) {
                    base + "\n✅ Supletorio aprobado con ${fmt(resultado.notaFinalSupletorio!!)}/40"
                } else if (resultado.notaFinalSupletorio != null) {
                    base + "\n❌ Supletorio reprobado (${fmt(resultado.notaFinalSupletorio!!)}/40, mínimo 24)"
                } else {
                    base
                }
            }
            EstadoAcademico.REPROBADO -> {
                "❌ Reprobado (sin derecho a supletorio)\n" +
                "Nota 1: ${fmt(resultado.nota1!!)}/20\n" +
                "Nota 2: ${fmt(resultado.nota2!!)}/20\n" +
                "Suma: ${fmt(resultado.suma!!)}/40 (< 18)\n" +
                "Promedio: ${fmt(resultado.promedio!!)}/20"
            }
            null -> "Notas incompletas: ingresa Nota 1 y Nota 2 para calcular el estado"
        }
    }
}