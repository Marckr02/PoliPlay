package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY type, sortOrder, name")
    fun observeActive(): kotlinx.coroutines.flow.Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY type, sortOrder, name")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE archived = 0 AND type = :type ORDER BY sortOrder, name")
    suspend fun getActiveByType(type: String): List<AccountEntity>

    /**
     * Saldo actual en una sola consulta (centavos), con la misma fórmula que
     * ComputeAccountBalances: inicial + ingresos/reembolsos - gastos - transferencias
     * salientes + transferencias entrantes. Se usa dentro de la transacción del retiro.
     */
    @Query(
        """
        SELECT a.initialBalanceCents
             + COALESCE((
                 SELECT SUM(CASE t.type
                                WHEN 'INCOME' THEN t.amountCents
                                WHEN 'REFUND' THEN t.amountCents
                                ELSE -t.amountCents
                            END)
                 FROM transactions t
                 WHERE t.accountId = a.id
             ), 0)
             + COALESCE((
                 SELECT SUM(t.amountCents)
                 FROM transactions t
                 WHERE t.type = 'TRANSFER' AND t.targetAccountId = a.id
             ), 0)
        FROM accounts a
        WHERE a.id = :id
        """
    )
    suspend fun getBalanceCents(id: Long): Long?
}