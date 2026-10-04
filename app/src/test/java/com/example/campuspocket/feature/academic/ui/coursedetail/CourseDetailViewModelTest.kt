package com.example.campuspocket.feature.academic.ui.coursedetail

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testSession
import com.example.campuspocket.feature.academic.testTask
import com.example.campuspocket.feature.notes.FakeNoteRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(
        courseId: Long,
        repository: FakeCourseRepository,
        taskRepository: FakeTaskRepository = FakeTaskRepository(),
        noteRepository: FakeNoteRepository = FakeNoteRepository()
    ) = CourseDetailViewModel(
        SavedStateHandle(mapOf(AcademicDestinations.COURSE_ID_ARG to courseId)),
        repository,
        taskRepository,
        noteRepository
    )

    @Test
    fun `carga la materia con las sesiones ordenadas por dia y hora`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(
            listOf(
                testCourse(
                    id = 7,
                    code = "FIS-201",
                    name = "Física",
                    section = "B",
                    teacher = "Pérez",
                    sessions = listOf(
                        testSession(id = 1, dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0)),
                        testSession(id = 2, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(9, 0))
                    )
                )
            )
        )
        val viewModel = viewModelFor(7, repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailUiState.Content)
        val course = (state as CourseDetailUiState.Content).course
        assertEquals("FIS-201", course.code)
        assertEquals("Física", course.name)
        assertEquals("B", course.section)
        assertEquals("Pérez", course.teacher)
        assertEquals(DayOfWeek.MONDAY, course.sessions[0].dayOfWeek)
        assertEquals(DayOfWeek.THURSDAY, course.sessions[1].dayOfWeek)
    }

    @Test
    fun `una materia inexistente marca el estado no encontrada`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository()
        val viewModel = viewModelFor(99, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CourseDetailUiState.NotFound)
    }

    @Test
    fun `borrar elimina la materia y avisa a la pantalla`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(listOf(testCourse(id = 7)))
        val viewModel = viewModelFor(7, repository)
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        assertEquals(listOf(7L), repository.deleted)
        assertTrue(viewModel.deleted.value)
    }

    @Test
    fun `el detalle trae las tareas de la materia ordenadas por entrega`() = runTest(testDispatcher) {
        val repository = FakeCourseRepository(listOf(testCourse(id = 7, name = "Física")))
        val taskRepository = FakeTaskRepository(
            listOf(
                testTask(id = 1, courseId = 7, title = "Tarde", dueAt = java.time.Instant.ofEpochMilli(2_000_000_000_000)),
                testTask(id = 2, courseId = 7, title = "Pronto", dueAt = java.time.Instant.ofEpochMilli(1_000_000_000_000)),
                testTask(id = 3, courseId = 8, title = "De otra materia")
            )
        )
        val viewModel = viewModelFor(7, repository, taskRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CourseDetailUiState.Content)
        assertEquals(
            listOf("Pronto", "Tarde"),
            (state as CourseDetailUiState.Content).tasks.map { it.title }
        )
    }
}
