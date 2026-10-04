package com.example.campuspocket.feature.academic.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "semesters",
    indices = [Index("startDate")]
)
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "semesters"
    }
}