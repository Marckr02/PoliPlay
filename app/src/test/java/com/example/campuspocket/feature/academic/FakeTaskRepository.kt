package com.example.campuspocket.feature.academic

import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Fake en memoria para las pruebas de ViewModels de tareas. Registra cada operación recibida. */
class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {

    private val tasksFlow = MutableStateFlow(initial)
    private val remindersByTask = mutableMapOf<Long, MutableList<TaskReminder>>()
    /** Recordatorios tal cual llegaron por tarea al insertar/actualizar (para aserciones). */
    val savedReminderMinutes = mutableListOf<List<Int>>()
    val completed = mutableListOf<Long>()
    val deleted = mutableListOf<Long>()

    override suspend fun insert(task: Task): Long {
        val newId = (tasksFlow.value.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
        tasksFlow.value = tasksFlow.value + task.copy(id = newId)
        remindersByTask[newId] = task.reminders.toMutableList()
        savedReminderMinutes += task.reminders.map { r ->
            ((task.dueAt.epochSecond - r.remindAt.epochSecond) / 60).toInt()
        }
        return newId
    }

    override suspend fun update(task: Task) {
        val id = task.id ?: return
        if (task.completedAt != null) completed += id
        tasksFlow.value = tasksFlow.value.map { if (it.id == id) task else it }
    }

    override suspend fun delete(taskId: Long) {
        deleted += taskId
        remindersByTask.remove(taskId)
        tasksFlow.value = tasksFlow.value.filterNot { it.id == taskId }
    }

    override suspend fun getById(taskId: Long): Task? = tasksFlow.value.firstOrNull { it.id == taskId }

    override fun observePending(): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.completedAt == null } }

    override fun observeCompleted(): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.completedAt != null } }

    override fun observeByCourse(courseId: Long): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.courseId == courseId } }

    override suspend fun getPendingTasks(): List<Task> = tasksFlow.value.filter { it.completedAt == null }

    override suspend fun getCompletedTasks(): List<Task> = tasksFlow.value.filter { it.completedAt != null }

    override suspend fun getTasksByDateRange(start: Instant, end: Instant): List<Task> =
        tasksFlow.value.filter { it.dueAt >= start && it.dueAt <= end }

    override suspend fun getTasksByCourse(courseId: Long): List<Task> =
        tasksFlow.value.filter { it.courseId == courseId }

    override suspend fun insertReminder(reminder: TaskReminder): Long {
        val list = remindersByTask.getOrPut(reminder.taskId) { mutableListOf() }
        val id = (list.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
        list += reminder.copy(id = id)
        return id
    }

    override suspend fun insertReminders(reminders: List<TaskReminder>) {
        reminders.forEach { insertReminder(it) }
        reminders.firstOrNull()?.let { first ->
            savedReminderMinutes += reminders.mapNotNull { r ->
                val taskDue = tasksFlow.value.firstOrNull { it.id == r.taskId }?.dueAt ?: return@mapNotNull null
                ((taskDue.epochSecond - r.remindAt.epochSecond) / 60).toInt()
            }
        }
    }

    override suspend fun deleteRemindersByTask(taskId: Long) {
        remindersByTask.remove(taskId)
    }

    override suspend fun getRemindersByTask(taskId: Long): List<TaskReminder> =
        remindersByTask[taskId] ?: emptyList()

    override suspend fun getUpcomingReminders(now: Instant): List<TaskReminder> =
        remindersByTask.values.flatten().filter { it.remindAt > now }
}

fun testTask(
    id: Long? = null,
    courseId: Long? = null,
    title: String = "Deber",
    dueAt: Instant = Instant.ofEpochMilli(1_793_001_600_000), // 30/10/2026 08:00 UTC
    completedAt: Instant? = null
): Task = Task(id = id, courseId = courseId, title = title, dueAt = dueAt, completedAt = completedAt)
