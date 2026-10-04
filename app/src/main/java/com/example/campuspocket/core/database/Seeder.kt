package com.example.campuspocket.core.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.campuspocket.feature.finance.data.AccountEntity
import com.example.campuspocket.feature.finance.data.AccountType
import com.example.campuspocket.feature.finance.data.CategoryEntity
import com.example.campuspocket.feature.finance.data.CategoryKind

/**
 * Datos iniciales: categorías por defecto y la cuenta "Efectivo".
 * Se ejecuta UNA vez, al crear la base de datos (callback onCreate en DatabaseModule).
 */
object Seeder {

    fun defaultCategories(): List<CategoryEntity> = listOf(
        // Gastos
        category("Alimentación", CategoryKind.EXPENSE, "restaurant", 0xFFE5484D),
        category("Transporte", CategoryKind.EXPENSE, "directions_bus", 0xFF4C8DFF),
        category("Servicios", CategoryKind.EXPENSE, "receipt_long", 0xFF9B59B6),
        category("Educación y materiales", CategoryKind.EXPENSE, "school", 0xFF1ABC9C),
        category("Salud", CategoryKind.EXPENSE, "local_hospital", 0xFF2EB67D),
        category("Entretenimiento", CategoryKind.EXPENSE, "sports_esports", 0xFFF5A623),
        category("Ropa", CategoryKind.EXPENSE, "checkroom", 0xFFE67E22),
        category("Otros", CategoryKind.EXPENSE, "category", 0xFF5B6B7F),
        // Ingresos
        category("Mesada o salario", CategoryKind.INCOME, "payments", 0xFF2EB67D),
        category("Otros ingresos", CategoryKind.INCOME, "attach_money", 0xFF1ABC9C)
    )

    fun defaultAccount(): AccountEntity = AccountEntity(
        name = "Efectivo",
        type = AccountType.CASH.name,
        initialBalanceCents = 0L,
        colorArgb = 0xFF4C8DFF.toInt(),
        includeInTotal = true,
        archived = false,
        sortOrder = 0
    )

    /** Inserta los datos iniciales con SQL directo (en onCreate los DAOs aún no están disponibles). */
    fun seed(db: SupportSQLiteDatabase) {
        defaultCategories().forEach { c ->
            val values = ContentValues().apply {
                put("name", c.name)
                put("kind", c.kind)
                put("iconKey", c.iconKey)
                put("colorArgb", c.colorArgb)
                put("archived", if (c.archived) 1 else 0)
            }
            db.insert(CategoryEntity.TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, values)
        }

        val a = defaultAccount()
        val accountValues = ContentValues().apply {
            put("name", a.name)
            put("type", a.type)
            put("initialBalanceCents", a.initialBalanceCents)
            put("colorArgb", a.colorArgb)
            put("includeInTotal", if (a.includeInTotal) 1 else 0)
            put("archived", if (a.archived) 1 else 0)
            put("sortOrder", a.sortOrder)
        }
        db.insert(AccountEntity.TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, accountValues)
    }

    private fun category(name: String, kind: CategoryKind, icon: String, argb: Long) = CategoryEntity(
        name = name,
        kind = kind.name,
        iconKey = icon,
        colorArgb = argb.toInt()
    )
}
