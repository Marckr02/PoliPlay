package com.example.campuspocket.feature.academic.importer

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.academic.data.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Prueba instrumentada de la fusión de la reimportación, contra Room real (en memoria).
 * Lo que ImportMergePlan decide de forma pura aquí se comprueba ya ejecutado:
 * ids conservados, sesiones reemplazadas, tareas intactas y borrados limitados.
 */
@RunWith(AndroidJUnit4::class)
class ImportMergeTest {

    private lateinit var db: AppDatabase
    private lateinit var useCase: ImportScheduleUseCase

    private val start = LocalDate.of(2026, 8, 1)
    private val end = LocalDate.of(2027, 2, 28)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        useCase = ImportScheduleUseCase(db, db.semesterDao(), db.courseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun course(code: String, name: String = "Materia $code") = ImportedCourse(
        code = code,
        name = name,
        section = "GR1",
        teacher = "Docente",
        sessions = listOf(ImportedSession(dayOfWeek = 1, startMinute = 420, endMinute = 540, room = "E20", credits = 3))
    )

    private suspend fun semesterId(): Long = db.semesterDao().getByName("2026-B")!!.id

    @Test
    fun reimportarConservaElIdReemplazaSesionesYMantieneLasTareas() = runBlocking {
        useCase.save("2026-B", start, end, listOf(course("ISWD713")), emptyList())
        val original = db.courseDao().getBySemester(semesterId()).single()
        val taskId = db.taskDao().insert(TaskEntity(courseId = original.id, title = "Deber", dueAt = 123L))

        // Reimporta la misma materia con otro nombre y una sesión extra.
        val reviewed = course("ISWD713", name = "Aplicaciones Móviles").let {
            it.copy(sessions = it.sessions + ImportedSession(2, 660, 780, "E21", 3))
        }
        useCase.save("2026-B", start, end, listOf(reviewed), emptyList())

        val merged = db.courseDao().getBySemester(semesterId()).single()
        assertEquals(original.id, merged.id)
        assertEquals("Aplicaciones Móviles", merged.name)
        assertEquals(2, db.courseDao().getWithSessionsById(merged.id)!!.sessions.size)
        assertEquals(original.id, db.taskDao().getById(taskId)!!.courseId)
    }

    @Test
    fun materiaAusenteSinConfirmarSigueAhi() = runBlocking {
        useCase.save("2026-B", start, end, listOf(course("ISWD713"), course("ADMD700")), emptyList())

        // El PDF ya no trae ADMD700, pero el usuario no confirmó borrarla.
        useCase.save("2026-B", start, end, listOf(course("ISWD713")), emptyList())

        val codes = db.courseDao().getBySemester(semesterId()).map { it.code }.sorted()
        assertEquals(listOf("ADMD700", "ISWD713"), codes)
    }

    @Test
    fun ausenteConfirmadaSeBorraYSuTareaQuedaSinMateria() = runBlocking {
        useCase.save("2026-B", start, end, listOf(course("ISWD713"), course("ADMD700")), emptyList())
        val absent = db.courseDao().getBySemester(semesterId()).single { it.code == "ADMD700" }
        val taskId = db.taskDao().insert(TaskEntity(courseId = absent.id, title = "Deber", dueAt = 123L))

        useCase.save("2026-B", start, end, listOf(course("ISWD713")), listOf(absent.id))

        val remaining = db.courseDao().getBySemester(semesterId())
        assertEquals(listOf("ISWD713"), remaining.map { it.code })
        assertNull(db.taskDao().getById(taskId)!!.courseId) // FK SET_NULL deliberada
    }

    @Test
    fun noSeBorraNadaFueraDeLosAusentesCalculados() = runBlocking {
        useCase.save("2026-B", start, end, listOf(course("ISWD713")), emptyList())
        val presentId = db.courseDao().getBySemester(semesterId()).single().id

        // Llega a "confirmarse" el id de una materia que SÍ está en el PDF: no debe borrarse.
        useCase.save("2026-B", start, end, listOf(course("ISWD713")), listOf(presentId))

        assertNotNull(db.courseDao().getById(presentId))
    }
}
