package com.example.campuspocket.feature.finance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Frequency { WEEKLY, MONTHLY, YEARLY }

@Entity(
    tableName = "scheduled_payments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("nextDueDate")]
)
data class ScheduledPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val frequency: String = Frequency.MONTHLY.name,
    val nextDueDate: LocalDate,
    /** Día del mes original de la recurrencia (1-31; 0 = usar el de nextDueDate). Evita la deriva de fin de mes. */
    val anchorDayOfMonth: Int = 0,
    val remindDaysBefore: Int = 3,
    val active: Boolean = true
) {
    companion object {
        const val TABLE_NAME = "scheduled_payments"
    }
}