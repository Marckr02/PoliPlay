package com.example.campuspocket.core.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.campuspocket.core.database.AppDatabase
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.academic.data.TaskReminderEntity
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.finance.data.TransactionType
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * Prueba instrumentada del respaldo (Room en memoria):
 * exportar -> borrar todo -> importar y comprobar que queda IDÉNTICO;
 * y un archivo inválido no cambia nada.
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        manager = BackupManager(db, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportarBorrarImportarDejaTodoIdentico() = runBlocking {
        // Datos con claves foráneas cruzadas (materia -> tarea -> recordatorio, cuentas, nota anidada, transferencia).
        val semesterId = db.semesterDao().insert(SemesterEntity(name = "2026-B", startDate = LocalDate.of(2026, 8, 1), endDate = LocalDate.of(2027, 2, 28), isActive = true))
        val courseId = db.courseDao().insert(CourseEntity(semesterId = semesterId, code = "ISWD713", name = "Apps", section = "GR1", teacher = null, colorArgb = 1))
        val taskId = db.taskDao().insert(TaskEntity(courseId = courseId, title = "Deber", dueAt = 123L, createdAt = 99L))
        db.taskReminderDao().insert(TaskReminderEntity(taskId = taskId, remindAt = 100L))
        val cashId = db.accountDao().insert(AccountEntity(name = "Efectivo", type = AccountType.CASH.name, initialBalanceCents = 1_000))
        val bankId = db.accountDao().insert(AccountEntity(name = "Banco", type = AccountType.DEBIT.name, initialBalanceCents = 500))
        val catId = db.categoryDao().insert(CategoryEntity(name = "Comida", kind = CategoryKind.EXPENSE.name, iconKey = "category"))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE.name, amountCents = 100, date = LocalDate.of(2026, 10, 1), accountId = cashId, categoryId = catId, createdAt = 50L))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.TRANSFER.name, amountCents = 200, date = LocalDate.of(2026, 10, 2), accountId = cashId, targetAccountId = bankId, createdAt = 51L))
        val folderId = db.folderDao().insert(FolderEntity(name = "Uni"))
        val subId = db.folderDao().insert(FolderEntity(name = "Cálculo", parentId = folderId))
        db.noteDao().insert(NoteEntity(title = "Apunte", content = "contenido", folderId = subId, pinned = true, createdAt = 7L, updatedAt = 8L))

        // EXPORTAR
        val out = ByteArrayOutputStream()
        manager.exportBackup(out)
        val backupText = out.toString(Charsets.UTF_8)
        assertTrue(backupText.contains("\"formatVersion\":1"))
        assertTrue(backupText.contains("\"dbVersion\":3"))

        // BORRAR TODO (simula pérdida total)
        db.clearAllTables()
        assertTrue(db.courseDao().getAll().isEmpty())

        // IMPORTAR
        val file = manager.readAndValidate(ByteArrayInputStream(backupText.toByteArray(Charsets.UTF_8)))
        manager.importBackup(file)

        // COMPROBAR IDENTIDAD (tabla por tabla)
        val semesters = db.semesterDao().observeAll().first()
        assertEquals(1, semesters.size)
        assertEquals("2026-B", semesters.single().name)
        assertEquals(semesterId, semesters.single().id)

        val course = db.courseDao().getAll().single()
        assertEquals(courseId, course.id)
        assertEquals(semesterId, course.semesterId)

        val task = db.taskDao().getById(taskId)!!
        assertEquals("Deber", task.title)
        assertEquals(courseId, task.courseId)
        assertEquals(99L, task.createdAt)
        assertEquals(1, db.taskReminderDao().getByTask(taskId).size)

        val accounts = db.accountDao().observeAll().first()
        assertEquals(2, accounts.size)
        assertEquals("Efectivo", accounts.first { it.id == cashId }.name)
        assertEquals(1_000L, accounts.first { it.id == cashId }.initialBalanceCents)

        val categories = db.categoryDao().observeAll().first()
        assertEquals(1, categories.size)
        assertEquals("Comida", categories.single().name)

        val transactions = db.transactionDao().observeAll().first()
        assertEquals(2, transactions.size)
        val transfer = transactions.first { it.type == TransactionType.TRANSFER.name }
        assertEquals(cashId, transfer.accountId)
        assertEquals(bankId, transfer.targetAccountId)

        val folders = db.folderDao().observeAll().first()
        assertEquals(2, folders.size)
        assertEquals(folderId, folders.first { it.name == "Cálculo" }.parentId)

        val note = db.noteDao().observeAll().first().single()
        assertEquals("Apunte", note.title)
        assertEquals(subId, note.folderId)
        assertTrue(note.pinned)
    }

    @Test
    fun archivoInvalidoNoCambiaNada() = runBlocking {
        // Datos previos
        db.accountDao().insert(AccountEntity(name = "Efectivo", type = AccountType.CASH.name))
        val before = db.accountDao().observeAll().first()

        // Texto que no es respaldo
        val ex = try {
            manager.readAndValidate(ByteArrayInputStream("no es un respaldo json".toByteArray(Charsets.UTF_8)))
            null
        } catch (e: BackupImportException) {
            e
        }
        assertEquals(BackupImportIssue.NOT_JSON, ex?.issue)

        // Versión futura inventada (JSON válido pero mal formato)
        val wrong = BackupFile(
            formatVersion = 999,
            dbVersion = BACKUP_DB_VERSION,
            exportedAt = "2026-10-04T00:00:00Z",
            tables = BackupTables()
        )
        val ex2 = try {
            manager.readAndValidate(ByteArrayInputStream(kotlinx.serialization.json.Json.encodeToString(wrong).toByteArray(Charsets.UTF_8)))
            null
        } catch (e: BackupImportException) {
            e
        }
        assertEquals(BackupImportIssue.WRONG_FORMAT_VERSION, ex2?.issue)

        // Los datos siguen intactos
        assertEquals(before, db.accountDao().observeAll().first())
    }
}
