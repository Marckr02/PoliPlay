package com.example.campuspocket.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Pruebas instrumentadas de migración de Room.
 *
 * OJO: el `schemas/1.json` exportado se regeneró durante el desarrollo (ya trae los
 * cambios finales), así que las versiones viejas se crean A MANO con su DDL real:
 * - v1: 13 tablas CON `settings` y SIN `index_budgets_categoryId`/`index_transactions_refundOfId`.
 * - v2: las 13 tablas finales SIN `anchorDayOfMonth` en `scheduled_payments`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    /** v1 -> v2: retira `settings` y crea los dos índices. */
    @Test
    @Throws(IOException::class)
    fun migracionRealDe1a2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DB_1)

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_1), null).use { raw ->
            raw.execSQL("PRAGMA foreign_keys=OFF")
            V1_DDL.forEach { raw.execSQL(it) }
            raw.version = 1
        }

        val db = helper.runMigrationsAndValidate(DB_1, 2, true, Migrations.MIGRATION_1_2)
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='settings'").use {
            assertFalse("`settings` debió desaparecer", it.moveToFirst())
        }
        listOf("index_budgets_categoryId", "index_transactions_refundOfId").forEach { index ->
            db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='$index'").use {
                assertTrue("Falta `$index` tras la migración", it.moveToFirst())
            }
        }
        db.close()
        context.deleteDatabase(DB_1)
    }

    /** v2 -> v3: `scheduled_payments` gana `anchorDayOfMonth` (DEFAULT 0). */
    @Test
    @Throws(IOException::class)
    fun migracionRealDe2a3() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DB_2)

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_2), null).use { raw ->
            raw.execSQL("PRAGMA foreign_keys=OFF")
            V2_DDL.forEach { raw.execSQL(it) }
            raw.version = 2
        }

        val db = helper.runMigrationsAndValidate(DB_2, 3, true, Migrations.MIGRATION_2_3)
        db.query("PRAGMA table_info(`scheduled_payments`)").use { cursor ->
            val names = mutableListOf<String>()
            while (cursor.moveToNext()) names += cursor.getString(1)
            assertTrue(
                "Falta `anchorDayOfMonth` tras la migración",
                names.contains("anchorDayOfMonth")
            )
        }
        db.close()
        context.deleteDatabase(DB_2)
    }

    private companion object {
        const val DB_1 = "migration-test-1"
        const val DB_2 = "migration-test-2"

        /** v2 vieja: DDL exacto del schemas/2.json (sin anchorDayOfMonth en scheduled_payments). */
        val V2_DDL = listOf(
            "CREATE TABLE IF NOT EXISTS `semesters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `courses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semesterId` INTEGER NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `section` TEXT NOT NULL, `teacher` TEXT, `colorArgb` INTEGER NOT NULL, FOREIGN KEY(`semesterId`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `class_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startMinute` INTEGER NOT NULL, `endMinute` INTEGER NOT NULL, `room` TEXT, `credits` INTEGER, FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseId` INTEGER, `title` TEXT NOT NULL, `description` TEXT, `dueAt` INTEGER NOT NULL, `priority` TEXT NOT NULL, `completedAt` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE TABLE IF NOT EXISTS `task_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `remindAt` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalanceCents` INTEGER NOT NULL, `colorArgb` INTEGER NOT NULL, `includeInTotal` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `archived` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `yearMonth` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `date` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `targetAccountId` INTEGER, `categoryId` INTEGER, `description` TEXT, `refundOfId` INTEGER, `scheduledPaymentId` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`targetAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`refundOfId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`scheduledPaymentId`) REFERENCES `scheduled_payments`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE TABLE IF NOT EXISTS `scheduled_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER, `frequency` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `remindDaysBefore` INTEGER NOT NULL, `active` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, FOREIGN KEY(`parentId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `folderId` INTEGER, `courseId` INTEGER, `taskId` INTEGER, `pinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE TABLE IF NOT EXISTS `import_hints` (`rawKey` TEXT NOT NULL, `cleanValue` TEXT NOT NULL, PRIMARY KEY(`rawKey`))",
            "CREATE INDEX IF NOT EXISTS `index_semesters_startDate` ON `semesters` (`startDate`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_courses_semesterId_code_section` ON `courses` (`semesterId`, `code`, `section`)",
            "CREATE INDEX IF NOT EXISTS `index_courses_semesterId` ON `courses` (`semesterId`)",
            "CREATE INDEX IF NOT EXISTS `index_class_sessions_courseId` ON `class_sessions` (`courseId`)",
            "CREATE INDEX IF NOT EXISTS `index_class_sessions_dayOfWeek` ON `class_sessions` (`dayOfWeek`)",
            "CREATE INDEX IF NOT EXISTS `index_tasks_courseId` ON `tasks` (`courseId`)",
            "CREATE INDEX IF NOT EXISTS `index_tasks_dueAt` ON `tasks` (`dueAt`)",
            "CREATE INDEX IF NOT EXISTS `index_task_reminders_taskId` ON `task_reminders` (`taskId`)",
            "CREATE INDEX IF NOT EXISTS `index_accounts_type` ON `accounts` (`type`)",
            "CREATE INDEX IF NOT EXISTS `index_categories_kind` ON `categories` (`kind`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_yearMonth_categoryId` ON `budgets` (`yearMonth`, `categoryId`)",
            "CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_targetAccountId` ON `transactions` (`targetAccountId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_refundOfId` ON `transactions` (`refundOfId`)",
            "CREATE INDEX IF NOT EXISTS `index_transactions_scheduledPaymentId` ON `transactions` (`scheduledPaymentId`)",
            "CREATE INDEX IF NOT EXISTS `index_scheduled_payments_accountId` ON `scheduled_payments` (`accountId`)",
            "CREATE INDEX IF NOT EXISTS `index_scheduled_payments_categoryId` ON `scheduled_payments` (`categoryId`)",
            "CREATE INDEX IF NOT EXISTS `index_scheduled_payments_nextDueDate` ON `scheduled_payments` (`nextDueDate`)",
            "CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)",
            "CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)",
            "CREATE INDEX IF NOT EXISTS `index_notes_courseId` ON `notes` (`courseId`)",
            "CREATE INDEX IF NOT EXISTS `index_notes_taskId` ON `notes` (`taskId`)"
        )

        /** v1 vieja: como v2 + `settings`, sin los dos índices que añade 1->2. */
        val V1_DDL = V2_DDL.toMutableList().apply {
            add("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL PRIMARY KEY, `value` TEXT NOT NULL)")
        }
    }
}
