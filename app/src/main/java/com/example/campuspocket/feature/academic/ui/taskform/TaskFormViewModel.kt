package com.example.campuspocket.feature.academic.ui.taskform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Priority
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
    val isLoaded: Boolean = false,
    val titleError: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val notFound: Boolean = false
) {
    val isEditing: Boolean get() = taskId != null
    val dueAt: Instant get() = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
}

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    courseRepository: CourseRepository
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
            viewModelScope.launch {
                val task = taskRepository.getById(editingId)
                if (task == null) {
                    _uiState.value = _uiState.value.copy(notFound = true, isLoaded = true)
                } else {
                    val reminders = taskRepository.getRemindersByTask(editingId)
                    val due = task.dueAt.atZone(ZoneId.systemDefault())
                    _uiState.value = _uiState.value.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description.orEmpty(),
                        courseId = task.courseId,
                        date = due.toLocalDate(),
                        time = due.toLocalTime(),
                        priority = task.priority,
                        // Recordatorios como minutos de antelación respecto a la entrega.
                        reminderMinutes = reminders
                            .map { task.dueAt.epochSecond - it.remindAt.epochSecond }
                            .map { (it / 60).toInt() }
                            .filter { it in REMINDER_OFFSETS_MINUTES }
                            .sorted(),
                        completedAt = task.completedAt,
                        isLoaded = true
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, titleError = false)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onCourseChange(courseId: Long?) {
        _uiState.value = _uiState.value.copy(courseId = courseId)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onTimeChange(time: LocalTime) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun onPriorityChange(priority: Priority) {
        _uiState.value = _uiState.value.copy(priority = priority)
    }

    fun toggleReminder(minutesBefore: Int) {
        val current = _uiState.value.reminderMinutes
        _uiState.value = _uiState.value.copy(
            reminderMinutes = (if (minutesBefore in current) current - minutesBefore else current + minutesBefore).sorted()
        )
    }

    /** Guarda la tarea. Edición: reemplaza sus recordatorios (reprograma alarmas vía repo). */
    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(titleError = true)
            return
        }
        viewModelScope.launch {
            val task = Task(
                id = state.taskId,
                courseId = state.courseId,
                title = state.title.trim(),
                description = state.description.trim().ifEmpty { null },
                dueAt = state.dueAt,
                priority = state.priority,
                completedAt = state.completedAt
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
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun setCompleted(completed: Boolean) {
        val taskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            taskRepository.update(
                if (completed) task.copy(completedAt = Instant.now()) else task.copy(completedAt = null)
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun delete() {
        val taskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            taskRepository.delete(taskId)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }
}
