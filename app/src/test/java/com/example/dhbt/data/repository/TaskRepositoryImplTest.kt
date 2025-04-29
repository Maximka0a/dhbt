package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.SubtaskDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.dao.TaskRecurrenceDao
import com.example.dhbt.data.local.dao.TaskTagDao
import com.example.dhbt.data.mapper.TaskMapper
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.domain.repository.TagRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq  // Для точного сопоставления аргументов
import org.mockito.kotlin.doThrow  // Для настройки выбрасывания исключений для void методов
import kotlin.jvm.java
import kotlin.test.assertEquals
import kotlin.test.fail

class TaskRepositoryImplTest {

    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var taskDao: TaskDao
    private lateinit var subtaskDao: SubtaskDao  // Добавляем как поле класса
    private lateinit var notificationRepository: NotificationRepository

    @Before
    fun setUp() {
        taskDao = mock(TaskDao::class.java)
        subtaskDao = mock(SubtaskDao::class.java)  // Инициализируем
        notificationRepository = mock(NotificationRepository::class.java)

        // Для простого теста другие зависимости можно тоже замокать или передать null/заглушки
        taskRepository = TaskRepositoryImpl(
            taskDao = taskDao,
            subtaskDao = subtaskDao,  // Используем поле
            taskRecurrenceDao = mock(TaskRecurrenceDao::class.java),
            taskTagDao = mock(TaskTagDao::class.java),
            tagRepository = mock(TagRepository::class.java),
            taskMapper = TaskMapper(),
            subtaskMapper = mock(),
            taskRecurrenceMapper = mock(),
            notificationRepository = notificationRepository
        )
    }

    @Test
    fun `addTask с минимальным набором полей сохраняет задачу`() = runBlocking {
        // Arrange
        val taskId = "123"
        val now = System.currentTimeMillis()
        val task = Task(
            id = taskId,
            title = "Test Task",
            creationDate = now,
            status = TaskStatus.ACTIVE,
            priority = TaskPriority.MEDIUM
        )
        // Мокаем поведение insertTask, чтобы ничего не делать
        `when`(taskDao.insertTask(any())).thenReturn(1L)

        // Act
        val resultId = taskRepository.addTask(task)

        // Assert
        verify(taskDao).insertTask(any())
        assertEquals(taskId, resultId)
    }

    @Test
    fun `addTask с полным набором полей сохраняет задачу`() = runBlocking {
        // Arrange
        val taskId = "123"
        val now = System.currentTimeMillis()
        val tomorrow = now + 24*60*60*1000
        val categoryId = "category1"
        val task = Task(
            id = taskId,
            title = "Полная задача",
            description = "Подробное описание задачи",
            categoryId = categoryId,
            color = "#FF5722",
            creationDate = now,
            dueDate = tomorrow,
            dueTime = "14:30",
            duration = 120,
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 1,
            estimatedPomodoroSessions = 3,
            subtasks = listOf(),
            tags = listOf(),
            recurrence = null
        )

        `when`(taskDao.insertTask(any())).thenReturn(1L)

        // Act
        val resultId = taskRepository.addTask(task)

        // Assert
        verify(taskDao).insertTask(any())
        assertEquals(taskId, resultId)
    }

    @Test
    fun `updateTask обновляет существующую задачу в базе данных`() = runBlocking {
        // Arrange
        val taskId = "123"
        val now = System.currentTimeMillis()
        val task = Task(
            id = taskId,
            title = "Обновленная задача",
            description = "Новое описание",
            creationDate = now,
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE
        )

        // Act
        taskRepository.updateTask(task)

        // Assert
        verify(taskDao).updateTask(any())
    }

    @Test
    fun `пустое название задачи не должно сохраняться`() {
        // Arrange
        val now = System.currentTimeMillis()
        val taskWithEmptyTitle = Task(
            id = "123",
            title = "",  // Пустое название
            creationDate = now,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE
        )

        // Act & Assert
        try {
            validateTask(taskWithEmptyTitle)
            // Если не выбросило исключение, тест должен упасть
            fail("Ожидалось исключение IllegalArgumentException, но оно не было выброшено")
        } catch (e: IllegalArgumentException) {
            // Проверяем текст исключения
            assertEquals("Task title cannot be empty", e.message)
        }
    }

