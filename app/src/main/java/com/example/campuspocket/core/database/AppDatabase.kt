package com.example.campuspocket.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.campuspocket.feature.academic.data.ClassSessionDao
import com.example.campuspocket.feature.academic.data.ClassSessionEntity
import com.example.campuspocket.feature.academic.data.CourseDao
import com.example.campuspocket.feature.academic.data.CourseEntity
import com.example.campuspocket.feature.academic.data.SemesterDao
import com.example.campuspocket.feature.academic.data.SemesterEntity
import com.example.campuspocket.feature.academic.data.SubtaskEntity
import com.example.campuspocket.feature.academic.data.TaskDao
import com.example.campuspocket.feature.academic.data.TaskEntity
import com.example.campuspocket.feature.academic.data.TaskReminderDao
import com.example.campuspocket.feature.academic.data.TaskReminderEntity
import com.example.campuspocket.feature.finance.data.AccountDao
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.BudgetDao
import com.example.campuspocket.feature.finance.data.BudgetEntity
import com.example.campuspocket.feature.finance.data.CategoryDao
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.MonthlyBudgetDao
import com.example.campuspocket.feature.finance.data.MonthlyBudgetEntity
import com.example.campuspocket.feature.finance.data.ScheduledPaymentDao
import com.example.campuspocket.feature.finance.data.ScheduledPaymentEntity
import com.example.campuspocket.feature.finance.data.TransactionDao
import com.example.campuspocket.feature.finance.data.TransactionEntity
import com.example.campuspocket.feature.finance.data.TransactionTemplateDao
import com.example.campuspocket.feature.finance.data.TransactionTemplateEntity
import com.example.campuspocket.feature.notes.data.FolderDao
import com.example.campuspocket.feature.notes.data.FolderEntity
import com.example.campuspocket.feature.notes.data.NoteDao
import com.example.campuspocket.feature.notes.data.NoteEntity

/**
 * Base de datos única de la app. Se construye en DatabaseModule (Hilt).
 *
 * Esquema actual v5. Cada cambio de esquema debe subir `version`, agregar su Migration
 * en `Migrations` y actualizar los snapshots de `app/schemas`; DatabaseModule no usa
 * borrado destructivo.
 */
@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        ClassSessionEntity::class,
        TaskEntity::class,
        TaskReminderEntity::class,
        SubtaskEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        MonthlyBudgetEntity::class,
        TransactionEntity::class,
        TransactionTemplateEntity::class,
        ScheduledPaymentEntity::class,
        FolderEntity::class,
        NoteEntity::class,
        ImportHintEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun taskDao(): TaskDao
    abstract fun taskReminderDao(): TaskReminderDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun scheduledPaymentDao(): ScheduledPaymentDao
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun importHintDao(): ImportHintDao
}
