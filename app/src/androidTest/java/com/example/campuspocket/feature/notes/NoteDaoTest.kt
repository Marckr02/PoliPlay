package com.example.campuspocket.feature.notes

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import com.example.campuspocket.feature.notes.domain.escapeLikePattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * NoteDao en el emulador: sin carpeta con IS NULL, LIKE con comodines escapados,
 * y efectos SET_NULL al borrar la carpeta, la materia o la tarea.
 */
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun sinCarpetaYLikerEscapado() = runBlocking {
        // carpetas y notas
        val folder = db.folderDao().insert(FolderEntity(name = "Universidad"))
        val taskSemester = db.semesterDao().insert(SemesterEntity(name = "2026-B", startDate = LocalDate.of(2026, 8, 1), endDate = LocalDate.of(2027, 2, 28), isActive = true))
        val courseId = db.courseDao().insert(CourseEntity(semesterId = taskSemester, code = "FIS101", name = "Física", section = "A", teacher = null))
        val taskId = db.taskDao().insert(TaskEntity(courseId = courseId, title = "Deber", dueAt = 100L))

        val noteRoot = db.noteDao().insert(NoteEntity(title = "root", content = "sin carpeta"))
        val noteInFolder = db.noteDao().insert(NoteEntity(title = "comida 100% segura", content = "al_100%", folderId = folder, courseId = courseId, taskId = taskId))
        val noteTask = db.noteDao().insert(NoteEntity(title = "linked task", content = "", taskId = taskId))

        // IS NULL: notas sin carpeta (la root y la enlazada a tarea sin carpeta)
        assertEquals(setOf(noteRoot, noteTask), db.noteDao().observeWithoutFolder().first().map { it.id }.toSet())

        // LIKE con comodines: busco literal "100%" y "al_" deben escaparse
        val patternPercent = "%" + escapeLikePattern("100%") + "%"
        val patternUnderscore = "%" + escapeLikePattern("al_1") + "%"
        val resultsPercent = db.noteDao().search(patternPercent).first()
        val resultsUnderscore = db.noteDao().search(patternUnderscore).first()
        assertEquals(listOf(noteInFolder), resultsPercent.map { it.id })
        assertEquals(listOf(noteInFolder), resultsUnderscore.map { it.id })

        // borrar carpeta: nota queda sin carpeta (SET_NULL), borrar materia: curso/tarea = null
        db.folderDao().delete(FolderEntity(id = folder, name = "Universidad"))
        assertNull(db.noteDao().getById(noteInFolder)!!.folderId)

        db.courseDao().deleteCourse(courseId)
        assertNull(db.noteDao().getById(noteInFolder)!!.courseId)

        db.taskDao().delete(TaskEntity(id = taskId, courseId = courseId, title = "Deber", dueAt = 100L))
        val withoutTask = db.noteDao().getById(noteInFolder)!!
        val taskNote = db.noteDao().getById(noteTask)!!
        assertNull(withoutTask.taskId)
        assertNull(taskNote.taskId)
    }
}
