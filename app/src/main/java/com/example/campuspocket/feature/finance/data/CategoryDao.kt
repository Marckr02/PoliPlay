package com.example.campuspocket.feature.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE archived = 0 AND kind = :kind ORDER BY name")
    fun observeActiveByKind(kind: String): kotlinx.coroutines.flow.Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY kind, name")
    fun observeActive(): kotlinx.coroutines.flow.Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY kind, name")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}