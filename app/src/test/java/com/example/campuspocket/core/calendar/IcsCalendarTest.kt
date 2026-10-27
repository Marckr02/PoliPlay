package com.example.campuspocket.core.calendar

import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import com.example.campuspocket.feature.academic.testTask
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsCalendarTest {

    private val today = LocalDate.of(2026, 10, 21) // miércoles
    private val now = Instant.parse("2026-10-21T12:00:00Z")
    private val zone = ZoneOffset.UTC

    @Test
    fun `las clases se repiten semanalmente hasta el fin del semestre`() {
        val course = testCourse(
            id = 1,
            name = "Cálculo",
            code = "MAT-101",
            section = "A",
            teacher = "Ana",
            sessions = listOf(
                testSession(
                    id = 4,
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(7, 0),
                    endTime = LocalTime.of(9, 0),
                    room = "Aula 3"
                )
            )
        )

        val export = IcsCalendar.build(
            courses = listOf(course),
            tasks = emptyList(),
            semesterEnd = LocalDate.of(2026, 12, 31),
            today = today,
            zone = zone,
            now = now
        )

        assertEquals(1, export.eventCount)
        val content = export.content
        assertTrue(content.contains("BEGIN:VEVENT"))
        assertTrue(content.contains("DTSTART:20261026T070000"))
        assertTrue(content.contains("DTEND:20261026T090000"))
        assertTrue(content.contains("RRULE:FREQ=WEEKLY;COUNT=10"))
        assertTrue(content.contains("SUMMARY:Cálculo"))
        assertTrue(content.contains("LOCATION:Aula 3"))
        assertTrue(content.contains("DESCRIPTION:Código: MAT-101\\nSección: A\\nDocente: Ana"))
    }

    @Test
    fun `los textos escapan comas punto y coma y saltos de linea`() {
        val course = testCourse(
            id = 2,
            name = "Álgebra, lineal; avanzada",
            section = "B",
            sessions = listOf(
                testSession(
                    id = 1,
                    dayOfWeek = DayOfWeek.THURSDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0),
                    room = "Aula 3, piso 2"
                )
            )
        )

        val content = IcsCalendar.build(
            courses = listOf(course),
            tasks = emptyList(),
            semesterEnd = LocalDate.of(2026, 11, 30),
            today = today,
            zone = zone,
            now = now
        ).content

        assertTrue(content.contains("SUMMARY:Álgebra\\, lineal\\; avanzada"))
        assertTrue(content.contains("LOCATION:Aula 3\\, piso 2"))
    }

    @Test
    fun `solo exporta entregas pendientes de hoy en adelante con 30 minutos`() {
        val tasks = listOf(
            testTask(
                id = 1,
                title = "Ensayo",
                dueAt = Instant.parse("2026-10-21T23:50:00Z")
            ),
            testTask(
                id = 2,
                title = "Mañana",
                dueAt = Instant.parse("2026-10-22T08:00:00Z")
            ),
            testTask(
                id = 3,
                title = "Ayer",
                dueAt = Instant.parse("2026-10-20T08:00:00Z")
            ),
            testTask(
                id = 4,
                title = "Hecha",
                dueAt = Instant.parse("2026-10-21T09:00:00Z"),
                completedAt = Instant.parse("2026-10-21T10:00:00Z")
            )
        )

        val export = IcsCalendar.build(
            courses = emptyList(),
            tasks = tasks,
            semesterEnd = null,
            today = today,
            zone = zone,
            now = now
        )

        assertEquals(2, export.eventCount)
        val content = export.content
        assertTrue(content.contains("SUMMARY:Ensayo"))
        assertTrue(content.contains("DTSTART:20261021T235000"))
        assertTrue(content.contains("DTEND:20261022T002000"))
        assertTrue(content.contains("SUMMARY:Mañana"))
        assertFalse(content.contains("SUMMARY:Ayer"))
        assertFalse(content.contains("SUMMARY:Hecha"))
    }

    @Test
    fun `un calendario vacio sigue siendo valido`() {
        val export = IcsCalendar.build(
            courses = emptyList(),
            tasks = emptyList(),
            semesterEnd = null,
            today = today,
            zone = zone,
            now = now
        )

        assertEquals(0, export.eventCount)
        assertTrue(export.content.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(export.content.endsWith("END:VCALENDAR\r\n"))
        assertFalse(export.content.contains("BEGIN:VEVENT"))
    }

    @Test
    fun `las lineas largas se pliegan sin superar 75 octetos`() {
        val course = testCourse(
            id = 3,
            name = "Introducción a la programación, algoritmos y estructuras de datos",
            sessions = listOf(testSession(id = 1, dayOfWeek = DayOfWeek.FRIDAY))
        )

        val content = IcsCalendar.build(
            courses = listOf(course),
            tasks = emptyList(),
            semesterEnd = LocalDate.of(2026, 12, 31),
            today = today,
            zone = zone,
            now = now
        ).content

        assertTrue(content.contains("\r\n "))
        content.split("\r\n")
            .filter { it.isNotEmpty() }
            .forEach { line ->
                assertTrue(
                    "Línea de ${line.toByteArray(Charsets.UTF_8).size} octetos: $line",
                    line.toByteArray(Charsets.UTF_8).size <= 75
                )
            }
    }
}
