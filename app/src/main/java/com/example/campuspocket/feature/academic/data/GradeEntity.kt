package com.example.campuspocket.feature.academic.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Calificación de una evaluación dentro de una materia (sistema EPN).
 * 
 * Reglas EPN:
 * - BIMESTRE_1: Primer bimestre (sobre 20)
 * - BIMESTRE_2: Segundo bimestre (sobre 20)
 * - SUPLETORIO: Examen supletorio (sobre 40)
 * - Suma = Nota_1 + Nota_2 (máx 40)
 * - Aprobado directo: Suma >= 28
 * - Supletorio: 18 <= Suma < 28
 * - Reprobado: Suma < 18
 * - Nota final supletorio = (Nota_1 + Nota_2 + Nota_Suple) / 2
 * - Para aprobar supletorio: Nota final supletorio >= 24/40
 * - Nota requerida en supletorio = 48 - (Nota_1 + Nota_2)
 */
@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("date"), Index("tipo")]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val tipo: String,           // "BIMESTRE_1", "BIMESTRE_2", "SUPLETORIO"
    val score: Double,          // Nota obtenida
    val maxScore: Double = 20.0, // Nota máxima (20 para bimestres, 40 para supletorio)
    val date: Long = System.currentTimeMillis() // epoch millis
) {
    companion object {
        const val TABLE_NAME = "grades"
    }
}