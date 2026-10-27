package com.example.campuspocket.feature.academic.domain.repository

import com.example.campuspocket.feature.academic.domain.model.Grade
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de calificaciones por materia (sistema EPN).
 */
interface GradeRepository {

    suspend fun insertGrade(grade: Grade): Long
    suspend fun updateGrade(grade: Grade)
    suspend fun deleteGrade(gradeId: Long)

    fun observeGrade(gradeId: Long): Flow<Grade?>
    fun observeGradesByCourse(courseId: Long): Flow<List<Grade>>
    fun observeSupletorio(courseId: Long): Flow<Grade?>
    fun observeBimestres(courseId: Long): Flow<List<Grade>>

    suspend fun getGradesByCourse(courseId: Long): List<Grade>
    suspend fun countGradesByCourse(courseId: Long): Int
}