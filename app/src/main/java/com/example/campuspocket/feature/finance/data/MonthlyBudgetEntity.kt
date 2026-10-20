package com.example.campuspocket.feature.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Presupuesto TOTAL de un mes. Coexiste con [BudgetEntity] (por categoría) y no se
 * suma ni se arrastra: cada mes define el suyo o no tiene.
 */
@Entity(
    tableName = "monthly_budgets",
    indices = [Index(value = ["yearMonth"], unique = true)]
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val amountCents: Long
) {
    companion object {
        const val TABLE_NAME = "monthly_budgets"
    }
}
