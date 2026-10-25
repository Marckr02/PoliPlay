package com.example.campuspocket.feature.academic.domain.repository

import com.example.campuspocket.feature.academic.domain.model.Course
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Materias del semestre activo, con sus sesiones de clase incluidas.
 * Si todavía no hay semestre activo, insertar crea uno con los límites del periodo actual.
 */
interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>

    /** La materia con sus sesiones; emite null y se actualiza si se borra o se edita. */
    fun observeCourse(courseId: Long): Flow<Course?>

    suspend fun getCourse(courseId: Long): Course?
    suspend fun insertCourse(course: Course): Long
    suspend fun updateCourse(course: Course)
    suspend fun deleteCourse(courseId: Long)

    /** Fin del semestre activo; null si aún no hay ninguno (los fakes pueden omitirlo). */
    suspend fun getActiveSemesterEnd(): LocalDate? = null
}
