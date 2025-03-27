package com.example.dhbt.presentation.task.detail

import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskRecurrence

/**
 * Состояние экрана деталей задачи
 */
data class TaskDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val task: Task? = null,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val category: Category? = null,
    val recurrence: TaskRecurrence? = null,
    val totalFocusTime: Int? = null,
    val relatedTasks: List<Task> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val showEditTask: Boolean = false,
    val showPomodoroDialog: Boolean = false,
    var showDeleteConfirmDialog: Boolean = false
)