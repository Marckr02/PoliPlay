package com.example.campuspocket.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones explícitas. Regla de aquí en adelante: cualquier cambio de esquema
 * exige subir `version` y escribir su Migration (nada de borrado destructivo).
 */
object Migrations {

    /**
     * v1 -> v2: retirada de la tabla `settings` (los ajustes van a DataStore en la Fase 7)
     * e índices de las claves foráneas que antes se creaban sin elevar la versión.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `settings`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_refundOfId` ON `transactions` (`refundOfId`)"
            )
        }
    }

    /**
     * v2 -> v3: pagos programados ganan `anchorDayOfMonth` (día original de la recurrencia,
     * para no derivar el 31 -> 28 para siempre). Sin pérdida de datos.
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `scheduled_payments` ADD COLUMN `anchorDayOfMonth` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * v3 -> v4 (Fase 8):
     * - `transactions.subtype` identifica el retiro en cajero (nullable, sin tocar filas viejas).
     * - `monthly_budgets` guarda el presupuesto TOTAL de cada mes (independiente del de categorías).
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `subtype` TEXT")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `monthly_budgets` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`yearMonth` TEXT NOT NULL, " +
                    "`amountCents` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_monthly_budgets_yearMonth` " +
                    "ON `monthly_budgets` (`yearMonth`)"
            )
        }
    }

    /**
     * v4 -> v5 (fases 4 y 5 del rediseño):
     * - subtareas de tarea con borrado en cascada;
     * - repetición de tareas (`repeatRule` / `repeatUntil`);
     * - plantillas de movimientos con orden por uso reciente.
     */
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `tasks` ADD COLUMN `repeatRule` TEXT NOT NULL DEFAULT 'NONE'"
            )
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `repeatUntil` INTEGER")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `subtasks` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`taskId` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`done` INTEGER NOT NULL, " +
                    "`position` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transaction_templates` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`amountCents` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`accountId` INTEGER, " +
                    "`categoryId` INTEGER, " +
                    "`lastUsedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL , " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL )"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` " +
                    "ON `transaction_templates` (`accountId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` " +
                    "ON `transaction_templates` (`categoryId`)"
            )
        }
    }

    /**
     * v5 -> v6 (Ola 3 Bloque D): calificaciones por materia con simulador.
     * - Nueva tabla `grades` para notas de evaluaciones (parciales, trabajos, final).
     * - `isFinalExam` marca el examen final para el simulador "¿cuánto necesito?".
     */
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `grades` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`courseId` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`score` REAL NOT NULL, " +
                    "`maxScore` REAL NOT NULL, " +
                    "`weight` REAL NOT NULL DEFAULT 1.0, " +
                    "`isFinalExam` INTEGER NOT NULL DEFAULT 0, " +
                    "`date` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_grades_courseId` ON `grades` (`courseId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_grades_date` ON `grades` (`date`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}
