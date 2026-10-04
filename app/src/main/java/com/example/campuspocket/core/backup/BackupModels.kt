package com.example.campuspocket.core.backup

import com.example.campuspocket.core.database.ImportHintEntity
import com.example.campuspocket.feature.academic.data.ClassSessionEntity
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.academic.data.TaskReminderEntity
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.BudgetEntity
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Formato del respaldo JSON (versionado). Las 13 tablas viajan con sus ids tal cual.
 * Reglas:
 * - formatVersion solo cambia si cambia la ESTRUCTURA del archivo.
 * - dbVersion debe coincidir con la de Room al importar (v2 congelada).
 * - Fechas como texto ISO ("yyyy-MM-dd"); booleans y Long tal cual.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val dbVersion: Int,
    val exportedAt: String,
    val tables: BackupTables
)

@Serializable
data class BackupTables(
    val semesters: List<SemesterData> = emptyList(),
    val courses: List<CourseData> = emptyList(),
    @SerialName("class_sessions") val classSessions: List<ClassSessionData> = emptyList(),
    val tasks: List<TaskData> = emptyList(),
    @SerialName("task_reminders") val taskReminders: List<TaskReminderData> = emptyList(),
    val accounts: List<AccountData> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val budgets: List<BudgetData> = emptyList(),
    val transactions: List<TransactionData> = emptyList(),
    @SerialName("scheduled_payments") val scheduledPayments: List<ScheduledPaymentData> = emptyList(),
    val folders: List<FolderData> = emptyList(),
    val notes: List<NoteData> = emptyList(),
    @SerialName("import_hints") val importHints: List<ImportHintData> = emptyList()
)

@Serializable data class SemesterData(val id: Long, val name: String, val startDate: String, val endDate: String, val isActive: Boolean)
@Serializable data class CourseData(val id: Long, val semesterId: Long, val code: String, val name: String, val section: String, val teacher: String?, val colorArgb: Int)
@Serializable data class ClassSessionData(val id: Long, val courseId: Long, val dayOfWeek: Int, val startMinute: Int, val endMinute: Int, val room: String?, val credits: Int?)
@Serializable data class TaskData(val id: Long, val courseId: Long?, val title: String, val description: String?, val dueAt: Long, val priority: String, val completedAt: Long?, val createdAt: Long)
@Serializable data class TaskReminderData(val id: Long, val taskId: Long, val remindAt: Long)
@Serializable data class AccountData(val id: Long, val name: String, val type: String, val initialBalanceCents: Long, val colorArgb: Int, val includeInTotal: Boolean, val archived: Boolean, val sortOrder: Int)
@Serializable data class CategoryData(val id: Long, val name: String, val kind: String, val iconKey: String, val colorArgb: Int, val archived: Boolean)
@Serializable data class BudgetData(val id: Long, val yearMonth: String, val categoryId: Long, val amountCents: Long)
@Serializable data class TransactionData(val id: Long, val type: String, val amountCents: Long, val date: String, val accountId: Long, val targetAccountId: Long?, val categoryId: Long?, val description: String?, val refundOfId: Long?, val scheduledPaymentId: Long?, val createdAt: Long)
@Serializable data class ScheduledPaymentData(val id: Long, val name: String, val amountCents: Long, val accountId: Long, val categoryId: Long?, val frequency: String, val nextDueDate: String, val anchorDayOfMonth: Int = 0, val remindDaysBefore: Int, val active: Boolean)
@Serializable data class FolderData(val id: Long, val name: String, val parentId: Long?)
@Serializable data class NoteData(val id: Long, val title: String, val content: String, val folderId: Long?, val courseId: Long?, val taskId: Long?, val pinned: Boolean, val createdAt: Long, val updatedAt: Long)
@Serializable data class ImportHintData(val rawKey: String, val cleanValue: String)

// ---- Mapeos entidad <-> datos del respaldo --------------------------------

fun SemesterEntity.toBackup() = SemesterData(id, name, startDate.toString(), endDate.toString(), isActive)
fun SemesterData.toEntity() = SemesterEntity(id, name, LocalDate.parse(startDate), LocalDate.parse(endDate), isActive)

fun CourseEntity.toBackup() = CourseData(id, semesterId, code, name, section, teacher, colorArgb)
fun CourseData.toEntity() = CourseEntity(id, semesterId, code, name, section, teacher, colorArgb)

fun ClassSessionEntity.toBackup() = ClassSessionData(id, courseId, dayOfWeek, startMinute, endMinute, room, credits)
fun ClassSessionData.toEntity() = ClassSessionEntity(id, courseId, dayOfWeek, startMinute, endMinute, room, credits)

fun TaskEntity.toBackup() = TaskData(id, courseId, title, description, dueAt, priority, completedAt, createdAt)
fun TaskData.toEntity() = TaskEntity(id, courseId, title, description, dueAt, priority, completedAt, createdAt)

fun TaskReminderEntity.toBackup() = TaskReminderData(id, taskId, remindAt)
fun TaskReminderData.toEntity() = TaskReminderEntity(id, taskId, remindAt)

fun AccountEntity.toBackup() = AccountData(id, name, type, initialBalanceCents, colorArgb, includeInTotal, archived, sortOrder)
fun AccountData.toEntity() = AccountEntity(id, name, type, initialBalanceCents, colorArgb, includeInTotal, archived, sortOrder)

fun CategoryEntity.toBackup() = CategoryData(id, name, kind, iconKey, colorArgb, archived)
fun CategoryData.toEntity() = CategoryEntity(id, name, kind, iconKey, colorArgb, archived)

fun BudgetEntity.toBackup() = BudgetData(id, yearMonth, categoryId, amountCents)
fun BudgetData.toEntity() = BudgetEntity(id, yearMonth, categoryId, amountCents)

fun TransactionEntity.toBackup() = TransactionData(id, type, amountCents, date.toString(), accountId, targetAccountId, categoryId, description, refundOfId, scheduledPaymentId, createdAt)
fun TransactionData.toEntity() = TransactionEntity(id, type, amountCents, LocalDate.parse(date), accountId, targetAccountId, categoryId, description, refundOfId, scheduledPaymentId, createdAt)

fun ScheduledPaymentEntity.toBackup() = ScheduledPaymentData(id, name, amountCents, accountId, categoryId, frequency, nextDueDate.toString(), anchorDayOfMonth, remindDaysBefore, active)
fun ScheduledPaymentData.toEntity() = ScheduledPaymentEntity(id, name, amountCents, accountId, categoryId, frequency, LocalDate.parse(nextDueDate), anchorDayOfMonth, remindDaysBefore, active)

fun FolderEntity.toBackup() = FolderData(id, name, parentId)
fun FolderData.toEntity() = FolderEntity(id, name, parentId)

fun NoteEntity.toBackup() = NoteData(id, title, content, folderId, courseId, taskId, pinned, createdAt, updatedAt)
fun NoteData.toEntity() = NoteEntity(id, title, content, folderId, courseId, taskId, pinned, createdAt, updatedAt)

fun ImportHintEntity.toBackup() = ImportHintData(rawKey, cleanValue)
fun ImportHintData.toEntity() = ImportHintEntity(rawKey, cleanValue)
