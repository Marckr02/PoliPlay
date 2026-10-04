package com.example.campuspocket.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImportHintDao {
    /** La clave es natural (texto del PDF) y no tiene tablas hijas: reemplazar es seguro. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hint: ImportHintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(hints: List<ImportHintEntity>)

    @Query("SELECT * FROM import_hints WHERE rawKey = :rawKey")
    suspend fun getHint(rawKey: String): ImportHintEntity?

    @Query("SELECT * FROM import_hints")
    suspend fun getAll(): List<ImportHintEntity>

    @Query("DELETE FROM import_hints WHERE rawKey = :rawKey")
    suspend fun deleteByKey(rawKey: String)
}
