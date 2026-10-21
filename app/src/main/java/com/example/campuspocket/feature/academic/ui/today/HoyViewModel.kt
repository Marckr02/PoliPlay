package com.example.campuspocket.feature.academic.ui.today

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.R
import com.example.campuspocket.core.designsystem.components.DayStripDay
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.Task
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import com.example.campuspocket.feature.academic.ui.TimeText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HoyClass(
    val courseId: Long,
    val courseName: String,
    val courseColorArgb: Int,
    val startLabel: String,
    val endLabel: String,
    val room: String?,
    val isCurrent: Boolean,
    val progress: Float,
    val start: LocalTime,
    val end: LocalTime
)

data class HoyTask(
    val task: Task,
    val courseName: String?
)

data class HoyUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<DayStripDay> = emptyList(),
    val classes: List<HoyClass> = emptyList(),
    val tasks: List<HoyTask> = emptyList()
)

/**
 * Vista de Hoy/Semana: horario de la semana y tareas que vencen hoy.
 * La selección de día es estado de UI; completar una tarea reutiliza el
 * repositorio existente, igual que la pantalla de Tareas.
 */
@HiltViewModel
class HoyViewModel @Inject constructor(
    courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    val selectedDay: StateFlow<LocalDate> = selectedDate.asStateFlow()

    private val ticker: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(60_000L)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<HoyUiState> = combine(
        courseRepository.observeCourses(),
        taskRepository.observePending(),
        selectedDate,
        ticker
    ) { courses, tasks, day, today ->
        val zone = ZoneId.systemDefault()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val days = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            DayStripDay(
                date = date,
                letterRes = dayLetterRes(date.dayOfWeek),
                dayOfMonth = date.dayOfMonth
            )
        }
        HoyUiState(
            today = today,
            selectedDate = day,
            weekDays = days,
            classes = classesFor(courses, day, today),
            tasks = tasks
                .filter { it.dueAt.atZone(zone).toLocalDate() == today }
                .sortedBy { it.dueAt }
                .map { task ->
                    HoyTask(
                        task = task,
                        courseName = courses.firstOrNull { it.id == task.courseId }?.name
                    )
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HoyUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun setCompleted(task: Task, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.update(
                task.copy(completedAt = if (completed) Instant.now(clock) else null)
            )
        }
    }

    private fun classesFor(courses: List<Course>, date: LocalDate, today: LocalDate): List<HoyClass> {
        val now = LocalTime.now(clock)
        return courses.flatMap { course ->
            course.sessions
                .filter { it.dayOfWeek == date.dayOfWeek }
                .map { session ->
                    val current = date == today &&
                        !now.isBefore(session.startTime) &&
                        now.isBefore(session.endTime)
                    val progress = if (current) {
                        val total = Duration.between(session.startTime, session.endTime).seconds
                            .coerceAtLeast(1L)
                        (Duration.between(session.startTime, now).seconds.toFloat() / total)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    HoyClass(
                        courseId = course.id ?: 0L,
                        courseName = course.name,
                        courseColorArgb = course.colorArgb,
                        startLabel = TimeText.format(session.startTime),
                        endLabel = TimeText.format(session.endTime),
                        room = session.room,
                        isCurrent = current,
                        progress = progress,
                        start = session.startTime,
                        end = session.endTime
                    )
                }
        }.sortedBy { it.start }
    }

    private fun dayLetterRes(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> R.string.day_letter_monday
        DayOfWeek.TUESDAY -> R.string.day_letter_tuesday
        DayOfWeek.WEDNESDAY -> R.string.day_letter_wednesday
        DayOfWeek.THURSDAY -> R.string.day_letter_thursday
        DayOfWeek.FRIDAY -> R.string.day_letter_friday
        DayOfWeek.SATURDAY -> R.string.day_letter_saturday
        DayOfWeek.SUNDAY -> R.string.day_letter_sunday
    }
}
