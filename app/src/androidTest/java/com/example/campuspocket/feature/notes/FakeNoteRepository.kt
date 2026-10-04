package com.example.campuspocket.feature.notes

import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fake de notas para pruebas instrumentadas con Compose (sin Hilt, sin BD real). */
class FakeNoteRepository(initial: List<NoteEntity> = emptyList()) : NoteRepository {
    val notes = initial.toMutableList()

    override fun observeAllNotes(): Flow<List<NoteEntity>> = flowOf(notes.toList())
    override fun observeRootFolders(): Flow<List<FolderEntity>> = flowOf(emptyList())
    override fun observeChildFolders(parentId: Long): Flow<List<FolderEntity>> = flowOf(emptyList())
    override fun observeNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = flowOf(notes.filter { it.folderId == folderId })
    override fun observeNotesWithoutFolder(): Flow<List<NoteEntity>> = flowOf(notes.filter { it.folderId == null })
    override fun observeNotesByCourse(courseId: Long): Flow<List<NoteEntity>> = flowOf(notes.filter { it.courseId == courseId })
    override fun observeNotesByTask(taskId: Long): Flow<List<NoteEntity>> = flowOf(notes.filter { it.taskId == taskId })
    override fun observePinnedNotes(): Flow<List<NoteEntity>> = flowOf(notes.filter { it.pinned })
    override fun searchNotes(escapedPattern: String): Flow<List<NoteEntity>> {
        val q = escapedPattern.trim('%').replace("\\", "")
        return flowOf(notes.filter { it.title.contains(q) || it.content.contains(q) })
    }

    override suspend fun getNote(id: Long) = notes.firstOrNull { it.id == id }
    override suspend fun insertNote(note: NoteEntity): Long {
        val id = (notes.map { it.id }.maxOrNull() ?: 0L) + 1
        notes += note.copy(id = id)
        return id
    }
    override suspend fun updateNote(note: NoteEntity) {
        notes.indexOfFirst { it.id == note.id }.takeIf { it >= 0 }?.let { notes[it] = note }
    }
    override suspend fun deleteNote(id: Long) { notes.removeAll { it.id == id } }
    override suspend fun togglePin(id: Long, pinned: Boolean) {
        notes.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { notes[it] = notes[it].copy(pinned = pinned) }
    }
    override suspend fun getFolder(id: Long) = null
    override suspend fun insertFolder(folder: FolderEntity) = 0L
    override suspend fun updateFolder(folder: FolderEntity) = Unit
    override suspend fun deleteFolder(id: Long) = Unit
}
