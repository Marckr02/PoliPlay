package com.example.campuspocket.feature.academic.domain.model

import java.time.Instant

enum class Priority { LOW, MEDIUM, HIGH }

/** Repetición de una tarea al completarla (v5 §3.5). */
enum class RepeatRule { NONE, DAILY, WEEKLY }

data class Task(
    val id: Long? = null,
    val courseId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueAt: Instant,
    val priority: Priority = Priority.MEDIUM,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val reminders: List<TaskReminder> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val repeatUntil: Instant? = null
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
            createdAt = createdAt.toEpochMilli(),
            repeatRule = repeatRule.name,
            repeatUntil = repeatUntil?.toEpochMilli()
        )
    }

    companion object {
        fun fromEntity(
            entity: com.example.campuspocket.feature.academic.data.TaskEntity,
            subtasks: List<Subtask> = emptyList()
        ): Task {
            return Task(
                id = if (entity.id == 0L) null else entity.id,
                courseId = entity.courseId,
                title = entity.title,
                description = entity.description,
                dueAt = Instant.ofEpochMilli(entity.dueAt),
                priority = Priority.valueOf(entity.priority),
                completedAt = entity.completedAt?.let { Instant.ofEpochMilli(it) },
                createdAt = Instant.ofEpochMilli(entity.createdAt),
                reminders = emptyList(), // Loaded separately
                subtasks = subtasks,
                repeatRule = runCatching { RepeatRule.valueOf(entity.repeatRule) }
                    .getOrDefault(RepeatRule.NONE),
                repeatUntil = entity.repeatUntil?.let { Instant.ofEpochMilli(it) }
            )
        }
    }
}

data class Subtask(
    val id: Long? = null,
    val taskId: Long = 0L,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0
) {
    fun toEntity(): com.example.campuspocket.feature.academic.data.SubtaskEntity =
        com.example.campuspocket.feature.academic.data.SubtaskEntity(
            id = id ?: 0,
            taskId = taskId,
            title = title,
            done = done,
            position = position
        )

    companion object {
        fun fromEntity(
            entity: com.example.campuspocket.feature.academic.data.SubtaskEntity
        ): Subtask = Subtask(
            id = if (entity.id == 0L) null else entity.id,
            taskId = entity.taskId,
            title = entity.title,
            done = entity.done,
            position = entity.position
        )
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
