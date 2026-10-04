package com.example.campuspocket.feature.notes.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import com.example.campuspocket.feature.notes.ui.NotesDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val pinned: Boolean = false,
    val folderId: Long? = null,
    val courseId: Long? = null,
    val taskId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isLoaded: Boolean = false,
    val notFound: Boolean = false,
    val deleted: Boolean = false
)

/**
 * Autoguardado en 2 pasos: cada edición emite el estado de la UI, y con debounce se guarda
 * en Room (título/cuerpo). Una NOTA VACÍA no se guarda (ni al salir). updatedAt solo cambia
 * si cambió el contenido real.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    @Named("default") private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<Long>(NotesDestinations.NOTE_ID_ARG) ?: -1L

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = editingId.takeIf { it != -1L }))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val edits = MutableStateFlow(Pair("", ""))

    init {
        viewModelScope.launch {
            // carga inicial
            if (editingId != -1L) {
                val note = noteRepository.getNote(editingId)
                if (note == null) {
                    _uiState.value = _uiState.value.copy(notFound = true, isLoaded = true)
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    title = note.title,
                    content = note.content,
                    pinned = note.pinned,
                    folderId = note.folderId,
                    courseId = note.courseId,
                    taskId = note.taskId,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isLoaded = true
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoaded = true)
            }

            // autoguardado con debounce y solo si hay contenido real
            edits
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { (title, content) -> saveIfDirty(title, content) }
        }
    }

    fun onTitleChange(v: String) {
        _uiState.value = _uiState.value.copy(title = v)
        edits.value = Pair(v, _uiState.value.content)
    }

    fun onContentChange(v: String) {
        _uiState.value = _uiState.value.copy(content = v)
        edits.value = Pair(_uiState.value.title, v)
    }

    fun onSelectionChange(start: Int, end: Int) {
        _uiState.value = _uiState.value.copy(selectionStart = start, selectionEnd = end)
    }

    fun togglePin() {
        val state = _uiState.value
        val id = state.noteId ?: return
        viewModelScope.launch {
            noteRepository.togglePin(id, !state.pinned)
            _uiState.value = state.copy(pinned = !state.pinned)
        }
    }

    fun delete() {
        val id = _uiState.value.noteId ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(id)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }

    fun setFolderId(id: Long?) = _uiState.update { it.copy(folderId = id) }
    fun setCourseId(id: Long?) = _uiState.update { it.copy(courseId = id) }
    fun setTaskId(id: Long?) = _uiState.update { it.copy(taskId = id) }

    /** Guardado al salir/action explícita (usa la última edición acumulada). */
    suspend fun flush() {
        val (title, content) = edits.value
        saveIfDirty(title, content)
    }

    private suspend fun saveIfDirty(title: String, content: String) = withContext(dispatcher) {
        if (title.isBlank() && content.isBlank()) return@withContext  // nota vacía: no se guarda
        val state = _uiState.value
        if (state.noteId == null) {
            val id = noteRepository.insertNote(
                NoteEntity(
                    title = title,
                    content = content,
                    pinned = state.pinned,
                    folderId = state.folderId,
                    courseId = state.courseId,
                    taskId = state.taskId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(noteId = id, updatedAt = System.currentTimeMillis())
        } else {
            val id = state.noteId
            val current = noteRepository.getNote(id) ?: return@withContext
            if (current.title != title || current.content != content) {
                noteRepository.updateNote(
                    current.copy(title = title, content = content, pinned = state.pinned,
                        folderId = state.folderId, courseId = state.courseId, taskId = state.taskId)
                )
                _uiState.value = _uiState.value.copy(updatedAt = System.currentTimeMillis())
            }
        }
    }
}
