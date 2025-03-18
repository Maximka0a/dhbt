package com.example.dhbt.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val color: String? = null,
    val creationDate: Long,
    val dueDate: Long? = null,
    val dueTime: String? = null,
    val duration: Int? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val completionDate: Long? = null,
    val eisenhowerQuadrant: Int? = null,
    val estimatedPomodoroSessions: Int? = null,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val recurrence: TaskRecurrence? = null
)

enum class TaskPriority(val value: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2);

    companion object {
        fun fromInt(value: Int): TaskPriority = values().firstOrNull { it.value == value } ?: MEDIUM
    }
}

enum class TaskStatus(val value: Int) {
    ACTIVE(0),
    COMPLETED(1),
    ARCHIVED(2);

    companion object {
        fun fromInt(value: Int): TaskStatus = values().firstOrNull { it.value == value } ?: ACTIVE
    }
}