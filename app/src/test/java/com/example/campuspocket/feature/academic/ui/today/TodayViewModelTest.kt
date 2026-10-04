package com.example.campuspocket.feature.academic.ui.today

import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 2026-10-01 es jueves.
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `muestra solo las sesiones de hoy, ordenadas por hora de inicio`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 1,
                    name = "Física",
                    sessions = listOf(
                        testSession(id = 1, dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0)),
                        testSession(id = 2, dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(9, 0))
                    )
                ),
                testCourse(
                    id = 2,
                    name = "Cálculo",
                    sessions = listOf(
                        testSession(id = 3, dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(9, 0))
                    )
                )
            )
        )
        val viewModel = TodayViewModel(repository, fixedClock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2026, 10, 1), state.date)
        assertEquals(2, state.entries.size)
        assertEquals("Cálculo", state.entries[0].courseName)
        assertEquals("Física", state.entries[1].courseName)
    }

    @Test
    fun `sin clases hoy el estado queda cargado y vacio`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 1,
                    sessions = listOf(testSession(id = 1, dayOfWeek = DayOfWeek.FRIDAY))
                )
            )
        )
        val viewModel = TodayViewModel(repository, fixedClock)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
