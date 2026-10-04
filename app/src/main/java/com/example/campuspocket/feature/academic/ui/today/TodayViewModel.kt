package com.example.campuspocket.feature.academic.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Clase del día de hoy: la sesión más los datos de su materia para pintarla y navegar a ella. */
data class TodayEntry(
    val courseId: Long,
    val courseName: String,
    val courseColorArgb: Int,
    val session: ClassSession
)

data class TodayUiState(
    val date: LocalDate,
    val entries: List<TodayEntry> = emptyList(),
    val isLoading: Boolean = true
)

/** Sesiones de hoy (del semestre activo), ordenadas por hora de inicio. */
@HiltViewModel
class TodayViewModel @Inject constructor(
    courseRepository: CourseRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = courseRepository.observeCourses()
        .map { courses -> buildState(courses) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TodayUiState(date = LocalDate.now(clock))
        )

    private fun buildState(courses: List<Course>): TodayUiState {
        val today = LocalDate.now(clock)
        val entries = courses.flatMap { course ->
            val courseId = course.id ?: return@flatMap emptyList()
            course.sessions
                .filter { it.dayOfWeek == today.dayOfWeek }
                .map { session -> TodayEntry(courseId, course.name, course.colorArgb, session) }
        }.sortedBy { it.session.startTime }
        return TodayUiState(date = today, entries = entries, isLoading = false)
    }
}
