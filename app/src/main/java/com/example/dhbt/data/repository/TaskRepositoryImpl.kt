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
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskRecurrenceDao: TaskRecurrenceDao,
    private val taskTagDao: TaskTagDao,
    private val tagRepository: TagRepository,
    private val taskMapper: TaskMapper,
    private val subtaskMapper: SubtaskMapper,
    private val taskRecurrenceMapper: TaskRecurrenceMapper,
    private val notificationRepository: NotificationRepository // Add this
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { taskEntities ->
            taskEntities.map { taskEntity ->
                taskMapper.mapFromEntity(taskEntity)
            }
        }
    }

    override suspend fun saveTaskRecurrence(recurrence: TaskRecurrence) {
        val recurrenceEntity = taskRecurrenceMapper.mapToEntity(recurrence)
        taskRecurrenceDao.insertTaskRecurrence(recurrenceEntity)
    }

    override suspend fun deleteTaskRecurrence(taskId: String) {
        taskRecurrenceDao.deleteRecurrenceForTask(taskId)
    }

    override suspend fun deleteTagsForTask(taskId: String) {
        taskTagDao.deleteAllTagsForTask(taskId)
    }

    override suspend fun deleteSubtasksForTask(taskId: String) {
        subtaskDao.deleteSubtasksForTask(taskId)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        // Получаем задачу
        val task = taskDao.getTaskById(taskId)

        // Если задача найдена, обновляем статус
        task?.let {
            // Обновляем статус задачи
            taskDao.updateTaskStatus(taskId, status.value)

            // Если задача помечена как выполненная, устанавливаем время выполнения
            if (status == TaskStatus.COMPLETED) {
                val completionTime = System.currentTimeMillis()
                taskDao.updateTaskCompletion(taskId, completionTime)
            } else if (status == TaskStatus.ACTIVE) {
                // Если задача возвращена в активное состояние, сбрасываем дату выполнения
                taskDao.updateTaskCompletion(taskId, null)
            }

            // Обработка повторяющихся задач при завершении
            if (status == TaskStatus.COMPLETED) {
                handleRecurringTask(taskId)
            }
        }
    }

    // Вспомогательный метод для обработки повторяющихся задач
    private suspend fun handleRecurringTask(taskId: String) {
        // Получаем информацию о повторении задачи
        val recurrence = taskRecurrenceDao.getRecurrenceForTask(taskId)
        recurrence?.let {
            val task = taskDao.getTaskById(taskId) ?: return

            // Создаем новую задачу в зависимости от типа повторения
            val nextDueDate = calculateNextDueDate(
                task.dueDate ?: System.currentTimeMillis(),
                recurrence.recurrenceType,
                recurrence.daysOfWeek,
                recurrence.monthDay,
                recurrence.customInterval
            )

            // Создаем копию задачи с обновленной датой
            val newTask = task.copy(
                taskId = UUID.randomUUID().toString(),
                status = 0, // 0 = ACTIVE
                completionDate = null,
                dueDate = nextDueDate
            )

            // Сохраняем новую задачу
            val newTaskId = newTask.taskId
            taskDao.insertTask(newTask)

            // Копируем подзадачи - используем список вместо Flow
            val subtasks = subtaskDao.getSubtasksForTaskSync(taskId)
            subtasks.forEach { subtaskEntity ->
                val newSubtask = subtaskEntity.copy(
                    subtaskId = UUID.randomUUID().toString(),
                    taskId = newTaskId,
                    isCompleted = false,
                    completionDate = null
                )
                subtaskDao.insertSubtask(newSubtask)
            }

            // Копируем связи с тегами - используем список вместо Flow
            val tagRefs = taskTagDao.getTaskTagCrossRefsForTask(taskId)
            tagRefs.forEach { ref ->
                taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(newTaskId, ref.tagId))
            }

            // Сохраняем правило повторения для новой задачи
            val newRecurrence = recurrence.copy(
                recurrenceId = UUID.randomUUID().toString(),
                taskId = newTaskId
            )
            taskRecurrenceDao.insertTaskRecurrence(newRecurrence)
        }
    }


    private fun calculateNextDueDate(
        currentDueDate: Long,
        recurrenceType: Int,
        daysOfWeekString: String?,
        monthDay: Int?,
        customInterval: Int?
    ): Long {
        val currentDate = LocalDate.ofEpochDay(currentDueDate / (24 * 60 * 60 * 1000))

        return when (recurrenceType) {
            0 -> { // Ежедневно
                currentDate.plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            1 -> { // По дням недели
                val daysOfWeek = daysOfWeekString?.split(",")?.map { it.toInt() } ?: listOf()
                var nextDate = currentDate.plusDays(1)

                // Ищем следующий подходящий день недели
                while (!daysOfWeek.contains(nextDate.dayOfWeek.value)) {
                    nextDate = nextDate.plusDays(1)
                }

                nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            2 -> { // Ежемесячно
                val day = monthDay ?: currentDate.dayOfMonth
                currentDate.plusMonths(1).withDayOfMonth(
                    minOf(day, currentDate.plusMonths(1).lengthOfMonth())
                ).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            3 -> { // Пользовательский интервал
                val interval = customInterval ?: 1
                currentDate.plusDays(interval.toLong())
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            else -> currentDueDate
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
        // Генерируем идентификатор для задачи, если он не был предоставлен
        val taskId = task.id

        // Создаем entity для вставки
        val taskEntity = taskMapper.mapToEntity(task)

        // Создаем уведомление, если у задачи есть срок выполнения
        if (task.dueDate != null) {
            notificationRepository.scheduleTaskNotification(
                taskId = task.id,
                dueDate = task.dueDate,
                dueTime = task.dueTime
            )
        }

        // Сохраняем задачу и возвращаем созданный ID
        taskDao.insertTask(taskEntity)

        // Возвращаем исходный ID задачи
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

        notificationRepository.deleteNotificationsForTarget(task.id, NotificationTarget.TASK)

        if (task.dueDate != null) {
            notificationRepository.scheduleTaskNotification(
                taskId = task.id,
                dueDate = task.dueDate,
                dueTime = task.dueTime
            )
        }

        // Обновляем связи с тегами
        taskTagDao.deleteAllTagsForTask(task.id)
        task.tags.forEach { tag ->
            taskTagDao.insertTaskTagCrossRef(TaskTagCrossRef(task.id, tag.id))
        }
    }

    override suspend fun deleteTask(taskId: String) {
        notificationRepository.deleteNotificationsForTarget(taskId, NotificationTarget.TASK)
        taskDao.deleteTaskById(taskId)
        // Каскадное удаление подзадач, повторений и связей с тегами
        // должно происходить автоматически благодаря настройке внешних ключей в Room
    }

    override suspend fun completeTask(taskId: String, completionTime: Long) {
        taskDao.updateTaskStatus(taskId, TaskStatus.COMPLETED.value)
        taskDao.updateTaskCompletion(taskId, completionTime)
        notificationRepository.deleteNotificationsForTarget(taskId, NotificationTarget.TASK)
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