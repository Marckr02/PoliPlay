package com.example.campuspocket.feature.academic.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("dayOfWeek")]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String? = null,
    val credits: Int? = null
) {
    companion object {
        const val TABLE_NAME = "class_sessions"
    }
}