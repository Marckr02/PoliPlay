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
}