package com.example.campuspocket.feature.academic

import com.example.campuspocket.feature.academic.data.GradeEntity
import com.example.campuspocket.feature.academic.domain.model.Grade
import com.example.campuspocket.feature.academic.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Fake en memoria para las pruebas de calificaciones. */
class FakeGradeRepository(initial: List<Grade> = emptyList()) : GradeRepository {

    private val gradesFlow = MutableStateFlow(initial.map { it.toEntity() })
    val inserted = mutableListOf<Grade>()
    val updated = mutableListOf<Grade>()
    val deleted = mutableListOf<Long>()

    override fun observeGrade(gradeId: Long): Flow<Grade?> =
        gradesFlow.map { list -> list.firstOrNull { it.id == gradeId }?.let(Grade::fromEntity) }

    override fun observeGradesByCourse(courseId: Long): Flow<List<Grade>> =
        gradesFlow.map { list -> list.filter { it.courseId == courseId }.map(Grade::fromEntity) }

    override fun observeSupletorio(courseId: Long): Flow<Grade?> =
        gradesFlow.map { list -> list.firstOrNull { it.courseId == courseId && it.tipo == "SUPLETORIO" }?.let(Grade::fromEntity) }

    override fun observeBimestres(courseId: Long): Flow<List<Grade>> =
        gradesFlow.map { list -> list.filter { it.courseId == courseId && it.tipo in listOf("BIMESTRE_1", "BIMESTRE_2") }.map(Grade::fromEntity) }

    override suspend fun insertGrade(grade: Grade): Long {
        val id = (gradesFlow.value.maxByOrNull { it.id }?.id ?: 0L) + 1
        val entity = grade.toEntity().copy(id = id)
        gradesFlow.value = gradesFlow.value + entity
        inserted += grade
        return id
    }

    override suspend fun updateGrade(grade: Grade) {
        val index = gradesFlow.value.indexOfFirst { it.id == grade.id!! }
        if (index >= 0) {
            gradesFlow.value = gradesFlow.value.toMutableList().apply { set(index, grade.toEntity()) }
            updated += grade
        }
    }

    override suspend fun deleteGrade(gradeId: Long) {
        gradesFlow.value = gradesFlow.value.filterNot { it.id == gradeId }
        deleted += gradeId
    }

    override suspend fun getGradesByCourse(courseId: Long): List<Grade> =
        gradesFlow.value.filter { it.courseId == courseId }.map(Grade::fromEntity)

    override suspend fun countGradesByCourse(courseId: Long): Int =
        gradesFlow.value.count { it.courseId == courseId }

    fun setGrades(grades: List<Grade>) {
        gradesFlow.value = grades.map { it.toEntity() }
    }
}