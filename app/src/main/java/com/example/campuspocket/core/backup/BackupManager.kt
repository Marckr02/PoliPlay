package com.example.campuspocket.core.backup

import androidx.room.withTransaction
import com.example.campuspocket.core.database.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.time.LocalDate
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

const val BACKUP_FORMAT_VERSION = 1
const val BACKUP_DB_VERSION = 3   // esquema actual de Room (v3, con anchorDayOfMonth)
const val BACKUP_MIN_DB_VERSION = 2   // aceptamos también respaldos de la v2

/** Por qué un respaldo no sirve (la UI lo traduce con strings.xml). */
enum class BackupImportIssue {
    NOT_JSON,
    WRONG_FORMAT_VERSION,
    /** dbVersion < MIN (demasiado vieja) o > CURRENT (de una app más nueva). */
    WRONG_DB_VERSION_OLD,
    WRONG_DB_VERSION_NEW
}

class BackupImportException(val issue: BackupImportIssue) : Exception("Respaldo no válido: $issue")

/**
 * Exporta e importa el respaldo JSON completo (las 13 tablas, con ids).
 * La importación es todo-o-nada: valida ANTES de tocar nada y corre en UNA
 * transacción de Room (si algo falla, la base de datos queda intacta).
 */
@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ---- Exportar ----------------------------------------------------------

    suspend fun exportBackup(out: OutputStream) = withContext(ioDispatcher) {
        val tables = BackupTables(
            semesters = db.semesterDao().observeAll().firstList().map { it.toBackup() },
            courses = db.courseDao().getAll().map { it.toBackup() },
            classSessions = db.classSessionDao().observeAll().firstList().map { it.toBackup() },
            tasks = db.taskDao().getAll().map { it.toBackup() },
            taskReminders = db.taskReminderDao().getAllSuspend().map { it.toBackup() },
            accounts = db.accountDao().observeAll().firstList().map { it.toBackup() },
            categories = db.categoryDao().observeAll().firstList().map { it.toBackup() },
            budgets = db.budgetDao().getAll().map { it.toBackup() },
            transactions = db.transactionDao().observeAll().firstList().map { it.toBackup() },
            scheduledPayments = db.scheduledPaymentDao().observeAll().firstList().map { it.toBackup() },
            folders = db.folderDao().observeAll().firstList().map { it.toBackup() },
            notes = db.noteDao().observeAll().firstList().map { it.toBackup() },
            importHints = db.importHintDao().getAll().map { it.toBackup() }
        )
        val file = BackupFile(
            formatVersion = BACKUP_FORMAT_VERSION,
            dbVersion = BACKUP_DB_VERSION,
            exportedAt = Instant.now().toString(),
            tables = tables
        )
        out.bufferedWriter(Charsets.UTF_8).use { it.write(json.encodeToString(file)) }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.firstList(): List<T> =
        this.first()

    // ---- Importar ----------------------------------------------------------

    /**
     * Solo lee y valida el archivo; NO toca la base de datos.
     * Lanza [BackupImportException] si no sirve.
     */
    fun readAndValidate(input: InputStream): BackupFile {
        val file = try {
            json.decodeFromString<BackupFile>(input.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } catch (e: Exception) {
            throw BackupImportException(BackupImportIssue.NOT_JSON)
        }
        if (file.formatVersion != BACKUP_FORMAT_VERSION) {
            throw BackupImportException(BackupImportIssue.WRONG_FORMAT_VERSION)
        }
        if (file.dbVersion < BACKUP_MIN_DB_VERSION) {
            throw BackupImportException(BackupImportIssue.WRONG_DB_VERSION_OLD)
        }
        if (file.dbVersion > BACKUP_DB_VERSION) {
            throw BackupImportException(BackupImportIssue.WRONG_DB_VERSION_NEW)
        }
        // Archivo de la v2: deriva anchorDayOfMonth del día de la próxima fecha (spec 7.4, Fase 5B).
        if (file.dbVersion == 2) {
            val migrated = file.copy(
                tables = file.tables.copy(
                    scheduledPayments = file.tables.scheduledPayments.map { p ->
                        p.copy(anchorDayOfMonth = LocalDate.parse(p.nextDueDate).dayOfMonth)
                    }
                )
            )
            return migrated.copy(dbVersion = BACKUP_DB_VERSION)
        }
        return file
    }

    /**
     * Reemplaza TODOS los datos con los del respaldo, en una sola transacción.
     * defer_foreign_keys permite insertar en cualquier orden dentro de la
     * transacción (las FKs se verifican al commit; si falla, se revierte todo).
     */
    suspend fun importBackup(file: BackupFile) = withContext(ioDispatcher) {
        db.withTransaction {
            // Difiere la verificación de claves foráneas hasta el commit.
            db.openHelper.writableDatabase.execSQL("PRAGMA defer_foreign_keys = ON")

            // Borrado (hijos antes que padres).
            listOf(
                "transactions", "budgets", "task_reminders", "tasks", "class_sessions",
                "courses", "notes", "scheduled_payments", "folders", "import_hints",
                "categories", "accounts", "semesters"
            ).forEach { table ->
                db.openHelper.writableDatabase.execSQL("DELETE FROM `$table`")
            }

            // Inserción (padres antes que hijos; los ids viajan explícitos).
            val t = file.tables
            t.semesters.forEach { db.semesterDao().insert(it.toEntity()) }
            t.accounts.forEach { db.accountDao().insert(it.toEntity()) }
            t.categories.forEach { db.categoryDao().insert(it.toEntity()) }
            t.importHints.forEach { db.importHintDao().upsert(it.toEntity()) }
            t.folders.forEach { db.folderDao().insert(it.toEntity()) }
            t.scheduledPayments.forEach { db.scheduledPaymentDao().insert(it.toEntity()) }
            t.notes.forEach { db.noteDao().insert(it.toEntity()) }
            t.courses.forEach { db.courseDao().insert(it.toEntity()) }
            t.classSessions.forEach { db.classSessionDao().insert(it.toEntity()) }
            t.tasks.forEach { db.taskDao().insert(it.toEntity()) }
            t.taskReminders.forEach { db.taskReminderDao().insert(it.toEntity()) }
            t.budgets.forEach { db.budgetDao().insert(it.toEntity()) }
            t.transactions.forEach { db.transactionDao().insert(it.toEntity()) }
        }
    }
}
