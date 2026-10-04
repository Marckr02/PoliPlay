package com.example.campuspocket.feature.academic

import com.example.campuspocket.feature.academic.domain.model.ClassSession
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

/** Fake en memoria para las pruebas de ViewModels. Registra cada operación recibida. */
class FakeCourseRepository(initial: List<Course> = emptyList()) : CourseRepository {

    private val coursesFlow = MutableStateFlow(initial)
    val inserted = mutableListOf<Course>()
    val updated = mutableListOf<Course>()
    val deleted = mutableListOf<Long>()

    override fun observeCourses(): Flow<List<Course>> = coursesFlow

    override fun observeCourse(courseId: Long): Flow<Course?> =
        coursesFlow.map { list -> list.firstOrNull { it.id == courseId } }

    override suspend fun getCourse(courseId: Long): Course? =
        coursesFlow.value.firstOrNull { it.id == courseId }

    override suspend fun insertCourse(course: Course): Long {
        inserted += course
        val newId = (coursesFlow.value.mapNotNull { it.id }.maxOrNull() ?: 0L) + 1
        coursesFlow.value = coursesFlow.value + course.copy(id = newId)
        return newId
    }

    override suspend fun updateCourse(course: Course) {
        updated += course
        coursesFlow.value = coursesFlow.value.map { if (it.id == course.id) course else it }
    }

    override suspend fun deleteCourse(courseId: Long) {
        deleted += courseId
        coursesFlow.value = coursesFlow.value.filterNot { it.id == courseId }
    }

    fun setCourses(courses: List<Course>) {
        coursesFlow.value = courses
    }
}

fun testCourse(
    id: Long? = null,
    code: String = "MAT-101",
    name: String = "Cálculo",
    section: String = "A",
    teacher: String? = null,
    sessions: List<ClassSession> = emptyList()
): Course = Course(id = id, code = code, name = name, section = section, teacher = teacher, sessions = sessions)

fun testSession(
    id: Long? = null,
    dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    startTime: LocalTime = LocalTime.of(7, 0),
    endTime: LocalTime = LocalTime.of(9, 0),
    room: String? = null
): ClassSession = ClassSession(
    id = id,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    room = room
)
