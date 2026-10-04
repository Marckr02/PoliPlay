package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Insert
    suspend fun insertAll(courses: List<CourseEntity>): List<Long>

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name, section")
    fun observeBySemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name, section")
    suspend fun getBySemester(semesterId: Long): List<CourseEntity>

    /** Materias del semestre activo con todas sus sesiones (observa ambos cambios). */
    @Transaction
    @Query("SELECT * FROM courses WHERE semesterId IN (SELECT id FROM semesters WHERE isActive = 1) ORDER BY name, section")
    fun observeWithSessionsByActiveSemester(): Flow<List<CourseWithSessions>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getWithSessionsById(id: Long): CourseWithSessions?

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    fun observeWithSessionsById(id: Long): Flow<CourseWithSessions?>

    @Query("SELECT * FROM courses WHERE semesterId IN (SELECT id FROM semesters WHERE isActive = 1) ORDER BY name, section")
    fun getCoursesByActiveSemester(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE id = :id")
    fun observeById(id: Long): Flow<CourseEntity?>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId AND code = :code AND section = :section")
    suspend fun getByCodeAndSection(semesterId: Long, code: String, section: String): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY name, section")
    suspend fun getAll(): List<CourseEntity>

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourse(id: Long): Int

    // Sesiones (hijas de una materia; se guardan junto con ella en una transacción)

    @Insert
    suspend fun insertSessions(sessions: List<ClassSessionEntity>): List<Long>

    @Query("DELETE FROM class_sessions WHERE courseId = :courseId")
    suspend fun deleteSessionsByCourse(courseId: Long)

    /** Inserta una materia y sus sesiones en una sola transacción. */
    @Transaction
    suspend fun insertWithSessions(course: CourseEntity, sessions: List<ClassSessionEntity>): Long {
        val courseId = insert(course)
        if (sessions.isNotEmpty()) {
            insertSessions(sessions.map { it.copy(courseId = courseId) })
        }
        return courseId
    }

    /** Actualiza la materia y reemplaza sus sesiones, todo en una transacción. */
    @Transaction
    suspend fun updateWithSessions(course: CourseEntity, sessions: List<ClassSessionEntity>) {
        update(course)
        deleteSessionsByCourse(course.id)
        if (sessions.isNotEmpty()) {
            insertSessions(sessions.map { it.copy(courseId = course.id) })
        }
    }
}