package com.example.campuspocket.feature.academic.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.SchoolDays
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import javax.inject.Inject

data class WeekEntry(
    val courseId: Long,
    val courseName: String,
    val courseColorArgb: Int,
    val session: ClassSession
)

data class DaySchedule(
    val day: DayOfWeek,
    val entries: List<WeekEntry>
)

data class WeekUiState(
    val days: List<DaySchedule> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Vista semanal: de lunes a viernes siempre; el sábado solo aparece si tiene clases.
 * El domingo no existe en esta universidad, así que nunca se muestra.
 */
@HiltViewModel
class WeekViewModel @Inject constructor(
    courseRepository: CourseRepository
) : ViewModel() {

    val uiState: StateFlow<WeekUiState> = courseRepository.observeCourses()
        .map { courses -> buildUiState(courses) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            WeekUiState(isLoading = true)
        )

    private fun buildUiState(courses: List<Course>): WeekUiState {
        fun entriesOf(day: DayOfWeek): List<WeekEntry> = courses
            .flatMap { course ->
                val courseId = course.id ?: return@flatMap emptyList()
                course.sessions
                    .filter { session -> session.dayOfWeek == day }
                    .map { session -> WeekEntry(courseId, course.name, course.colorArgb, session) }
            }
            .sortedBy { it.session.startTime }

        val days = SchoolDays.weekdays.map { day -> DaySchedule(day, entriesOf(day)) }
            .toMutableList()

        val saturday = entriesOf(DayOfWeek.SATURDAY)
        if (saturday.isNotEmpty()) {
            days += DaySchedule(DayOfWeek.SATURDAY, saturday)
        }

        return WeekUiState(days = days, isLoading = false)
    }
}
