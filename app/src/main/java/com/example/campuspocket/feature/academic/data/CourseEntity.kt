package com.example.campuspocket.feature.academic.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semesterId", "code", "section"], unique = true),
        Index("semesterId")
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val code: String,
    val name: String,
    val section: String,
    val teacher: String? = null,
    val colorArgb: Int = 0xFF4C8DFF.toInt()
) {
    companion object {
        const val TABLE_NAME = "courses"
    }
}