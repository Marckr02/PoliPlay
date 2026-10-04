package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskReminderDao {
    @Insert
    suspend fun insert(reminder: TaskReminderEntity): Long

    @Insert
    suspend fun insertAll(reminders: List<TaskReminderEntity>): List<Long>

    @Delete
    suspend fun delete(reminder: TaskReminderEntity)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY remindAt ASC")
    suspend fun getByTask(taskId: Long): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY remindAt ASC")
    fun observeByTask(taskId: Long): Flow<List<TaskReminderEntity>>

    /** Recordatorios aún por disparar (se usan para reprogramar alarmas tras reiniciar). */
    @Query("SELECT * FROM task_reminders WHERE remindAt > :nowMillis ORDER BY remindAt ASC")
    suspend fun getUpcoming(nowMillis: Long): List<TaskReminderEntity>

    /** Todo, para el respaldo JSON. */
    @Query("SELECT * FROM task_reminders")
    suspend fun getAllSuspend(): List<TaskReminderEntity>
}
