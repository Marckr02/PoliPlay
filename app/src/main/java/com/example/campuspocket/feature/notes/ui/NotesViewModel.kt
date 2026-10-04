package com.example.campuspocket.feature.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.escapeLikePattern
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val query: String = "",
    val folders: List<FolderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val currentFolderId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<NotesUiState> = combine(
        query,
        currentFolderId
    ) { q, folderId -> q to folderId }
        .flatMapLatest { (q, folderId) ->
            if (q.isNotBlank()) {
                val escaped = "%" + escapeLikePattern(q.trim()) + "%"
                combine(noteRepository.searchNotes(escaped), MutableStateFlow(emptyList<FolderEntity>())) { notes, folders ->
                    NotesUiState(query = q, folders = folders, notes = notes, isLoading = false)
                }
            } else {
                val foldersFlow = if (folderId == null) noteRepository.observeRootFolders() else noteRepository.observeChildFolders(folderId)
                val notesFlow = if (folderId == null) noteRepository.observeNotesWithoutFolder() else noteRepository.observeNotesByFolder(folderId)
                combine(foldersFlow, notesFlow) { folders, notes ->
                    NotesUiState(query = "", folders = folders, notes = notes, isLoading = false)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NotesUiState())

    fun setQuery(q: String) { query.value = q }
    fun enterFolder(id: Long) { currentFolderId.value = id }
    fun goUp() { currentFolderId.value = null }
    fun goRoot() { currentFolderId.value = null }

    fun actionsForFolder(id: Long?) = Unit // placeholder: el diálogo entero lo hace la pantalla

    fun createFolder(name: String, parentId: Long?) {
        viewModelScope.launch {
            noteRepository.insertFolder(FolderEntity(name = name.trim(), parentId = parentId))
        }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch {
            noteRepository.getFolder(id)?.let { noteRepository.updateFolder(it.copy(name = name.trim())) }
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch { noteRepository.deleteFolder(id) }
    }

    fun createNote(folderId: Long?): Long? {
        var newId: Long? = null
        viewModelScope.launch {
            newId = noteRepository.insertNote(NoteEntity(title = "", content = "", folderId = folderId))
        }
        return newId
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { noteRepository.deleteNote(id) }
    }

    fun togglePin(id: Long, pinned: Boolean) {
        viewModelScope.launch { noteRepository.togglePin(id, pinned) }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long?) {
        viewModelScope.launch {
            noteRepository.getNote(noteId)?.let { noteRepository.updateNote(it.copy(folderId = folderId)) }
        }
    }
}
