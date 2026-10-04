package com.example.campuspocket.feature.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY name")
    fun observeRoot(): kotlinx.coroutines.flow.Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name")
    fun observeByParent(parentId: Long): kotlinx.coroutines.flow.Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?
}