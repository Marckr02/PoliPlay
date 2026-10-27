package com.example.campuspocket.feature.academic.data.repository

import com.example.campuspocket.feature.academic.data.GradeDao
import com.example.campuspocket.feature.academic.data.GradeEntity
import com.example.campuspocket.feature.academic.domain.model.Grade
import com.example.campuspocket.feature.academic.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepositoryImpl @Inject constructor(
    private val gradeDao: GradeDao
) : GradeRepository {

    override suspend fun insertGrade(grade: Grade): Long =
        gradeDao.insertGrade(grade.toEntity())

    override suspend fun updateGrade(grade: Grade) {
        gradeDao.updateGrade(grade.toEntity())
    }

    override suspend fun deleteGrade(gradeId: Long) {
        gradeDao.deleteGradeById(gradeId)
    }

    override fun observeGrade(gradeId: Long): Flow<Grade?> =
        gradeDao.observeGrade(gradeId).map { it?.let(Grade::fromEntity) }

    override fun observeGradesByCourse(courseId: Long): Flow<List<Grade>> =
        gradeDao.observeGradesByCourse(courseId).map { it.map(Grade::fromEntity) }

    override fun observeSupletorio(courseId: Long): Flow<Grade?> =
        gradeDao.observeSupletorio(courseId).map { it?.let(Grade::fromEntity) }

    override fun observeBimestres(courseId: Long): Flow<List<Grade>> =
        gradeDao.observeBimestres(courseId).map { it.map(Grade::fromEntity) }

    override suspend fun getGradesByCourse(courseId: Long): List<Grade> =
        gradeDao.getGradesWithCourse(courseId).map(Grade::fromEntity)

    override suspend fun countGradesByCourse(courseId: Long): Int =
        gradeDao.countGradesByCourse(courseId)
}