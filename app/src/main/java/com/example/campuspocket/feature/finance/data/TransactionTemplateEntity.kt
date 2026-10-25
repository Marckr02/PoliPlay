package com.example.campuspocket.feature.finance.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Plantilla de captura rápida de movimientos (v5 §4).
 * `lastUsedAt` permite ordenar por uso reciente sin duplicar filas.
 */
@Entity(
    tableName = "transaction_templates",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("accountId"), Index("categoryId")]
)
data class TransactionTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val type: String,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val lastUsedAt: Long = 0L
) {
    companion object {
        const val TABLE_NAME = "transaction_templates"
    }
}
