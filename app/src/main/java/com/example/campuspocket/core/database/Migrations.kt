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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
