package com.example.campuspocket.feature.academic.data

import com.example.campuspocket.feature.academic.data.repository.toDomain
import com.example.campuspocket.feature.academic.data.repository.toEntity
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class CourseMappingTest {

    @Test
    fun `entidad con sesiones pasa a dominio conservando dia y horas`() {
        val joined = CourseWithSessions(
            course = CourseEntity(
                id = 7,
                semesterId = 3,
                code = "MAT-101",
                name = "Cálculo",
                section = "A",
                teacher = "Pérez",
                colorArgb = 0xFF4C8DFF.toInt()
            ),
            sessions = listOf(
                ClassSessionEntity(id = 9, courseId = 7, dayOfWeek = 1, startMinute = 420, endMinute = 540, room = "A-1")
            )
        )

        val course = joined.toDomain()

        assertEquals(7L, course.id)
        assertEquals("MAT-101", course.code)
        assertEquals(1, course.sessions.size)
        val session = course.sessions[0]
        assertEquals(DayOfWeek.MONDAY, session.dayOfWeek)
        assertEquals(LocalTime.of(7, 0), session.startTime)
        assertEquals(LocalTime.of(9, 0), session.endTime)
        assertEquals("A-1", session.room)
    }

    @Test
    fun `dominio pasa a entidad en minutos y codigo de dia`() {
        val course = Course(id = null, code = "FIS-201", name = "Física", section = "B", teacher = null)
        val session = ClassSession(
            dayOfWeek = DayOfWeek.FRIDAY,
            startTime = LocalTime.of(14, 30),
            endTime = LocalTime.of(16, 0)
        )

        val entity = course.toEntity(semesterId = 11)
        assertEquals(0L, entity.id) // sin id = autogenerado (0 para Room)
        assertEquals(11L, entity.semesterId)
        assertEquals("FIS-201", entity.code)

        val sessionEntity = session.toEntity()
        assertEquals(5, sessionEntity.dayOfWeek)
        assertEquals(14 * 60 + 30, sessionEntity.startMinute)
        assertEquals(16 * 60, sessionEntity.endMinute)
    }
}