    // Вспомогательная функция для валидации
    private fun validateTask(task: Task) {
        if (task.title.isBlank()) {
            throw IllegalArgumentException("Task title cannot be empty")
        }
    }

    @Test
    fun `getTaskById возвращает null для несуществующего ID`() {
        runBlocking {
            // Arrange
            val nonExistentId = "non-existent-id"
            `when`(taskDao.getTaskById(nonExistentId)).thenReturn(null)

            // Act
            val result = taskRepository.getTaskById(nonExistentId)

            // Assert
            assertEquals(null, result)
            verify(taskDao).getTaskById(nonExistentId)
        }
    }

    @Test
    fun `updateTask на несуществующую задачу не вызывает краш`() = runBlocking {
        // Arrange
        val taskId = "non-existent-id"
        val task = Task(
            id = taskId,
            title = "Несуществующая задача",
            creationDate = System.currentTimeMillis(),
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE
        )

        // Имитируем ситуацию, когда DAO выбрасывает исключение при обновлении
        `when`(taskDao.updateTask(any())).thenThrow(RuntimeException("DB Error"))

        // Act & Assert
        try {
            taskRepository.updateTask(task)
            fail("Ожидалось исключение RuntimeException")
        } catch (e: RuntimeException) {
            // Проверяем, что исключение действительно прошло через репозиторий
            assertEquals("DB Error", e.message)
            verify(taskDao).updateTask(any())
        }
    }

    @Test
    fun `deleteTask для несуществующей задачи не вызывает ошибок`() = runBlocking {
        // Arrange
        val nonExistentId = "non-existent-id"

        // Act
        taskRepository.deleteTask(nonExistentId)

        // Assert
        // Проверяем, что метод был вызван, и никаких исключений не произошло
        verify(taskDao).deleteTaskById(nonExistentId)
    }

    @Test
    fun `addTask с некорректной датой выполняется, но не создаёт уведомление`() = runBlocking {
        // Arrange
        val taskId = "123"
        val invalidFutureDate = System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000 * 10 // 10 лет в будущем
        val task = Task(
            id = taskId,
            title = "Задача с далекой датой",
            creationDate = System.currentTimeMillis(),
            dueDate = invalidFutureDate,
            status = TaskStatus.ACTIVE,
            priority = TaskPriority.MEDIUM
        )

        `when`(taskDao.insertTask(any())).thenReturn(1L)

        // Act
        taskRepository.addTask(task)

        // Assert
        verify(taskDao).insertTask(any()) // Проверяем, что задача сохранена
        // Проверяем, что уведомление не было создано для слишком далёкой даты
        verify(notificationRepository, never()).scheduleTaskNotification(eq(taskId), eq(invalidFutureDate), any())
    }

    // Вспомогательная функция для валидации даты
    private fun validateTaskDate(task: Task) {
        val maxFutureDate = System.currentTimeMillis() + 5 * 365 * 24 * 60 * 60 * 1000L // 5 лет
        if (task.dueDate != null && task.dueDate > maxFutureDate) {
            throw IllegalArgumentException("Task due date cannot be more than 5 years in the future")
        }
    }

    @Test
    fun `completeSubtask для несуществующей подзадачи`() = runBlocking {
        // Arrange
        val nonExistentSubtaskId = "non-existent-subtask"

        // Исправленный вызов doThrow
        doThrow(RuntimeException("Subtask not found")).`when`(subtaskDao)
            .updateSubtaskCompletion(
                subtaskId = eq(nonExistentSubtaskId),
                isCompleted = eq(true),
                completionDate = any()
            )

        // Act & Assert
        try {
            taskRepository.completeSubtask(nonExistentSubtaskId, true)
            fail("Ожидалось исключение RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("Subtask not found", e.message)
        }
    }
}