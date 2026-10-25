package com.example.campuspocket.feature.notes.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import com.example.campuspocket.feature.notes.ui.NotesDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
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
    val deleted: Boolean = false,
    /** Autoguardado visible: «Guardando…» mientras hay una edición pendiente. */
    val isSaving: Boolean = false,
    val lastSavedAt: Long = 0L,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

/**
 * Autoguardado en 2 pasos: cada edición emite el estado de la UI, y con debounce se guarda
 * en Room (título/cuerpo). Una NOTA VACÍA no se guarda (ni al salir). updatedAt solo cambia
 * si cambió el contenido real.
 *
 * Historial (v5 §5): se agrupan las ediciones en pausas de 400 ms y se guardan hasta 100
 * instantáneas. Aplicar una acción de Markdown fuerza la instantánea de inmediato.
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val courseRepository: CourseRepository,
    @Named("default") private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<Long>(NotesDestinations.NOTE_ID_ARG) ?: -1L
    private val initialCourseId: Long? = savedStateHandle
        .get<Long>(NotesDestinations.COURSE_ID_ARG)
        ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = editingId.takeIf { it != -1L },
            courseId = initialCourseId
        )
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    val courses: StateFlow<List<Course>> = courseRepository.observeCourses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<FolderEntity>> = noteRepository.observeAllFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val edits = MutableStateFlow(Pair("", ""))
    private var historyCommitJob: Job? = null

    private data class HistorySnapshot(
        val title: String,
        val content: String,
        val selectionStart: Int,
        val selectionEnd: Int
    )

    private val undoStack = ArrayDeque<HistorySnapshot>()
    private val redoStack = ArrayDeque<HistorySnapshot>()
    private var lastSnapshot = HistorySnapshot("", "", 0, 0)

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
                    lastSavedAt = note.updatedAt,
                    isLoaded = true
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoaded = true)
            }
            lastSnapshot = snapshotOf(_uiState.value)

            // autoguardado con debounce y solo si hay contenido real
            edits
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { (title, content) ->
                    saveIfDirty(title, content)
                    _uiState.update { it.copy(isSaving = false) }
                }
        }
    }

    fun onTitleChange(v: String) {
        _uiState.value = _uiState.value.copy(title = v)
        markEdited()
    }

    fun onContentChange(v: String) {
        _uiState.value = _uiState.value.copy(content = v)
        markEdited()
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

    fun setFolderId(id: Long?) {
        _uiState.update { it.copy(folderId = id) }
        markEdited()
    }

    fun setCourseId(id: Long?) {
        _uiState.update { it.copy(courseId = id) }
        markEdited()
    }

    fun setTaskId(id: Long?) {
        _uiState.update { it.copy(taskId = id) }
        markEdited()
    }

    /** Deshacer la última pausa de edición. */
    fun undo() {
        historyCommitJob?.cancel()
        // Una edición aún no agrupada se incorpora al historial antes de deshacer.
        commitHistorySnapshot()
        if (undoStack.isEmpty()) return
        val target = undoStack.removeLast()
        redoStack.addLast(lastSnapshot)
        lastSnapshot = target
        applySnapshot(target)
    }

    /** Rehacer una edición deshecha. */
    fun redo() {
        historyCommitJob?.cancel()
        if (redoStack.isEmpty()) return
        val target = redoStack.removeLast()
        undoStack.addLast(lastSnapshot)
        lastSnapshot = target
        applySnapshot(target)
    }

    /** Las acciones de Markdown cierran el grupo de edición de inmediato (v5 §5). */
    fun commitHistoryNow() {
        historyCommitJob?.cancel()
        commitHistorySnapshot()
    }

    /** Guardado al salir/action explícita (usa la última edición acumulada). */
    suspend fun flush() {
        val (title, content) = edits.value
        saveIfDirty(title, content)
        _uiState.update { it.copy(isSaving = false) }
    }

    private fun markEdited() {
        val state = _uiState.value
        edits.value = Pair(state.title, state.content)
        _uiState.value = state.copy(isSaving = true)
        // Una edición nueva invalida el rehacer inmediatamente.
        redoStack.clear()
        syncHistoryFlags()
        historyCommitJob?.cancel()
        historyCommitJob = viewModelScope.launch {
            delay(HISTORY_GROUP_MS)
            commitHistorySnapshot()
        }
    }

    private fun snapshotOf(state: NoteEditorUiState) = HistorySnapshot(
        title = state.title,
        content = state.content,
        selectionStart = state.selectionStart,
        selectionEnd = state.selectionEnd
    )

    private fun commitHistorySnapshot() {
        val current = snapshotOf(_uiState.value)
        if (current == lastSnapshot) {
            syncHistoryFlags()
            return
        }
        undoStack.addLast(lastSnapshot)
        while (undoStack.size > HISTORY_LIMIT) undoStack.removeFirst()
        lastSnapshot = current
        redoStack.clear()
        syncHistoryFlags()
    }

    private fun applySnapshot(snapshot: HistorySnapshot) {
        _uiState.update {
            it.copy(
                title = snapshot.title,
                content = snapshot.content,
                selectionStart = snapshot.selectionStart,
                selectionEnd = snapshot.selectionEnd,
                isSaving = true
            )
        }
        edits.value = Pair(snapshot.title, snapshot.content)
        syncHistoryFlags()
    }

    private fun syncHistoryFlags() {
        _uiState.update {
            it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
        }
    }

    private suspend fun saveIfDirty(title: String, content: String) = withContext(dispatcher) {
        if (title.isBlank() && content.isBlank()) return@withContext  // nota vacía: no se guarda
        val state = _uiState.value
        val now = System.currentTimeMillis()
        if (state.noteId == null) {
            val id = noteRepository.insertNote(
                NoteEntity(
                    title = title,
                    content = content,
                    pinned = state.pinned,
                    folderId = state.folderId,
                    courseId = state.courseId,
                    taskId = state.taskId,
                    createdAt = now,
                    updatedAt = now
                )
            )
            _uiState.value = _uiState.value.copy(
                noteId = id,
                updatedAt = now,
                lastSavedAt = now
            )
        } else {
            val id = state.noteId
            val current = noteRepository.getNote(id) ?: return@withContext
            val contentChanged = current.title != title || current.content != content
            val metadataChanged = current.pinned != state.pinned ||
                current.folderId != state.folderId ||
                current.courseId != state.courseId ||
                current.taskId != state.taskId
            if (contentChanged || metadataChanged) {
                noteRepository.updateNote(
                    current.copy(
                        title = title,
                        content = content,
                        pinned = state.pinned,
                        folderId = state.folderId,
                        courseId = state.courseId,
                        taskId = state.taskId
                    )
                )
                _uiState.value = _uiState.value.copy(
                    updatedAt = if (contentChanged) now else current.updatedAt,
                    lastSavedAt = now
                )
            } else {
                _uiState.value = _uiState.value.copy(lastSavedAt = current.updatedAt)
            }
        }
    }

    private companion object {
        const val HISTORY_GROUP_MS = 400L
        const val HISTORY_LIMIT = 100
    }
}
