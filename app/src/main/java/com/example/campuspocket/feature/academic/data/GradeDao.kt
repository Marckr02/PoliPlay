package com.example.campuspocket.feature.academic.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Update
    suspend fun updateGrade(grade: GradeEntity): Int

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE id = :gradeId")
    suspend fun deleteGradeById(gradeId: Long): Int

    @Query("SELECT * FROM grades WHERE id = :gradeId")
    fun observeGrade(gradeId: Long): Flow<GradeEntity?>

    @Query("SELECT * FROM grades WHERE courseId = :courseId ORDER BY date ASC")
    fun observeGradesByCourse(courseId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE courseId = :courseId AND tipo = 'SUPLETORIO'")
    fun observeSupletorio(courseId: Long): Flow<GradeEntity?>

    @Query("SELECT * FROM grades WHERE courseId = :courseId AND tipo IN ('BIMESTRE_1', 'BIMESTRE_2') ORDER BY date ASC")
    fun observeBimestres(courseId: Long): Flow<List<GradeEntity>>

    @Transaction
    @Query("SELECT * FROM grades WHERE courseId = :courseId ORDER BY date ASC")
    suspend fun getGradesWithCourse(courseId: Long): List<GradeEntity>

    @Query("SELECT COUNT(*) FROM grades WHERE courseId = :courseId")
    suspend fun countGradesByCourse(courseId: Long): Int
}