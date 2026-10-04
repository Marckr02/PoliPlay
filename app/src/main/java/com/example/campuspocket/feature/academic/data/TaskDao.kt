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

    /** Inserta una tarea y sus recordatorios en una sola transacción. */
    @Transaction
    suspend fun insertWithReminders(task: TaskEntity, reminders: List<TaskReminderEntity>): Long {
        val taskId = insert(task)
        if (reminders.isNotEmpty()) {
            insertReminderEntities(reminders.map { it.copy(taskId = taskId) })
        }
        return taskId
    }

    @Update
    suspend fun update(task: TaskEntity)

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
}