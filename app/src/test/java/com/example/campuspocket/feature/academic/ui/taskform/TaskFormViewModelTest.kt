package com.example.campuspocket.feature.academic.ui.taskform

import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.feature.academic.FakeCourseRepository
import com.example.campuspocket.feature.academic.FakeTaskRepository
import com.example.campuspocket.feature.academic.testCourse
import com.example.campuspocket.feature.academic.testTask
import com.example.campuspocket.feature.academic.domain.model.Priority
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {

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
        taskId: Long? = null,
        taskRepository: FakeTaskRepository = FakeTaskRepository(),
        courseRepository: FakeCourseRepository = FakeCourseRepository(),
        noteRepository: com.example.campuspocket.feature.notes.FakeNoteRepository = com.example.campuspocket.feature.notes.FakeNoteRepository()
    ) = TaskFormViewModel(
        SavedStateHandle(mapOf(AcademicDestinations.TASK_ID_ARG to (taskId ?: -1L))),
        taskRepository,
        noteRepository,
        courseRepository
    )

    @Test
    fun `formulario nuevo con fecha mañana y hora 8 por defecto`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertEquals(LocalDate.now().plusDays(1), state.date)
        assertEquals(LocalTime.of(8, 0), state.time)
    }

    @Test
    fun `guardar sin titulo marca error y no inserta`() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val vm = viewModel(taskRepository = repository)
        advanceUntilIdle()

        vm.onTitleChange("   ")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.titleError)
        assertTrue(repository.getPendingTasks().isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `guardar nueva inserta tarea con recordatorios en minutos antes de la entrega`() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val vm = viewModel(taskRepository = repository)
        advanceUntilIdle()

        vm.onTitleChange("Entregar informe")
        vm.onDescriptionChange("Capítulos 1 y 2")
        vm.onPriorityChange(Priority.HIGH)
        vm.toggleReminder(REMINDER_OFFSETS_MINUTES[1]) // 10 min antes
        vm.toggleReminder(REMINDER_OFFSETS_MINUTES[3]) // 1 día antes
        vm.save()
        advanceUntilIdle()

        val saved = repository.getPendingTasks().single()
        assertEquals("Entregar informe", saved.title)
        assertEquals("Capítulos 1 y 2", saved.description)
        assertEquals(Priority.HIGH, saved.priority)
        assertEquals(listOf(10, 24 * 60), repository.savedReminderMinutes.single())
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `editar carga la tarea y reemplaza sus recordatorios`() = runTest(testDispatcher) {
        val due = LocalDate.now().plusDays(2).atTime(18, 30)
        val dueInstant = due.atZone(ZoneId.systemDefault()).toInstant()
        val repository = FakeTaskRepository(
            listOf(testTask(id = 9, courseId = 7, title = "Viejo", dueAt = dueInstant))
        )
        repository.insertReminder(
            TaskReminder(id = 1, taskId = 9, remindAt = dueInstant.minusSeconds(600))
        )
        val vm = viewModel(taskId = 9, taskRepository = repository)
        advanceUntilIdle()

        val loaded = vm.uiState.value
        assertTrue(loaded.isLoaded)
        assertEquals("Viejo", loaded.title)
        assertEquals(7L, loaded.courseId)
        assertEquals(due.toLocalDate(), loaded.date)
        assertEquals(due.toLocalTime(), loaded.time)
        assertEquals(listOf(10), loaded.reminderMinutes)

        vm.onTitleChange("Nuevo título")
        vm.toggleReminder(10)     // quita el de 10 min
        vm.toggleReminder(60)     // añade el de 1 h
        vm.save()
        advanceUntilIdle()

        val saved = repository.getById(9)!!
        assertEquals("Nuevo título", saved.title)
        assertEquals(listOf(60), repository.savedReminderMinutes.last())
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `completar desde el formulario marca la tarea y cierra`() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(listOf(testTask(id = 4, title = "Deber")))
        val vm = viewModel(taskId = 4, taskRepository = repository)
        advanceUntilIdle()

        vm.setCompleted(true)
        advanceUntilIdle()

        assertEquals(listOf(4L), repository.completed)
        assertTrue(repository.getById(4)!!.completedAt != null)
        assertTrue(vm.uiState.value.saved)
    }

    @Test
    fun `borrar elimina y avisa`() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(listOf(testTask(id = 6, title = "Deber")))
        val vm = viewModel(taskId = 6, taskRepository = repository)
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertEquals(listOf(6L), repository.deleted)
        assertTrue(vm.uiState.value.deleted)
        assertNull(repository.getById(6))
    }

    @Test
    fun `tarea inexistente termina en estado no encontrada`() = runTest(testDispatcher) {
        val vm = viewModel(taskId = 99, taskRepository = FakeTaskRepository())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notFound)
    }
}
