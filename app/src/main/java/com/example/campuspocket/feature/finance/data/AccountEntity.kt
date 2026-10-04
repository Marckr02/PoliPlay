package com.example.campuspocket.feature.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType { CASH, DEBIT, SAVINGS, CREDIT_CARD }

@Entity(
    tableName = "accounts",
    indices = [Index("type")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalanceCents: Long = 0,
    val colorArgb: Int = 0xFF4C8DFF.toInt(),
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        const val TABLE_NAME = "accounts"
    }
}