package com.example.campuspocket.core.calendar

import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import com.example.campuspocket.feature.academic.testTask
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarExportManagerTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `reune el horario del semestre y las entregas pendientes`() = runTest {
        val course = testCourse(
            id = 1,
            name = "Cálculo",
            sessions = listOf(
                testSession(
                    id = 1,
                    dayOfWeek = DayOfWeek.WEDNESDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0)
                )
            )
        )
        val tasks = listOf(
            testTask(
                id = 1,
                title = "Ensayo",
                dueAt = Instant.parse("2026-10-21T15:00:00Z")
            ),
            testTask(
                id = 2,
                title = "Hecha",
                dueAt = Instant.parse("2026-10-21T09:00:00Z"),
                completedAt = Instant.parse("2026-10-21T10:00:00Z")
            )
        )
        val manager = CalendarExportManager(
            courseRepository = FakeCourseRepository(
                initial = listOf(course),
                semesterEnd = LocalDate.of(2026, 12, 31)
            ),
            taskRepository = FakeTaskRepository(tasks),
            clock = clock,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val export = manager.build()

        assertEquals(2, export.eventCount)
        assertTrue(export.content.contains("SUMMARY:Cálculo"))
        assertTrue(export.content.contains("RRULE:FREQ=WEEKLY;COUNT=11"))
        assertTrue(export.content.contains("SUMMARY:Ensayo"))
        assertFalse(export.content.contains("SUMMARY:Hecha"))
    }
}
