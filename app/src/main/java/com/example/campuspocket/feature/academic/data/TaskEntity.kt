package com.example.campuspocket.feature.academic.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("courseId"), Index("dueAt")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueAt: Long,
    val priority: String = "MEDIUM",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "tasks"
        const val PRIORITY_LOW = "LOW"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_HIGH = "HIGH"
    }
}