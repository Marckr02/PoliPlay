package com.example.campuspocket.feature.academic.domain.repository

import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TaskRepository {
    // Tasks
    suspend fun insert(task: Task): Long
    suspend fun update(task: Task)
    suspend fun delete(taskId: Long)
    suspend fun getById(taskId: Long): Task?
    fun observePending(): Flow<List<Task>>
    fun observeCompleted(): Flow<List<Task>>
    fun observeByCourse(courseId: Long): Flow<List<Task>>
    suspend fun getPendingTasks(): List<Task>
    suspend fun getCompletedTasks(): List<Task>
    suspend fun getTasksByDateRange(start: Instant, end: Instant): List<Task>
    suspend fun getTasksByCourse(courseId: Long): List<Task>

    /**
     * Marca la tarea como completada y, si se repite, crea la siguiente
     * ocurrencia. Devuelve el id de la ocurrencia creada (o null).
     */
    suspend fun complete(task: Task, completedAt: Instant = Instant.now()): Long?

    /** Deshace una finalización y elimina la ocurrencia generada, si existe. */
    suspend fun undoCompletion(task: Task, nextOccurrenceId: Long?)

    // Reminders
    suspend fun insertReminder(reminder: TaskReminder): Long
    suspend fun insertReminders(reminders: List<TaskReminder>)
    suspend fun deleteRemindersByTask(taskId: Long)
    suspend fun getRemindersByTask(taskId: Long): List<TaskReminder>
    suspend fun getUpcomingReminders(now: Instant): List<TaskReminder>
}