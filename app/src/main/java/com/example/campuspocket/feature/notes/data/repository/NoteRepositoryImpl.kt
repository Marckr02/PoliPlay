package com.example.campuspocket.feature.notes.data.repository

import com.example.campuspocket.feature.notes.data.FolderDao
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteDao
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : NoteRepository {

    override fun observeAllNotes(): Flow<List<NoteEntity>> = noteDao.observeAll()
    override fun observeRootFolders(): Flow<List<FolderEntity>> = folderDao.observeRoot()
    override fun observeChildFolders(parentId: Long): Flow<List<FolderEntity>> = folderDao.observeByParent(parentId)
    override fun observeNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = noteDao.observeByFolder(folderId)
    override fun observeNotesWithoutFolder(): Flow<List<NoteEntity>> = noteDao.observeWithoutFolder()
    override fun observeNotesByCourse(courseId: Long) = noteDao.observeByCourse(courseId)
    override fun observeNotesByTask(taskId: Long) = noteDao.observeByTask(taskId)
    override fun observePinnedNotes() = noteDao.observePinned()
    override fun searchNotes(escapedPattern: String) = noteDao.search(escapedPattern)

    override suspend fun getNote(id: Long) = withContext(ioDispatcher) { noteDao.getById(id) }
    override suspend fun insertNote(note: NoteEntity) = withContext(ioDispatcher) { noteDao.insert(note) }
    override suspend fun updateNote(note: NoteEntity) {
        withContext(ioDispatcher) {
            // updatedAt solo cambia si cambió el contenido real (título o cuerpo).
            val current = noteDao.getById(note.id)
            if (current != null && (current.title != note.title || current.content != note.content)) {
                noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
            } else if (current != null) {
                noteDao.update(note.copy(updatedAt = current.updatedAt))
            }
        }
    }
    override suspend fun deleteNote(id: Long) { withContext(ioDispatcher) { noteDao.getById(id)?.let { noteDao.delete(it) } } }
    override suspend fun togglePin(id: Long, pinned: Boolean) {
        withContext(ioDispatcher) {
            noteDao.getById(id)?.let { noteDao.update(it.copy(pinned = pinned)) }
        }
    }

    override suspend fun getFolder(id: Long) = withContext(ioDispatcher) { folderDao.getById(id) }
    override suspend fun insertFolder(folder: FolderEntity) = withContext(ioDispatcher) { folderDao.insert(folder) }
    override suspend fun updateFolder(folder: FolderEntity) { withContext(ioDispatcher) { folderDao.update(folder) } }
    override suspend fun deleteFolder(id: Long) {
        withContext(ioDispatcher) {
            folderDao.getById(id)?.let { folderDao.delete(it) }
        }
    }
}
