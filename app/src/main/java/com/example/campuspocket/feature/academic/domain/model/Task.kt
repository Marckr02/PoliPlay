package com.example.campuspocket.feature.academic.domain.model

import java.time.Instant

enum class Priority { LOW, MEDIUM, HIGH }

data class Task(
    val id: Long? = null,
    val courseId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueAt: Instant,
    val priority: Priority = Priority.MEDIUM,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val reminders: List<TaskReminder> = emptyList()
) {
    fun toEntity(): com.example.campuspocket.feature.academic.data.TaskEntity {
        return com.example.campuspocket.feature.academic.data.TaskEntity(
            id = id ?: 0,
            courseId = courseId,
            title = title,
            description = description,
            dueAt = dueAt.toEpochMilli(),
            priority = priority.name,
            completedAt = completedAt?.toEpochMilli(),
            createdAt = createdAt.toEpochMilli()
        )
    }

    companion object {
        fun fromEntity(entity: com.example.campuspocket.feature.academic.data.TaskEntity): Task {
            return Task(
                id = if (entity.id == 0L) null else entity.id,
                courseId = entity.courseId,
                title = entity.title,
                description = entity.description,
                dueAt = Instant.ofEpochMilli(entity.dueAt),
                priority = Priority.valueOf(entity.priority),
                completedAt = entity.completedAt?.let { Instant.ofEpochMilli(it) },
                createdAt = Instant.ofEpochMilli(entity.createdAt),
                reminders = emptyList() // Loaded separately
            )
        }
    }
}

data class TaskReminder(
    val id: Long? = null,
    val taskId: Long,
    val remindAt: Instant
) {
    fun toEntity(): com.example.campuspocket.feature.academic.data.TaskReminderEntity {
        return com.example.campuspocket.feature.academic.data.TaskReminderEntity(
            id = id ?: 0,
            taskId = taskId,
            remindAt = remindAt.toEpochMilli()
        )
    }

    companion object {
        fun fromEntity(entity: com.example.campuspocket.feature.academic.data.TaskReminderEntity): TaskReminder {
            return TaskReminder(
                id = if (entity.id == 0L) null else entity.id,
                taskId = entity.taskId,
                remindAt = Instant.ofEpochMilli(entity.remindAt)
            )
        }
    }
}