package com.example.campuspocket.feature.academic.data.repository

import com.example.campuspocket.core.notifications.ReminderScheduler
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.academic.domain.model.RepeatRule
import com.example.campuspocket.feature.academic.domain.model.Subtask
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val reminderDao: TaskReminderDao,
    private val reminderScheduler: ReminderScheduler,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : TaskRepository {

    override suspend fun insert(task: Task): Long {
        return withContext(ioDispatcher) {
            val taskId = taskDao.insertWithDetails(
                task.toEntity(),
                task.reminders.map(TaskReminder::toEntity),
                task.subtasks.mapIndexed { index, subtask ->
                    subtask.copy(position = index).toEntity()
                }
            )
            scheduleAlarms(taskId, task.title)
            taskId
        }
    }

    override suspend fun update(task: Task) {
        withContext(ioDispatcher) {
            val taskId = task.id ?: return@withContext
            taskDao.update(task.toEntity())
            taskDao.replaceSubtasks(
                taskId,
                task.subtasks.mapIndexed { index, subtask ->
                    subtask.copy(position = index).toEntity()
                }
            )
            if (task.completedAt == null) {
                // Editar: reprogramar (los PendingIntent usan FLAG_UPDATE_CURRENT).
                scheduleAlarms(taskId, task.title)
            } else {
                // Completada: ya no hay nada que recordar.
                cancelAlarms(taskId)
            }
        }
    }

    override suspend fun delete(taskId: Long) {
        withContext(ioDispatcher) {
            cancelAlarms(taskId)
            reminderDao.deleteByTask(taskId)
            taskDao.deleteById(taskId)
        }
    }

    override suspend fun getById(taskId: Long): Task? {
        return withContext(ioDispatcher) {
            taskDao.getById(taskId)?.let { entity ->
                Task.fromEntity(entity, loadSubtasks(taskId))
            }
        }
    }

    override fun observePending(): Flow<List<Task>> = taskDao.observePending().withDetails()

    override fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted().withDetails()

    override fun observeByCourse(courseId: Long): Flow<List<Task>> =
        taskDao.observeByCourse(courseId).withDetails()

    override suspend fun getPendingTasks(): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getPendingTasks().map { entity ->
                Task.fromEntity(entity, loadSubtasks(entity.id))
            }
        }
    }

    override suspend fun getCompletedTasks(): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getCompletedTasks().map { entity ->
                Task.fromEntity(entity, loadSubtasks(entity.id))
            }
        }
    }

    override suspend fun getTasksByDateRange(start: Instant, end: Instant): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getTasksByDateRange(start.toEpochMilli(), end.toEpochMilli())
                .map { entity -> Task.fromEntity(entity, loadSubtasks(entity.id)) }
        }
    }

    override suspend fun getTasksByCourse(courseId: Long): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getTasksByCourse(courseId).map { entity ->
                Task.fromEntity(entity, loadSubtasks(entity.id))
            }
        }
    }

    override suspend fun complete(task: Task, completedAt: Instant): Long? {
        return withContext(ioDispatcher) {
            val taskId = task.id ?: return@withContext null
            val current = taskDao.getById(taskId) ?: return@withContext null
            if (current.completedAt != null) return@withContext null

            val detailed = Task.fromEntity(current, loadSubtasks(taskId))
            taskDao.update(detailed.copy(completedAt = completedAt).toEntity())
            cancelAlarms(taskId)

            if (detailed.repeatRule == RepeatRule.NONE) null
            else createNextOccurrence(detailed, completedAt)
        }
    }

    override suspend fun undoCompletion(task: Task, nextOccurrenceId: Long?) {
        withContext(ioDispatcher) {
            val taskId = task.id ?: return@withContext
            val current = taskDao.getById(taskId) ?: return@withContext
            taskDao.update(current.copy(completedAt = null))
            scheduleAlarms(taskId, current.title)
            if (nextOccurrenceId != null) {
                delete(nextOccurrenceId)
            }
        }
    }

    /** Crea la siguiente ocurrencia de una tarea repetida y devuelve su id. */
    private suspend fun createNextOccurrence(task: Task, now: Instant): Long? {
        val zone = ZoneId.systemDefault()
        var nextDue = task.dueAt
        do {
            val zoned = nextDue.atZone(zone)
            nextDue = when (task.repeatRule) {
                RepeatRule.DAILY -> zoned.plusDays(1).toInstant()
                RepeatRule.WEEKLY -> zoned.plusWeeks(1).toInstant()
                RepeatRule.NONE -> return null
            }
        } while (!nextDue.isAfter(now))

        val untilDate = task.repeatUntil?.atZone(zone)?.toLocalDate()
        if (untilDate != null && nextDue.atZone(zone).toLocalDate().isAfter(untilDate)) return null

        // Evita duplicar la ocurrencia si se completa dos veces tras un error de red/estado.
        if (taskDao.findPendingOccurrence(task.title, nextDue.toEpochMilli(), task.courseId) != null) {
            return null
        }

        val deltaSeconds = nextDue.epochSecond - task.dueAt.epochSecond
        val shiftedReminders = reminderDao.getByTask(task.id ?: return null).mapNotNull { reminder ->
            reminder.id.takeIf { it > 0 }?.let {
                TaskReminder(
                    taskId = 0,
                    remindAt = Instant.ofEpochMilli(reminder.remindAt).plusSeconds(deltaSeconds)
                )
            }
        }
        val resetSubtasks = task.subtasks.mapIndexed { index, subtask ->
            Subtask(title = subtask.title, done = false, position = index)
        }
        return insert(
            task.copy(
                id = null,
                dueAt = nextDue,
                completedAt = null,
                createdAt = now,
                reminders = shiftedReminders,
                subtasks = resetSubtasks
            )
        )
    }

    // Recordatorios

    override suspend fun insertReminder(reminder: TaskReminder): Long {
        return withContext(ioDispatcher) {
            val id = reminderDao.insert(reminder.toEntity())
            scheduleAlarm(id, reminder.taskId, reminder.remindAt.toEpochMilli())
            id
        }
    }

    override suspend fun insertReminders(reminders: List<TaskReminder>) {
        withContext(ioDispatcher) {
            val ids = reminderDao.insertAll(reminders.map(TaskReminder::toEntity))
            reminders.zip(ids).forEach { (reminder, id) ->
                scheduleAlarm(id, reminder.taskId, reminder.remindAt.toEpochMilli())
            }
        }
    }

    override suspend fun deleteRemindersByTask(taskId: Long) {
        withContext(ioDispatcher) {
            cancelAlarms(taskId)
            reminderDao.deleteByTask(taskId)
        }
    }

    override suspend fun getRemindersByTask(taskId: Long): List<TaskReminder> {
        return withContext(ioDispatcher) {
            reminderDao.getByTask(taskId).map(TaskReminder::fromEntity)
        }
    }

    override suspend fun getUpcomingReminders(now: Instant): List<TaskReminder> {
        return withContext(ioDispatcher) {
            reminderDao.getUpcoming(now.toEpochMilli()).map(TaskReminder::fromEntity)
        }
    }

    /** Programa (o reprograma) las alarmas de todos los recordatorios de la tarea. */
    private suspend fun scheduleAlarms(taskId: Long, title: String) {
        reminderDao.getByTask(taskId).forEach { reminder ->
            reminderScheduler.schedule(reminder.id, taskId, reminder.remindAt, title)
        }
    }

    private suspend fun scheduleAlarm(reminderId: Long, taskId: Long, remindAtMillis: Long) {
        val title = taskDao.getById(taskId)?.title ?: return
        reminderScheduler.schedule(reminderId, taskId, remindAtMillis, title)
    }

    private suspend fun cancelAlarms(taskId: Long) {
        reminderDao.getByTask(taskId).forEach { reminder ->
            reminderScheduler.cancel(reminder.id)
        }
    }

    private suspend fun loadSubtasks(taskId: Long): List<Subtask> =
        taskDao.getSubtasks(taskId).map(Subtask::fromEntity)

    private fun Flow<List<TaskEntity>>.withDetails(): Flow<List<Task>> =
        combine(this, taskDao.observeAllSubtasks()) { tasks, subtasks ->
            val byTask = subtasks.groupBy { it.taskId }
            tasks.map { entity ->
                Task.fromEntity(entity, byTask[entity.id].orEmpty().map(Subtask::fromEntity))
            }
        }
}
