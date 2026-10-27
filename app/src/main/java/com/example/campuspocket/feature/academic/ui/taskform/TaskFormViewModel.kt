package com.example.campuspocket.feature.academic.ui.taskform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Priority
import com.example.campuspocket.feature.academic.domain.model.RepeatRule
import com.example.campuspocket.feature.academic.domain.model.SemesterTerm
import com.example.campuspocket.feature.academic.domain.model.Subtask
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.model.TaskReminder
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** Minutos antes de la entrega que ofrece el formulario (se pueden elegir varios). */
val REMINDER_OFFSETS_MINUTES = listOf(0, 10, 60, 24 * 60)

data class TaskFormUiState(
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val courseId: Long? = null,
    val date: LocalDate = LocalDate.now().plusDays(1),
    val time: LocalTime = LocalTime.of(8, 0),
    val priority: Priority = Priority.MEDIUM,
    val reminderMinutes: List<Int> = emptyList(),
    val completedAt: Instant? = null,
    val subtasks: List<Subtask> = emptyList(),
    val subtaskDraft: String = "",
    val subtaskError: Boolean = false,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val repeatUntil: LocalDate? = null,
    val semesterEnd: LocalDate? = null,
    val isLoaded: Boolean = false,
    val titleError: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val notFound: Boolean = false,
    /** Hay cambios del usuario que aún no se han guardado. */
    val hasUnsavedChanges: Boolean = false
) {
    val isEditing: Boolean get() = taskId != null
    val dueAt: Instant get() = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
    val completedSubtasks: Int get() = subtasks.count { it.done }
    val allSubtasksDone: Boolean get() = subtasks.isNotEmpty() && completedSubtasks == subtasks.size
}

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val editingId: Long? =
        savedStateHandle.get<Long>(AcademicDestinations.TASK_ID_ARG)?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(TaskFormUiState())
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    val courses: StateFlow<List<Course>> = courseRepository.observeCourses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Notas vinculadas a la tarea en edición (Fase 6).
    val linkedNotes = MutableStateFlow<List<com.example.campuspocket.feature.notes.data.NoteEntity>>(emptyList())

    init {
        if (editingId != null) {
            viewModelScope.launch {
                noteRepository.observeNotesByTask(editingId).collect { linkedNotes.value = it }
            }
        }
        viewModelScope.launch {
            val semesterEnd = courseRepository.getActiveSemesterEnd()
                ?: SemesterTerm.forDate(LocalDate.now()).end
            _uiState.update { it.copy(semesterEnd = semesterEnd) }

            if (editingId == null) {
                _uiState.update { it.copy(isLoaded = true) }
            } else {
                val task = taskRepository.getById(editingId)
                if (task == null) {
                    _uiState.update { it.copy(notFound = true, isLoaded = true) }
                } else {
                    val reminders = taskRepository.getRemindersByTask(editingId)
                    val due = task.dueAt.atZone(ZoneId.systemDefault())
                    _uiState.update {
                        it.copy(
                            taskId = task.id,
                            title = task.title,
                            description = task.description.orEmpty(),
                            courseId = task.courseId,
                            date = due.toLocalDate(),
                            time = due.toLocalTime(),
                            priority = task.priority,
                            // Recordatorios como minutos de antelación respecto a la entrega.
                            reminderMinutes = reminders
                                .map { reminder -> task.dueAt.epochSecond - reminder.remindAt.epochSecond }
                                .map { seconds -> (seconds / 60).toInt() }
                                .filter { minutes -> minutes in REMINDER_OFFSETS_MINUTES }
                                .sorted(),
                            completedAt = task.completedAt,
                            subtasks = task.subtasks.sortedBy { subtask -> subtask.position },
                            repeatRule = task.repeatRule,
                            repeatUntil = task.repeatUntil
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate(),
                            isLoaded = true
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false, hasUnsavedChanges = true) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, hasUnsavedChanges = true) }
    }

    fun onCourseChange(courseId: Long?) {
        _uiState.update { it.copy(courseId = courseId, hasUnsavedChanges = true) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date, hasUnsavedChanges = true) }
    }

    fun onTimeChange(time: LocalTime) {
        _uiState.update { it.copy(time = time, hasUnsavedChanges = true) }
    }

    fun onPriorityChange(priority: Priority) {
        _uiState.update { it.copy(priority = priority, hasUnsavedChanges = true) }
    }

    fun toggleReminder(minutesBefore: Int) {
        _uiState.update { state ->
            val current = state.reminderMinutes
            state.copy(
                reminderMinutes = (
                    if (minutesBefore in current) current - minutesBefore
                    else current + minutesBefore
                    ).sorted(),
                hasUnsavedChanges = true
            )
        }
    }

    // -- Subtareas ---------------------------------------------------------

    fun onSubtaskDraftChange(value: String) {
        _uiState.update {
            it.copy(subtaskDraft = value, subtaskError = false, hasUnsavedChanges = true)
        }
    }

    fun addSubtask() {
        val state = _uiState.value
        val title = state.subtaskDraft.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(subtaskError = true) }
            return
        }
        _uiState.update {
            it.copy(
                subtasks = state.subtasks + Subtask(
                    title = title,
                    position = state.subtasks.size
                ),
                subtaskDraft = "",
                subtaskError = false,
                hasUnsavedChanges = true
            )
        }
    }

    fun removeSubtask(index: Int) {
        _uiState.update { state ->
            if (index !in state.subtasks.indices) state
            else state.copy(
                subtasks = state.subtasks
                    .filterIndexed { i, _ -> i != index }
                    .mapIndexed { i, subtask -> subtask.copy(position = i) },
                hasUnsavedChanges = true
            )
        }
    }

    fun toggleSubtaskDone(index: Int) {
        _uiState.update { state ->
            if (index !in state.subtasks.indices) state
            else state.copy(
                subtasks = state.subtasks.mapIndexed { i, subtask ->
                    if (i == index) subtask.copy(done = !subtask.done) else subtask
                },
                hasUnsavedChanges = true
            )
        }
    }

    // -- Repetición --------------------------------------------------------

    fun onRepeatRuleChange(rule: RepeatRule) {
        _uiState.update { state ->
            state.copy(
                repeatRule = rule,
                repeatUntil = when {
                    rule == RepeatRule.NONE -> null
                    state.repeatUntil == null -> state.semesterEnd
                    else -> state.repeatUntil
                },
                hasUnsavedChanges = true
            )
        }
    }

    fun onRepeatUntilChange(date: LocalDate) {
        _uiState.update { it.copy(repeatUntil = date, hasUnsavedChanges = true) }
    }

    fun useSemesterEnd() {
        _uiState.update { state ->
            state.copy(
                repeatUntil = state.semesterEnd ?: state.repeatUntil,
                hasUnsavedChanges = true
            )
        }
    }

    /** Guarda la tarea. Edición: reemplaza sus recordatorios (reprograma alarmas vía repo). */
    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val task = Task(
                id = state.taskId,
                courseId = state.courseId,
                title = state.title.trim(),
                description = state.description.trim().ifEmpty { null },
                dueAt = state.dueAt,
                priority = state.priority,
                completedAt = state.completedAt,
                subtasks = state.subtasks.mapIndexed { index, subtask ->
                    subtask.copy(
                        title = subtask.title.trim(),
                        position = index
                    )
                }.filter { it.title.isNotEmpty() },
                repeatRule = state.repeatRule,
                repeatUntil = if (state.repeatRule == RepeatRule.NONE) {
                    null
                } else {
                    state.repeatUntil
                        ?.atTime(23, 59, 59)
                        ?.atZone(zone)
                        ?.toInstant()
                }
            )
            val reminderTasks = state.reminderMinutes.map { minutes ->
                TaskReminder(taskId = 0, remindAt = task.dueAt.minusSeconds(minutes * 60L))
            }
            if (state.isEditing) {
                // 1) cancela y borra los recordatorios viejos, 2) actualiza (sin reprogramar),
                // 3) inserta y programa los nuevos. Si está completada no hay alarmas.
                taskRepository.deleteRemindersByTask(task.id!!)
                taskRepository.update(task)
                if (task.completedAt == null) {
                    taskRepository.insertReminders(reminderTasks.map { it.copy(taskId = task.id!!) })
                }
            } else {
                taskRepository.insert(task.copy(reminders = reminderTasks))
            }
            _uiState.update { it.copy(saved = true, hasUnsavedChanges = false) }
        }
    }

    fun setCompleted(completed: Boolean) {
        val taskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            if (completed) {
                taskRepository.complete(task, Instant.now())
            } else {
                taskRepository.undoCompletion(task, nextOccurrenceId = null)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val taskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            taskRepository.delete(taskId)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
