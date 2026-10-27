package com.example.campuspocket.feature.academic.domain

import com.example.campuspocket.feature.academic.FakeGradeRepository
import com.example.campuspocket.feature.academic.domain.model.CalificacionResultado
import com.example.campuspocket.feature.academic.domain.model.EstadoAcademico
import com.example.campuspocket.feature.academic.domain.model.Grade
import com.example.campuspocket.feature.academic.domain.model.TipoNota
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalExamSimulatorTest {

    private val simulator = FinalExamSimulator(FakeGradeRepository())

    @Test
    fun `aprobado directo con suma mayor o igual a 28`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 15.0),
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_2, score = 14.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertEquals(EstadoAcademico.APROBADO_DIRECTO, resultado.estado)
        assertEquals(29.0, resultado.suma!!, 0.01)
        assertEquals(14.5, resultado.promedio!!, 0.01)
        assertFalse(resultado.aprobadoSupletorio)
        assertTrue(simulator.formatearResultado(resultado).contains("Aprobado directo"))
    }

    @Test
    fun `supletorio con suma entre 18 y 27`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 10.0),
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_2, score = 8.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertEquals(EstadoAcademico.SUPLETORIO, resultado.estado)
        assertEquals(18.0, resultado.suma!!, 0.01)
        assertEquals(9.0, resultado.promedio!!, 0.01)
        assertEquals(30.0, resultado.notaSupletorioRequerida!!, 0.01) // 48 - 18 = 30
        assertFalse(resultado.aprobadoSupletorio)
    }

    @Test
    fun `reprobado con suma menor a 18`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 5.0),
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_2, score = 6.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertEquals(EstadoAcademico.REPROBADO, resultado.estado)
        assertEquals(11.0, resultado.suma!!, 0.01)
        assertNull(resultado.notaSupletorioRequerida)
    }

    @Test
    fun `supletorio aprobado con nota final mayor o igual a 24`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 10.0),
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_2, score = 10.0),
            Grade(courseId = 1, tipo = TipoNota.SUPLETORIO, score = 30.0, maxScore = 40.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertEquals(EstadoAcademico.SUPLETORIO, resultado.estado)
        assertEquals(20.0, resultado.suma!!, 0.01)
        assertEquals(28.0, resultado.notaSupletorioRequerida!!, 0.01) // 48 - 20 = 28
        assertTrue(resultado.aprobadoSupletorio)
        assertEquals(25.0, resultado.notaFinalSupletorio!!, 0.01) // (20 + 30) / 2 = 25
    }

    @Test
    fun `supletorio reprobado con nota final menor a 24`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 10.0),
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_2, score = 10.0),
            Grade(courseId = 1, tipo = TipoNota.SUPLETORIO, score = 20.0, maxScore = 40.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertEquals(EstadoAcademico.SUPLETORIO, resultado.estado)
        assertFalse(resultado.aprobadoSupletorio)
        assertEquals(20.0, resultado.notaFinalSupletorio!!, 0.01) // (20 + 20) / 2 = 20
    }

    @Test
    fun `notas incompletas sin calcular estado`() = runTest {
        val grades = listOf(
            Grade(courseId = 1, tipo = TipoNota.BIMESTRE_1, score = 15.0)
        )
        val repo = FakeGradeRepository(grades)
        val simulator = FinalExamSimulator(repo)

        val resultado = simulator.calcularEstado(grades)

        assertNull(resultado.estado)
        assertEquals(15.0, resultado.nota1!!, 0.01)
        assertNull(resultado.nota2)
    }

    @Test
    fun `formatear aprobado directo`() = runTest {
        val resultado = CalificacionResultado(
            nota1 = 15.0,
            nota2 = 14.0,
            suma = 29.0,
            promedio = 14.5,
            estado = EstadoAcademico.APROBADO_DIRECTO
        )
        val formatted = simulator.formatearResultado(resultado)
        assertTrue(formatted.contains("Aprobado directo"))
        assertTrue(formatted.contains("15.0"))
        assertTrue(formatted.contains("14.0"))
        assertTrue(formatted.contains("29.0"))
        assertTrue(formatted.contains("14.5"))
    }

    @Test
    fun `formatear supletorio`() = runTest {
        val resultado = CalificacionResultado(
            nota1 = 10.0,
            nota2 = 8.0,
            suma = 18.0,
            promedio = 9.0,
            estado = EstadoAcademico.SUPLETORIO,
            notaSupletorioRequerida = 30.0
        )
        val formatted = simulator.formatearResultado(resultado)
        assertTrue(formatted.contains("Supletorio"))
        assertTrue(formatted.contains("30.0"))
        assertTrue(formatted.contains("10.0"))
        assertTrue(formatted.contains("8.0"))
        assertTrue(formatted.contains("18.0"))
        assertTrue(formatted.contains("9.0"))
    }

    @Test
    fun `formatear reprobado`() = runTest {
        val resultado = CalificacionResultado(
            nota1 = 5.0,
            nota2 = 6.0,
            suma = 11.0,
            promedio = 5.5,
            estado = EstadoAcademico.REPROBADO
        )
        val formatted = simulator.formatearResultado(resultado)
        assertTrue(formatted.contains("Reprobado"))
        assertTrue(formatted.contains("11.0"))
        assertTrue(formatted.contains("5.0"))
        assertTrue(formatted.contains("6.0"))
        assertTrue(formatted.contains("5.5"))
    }
}