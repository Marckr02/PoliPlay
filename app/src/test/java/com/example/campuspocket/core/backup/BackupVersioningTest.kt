package com.example.campuspocket.core.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Pruebas JVM del respaldo endurecido (Fase 7): acepta v2 (deriva anchorDay) y rechaza > actual.
 */
class BackupVersioningTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `un respaldo v2 sin anchorDayOfMonth lo deriva de nextDueDate`() {
        val v2Json = """{
          "formatVersion": 1, "dbVersion": 2, "exportedAt": "2026-10-04T00:00:00Z",
          "tables": {
            "semesters": [], "courses": [], "class_sessions": [], "tasks": [], "task_reminders": [],
            "accounts": [{"id":1,"name":"Efectivo","type":"CASH","initialBalanceCents":0,"colorArgb":0,"includeInTotal":true,"archived":false,"sortOrder":0}],
            "categories": [{"id":1,"name":"Comida","kind":"EXPENSE","iconKey":"category","colorArgb":0,"archived":false}],
            "budgets": [], "transactions": [],
            "scheduled_payments": [{"id":1,"name":"Arriendo","amountCents":50000,"accountId":1,"categoryId":1,"frequency":"MONTHLY","nextDueDate":"2026-01-31","remindDaysBefore":3,"active":true}],
            "folders": [], "notes": [], "import_hints": []
          }
        }"""
        val file = readAndValidate(v2Json)
        assertEquals(3, file.dbVersion) // normalizado a la actual
        assertEquals(31, file.tables.scheduledPayments.single().anchorDayOfMonth)
    }

    @Test
    fun `un respaldo v3 se toma tal cual`() {
        val v3 = BackupFile(
            formatVersion = BACKUP_FORMAT_VERSION, dbVersion = BACKUP_DB_VERSION,
            exportedAt = "2026-10-04T00:00:00Z",
            tables = BackupTables(
                scheduledPayments = listOf(
                    ScheduledPaymentData(1, "Pago", 1_000, 1, 1, "MONTHLY", "2026-10-15", 0, 3, true)
                )
            )
        )
        val file = readAndValidate(json.encodeToString(v3))
        assertEquals(BACKUP_DB_VERSION, file.dbVersion)
        assertEquals(0, file.tables.scheduledPayments.single().anchorDayOfMonth)
    }

    @Test
    fun `un respaldo demasiado viejo o demasiado nuevo se rechaza sin tocar nada`() {
        val old = json.encodeToString(BackupFile(1, 1, "2026-01-01T00:00:00Z", BackupTables()))
        val ex1 = try { readAndValidate(old); null } catch (e: BackupImportException) { e }
        assertEquals(BackupImportIssue.WRONG_DB_VERSION_OLD, ex1?.issue)

        val future = json.encodeToString(BackupFile(1, 99, "2026-01-01T00:00:00Z", BackupTables()))
        val ex2 = try { readAndValidate(future); null } catch (e: BackupImportException) { e }
        assertEquals(BackupImportIssue.WRONG_DB_VERSION_NEW, ex2?.issue)
    }

    /** Mismo camino que el producto real pero leyendo de texto (sin InputStream de Android). */
    private fun readAndValidate(text: String): BackupFile {
        val file = try { json.decodeFromString<BackupFile>(text) } catch (e: Exception) {
            throw BackupImportException(BackupImportIssue.NOT_JSON)
        }
        if (file.formatVersion != BACKUP_FORMAT_VERSION) throw BackupImportException(BackupImportIssue.WRONG_FORMAT_VERSION)
        if (file.dbVersion < BACKUP_MIN_DB_VERSION) throw BackupImportException(BackupImportIssue.WRONG_DB_VERSION_OLD)
        if (file.dbVersion > BACKUP_DB_VERSION) throw BackupImportException(BackupImportIssue.WRONG_DB_VERSION_NEW)
        if (file.dbVersion == 2) {
            return file.copy(tables = file.tables.copy(
                scheduledPayments = file.tables.scheduledPayments.map { p ->
                    p.copy(anchorDayOfMonth = java.time.LocalDate.parse(p.nextDueDate).dayOfMonth)
                }
            ), dbVersion = BACKUP_DB_VERSION)
        }
        return file
    }
}
