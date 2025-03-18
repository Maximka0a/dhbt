package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.SubtaskDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.dao.TaskRecurrenceDao
import com.example.dhbt.data.local.dao.TaskTagDao
import com.example.dhbt.data.local.entity.SubtaskEntity
import com.example.dhbt.data.local.entity.TaskEntity
import com.example.dhbt.data.local.entity.TaskRecurrenceEntity
import com.example.dhbt.data.local.entity.TaskTagCrossRef
import com.example.dhbt.data.mapper.SubtaskMapper
import com.example.dhbt.data.mapper.TagMapper
import com.example.dhbt.data.mapper.TaskMapper
import com.example.dhbt.data.mapper.TaskRecurrenceMapper
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskRecurrenceDao: TaskRecurrenceDao,
    private val taskTagDao: TaskTagDao,
    private val tagRepository: TagRepository,
    private val taskMapper: TaskMapper,
    private val subtaskMapper: SubtaskMapper,
    private val taskRecurrenceMapper: TaskRecurrenceMapper
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status.value).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksForToday(): Flow<List<Task>> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return taskDao.getTasksByDateRange(startOfDay, endOfDay).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksForWeek(): Flow<List<Task>> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfWeek = startOfWeek + 7 * 24 * 60 * 60 * 1000 - 1

        return taskDao.getTasksByDateRange(startOfWeek, endOfWeek).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksByCategory(categoryId: String): Flow<List<Task>> {
        return taskDao.getTasksByCategory(categoryId).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority.value).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksByEisenhowerQuadrant(quadrant: Int): Flow<List<Task>> {
        return taskDao.getTasksByEisenhowerQuadrant(quadrant).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override fun getTasksWithTags(tagIds: List<String>): Flow<List<Task>> {
        // Реализация зависит от конкретной структуры базы данных
        // В простом случае можно получать задачи для каждого тега и объединять результаты
        // Это упрощенная реализация, которую нужно доработать для оптимизации
        if (tagIds.isEmpty()) {
            return getAllTasks()
        }

        return taskTagDao.getTasksWithTag(tagIds.first()).map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override suspend fun getTaskById(taskId: String): Task? {
        val taskEntity = taskDao.getTaskById(taskId) ?: return null
        return taskMapper.mapFromEntity(taskEntity)
    }

    override suspend fun addTask(task: Task): String {
        val taskEntity = taskMapper.mapToEntity(task)
        val taskId = taskDao.insertTask(taskEntity).toString()

        // Добавляем подзадачи, если есть
        task.subtasks.forEach { subtask ->
            val subtaskEntity = subtaskMapper.mapToEntity(subtask.copy(taskId = taskId))
            subtaskDao.insertSubtask(subtaskEntity)
        }

        // Добавляем повторение задачи, если есть
        task.recurrence?.let { recurrence ->
            val recurrenceEntity = taskRecurrenceMapper.mapToEntity(recurrence.copy(taskId = taskId))
            taskRecurrenceDao.insertTaskRecurrence(recurrenceEntity)
        }

        // Добавляем связи с тегами
        task.tags.forEach { tag ->
            taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId, tag.id))
        }

        return taskId
    }

    override suspend fun updateTask(task: Task) {
        val taskEntity = taskMapper.mapToEntity(task)
        taskDao.updateTask(taskEntity)

        // Обновляем подзадачи
        // Сначала удаляем существующие подзадачи
        subtaskDao.deleteSubtasksForTask(task.id)
        // Затем добавляем обновленные подзадачи
        task.subtasks.forEach { subtask ->
            val subtaskEntity = subtaskMapper.mapToEntity(subtask)
            subtaskDao.insertSubtask(subtaskEntity)
        }

        // Обновляем повторение задачи
        taskRecurrenceDao.deleteRecurrenceForTask(task.id)
        task.recurrence?.let { recurrence ->
            val recurrenceEntity = taskRecurrenceMapper.mapToEntity(recurrence)
            taskRecurrenceDao.insertTaskRecurrence(recurrenceEntity)
        }

        // Обновляем связи с тегами
        taskTagDao.deleteAllTagsForTask(task.id)
        task.tags.forEach { tag ->
            taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(task.id, tag.id))
        }
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTaskById(taskId)
        // Каскадное удаление подзадач, повторений и связей с тегами
        // должно происходить автоматически благодаря настройке внешних ключей в Room
    }

    override suspend fun completeTask(taskId: String, completionTime: Long) {
        taskDao.updateTaskStatus(taskId, TaskStatus.COMPLETED.value)
        taskDao.updateTaskCompletion(taskId, completionTime)
    }

    override fun getSubtasksForTask(taskId: String): Flow<List<Subtask>> {
        return subtaskDao.getSubtasksForTask(taskId).map { subtaskEntities ->
            subtaskEntities.map { subtaskEntity ->
                subtaskMapper.mapFromEntity(subtaskEntity)
            }
        }
    }

    override suspend fun addSubtask(subtask: Subtask): String {
        val subtaskEntity = subtaskMapper.mapToEntity(subtask)
        subtaskDao.insertSubtask(subtaskEntity)
        return subtask.id
    }

    override suspend fun updateSubtask(subtask: Subtask) {
        val subtaskEntity = subtaskMapper.mapToEntity(subtask)
        subtaskDao.updateSubtask(subtaskEntity)
    }

    override suspend fun deleteSubtask(subtaskId: String) {
        subtaskDao.deleteSubtask(subtaskMapper.mapToEntity(Subtask(id = subtaskId, taskId = "", title = "")))
    }

    override suspend fun completeSubtask(subtaskId: String, isCompleted: Boolean) {
        val completionDate = if (isCompleted) System.currentTimeMillis() else null
        subtaskDao.updateSubtaskCompletion(subtaskId, isCompleted, completionDate)
    }

    override suspend fun getTaskRecurrence(taskId: String): TaskRecurrence? {
        val recurrenceEntity = taskRecurrenceDao.getRecurrenceForTask(taskId) ?: return null
        return taskRecurrenceMapper.mapFromEntity(recurrenceEntity)
    }

    override suspend fun setTaskRecurrence(taskId: String, recurrence: TaskRecurrence) {
        val recurrenceEntity = taskRecurrenceMapper.mapToEntity(recurrence)
        taskRecurrenceDao.insertTaskRecurrence(recurrenceEntity)
    }

    override suspend fun removeTaskRecurrence(taskId: String) {
        taskRecurrenceDao.deleteRecurrenceForTask(taskId)
    }

    override fun getTagsForTask(taskId: String): Flow<List<Tag>> {
        // Здесь можно реализовать получение тегов для задачи через TaskTagDao
        // Но для упрощения используем существующий метод из TagRepository
        return tagRepository.getTagsForTask(taskId)
    }

    override suspend fun addTagToTask(taskId: String, tagId: String) {
        taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    override suspend fun removeTagFromTask(taskId: String, tagId: String) {
        taskTagDao.deleteTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    override suspend fun setTagsForTask(taskId: String, tagIds: List<String>) {
        taskTagDao.deleteAllTagsForTask(taskId)
        tagIds.forEach { tagId ->
            taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
        }
    }
}