package com.example.campuspocket.feature.academic.ui.courseform

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.R
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.ui.AcademicDestinations
import com.example.campuspocket.feature.academic.ui.TimeText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/** Estado de una sesión tal como se escribe en el formulario (horas como texto "H:mm"). */
data class SessionFormState(
    val day: DayOfWeek? = null,
    val start: String = "",
    val end: String = "",
    val room: String = "",
    @StringRes val dayError: Int? = null,
    @StringRes val startError: Int? = null,
    @StringRes val endError: Int? = null
) {
    val hasErrors: Boolean get() = dayError != null || startError != null || endError != null
}

data class CourseFormUiState(
    val code: String = "",
    val name: String = "",
    val section: String = "",
    val teacher: String = "",
    val sessions: List<SessionFormState> = emptyList(),
    @StringRes val codeError: Int? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val sectionError: Int? = null,
    @StringRes val sessionsError: Int? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class CourseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val courseId: Long? = savedStateHandle.get<Long>(AcademicDestinations.COURSE_ID_ARG)
        ?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        CourseFormUiState(isEditing = courseId != null, isLoading = courseId != null)
    )
    val uiState: StateFlow<CourseFormUiState> = _uiState.asStateFlow()

    init {
        if (courseId != null) {
            viewModelScope.launch {
                val course = courseRepository.getCourse(courseId)
                _uiState.update {
                    if (course == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            code = course.code,
                            name = course.name,
                            section = course.section,
                            teacher = course.teacher.orEmpty(),
                            sessions = course.sessions.map { s ->
                                SessionFormState(
                                    day = s.dayOfWeek,
                                    start = TimeText.format(s.startTime),
                                    end = TimeText.format(s.endTime),
                                    room = s.room.orEmpty()
                                )
                            },
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value, codeError = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onSectionChange(value: String) = _uiState.update { it.copy(section = value, sectionError = null) }
    fun onTeacherChange(value: String) = _uiState.update { it.copy(teacher = value) }

    fun addSession() = _uiState.update {
        it.copy(sessions = it.sessions + SessionFormState(), sessionsError = null)
    }

    fun removeSession(index: Int) = _uiState.update {
        it.copy(sessions = it.sessions.toMutableList().apply { removeAt(index) })
    }

    fun updateSession(index: Int, session: SessionFormState) = _uiState.update {
        it.copy(
            sessions = it.sessions.toMutableList().apply {
                set(index, session.copy(dayError = null, startError = null, endError = null))
            }
        )
    }

    /** Valida y guarda. Si hay errores quedan marcados en el estado y no se guarda nada. */
    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.saved) return

        val codeError = if (state.code.isBlank()) R.string.error_required else null
        val nameError = if (state.name.isBlank()) R.string.error_required else null
        val sectionError = if (state.section.isBlank()) R.string.error_required else null

        val validatedSessions = state.sessions.map { session -> validateSession(session) }
        val sessionsError = if (state.sessions.isEmpty()) R.string.error_no_sessions else null

        val hasErrors = codeError != null || nameError != null || sectionError != null ||
            sessionsError != null || validatedSessions.any { it.hasErrors }

        _uiState.update {
            it.copy(
                codeError = codeError,
                nameError = nameError,
                sectionError = sectionError,
                sessionsError = sessionsError,
                sessions = validatedSessions
            )
        }
        if (hasErrors) return

        val course = Course(
            id = courseId,
            code = state.code.trim(),
            name = state.name.trim(),
            section = state.section.trim(),
            teacher = state.teacher.trim().ifBlank { null },
            sessions = validatedSessions.map { session ->
                // Ya validado: day/start/end no son nulos y el formato es correcto.
                ClassSession(
                    courseId = courseId,
                    dayOfWeek = session.day!!,
                    startTime = TimeText.parse(session.start)!!,
                    endTime = TimeText.parse(session.end)!!,
                    room = session.room.trim().ifBlank { null }
                )
            }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (courseId == null) {
                courseRepository.insertCourse(course)
            } else {
                courseRepository.updateCourse(course)
            }
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private fun validateSession(session: SessionFormState): SessionFormState {
        val start = TimeText.parse(session.start)
        val end = TimeText.parse(session.end)
        return session.copy(
            dayError = if (session.day == null) R.string.error_required else null,
            startError = when {
                session.start.isBlank() -> R.string.error_required
                start == null -> R.string.error_time_format
                else -> null
            },
            endError = when {
                session.end.isBlank() -> R.string.error_required
                end == null -> R.string.error_time_format
                start != null && !end.isAfter(start) -> R.string.error_time_order
                else -> null
            }
        )
    }
}
