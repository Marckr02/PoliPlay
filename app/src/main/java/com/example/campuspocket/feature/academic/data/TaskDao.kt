package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertReminderEntities(reminders: List<TaskReminderEntity>): List<Long>

    @Insert
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>): List<Long>

    /** Inserta una tarea y sus recordatorios en una sola transacción. */
    @Transaction
    suspend fun insertWithReminders(task: TaskEntity, reminders: List<TaskReminderEntity>): Long {
        val taskId = insert(task)
        if (reminders.isNotEmpty()) {
            insertReminderEntities(reminders.map { it.copy(taskId = taskId) })
        }
        return taskId
    }

    /** Inserta tarea, recordatorios y subtareas en una sola transacción. */
    @Transaction
    suspend fun insertWithDetails(
        task: TaskEntity,
        reminders: List<TaskReminderEntity>,
        subtasks: List<SubtaskEntity>
    ): Long {
        val taskId = insert(task)
        if (reminders.isNotEmpty()) {
            insertReminderEntities(reminders.map { it.copy(taskId = taskId) })
        }
        if (subtasks.isNotEmpty()) {
            insertSubtasks(
                subtasks.mapIndexed { index, subtask ->
                    subtask.copy(id = 0, taskId = taskId, position = index)
                }
            )
        }
        return taskId
    }

    @Update
    suspend fun update(task: TaskEntity)

    /** Reemplaza por completo las subtareas de una tarea (edición del formulario). */
    @Transaction
    suspend fun replaceSubtasks(taskId: Long, subtasks: List<SubtaskEntity>) {
        deleteSubtasksByTask(taskId)
        if (subtasks.isNotEmpty()) {
            insertSubtasks(
                subtasks.mapIndexed { index, subtask ->
                    subtask.copy(id = 0, taskId = taskId, position = index)
                }
            )
        }
    }

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completedAt IS NULL ORDER BY dueAt ASC")
    fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY dueAt ASC")
    fun observeByCourse(courseId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completedAt IS NULL AND dueAt >= :startMillis AND dueAt <= :endMillis ORDER BY dueAt ASC")
    fun observePendingDueBetween(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueAt ASC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueAt >= :start AND dueAt <= :end ORDER BY dueAt ASC")
    suspend fun getTasksByDateRange(start: Long, end: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY dueAt ASC")
    suspend fun getTasksByCourse(courseId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completedAt IS NULL ORDER BY dueAt ASC")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completedAt IS NOT NULL ORDER BY dueAt DESC")
    suspend fun getCompletedTasks(): List<TaskEntity>

    // -- Subtareas ----------------------------------------------------------

    @Query("SELECT * FROM subtasks ORDER BY taskId, position")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position")
    suspend fun getSubtasks(taskId: Long): List<SubtaskEntity>

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTask(taskId: Long)

    // -- Repetición ---------------------------------------------------------

    @Query(
        "SELECT * FROM tasks WHERE completedAt IS NULL AND title = :title " +
            "AND dueAt = :dueAt AND courseId IS :courseId LIMIT 1"
    )
    suspend fun findPendingOccurrence(title: String, dueAt: Long, courseId: Long?): TaskEntity?
}
