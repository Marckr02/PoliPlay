package com.example.campuspocket.feature.finance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TransactionType { EXPENSE, INCOME, TRANSFER, REFUND }

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["refundOfId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ScheduledPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduledPaymentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("date"),
        Index("accountId"),
        Index("targetAccountId"),
        Index("categoryId"),
        Index("refundOfId"),
        Index("scheduledPaymentId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val date: LocalDate,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val description: String? = null,
    val refundOfId: Long? = null,
    val scheduledPaymentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "transactions"
    }
}