package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun observeByDateRange(startDate: LocalDate, endDate: LocalDate): kotlinx.coroutines.flow.Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR targetAccountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun observeByAccount(accountId: Long): kotlinx.coroutines.flow.Flow<List<TransactionEntity>>

    data class AccountDelta(val accountId: Long, val delta: Long)
    data class CategorySpent(val categoryId: Long, val netSpent: Long)

    @Query("""
        SELECT accountId AS accountId,
               SUM(CASE type WHEN 'INCOME' THEN amountCents
                             WHEN 'REFUND' THEN amountCents
                             ELSE -amountCents END) AS delta
        FROM transactions
        GROUP BY accountId
    """)
    fun observeAccountDeltas(): Flow<List<AccountDelta>>

    @Query("""
        SELECT targetAccountId AS accountId, SUM(amountCents) AS delta
        FROM transactions
        WHERE type = 'TRANSFER' AND targetAccountId IS NOT NULL
        GROUP BY targetAccountId
    """)
    fun observeTransferInDeltas(): Flow<List<AccountDelta>>

    @Query("""
        SELECT categoryId AS categoryId,
               SUM(CASE type WHEN 'EXPENSE' THEN amountCents
                             WHEN 'REFUND' THEN -amountCents
                             ELSE 0 END) AS netSpent
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate AND categoryId IS NOT NULL
        GROUP BY categoryId
    """)
    fun observeNetSpentByCategory(startDate: LocalDate, endDate: LocalDate): Flow<List<CategorySpent>>
}