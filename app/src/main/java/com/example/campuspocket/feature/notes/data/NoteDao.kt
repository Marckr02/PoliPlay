package com.example.campuspocket.feature.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY pinned DESC, updatedAt DESC")
    fun observeByFolder(folderId: Long): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun observeWithoutFolder(): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE courseId = :courseId ORDER BY pinned DESC, updatedAt DESC")
    fun observeByCourse(courseId: Long): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY pinned DESC, updatedAt DESC")
    fun observeByTask(taskId: Long): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE pinned = 1 ORDER BY updatedAt DESC")
    fun observePinned(): kotlinx.coroutines.flow.Flow<List<NoteEntity>>

    /** Búsqueda con comodines escapados (el caller pasa un patrón con '\' escapados). */
    @Query("SELECT * FROM notes WHERE title LIKE :pattern ESCAPE '\\' OR content LIKE :pattern ESCAPE '\\' ORDER BY pinned DESC, updatedAt DESC")
    fun search(pattern: String): kotlinx.coroutines.flow.Flow<List<NoteEntity>>
}