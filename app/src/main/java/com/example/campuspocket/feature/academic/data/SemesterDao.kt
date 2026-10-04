package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Insert
    suspend fun insert(semester: SemesterEntity): Long

    @Update
    suspend fun update(semester: SemesterEntity)

    @Delete
    suspend fun delete(semester: SemesterEntity)

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): SemesterEntity?

    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getById(id: Long): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE name = :name LIMIT 1")
    suspend fun getSemesterByName(name: String): SemesterEntity?

    /** Deja un solo semestre activo. */
    @Query("UPDATE semesters SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: Long)
}
