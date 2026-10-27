package com.example.campuspocket.core.calendar

import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import com.example.campuspocket.feature.academic.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Reúne horario y entregas pendientes y genera el archivo `.ics`. La escritura
 * del archivo la hace la pantalla que obtuvo el `Uri` del selector del sistema.
 */
@Singleton
class CalendarExportManager @Inject constructor(
    private val courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun build(): CalendarExport = withContext(ioDispatcher) {
        IcsCalendar.build(
            courses = courseRepository.observeCourses().first(),
            tasks = taskRepository.observePending().first(),
            semesterEnd = courseRepository.getActiveSemesterEnd(),
            today = LocalDate.now(clock),
            zone = clock.zone,
            now = clock.instant()
        )
    }
}
