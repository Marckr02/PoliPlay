package com.example.campuspocket.feature.academic.data.repository

import com.example.campuspocket.core.notifications.ReminderScheduler
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
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
            val taskId = taskDao.insertWithReminders(
                task.toEntity(),
                task.reminders.map(TaskReminder::toEntity)
            )
            scheduleAlarms(taskId, task.title)
            taskId
        }
    }

    override suspend fun update(task: Task) {
        withContext(ioDispatcher) {
            val taskId = task.id ?: return@withContext
            taskDao.update(task.toEntity())
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
            taskDao.getById(taskId)?.let(Task::fromEntity)
        }
    }

    override fun observePending(): Flow<List<Task>> =
        taskDao.observePending().map { entities -> entities.map(Task::fromEntity) }

    override fun observeCompleted(): Flow<List<Task>> =
        taskDao.observeCompleted().map { entities -> entities.map(Task::fromEntity) }

    override fun observeByCourse(courseId: Long): Flow<List<Task>> =
        taskDao.observeByCourse(courseId).map { entities -> entities.map(Task::fromEntity) }

    override suspend fun getPendingTasks(): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getPendingTasks().map(Task::fromEntity)
        }
    }

    override suspend fun getCompletedTasks(): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getCompletedTasks().map(Task::fromEntity)
        }
    }

    override suspend fun getTasksByDateRange(start: Instant, end: Instant): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getTasksByDateRange(start.toEpochMilli(), end.toEpochMilli())
                .map(Task::fromEntity)
        }
    }

    override suspend fun getTasksByCourse(courseId: Long): List<Task> {
        return withContext(ioDispatcher) {
            taskDao.getTasksByCourse(courseId).map(Task::fromEntity)
        }
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
}
