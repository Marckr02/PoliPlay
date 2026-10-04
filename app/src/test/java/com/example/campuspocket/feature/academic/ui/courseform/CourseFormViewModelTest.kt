package com.example.campuspocket.feature.academic.ui.courseform

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class CourseFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: FakeCourseRepository) =
        CourseFormViewModel(
            SavedStateHandle(mapOf(AcademicDestinations.COURSE_ID_ARG to -1L)),
            repository
        )

    @Test
    fun `guardar un formulario vacio marca errores y no inserta nada`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = newViewModel(repository)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_required, state.codeError)
        assertEquals(R.string.error_required, state.nameError)
        assertEquals(R.string.error_required, state.sectionError)
        assertEquals(R.string.error_no_sessions, state.sessionsError)
        assertFalse(state.saved)
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun `hora con formato invalido se marca como error`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = newViewModel(repository)
        fillRequiredFields(viewModel)
        viewModel.addSession()

        viewModel.updateSession(
            0,
            viewModel.uiState.value.sessions[0].copy(day = DayOfWeek.MONDAY, start = "xx", end = "25:99")
        )
        viewModel.save()
        advanceUntilIdle()

        val session = viewModel.uiState.value.sessions[0]
        assertEquals(R.string.error_time_format, session.startError)
        assertEquals(R.string.error_time_format, session.endError)
        assertFalse(viewModel.uiState.value.saved)
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun `fin anterior al inicio se marca como error de orden`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = newViewModel(repository)
        fillRequiredFields(viewModel)
        viewModel.addSession()

        viewModel.updateSession(
            0,
            viewModel.uiState.value.sessions[0].copy(day = DayOfWeek.MONDAY, start = "9:00", end = "7:00")
        )
        viewModel.save()
        advanceUntilIdle()

        val session = viewModel.uiState.value.sessions[0]
        assertNull(session.startError)
        assertEquals(R.string.error_time_order, session.endError)
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun `una sesion sin dia se marca como obligatorio`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = newViewModel(repository)
        fillRequiredFields(viewModel)
        viewModel.addSession()

        viewModel.updateSession(0, viewModel.uiState.value.sessions[0].copy(start = "7:00", end = "9:00"))
        viewModel.save()
        advanceUntilIdle()

        assertEquals(R.string.error_required, viewModel.uiState.value.sessions[0].dayError)
        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun `guardado valido inserta la materia con sus sesiones`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = newViewModel(repository)
        fillRequiredFields(viewModel)
        viewModel.addSession()
        viewModel.updateSession(
            0,
            viewModel.uiState.value.sessions[0].copy(day = DayOfWeek.TUESDAY, start = "7:00", end = "9:00", room = "A-1")
        )

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertEquals(1, repository.inserted.size)
        val course = repository.inserted.first()
        assertEquals("MAT-101", course.code)
        assertEquals("Cálculo", course.name)
        assertEquals("A", course.section)
        assertEquals(1, course.sessions.size)
        assertEquals(DayOfWeek.TUESDAY, course.sessions[0].dayOfWeek)
        assertEquals(LocalTime.of(7, 0), course.sessions[0].startTime)
        assertEquals(LocalTime.of(9, 0), course.sessions[0].endTime)
        assertEquals("A-1", course.sessions[0].room)
    }

    @Test
    fun `en modo edicion carga la materia y actualiza al guardar`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 5,
                    code = "FIS-201",
                    name = "Física",
                    section = "B",
                    sessions = listOf(testSession(id = 9, dayOfWeek = DayOfWeek.FRIDAY))
                )
            )
        )
        val viewModel = CourseFormViewModel(
            SavedStateHandle(mapOf(AcademicDestinations.COURSE_ID_ARG to 5L)),
            repository
        )
        advanceUntilIdle()

        val loaded = viewModel.uiState.value
        assertTrue(loaded.isEditing)
        assertFalse(loaded.isLoading)
        assertEquals("FIS-201", loaded.code)
        assertEquals("Física", loaded.name)
        assertEquals("B", loaded.section)
        assertEquals(1, loaded.sessions.size)
        assertEquals(DayOfWeek.FRIDAY, loaded.sessions[0].day)
        assertEquals("7:00", loaded.sessions[0].start)

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertEquals(listOf(5L), repository.updated.map { it.id })
    }

    private fun fillRequiredFields(viewModel: CourseFormViewModel) {
        viewModel.onCodeChange("MAT-101")
        viewModel.onNameChange("Cálculo")
        viewModel.onSectionChange("A")
    }
}
