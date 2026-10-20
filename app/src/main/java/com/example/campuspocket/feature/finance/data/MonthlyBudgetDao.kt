package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Insert
    suspend fun insert(budget: MonthlyBudgetEntity): Long

    @Update
    suspend fun update(budget: MonthlyBudgetEntity)

    @Delete
    suspend fun delete(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun observeByMonth(yearMonth: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets")
    fun observeAll(): Flow<List<MonthlyBudgetEntity>>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    suspend fun getByMonth(yearMonth: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets")
    suspend fun getAll(): List<MonthlyBudgetEntity>
}
