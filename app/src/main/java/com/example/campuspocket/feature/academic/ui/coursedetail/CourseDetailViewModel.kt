package com.example.campuspocket.feature.academic.ui.coursedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.notes.domain.repository.NoteRepository
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data object NotFound : CourseDetailUiState
    data class Content(
        val course: Course,
        val tasks: List<Task>,
        val notes: List<com.example.campuspocket.feature.notes.data.NoteEntity> = emptyList()
    ) : CourseDetailUiState
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val courseId: Long = savedStateHandle.get<Long>(AcademicDestinations.COURSE_ID_ARG) ?: -1L

    // Observa la base de datos: al volver de editar, el detalle ya está actualizado.
    val uiState: StateFlow<CourseDetailUiState> = combine(
        courseRepository.observeCourse(courseId),
        taskRepository.observeByCourse(courseId),
        noteRepository.observeNotesByCourse(courseId)
    ) { course, tasks, notes ->
        when (course) {
            null -> CourseDetailUiState.NotFound
            else -> CourseDetailUiState.Content(
                course.copy(
                    sessions = course.sessions.sortedWith(
                        compareBy({ it.dayOfWeek.value }, { it.startTime })
                    )
                ),
                tasks.sortedBy { it.dueAt },
                notes
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CourseDetailUiState.Loading
    )

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun delete() {
        if (_deleted.value) return
        viewModelScope.launch {
            courseRepository.deleteCourse(courseId)
            _deleted.value = true
        }
    }
}
