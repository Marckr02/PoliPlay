package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSessionDao {
    @Insert
    suspend fun insert(session: ClassSessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<ClassSessionEntity>): List<Long>

    @Update
    suspend fun update(session: ClassSessionEntity)

    @Delete
    suspend fun delete(session: ClassSessionEntity)

    @Query("SELECT * FROM class_sessions WHERE courseId = :courseId ORDER BY dayOfWeek, startMinute")
    fun observeByCourse(courseId: Long): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startMinute")
    fun observeByDay(dayOfWeek: Int): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startMinute")
    fun observeAll(): Flow<List<ClassSessionEntity>>
}
