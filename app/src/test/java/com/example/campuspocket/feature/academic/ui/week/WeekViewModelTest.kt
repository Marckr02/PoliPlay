package com.example.campuspocket.feature.academic.ui.week

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
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `la semana va de lunes a viernes`() = runTest(testDispatcher) {
        val viewModel = WeekViewModel(FakeCourseRepository())
        advanceUntilIdle()

        val days = viewModel.uiState.value.days
        assertEquals(5, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().day)
        assertEquals(DayOfWeek.FRIDAY, days.last().day)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `el sabado aparece solo cuando tiene clases`() = runTest(testDispatcher) {
        val withSaturday = WeekViewModel(
            FakeCourseRepository(
                listOf(
                    testCourse(
                        id = 1,
                        sessions = listOf(testSession(id = 1, dayOfWeek = DayOfWeek.SATURDAY))
                    )
                )
            )
        )
        advanceUntilIdle()

        val days = withSaturday.uiState.value.days
        assertEquals(6, days.size)
        assertEquals(DayOfWeek.SATURDAY, days.last().day)
    }

    @Test
    fun `el domingo nunca aparece, aunque hubiera una sesion`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 1,
                    sessions = listOf(testSession(id = 1, dayOfWeek = DayOfWeek.SUNDAY))
                )
            )
        )
        val viewModel = WeekViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.days.none { it.day == DayOfWeek.SUNDAY })
    }

    @Test
    fun `agrupa las sesiones por dia y las ordena por hora`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 1,
                    name = "Física",
                    sessions = listOf(
                        testSession(id = 1, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0)),
                        testSession(id = 2, dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(11, 0))
                    )
                ),
                testCourse(
                    id = 2,
                    name = "Cálculo",
                    sessions = listOf(
                        testSession(id = 3, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(9, 0))
                    )
                )
            )
        )
        val viewModel = WeekViewModel(repository)
        advanceUntilIdle()

        val days = viewModel.uiState.value.days
        val monday = days.first { it.day == DayOfWeek.MONDAY }
        val wednesday = days.first { it.day == DayOfWeek.WEDNESDAY }
        val tuesday = days.first { it.day == DayOfWeek.TUESDAY }

        assertEquals(listOf("Cálculo", "Física"), monday.entries.map { it.courseName })
        assertEquals(listOf("Física"), wednesday.entries.map { it.courseName })
        assertTrue(tuesday.entries.isEmpty())
    }
}
