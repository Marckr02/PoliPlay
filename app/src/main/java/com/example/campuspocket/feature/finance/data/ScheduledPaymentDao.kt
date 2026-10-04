package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledPaymentDao {
    @Insert
    suspend fun insert(payment: ScheduledPaymentEntity): Long

    @Update
    suspend fun update(payment: ScheduledPaymentEntity)

    @Delete
    suspend fun delete(payment: ScheduledPaymentEntity)

    @Query("SELECT * FROM scheduled_payments WHERE active = 1 ORDER BY nextDueDate ASC")
    fun observeActive(): kotlinx.coroutines.flow.Flow<List<ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments ORDER BY nextDueDate ASC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments WHERE active = 1 AND nextDueDate <= :date ORDER BY nextDueDate ASC")
    suspend fun getDueUntil(date: LocalDate): List<ScheduledPaymentEntity>

    @Query("SELECT * FROM scheduled_payments WHERE id = :id")
    suspend fun getById(id: Long): ScheduledPaymentEntity?
}