package com.example.campuspocket.feature.academic.data.repository

import com.example.campuspocket.core.notifications.ReminderScheduler
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.academic.data.TaskReminderEntity
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Pruebas JVM de la programación/cancelación de alarmas del repositorio de tareas,
 * con Room sustituido por fakes en memoria y un ReminderScheduler falso.
 */
class TaskRepositoryImplTest {

    private lateinit var taskDao: FakeTaskDao
    private lateinit var reminderDao: FakeTaskReminderDao
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        reminderDao = FakeTaskReminderDao()
        taskDao = FakeTaskDao(reminderDao)
        scheduler = FakeReminderScheduler()
        repository = TaskRepositoryImpl(
            taskDao,
            reminderDao,
            scheduler,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `insertar programa una alarma por recordatorio con el título de la tarea`() = runTest {
        val due = Instant.ofEpochMilli(1_800_000_000_000L)
        val taskId = repository.insert(
            Task(
                title = "Entrega",
                dueAt = due,
                reminders = listOf(
                    TaskReminder(taskId = 0, remindAt = Instant.ofEpochMilli(1_800_000_000_000L - 600_000)),
                    TaskReminder(taskId = 0, remindAt = due)
                )
            )
        )

        assertEquals(1L, taskId)
        // Las alarmas se programan con el id de recordatorio persistido, no el provisional (0).
        assertEquals(listOf(1L, 2L), scheduler.scheduled.map { it.reminderId })
        assertTrue(scheduler.scheduled.all { it.taskId == taskId && it.title == "Entrega" })
        assertEquals(2, reminderDao.getByTask(taskId).size)
    }

    @Test
    fun `editar pendiente reprograma las alarmas existentes`() = runTest {
        val due = Instant.ofEpochMilli(1_800_000_000_000L)
        val taskId = repository.insert(
            Task(
                title = "Entrega",
                dueAt = due,
                reminders = listOf(TaskReminder(taskId = 0, remindAt = due))
            )
        )
        scheduler.scheduled.clear()

        repository.update(taskDao.getById(taskId)!!.toDomain(title = "Entrega v2"))
        assertEquals(listOf(1L), scheduler.scheduled.map { it.reminderId })
        assertEquals("Entrega v2", scheduler.scheduled.single().title)
    }

    @Test
    fun `completar cancela las alarmas y no reprograma`() = runTest {
        val due = Instant.ofEpochMilli(1_800_000_000_000L)
        val taskId = repository.insert(
            Task(
                title = "Entrega",
                dueAt = due,
                reminders = listOf(
                    TaskReminder(taskId = 0, remindAt = due),
                    TaskReminder(taskId = 0, remindAt = Instant.ofEpochMilli(1_800_000_000_000L + 1))
                )
            )
        )
        scheduler.scheduled.clear()

        repository.update(
            taskDao.getById(taskId)!!.toDomain().copy(completedAt = Instant.now())
        )
        assertEquals(listOf(1L, 2L), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `borrar cancela las alarmas y elimina la tarea con sus recordatorios`() = runTest {
        val due = Instant.ofEpochMilli(1_800_000_000_000L)
        val taskId = repository.insert(
            Task(
                title = "Entrega",
                dueAt = due,
                reminders = listOf(TaskReminder(taskId = 0, remindAt = due))
            )
        )

        repository.delete(taskId)

        assertEquals(listOf(1L), scheduler.cancelled)
        assertTrue(reminderDao.getByTask(taskId).isEmpty())
        assertEquals(null, taskDao.getById(taskId))
    }

    @Test
    fun `insertReminders programa cada alarma con su id persistido`() = runTest {
        val taskId = repository.insert(Task(title = "Solo tarea", dueAt = Instant.now()))
        scheduler.scheduled.clear()

        repository.insertReminders(
            listOf(
                TaskReminder(taskId = taskId, remindAt = Instant.ofEpochMilli(1_800_000_000_000L)),
                TaskReminder(taskId = taskId, remindAt = Instant.ofEpochMilli(1_800_000_600_000L))
            )
        )

        assertEquals(listOf(1L, 2L), scheduler.scheduled.map { it.reminderId })
        assertTrue(scheduler.scheduled.all { it.title == "Solo tarea" })
    }

    @Test
    fun `deleteRemindersByTask cancela todo lo programado de la tarea`() = runTest {
        val taskId = repository.insert(
            Task(
                title = "Entrega",
                dueAt = Instant.ofEpochMilli(1_800_000_000_000L),
                reminders = listOf(
                    TaskReminder(taskId = 0, remindAt = Instant.ofEpochMilli(1_800_000_000_000L)),
                    TaskReminder(taskId = 0, remindAt = Instant.ofEpochMilli(1_800_000_600_000L))
                )
            )
        )

        repository.deleteRemindersByTask(taskId)

        assertEquals(listOf(1L, 2L), scheduler.cancelled)
        assertTrue(reminderDao.getByTask(taskId).isEmpty())
    }

    // ---- Fakes -------------------------------------------------------------

    private fun TaskEntity.toDomain(title: String = this.title): Task = Task(
        id = id,
        courseId = courseId,
        title = title,
        description = description,
        dueAt = Instant.ofEpochMilli(dueAt),
        priority = com.example.campuspocket.feature.academic.domain.model.Priority.valueOf(priority),
        completedAt = completedAt?.let(Instant::ofEpochMilli)
    )

    private class FakeTaskDao(private val reminderDao: FakeTaskReminderDao) : TaskDao {
        private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(task: TaskEntity): Long {
            val entity = task.copy(id = nextId++)
            tasks.value = tasks.value + entity
            return entity.id
        }

        override suspend fun insertReminderEntities(reminders: List<TaskReminderEntity>): List<Long> =
            reminderDao.insertAll(reminders)

        override suspend fun update(task: TaskEntity) {
            tasks.value = tasks.value.map { if (it.id == task.id) task else it }
        }

        override suspend fun delete(task: TaskEntity) {
            tasks.value = tasks.value.filterNot { it.id == task.id }
        }

        override suspend fun deleteById(id: Long) {
            tasks.value = tasks.value.filterNot { it.id == id }
        }

        override suspend fun getById(id: Long): TaskEntity? = tasks.value.firstOrNull { it.id == id }

        override fun observePending(): Flow<List<TaskEntity>> =
            tasks.map { list -> list.filter { it.completedAt == null }.sortedBy { it.dueAt } }

        override fun observeCompleted(): Flow<List<TaskEntity>> =
            tasks.map { list -> list.filter { it.completedAt != null }.sortedByDescending { it.completedAt } }

        override fun observeByCourse(courseId: Long): Flow<List<TaskEntity>> =
            tasks.map { list -> list.filter { it.courseId == courseId }.sortedBy { it.dueAt } }

        override fun observePendingDueBetween(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>> =
            tasks.map { list -> list.filter { it.completedAt == null && it.dueAt in startMillis..endMillis }.sortedBy { it.dueAt } }

        override suspend fun getAll(): List<TaskEntity> = tasks.value.sortedBy { it.dueAt }

        override suspend fun getTasksByDateRange(start: Long, end: Long): List<TaskEntity> =
            tasks.value.filter { it.dueAt in start..end }.sortedBy { it.dueAt }

        override suspend fun getTasksByCourse(courseId: Long): List<TaskEntity> =
            tasks.value.filter { it.courseId == courseId }.sortedBy { it.dueAt }

        override suspend fun getPendingTasks(): List<TaskEntity> =
            tasks.value.filter { it.completedAt == null }.sortedBy { it.dueAt }

        override suspend fun getCompletedTasks(): List<TaskEntity> =
            tasks.value.filter { it.completedAt != null }.sortedByDescending { it.completedAt }
    }

    private class FakeTaskReminderDao : TaskReminderDao {
        private val reminders = mutableListOf<TaskReminderEntity>()
        private var nextId = 1L

        override suspend fun insert(reminder: TaskReminderEntity): Long {
            val entity = reminder.copy(id = nextId++)
            reminders += entity
            return entity.id
        }

        override suspend fun insertAll(reminders: List<TaskReminderEntity>): List<Long> =
            reminders.map { insert(it) }

        override suspend fun delete(reminder: TaskReminderEntity) {
            reminders.removeIf { it.id == reminder.id }
        }

        override suspend fun deleteByTask(taskId: Long) {
            reminders.removeIf { it.taskId == taskId }
        }

        override suspend fun getByTask(taskId: Long): List<TaskReminderEntity> =
            reminders.filter { it.taskId == taskId }.sortedBy { it.remindAt }

        override fun observeByTask(taskId: Long): Flow<List<TaskReminderEntity>> =
            MutableStateFlow(getRemindersSync(taskId))

        private fun getRemindersSync(taskId: Long): List<TaskReminderEntity> =
            reminders.filter { it.taskId == taskId }.sortedBy { it.remindAt }

        override suspend fun getUpcoming(nowMillis: Long): List<TaskReminderEntity> =
            reminders.filter { it.remindAt > nowMillis }.sortedBy { it.remindAt }

        override suspend fun getAllSuspend(): List<TaskReminderEntity> =
            reminders.sortedBy { it.id }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        data class Scheduled(val reminderId: Long, val taskId: Long, val remindAtMillis: Long, val title: String)

        val scheduled = mutableListOf<Scheduled>()
        val cancelled = mutableListOf<Long>()

        override fun schedule(reminderId: Long, taskId: Long, remindAtMillis: Long, title: String) {
            scheduled += Scheduled(reminderId, taskId, remindAtMillis, title)
        }

        override fun cancel(reminderId: Long) {
            cancelled += reminderId
        }

        override suspend fun rescheduleAll() = Unit
    }
}
