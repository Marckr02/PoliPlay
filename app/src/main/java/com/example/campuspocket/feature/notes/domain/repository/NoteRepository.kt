package com.example.campuspocket.feature.notes.domain.repository

import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAllNotes(): Flow<List<NoteEntity>>
    fun observeRootFolders(): Flow<List<FolderEntity>>
    fun observeChildFolders(parentId: Long): Flow<List<FolderEntity>>
    fun observeNotesByFolder(folderId: Long): Flow<List<NoteEntity>>
    fun observeNotesWithoutFolder(): Flow<List<NoteEntity>>
    fun observeNotesByCourse(courseId: Long): Flow<List<NoteEntity>>
    fun observeNotesByTask(taskId: Long): Flow<List<NoteEntity>>
    fun observePinnedNotes(): Flow<List<NoteEntity>>
    fun searchNotes(escapedPattern: String): Flow<List<NoteEntity>>

    suspend fun getNote(id: Long): NoteEntity?
    suspend fun insertNote(note: NoteEntity): Long
    suspend fun updateNote(note: NoteEntity)
    suspend fun deleteNote(id: Long)
    suspend fun togglePin(id: Long, pinned: Boolean)

    suspend fun getFolder(id: Long): FolderEntity?
    suspend fun insertFolder(folder: FolderEntity): Long
    suspend fun updateFolder(folder: FolderEntity)
    suspend fun deleteFolder(id: Long)
}
