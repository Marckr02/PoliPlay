package com.example.campuspocket.feature.academic.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
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
import java.time.Instant
import javax.inject.Inject

/**
 * Lista de tareas del semestre: pendientes/completadas, filtrables por materia y
 * ordenadas por fecha de entrega.
 */
data class TasksUiState(
    val showCompleted: Boolean = false,
    val courseFilterId: Long? = null,
    val tasks: List<Task> = emptyList(),
    val courses: List<Course> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val showCompleted = MutableStateFlow(false)
    private val courseFilterId = MutableStateFlow<Long?>(null)

    private val tasksFlow = showCompleted.flatMapLatest { completed ->
        if (completed) taskRepository.observeCompleted() else taskRepository.observePending()
    }

    val uiState: StateFlow<TasksUiState> = combine(
        tasksFlow,
        courseRepository.observeCourses(),
        showCompleted,
        courseFilterId
    ) { tasks, courses, showDone, filterId ->
        TasksUiState(
            showCompleted = showDone,
            courseFilterId = filterId,
            courses = courses,
            tasks = tasks
                .filter { filterId == null || it.courseId == filterId }
                .sortedBy { it.dueAt },
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TasksUiState()
    )

    private val _deleted = MutableStateFlow<Long?>(null)
    val deleted: StateFlow<Long?> = _deleted.asStateFlow()

    fun setShowCompleted(value: Boolean) {
        showCompleted.value = value
    }

    fun setCourseFilter(courseId: Long?) {
        courseFilterId.value = courseId
    }

    /** Completar cancela sus alarmas dentro del repositorio. */
    fun setCompleted(task: Task, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.update(
                if (completed) task.copy(completedAt = Instant.now()) else task.copy(completedAt = null)
            )
        }
    }

    fun delete(taskId: Long) {
        viewModelScope.launch {
            taskRepository.delete(taskId)
        }
    }
}
