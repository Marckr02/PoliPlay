package com.example.campuspocket.feature.academic.ui.tasks

import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testTask
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        tasks: List<com.example.campuspocket.feature.academic.domain.model.Task> = emptyList(),
        courses: List<com.example.campuspocket.feature.academic.domain.model.Course> = emptyList()
    ): Pair<TasksViewModel, FakeTaskRepository> {
        val repository = FakeTaskRepository(tasks)
        return TasksViewModel(repository, FakeCourseRepository(courses)) to repository
    }

    @Test
    fun `pendientes ordenadas por fecha de entrega y completadas ocultas`() = runTest(testDispatcher) {
        val (viewModel, _) = viewModel(
            tasks = listOf(
                testTask(id = 1, title = "Tarde", dueAt = Instant.ofEpochMilli(2_000_000_000_000)),
                testTask(id = 2, title = "Pronto", dueAt = Instant.ofEpochMilli(1_000_000_000_000)),
                testTask(id = 3, title = "Hecha", dueAt = Instant.ofEpochMilli(500_000_000_000),
                    completedAt = Instant.ofEpochMilli(1_000_000_000_000))
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.showCompleted)
        assertEquals(listOf("Pronto", "Tarde"), state.tasks.map { it.title })
    }

    @Test
    fun `cambiar a completadas muestra solo las completadas`() = runTest(testDispatcher) {
        val (viewModel, _) = viewModel(
            tasks = listOf(
                testTask(id = 1, title = "Pendiente"),
                testTask(id = 2, title = "Hecha", completedAt = Instant.ofEpochMilli(1_000_000_000_000))
            )
        )
        advanceUntilIdle()

        viewModel.setShowCompleted(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showCompleted)
        assertEquals(listOf("Hecha"), state.tasks.map { it.title })
    }

    @Test
    fun `el filtro por materia deja solo sus tareas`() = runTest(testDispatcher) {
        val (viewModel, _) = viewModel(
            tasks = listOf(
                testTask(id = 1, courseId = 7, title = "De Física"),
                testTask(id = 2, courseId = 8, title = "De otra"),
                testTask(id = 3, courseId = null, title = "Libre")
            ),
            courses = listOf(testCourse(id = 7, name = "Física"), testCourse(id = 8, name = "Cálculo"))
        )
        advanceUntilIdle()

        viewModel.setCourseFilter(7)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("De Física"), state.tasks.map { it.title })
        assertEquals(listOf("Física", "Cálculo"), state.courses.map { it.name })

        viewModel.setCourseFilter(null)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.tasks.size)
    }

    @Test
    fun `completar marca completada y borrar llama al repositorio`() = runTest(testDispatcher) {
        val (viewModel, repository) = viewModel(tasks = listOf(testTask(id = 5, title = "Deber")))
        advanceUntilIdle()

        viewModel.setCompleted(repository.getById(5)!!, completed = true)
        advanceUntilIdle()
        assertEquals(listOf(5L), repository.completed)
        assertTrue(repository.getById(5)!!.completedAt != null)

        viewModel.delete(5)
        advanceUntilIdle()
        assertEquals(listOf(5L), repository.deleted)
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }
}
