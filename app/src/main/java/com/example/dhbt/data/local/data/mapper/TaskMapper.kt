package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.TaskEntity
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import javax.inject.Inject

class TaskMapper @Inject constructor() {

    fun mapFromEntity(entity: TaskEntity): Task {
        return Task(
            id = entity.taskId,
            title = entity.title,
            description = entity.description,
            categoryId = entity.categoryId,
            color = entity.color,
            creationDate = entity.creationDate,
            dueDate = entity.dueDate,
            dueTime = entity.dueTime,
            duration = entity.duration,
            priority = TaskPriority.fromInt(entity.priority),
            status = TaskStatus.fromInt(entity.status),
            completionDate = entity.completionDate,
            eisenhowerQuadrant = entity.eisenhowerQuadrant,
            estimatedPomodoroSessions = entity.estimatedPomodoroSessions,
            // Подзадачи, теги и повторения будут загружены отдельно
            subtasks = emptyList(),
            tags = emptyList(),
            recurrence = null
        )
    }

    fun mapToEntity(domain: Task): TaskEntity {
        return TaskEntity(
            taskId = domain.id,
            title = domain.title,
            description = domain.description,
            categoryId = domain.categoryId,
            color = domain.color,
            creationDate = domain.creationDate,
            dueDate = domain.dueDate,
            dueTime = domain.dueTime,
            duration = domain.duration,
            priority = domain.priority.value,
            status = domain.status.value,
            completionDate = domain.completionDate,
            eisenhowerQuadrant = domain.eisenhowerQuadrant,
            estimatedPomodoroSessions = domain.estimatedPomodoroSessions
        )
    }
}