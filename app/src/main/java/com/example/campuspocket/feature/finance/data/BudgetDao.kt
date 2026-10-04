package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth ORDER BY categoryId")
    fun observeByMonth(yearMonth: String): kotlinx.coroutines.flow.Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE yearMonth IN (:yearMonths)")
    fun observeByMonths(yearMonths: List<String>): kotlinx.coroutines.flow.Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun getByMonthAndCategory(yearMonth: String, categoryId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getByMonth(yearMonth: String): List<BudgetEntity>

    /** Todo, en vivo (para la vista de semestre). */
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    /** Todo, para el respaldo JSON. */
    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>
}