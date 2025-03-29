package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun getTasksForToday(): Flow<List<Task>>
    fun getTasksForWeek(): Flow<List<Task>>
    fun getTasksByCategory(categoryId: String): Flow<List<Task>>
    fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>>
    fun getTasksByEisenhowerQuadrant(quadrant: Int): Flow<List<Task>>
    fun getTasksWithTags(tagIds: List<String>): Flow<List<Task>>

    suspend fun getTaskById(taskId: String): Task?
    suspend fun addTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun completeTask(taskId: String, completionTime: Long)
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    // Подзадачи
    fun getSubtasksForTask(taskId: String): Flow<List<Subtask>>
    suspend fun addSubtask(subtask: Subtask): String
    suspend fun updateSubtask(subtask: Subtask)
    suspend fun deleteSubtask(subtaskId: String)
    suspend fun completeSubtask(subtaskId: String, isCompleted: Boolean)

    // Повторения задач
    suspend fun getTaskRecurrence(taskId: String): TaskRecurrence?
    suspend fun setTaskRecurrence(taskId: String, recurrence: TaskRecurrence)
    suspend fun removeTaskRecurrence(taskId: String)

    // Теги задач
    fun getTagsForTask(taskId: String): Flow<List<Tag>>
    suspend fun addTagToTask(taskId: String, tagId: String)
    suspend fun removeTagFromTask(taskId: String, tagId: String)
    suspend fun setTagsForTask(taskId: String, tagIds: List<String>)

    // Добавляем методы для работы с повторениями
    suspend fun saveTaskRecurrence(recurrence: TaskRecurrence)
    suspend fun deleteTaskRecurrence(taskId: String)
    suspend fun deleteTagsForTask(taskId: String)
    suspend fun deleteSubtasksForTask(taskId: String)
}