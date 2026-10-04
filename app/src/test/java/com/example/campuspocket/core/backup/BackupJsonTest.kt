package com.example.campuspocket.core.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas JVM del formato del respaldo (sin Android): ida y vuelta del JSON
 * conserva TODAS las tablas con sus ids, y la validación rechaza lo que no sirve.
 */
class BackupJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleBackup(): BackupFile = BackupFile(
        formatVersion = BACKUP_FORMAT_VERSION,
        dbVersion = BACKUP_DB_VERSION,
        exportedAt = "2026-10-04T12:00:00Z",
        tables = BackupTables(
            semesters = listOf(
                SemesterData(1, "2026-B", "2026-08-01", "2027-02-28", true)
            ),
            courses = listOf(
                CourseData(1, 1, "ISWD713", "Aplicaciones Móviles", "GR1", "Docente", 0xFF4C8DFF.toInt())
            ),
            classSessions = listOf(
                ClassSessionData(1, 1, 1, 420, 540, "E20/P3/E003", 3)
            ),
            tasks = listOf(
                TaskData(1, 1, "Deber", "descripción", 1_793_001_600_000, "HIGH", null, 1_792_990_000_000)
            ),
            taskReminders = listOf(TaskReminderData(1, 1, 1_793_001_000_000)),
            accounts = listOf(
                AccountData(1, "Efectivo", "CASH", 10_000, 0xFF4C8DFF.toInt(), true, false, 0),
                AccountData(2, "Visa", "CREDIT_CARD", -20_000, 0xFFE5484D.toInt(), false, false, 1)
            ),
            categories = listOf(
                CategoryData(1, "Comida", "EXPENSE", "restaurant", 0xFFE5484D.toInt(), false)
            ),
            budgets = listOf(BudgetData(1, "2026-10", 1, 100_000)),
            transactions = listOf(
                TransactionData(1, "EXPENSE", 300, "2026-10-01", 1, null, 1, "almuerzo", null, null, 1_793_000_000_000),
                TransactionData(2, "TRANSFER", 2_000, "2026-10-02", 1, 2, null, null, null, null, 1_793_000_001_000)
            ),
            scheduledPayments = listOf(
                ScheduledPaymentData(1, "Netflix", 1_500, 1, 1, "MONTHLY", "2026-10-15", 0, 3, true)
            ),
            folders = listOf(FolderData(1, "Universidad", null), FolderData(2, "Cálculo", 1)),
            notes = listOf(
                NoteData(1, "Apuntes", "texto **negrita**", 2, null, null, false, 1_792_000_000_000, 1_792_000_000_000)
            ),
            importHints = listOf(ImportHintData("vista previa generada", "Vista Previa Generada"))
        )
    )

    @Test
    fun `ida y vuelta conserva las 13 tablas con ids y fechas`() {
        val original = sampleBackup()

        val text = json.encodeToString(original)
        val parsed = json.decodeFromString<BackupFile>(text)

        assertEquals(original, parsed)
        assertEquals(1, parsed.formatVersion)
        assertEquals(BACKUP_DB_VERSION, parsed.dbVersion)
        assertEquals("2026-10-04T12:00:00Z", parsed.exportedAt)
    }

    @Test
    fun `fechas en ISO se leen como LocalDate de vuelta`() {
        val backup = sampleBackup()
        val semester = backup.tables.semesters.single().toEntity()
        assertEquals(LocalDate.of(2026, 8, 1), semester.startDate)
        assertEquals(LocalDate.of(2027, 2, 28), semester.endDate)

        val tx = backup.tables.transactions.first().toEntity()
        assertEquals(LocalDate.of(2026, 10, 1), tx.date)
    }

    @Test
    fun `texto que no es JSON lanza NOT_JSON sin tocar nada`() {
        val ex = try {
            jsonToBackup("esto no es json")
            null
        } catch (e: BackupImportException) {
            e
        }
        assertEquals(BackupImportIssue.NOT_JSON, ex?.issue)
    }

    @Test
    fun `otra version de formato o de esquema se rechaza sin tocar nada`() {
        val wrongFormat = sampleBackup().copy(formatVersion = 99)
        val ex1 = try {
            jsonToBackup(json.encodeToString(wrongFormat)); null
        } catch (e: BackupImportException) { e }
        assertEquals(BackupImportIssue.WRONG_FORMAT_VERSION, ex1?.issue)

        val wrongDb = sampleBackup().copy(dbVersion = 99)
        val ex2 = try {
            jsonToBackup(json.encodeToString(wrongDb)); null
        } catch (e: BackupImportException) { e }
        assertEquals(BackupImportIssue.WRONG_DB_VERSION_NEW, ex2?.issue)

        val tooOld = sampleBackup().copy(dbVersion = 1)
        val ex3 = try {
            jsonToBackup(json.encodeToString(tooOld)); null
        } catch (e: BackupImportException) { e }
        assertEquals(BackupImportIssue.WRONG_DB_VERSION_OLD, ex3?.issue)
    }

    /** Mismo camino de validación que BackupManager.readAndValidate, sin streams. */
    private fun jsonToBackup(text: String): BackupFile {
        val file = try {
            json.decodeFromString<BackupFile>(text)
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
        if (file.dbVersion == 2) {
            return file.copy(
                tables = file.tables.copy(
                    scheduledPayments = file.tables.scheduledPayments.map { p ->
                        p.copy(anchorDayOfMonth = java.time.LocalDate.parse(p.nextDueDate).dayOfMonth)
                    }
                ),
                dbVersion = BACKUP_DB_VERSION
            )
        }
        return file
    }
}
