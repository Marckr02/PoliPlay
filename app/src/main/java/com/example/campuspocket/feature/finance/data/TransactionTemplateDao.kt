package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Insert
    suspend fun insert(template: TransactionTemplateEntity): Long

    @Query(
        "SELECT * FROM transaction_templates " +
            "ORDER BY lastUsedAt DESC, id DESC LIMIT :limit"
    )
    fun observeRecent(limit: Int): Flow<List<TransactionTemplateEntity>>

    @Query("UPDATE transaction_templates SET lastUsedAt = :usedAt WHERE id = :id")
    suspend fun touch(id: Long, usedAt: Long)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
