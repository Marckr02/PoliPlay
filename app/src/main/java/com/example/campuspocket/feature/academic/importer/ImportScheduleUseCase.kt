package com.example.campuspocket.feature.academic.importer

import androidx.room.withTransaction
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.academic.data.ClassSessionEntity
import com.example.campuspocket.feature.academic.data.CourseDao
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.SemesterDao
import com.example.campuspocket.feature.academic.data.SemesterEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Materia tal como queda tras la revisión del usuario (lista para guardar). */
data class ImportedSession(
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String?,
    val credits: Int?
)

data class ImportedCourse(
    val code: String,
    val name: String,
    val section: String,
    val teacher: String?,
    val sessions: List<ImportedSession>
)

/**
 * Guardado de la importación, en UNA transacción de Room, con FUSIÓN en vez de
 * borrado total: se empareja por (semestre, código, paralelo).
 * - Materia ya guardada: se actualiza en su sitio (mismo id -> sus tareas quedan intactas)
 *   y se reemplazan solo sus sesiones.
 * - Materia nueva: se inserta.
 * - Materia guardada que el PDF ya no trae: se borra SOLO si el usuario lo confirmó
 *   (`deleteAbsentCourseIds`); la cascada borra sus sesiones y sus tareas quedan
 *   con courseId = null (SET_NULL deliberado).
 * - El semestre importado queda activo (nunca hay dos activos).
 */
@Singleton
class ImportScheduleUseCase @Inject constructor(
    private val db: AppDatabase,
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao
) {

    /** Materias guardadas del semestre con ese nombre (para previsualizar la fusión). */
    suspend fun semesterCourses(termName: String): List<CourseEntity> =
        semesterDao.getByName(termName)?.let { courseDao.getBySemester(it.id) } ?: emptyList()

    suspend fun save(
        termName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        courses: List<ImportedCourse>,
        deleteAbsentCourseIds: List<Long>
    ) {
        db.withTransaction {
            // Semestre: reutiliza el existente por nombre o lo crea; queda activo siempre.
            val existingSemester = semesterDao.getByName(termName)
            val semesterId = if (existingSemester != null) {
                semesterDao.update(existingSemester.copy(startDate = startDate, endDate = endDate))
                existingSemester.id
            } else {
                semesterDao.insert(
                    SemesterEntity(name = termName, startDate = startDate, endDate = endDate, isActive = true)
                )
            }
            semesterDao.setActive(semesterId)

            // La fusión se decide en una función pura (probada en JVM); aquí solo se ejecuta.
            val plan = planImportMerge(
                existing = courseDao.getBySemester(semesterId).map {
                    ExistingCourseRef(id = it.id, code = it.code, section = it.section, colorArgb = it.colorArgb)
                },
                imported = courses
            )

            plan.updates.forEach { update ->
                // Mismo id: las tareas vinculadas siguen apuntando a esta materia.
                courseDao.updateWithSessions(
                    CourseEntity(
                        id = update.existingId,
                        semesterId = semesterId,
                        code = update.course.code,
                        name = update.course.name,
                        section = update.course.section,
                        teacher = update.course.teacher,
                        colorArgb = update.colorArgb
                    ),
                    update.course.sessions.toEntities()
                )
            }
            plan.inserts.forEach { insert ->
                courseDao.insertWithSessions(
                    CourseEntity(
                        semesterId = semesterId,
                        code = insert.course.code,
                        name = insert.course.name,
                        section = insert.course.section,
                        teacher = insert.course.teacher,
                        colorArgb = insert.colorArgb
                    ),
                    insert.course.sessions.toEntities()
                )
            }

            // Solo se borran ausentes confirmados; cualquier otro id se descarta.
            plan.confirmedDeletions(deleteAbsentCourseIds).forEach { courseDao.deleteCourse(it) }
        }
    }

    private fun List<ImportedSession>.toEntities(): List<ClassSessionEntity> = map { session ->
        ClassSessionEntity(
            courseId = 0, // insertWithSessions/updateWithSessions le asignan el id real
            dayOfWeek = session.dayOfWeek,
            startMinute = session.startMinute,
            endMinute = session.endMinute,
            room = session.room,
            credits = session.credits
        )
    }
}
