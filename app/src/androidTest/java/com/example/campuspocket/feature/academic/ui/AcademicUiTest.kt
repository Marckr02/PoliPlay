@file:SuppressLint("ViewModelConstructorInComposable")

package com.example.campuspocket.feature.academic.ui

import android.annotation.SuppressLint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import com.example.campuspocket.core.designsystem.CampusTheme
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.academic.ui.coursedetail.CourseDetailScreen
import com.example.campuspocket.feature.academic.ui.coursedetail.CourseDetailViewModel
import com.example.campuspocket.feature.academic.ui.courses.CoursesViewModel
import com.example.campuspocket.feature.academic.ui.courses.MateriasScreen
import com.example.campuspocket.feature.academic.ui.taskform.TaskFormScreen
import com.example.campuspocket.feature.academic.ui.taskform.TaskFormViewModel
import com.example.campuspocket.feature.academic.ui.tasks.TasksScreen
import com.example.campuspocket.feature.academic.ui.tasks.TasksViewModel
import com.example.campuspocket.feature.notes.FakeNoteRepository
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Pruebas de interfaz (Compose) con repositorios falsos en memoria (sin Hilt ni la BD del teléfono).
 */
class AcademicUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tocarMateriaNotificaSuIdParaNavegarAlDetalle() {
        val course = Course(id = 7, code = "FIS-201", name = "Física Cuántica", section = "B")
        var clickedId: Long? = null

        composeRule.setContent {
            CampusTheme {
                MateriasScreen(
                    onAddCourse = {},
                    onOpenCourse = { clickedId = it },
                    onImport = {},
                    viewModel = CoursesViewModel(FakeCourseRepository(listOf(course)))
                )
            }
        }

        composeRule.onNodeWithText("Física Cuántica").performClick()
        composeRule.waitForIdle()
        assertEquals(7L, clickedId)
    }

    @Test
    fun elDetalleMuestraLaMateria() {
        val course = Course(id = 7, code = "FIS-201", name = "Física Cuántica", section = "B")

        composeRule.setContent {
            CampusTheme {
                CourseDetailScreen(
                    onEditCourse = {},
                    onBack = {},
                    viewModel = CourseDetailViewModel(
                        SavedStateHandle(mapOf(AcademicDestinations.COURSE_ID_ARG to 7L)),
                        FakeCourseRepository(listOf(course)),
                        FakeTaskRepository(),
                        FakeNoteRepository()
                    )
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("FIS-201")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("FIS-201").assertIsDisplayed()
    }

    @Test
    fun crearTareaConRecordatorioLaGuarda() {
        val taskRepo = FakeTaskRepository()
        var formFinished = false

        composeRule.setContent {
            CampusTheme {
                TaskFormScreen(
                    onDone = { formFinished = true },
                    viewModel = TaskFormViewModel(
                        SavedStateHandle(mapOf(AcademicDestinations.TASK_ID_ARG to -1L)),
                        taskRepo,
                        FakeNoteRepository(),
                        FakeCourseRepository()
                    )
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextReplacement("Entregar informe de laboratorio")
        composeRule.onNodeWithText("A la hora").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodes(hasText("Ahora no")).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Ahora no").performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Guardar").performClick()

        composeRule.waitUntil(5_000) { formFinished }
        assertEquals(
            listOf("Entregar informe de laboratorio"),
            taskRepo.currentTasks().map { it.title }
        )
        assertEquals(1, taskRepo.currentReminders(1).size)
    }

    @Test
    fun laListaMuestraLaTareaCreada() {
        val course = Course(id = 7, code = "FIS-201", name = "Física", section = "B")
        val taskRepo = FakeTaskRepository()
        taskRepo.addExternalTask(
            Task(id = 1, courseId = 7, title = "Entregar informe de laboratorio", dueAt = Instant.ofEpochMilli(1_800_000_000_000))
        )

        composeRule.setContent {
            CampusTheme {
                TasksScreen(
                    onAddTask = {},
                    onOpenTask = {},
                    viewModel = TasksViewModel(taskRepo, FakeCourseRepository(listOf(course)))
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Entregar informe de laboratorio")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Entregar informe de laboratorio").assertIsDisplayed()
    }

    // ---- Fakes en memoria ---------------------------------------------------

    private class FakeCourseRepository(initial: List<Course> = emptyList()) : CourseRepository {
        private val coursesFlow = MutableStateFlow(initial)
        override fun observeCourses(): Flow<List<Course>> = coursesFlow
        override fun observeCourse(courseId: Long): Flow<Course?> = coursesFlow.map { list -> list.firstOrNull { it.id == courseId } }
        override suspend fun getCourse(courseId: Long): Course? = coursesFlow.value.firstOrNull { it.id == courseId }
        override suspend fun insertCourse(course: Course): Long = 0L
        override suspend fun updateCourse(course: Course) = Unit
        override suspend fun deleteCourse(courseId: Long) = Unit
    }

    private class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {
        private val tasksFlow = MutableStateFlow(initial)
        private val reminders = mutableMapOf<Long, MutableList<TaskReminder>>()

        fun currentTasks(): List<Task> = tasksFlow.value
        fun currentReminders(taskId: Long): List<TaskReminder> = reminders[taskId] ?: emptyList()

        fun addExternalTask(task: Task) {
            tasksFlow.value = tasksFlow.value + task
            reminders[task.id!!] = mutableListOf(TaskReminder(id = 1, taskId = task.id!!, remindAt = task.dueAt.minusSeconds(600)))
        }

        override suspend fun insert(task: Task): Long {
            val newId = (tasksFlow.value.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
            tasksFlow.value = tasksFlow.value + task.copy(id = newId)
            reminders[newId] = task.reminders.mapIndexed { i, r -> r.copy(id = i + 1L, taskId = newId) }.toMutableList()
            return newId
        }
        override suspend fun update(task: Task) { tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it } }
        override suspend fun delete(taskId: Long) { tasksFlow.value = tasksFlow.value.filterNot { it.id == taskId } }
        override suspend fun getById(taskId: Long): Task? = tasksFlow.value.firstOrNull { it.id == taskId }
        override fun observePending(): Flow<List<Task>> = tasksFlow.map { list -> list.filter { it.completedAt == null } }
        override fun observeCompleted(): Flow<List<Task>> = tasksFlow.map { list -> list.filter { it.completedAt != null } }
        override fun observeByCourse(courseId: Long): Flow<List<Task>> = tasksFlow.map { list -> list.filter { it.courseId == courseId } }
        override suspend fun getPendingTasks(): List<Task> = currentTasks().filter { it.completedAt == null }
        override suspend fun getCompletedTasks(): List<Task> = currentTasks().filter { it.completedAt != null }
        override suspend fun getTasksByDateRange(start: Instant, end: Instant) = emptyList<Task>()
        override suspend fun getTasksByCourse(courseId: Long) = emptyList<Task>()
        override suspend fun insertReminder(reminder: TaskReminder) = 0L
        override suspend fun insertReminders(reminders: List<TaskReminder>) = Unit
        override suspend fun deleteRemindersByTask(taskId: Long) = Unit
        override suspend fun getRemindersByTask(taskId: Long): List<TaskReminder> = currentReminders(taskId)
        override suspend fun getUpcomingReminders(now: Instant) = emptyList<TaskReminder>()
    }
}
