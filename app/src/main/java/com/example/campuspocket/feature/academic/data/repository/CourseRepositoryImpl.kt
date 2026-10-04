package com.example.campuspocket.feature.academic.data.repository

import com.example.campuspocket.feature.academic.data.ClassSessionEntity
import com.example.campuspocket.feature.academic.data.CourseDao
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.CourseWithSessions
import com.example.campuspocket.feature.academic.data.SemesterDao
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.model.SemesterTerm
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> =
        courseDao.observeWithSessionsByActiveSemester()
            .map { list -> list.map(CourseWithSessions::toDomain) }

    override fun observeCourse(courseId: Long): Flow<Course?> =
        courseDao.observeWithSessionsById(courseId)
            .map { it?.toDomain() }

    override suspend fun getCourse(courseId: Long): Course? {
        return withContext(ioDispatcher) {
            courseDao.getWithSessionsById(courseId)?.toDomain()
        }
    }

    override suspend fun insertCourse(course: Course): Long {
        return withContext(ioDispatcher) {
            val semesterId = ensureActiveSemester()
            courseDao.insertWithSessions(
                course.toEntity(semesterId),
                course.sessions.map(ClassSession::toEntity)
            )
        }
    }

    override suspend fun updateCourse(course: Course) {
        withContext(ioDispatcher) {
            val id = course.id ?: return@withContext
            // El semestre no cambia al editar: se conserva el que tiene en base de datos.
            val current = courseDao.getById(id) ?: return@withContext
            courseDao.updateWithSessions(
                course.toEntity(current.semesterId),
                course.sessions.map(ClassSession::toEntity)
            )
        }
    }

    override suspend fun deleteCourse(courseId: Long) {
        withContext(ioDispatcher) {
            // Las sesiones se borran en cascada; las tareas de la materia quedan (SET_NULL).
            courseDao.deleteCourse(courseId)
        }
    }

    private suspend fun ensureActiveSemester(): Long {
        semesterDao.getActive()?.let { return it.id }
        val term = SemesterTerm.forDate(LocalDate.now())
        return semesterDao.insert(
            SemesterEntity(name = term.name, startDate = term.start, endDate = term.end, isActive = true)
        )
    }
}

// Mapeos entidad <-> dominio (viven en la capa de datos; dominio no conoce Room).

internal fun CourseWithSessions.toDomain(): Course = Course(
    id = course.id,
    code = course.code,
    name = course.name,
    section = course.section,
    teacher = course.teacher,
    colorArgb = course.colorArgb,
    sessions = sessions.map { it.toDomain(course.id) }
)

internal fun ClassSessionEntity.toDomain(courseId: Long): ClassSession = ClassSession(
    id = id.takeIf { it != 0L },
    courseId = courseId,
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    startTime = LocalTime.of(startMinute / 60, startMinute % 60),
    endTime = LocalTime.of(endMinute / 60, endMinute % 60),
    room = room
)

internal fun Course.toEntity(semesterId: Long): CourseEntity = CourseEntity(
    id = id ?: 0,
    semesterId = semesterId,
    code = code,
    name = name,
    section = section,
    teacher = teacher,
    colorArgb = colorArgb
)

internal fun ClassSession.toEntity(): ClassSessionEntity = ClassSessionEntity(
    id = id ?: 0,
    courseId = courseId ?: 0,
    dayOfWeek = dayOfWeek.value,
    startMinute = startTime.hour * 60 + startTime.minute,
    endMinute = endTime.hour * 60 + endTime.minute,
    room = room
)
