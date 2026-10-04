package com.example.campuspocket.feature.academic.data

import androidx.room.Embedded
import androidx.room.Relation

/** Materia con sus sesiones, para consultas Room con @Relation. */
data class CourseWithSessions(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val sessions: List<ClassSessionEntity>
)
