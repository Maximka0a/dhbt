package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.SubtaskDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.dao.TaskRecurrenceDao
import com.example.dhbt.data.local.dao.TaskTagDao
import com.example.dhbt.data.local.entity.TaskEntity
import com.example.dhbt.data.mapper.TaskMapper
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.domain.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class TaskRepositoryFilterTest {

    @Mock
    private lateinit var taskDao: TaskDao

    @Mock
    private lateinit var subtaskDao: SubtaskDao

    @Mock
    private lateinit var taskRecurrenceDao: TaskRecurrenceDao

    @Mock
    private lateinit var taskTagDao: TaskTagDao

    @Mock
    private lateinit var tagRepository: TagRepository

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    private lateinit var taskMapper: TaskMapper
    private lateinit var taskRepository: TaskRepositoryImpl

    // Тестовые данные
    private val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val tomorrow = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val nextWeek = LocalDate.now().plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val lastMonth = LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        taskMapper = TaskMapper()

        taskRepository = TaskRepositoryImpl(
            taskDao = taskDao,
            subtaskDao = subtaskDao,
            taskRecurrenceDao = taskRecurrenceDao,
            taskTagDao = taskTagDao,
            tagRepository = tagRepository,
            taskMapper = taskMapper,
            subtaskMapper = mock(),
            taskRecurrenceMapper = mock(),
            notificationRepository = notificationRepository
        )
    }

    @Test
    fun `getTasksByStatus возвращает только активные задачи`() = runTest {
        // Arrange
        val activeTask1 = TaskEntity(
            taskId = "active1",
            title = "Активная задача 1",
            status = TaskStatus.ACTIVE.ordinal
        )
        val activeTask2 = TaskEntity(
            taskId = "active2",
            title = "Активная задача 2",
            status = TaskStatus.ACTIVE.ordinal
        )
        val completedTask = TaskEntity(
            taskId = "completed1",
            title = "Завершенная задача",
            status = TaskStatus.COMPLETED.ordinal
        )

        val allTasks = listOf(activeTask1, activeTask2, completedTask)
        whenever(taskDao.getTasksByStatus(TaskStatus.ACTIVE.ordinal)).thenReturn(flowOf(listOf(activeTask1, activeTask2)))

        // Act
        val result = taskRepository.getTasksByStatus(TaskStatus.ACTIVE).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == TaskStatus.ACTIVE })
        assertTrue(result.any { it.id == "active1" })
        assertTrue(result.any { it.id == "active2" })
    }

    @Test
    fun `getTasksByPriority возвращает только задачи с высоким приоритетом`() = runTest {
        // Arrange
        val highPriorityTask1 = TaskEntity(
            taskId = "high1",
            title = "Важная задача 1",
            priority = TaskPriority.HIGH.ordinal
        )
        val highPriorityTask2 = TaskEntity(
            taskId = "high2",
            title = "Важная задача 2",
            priority = TaskPriority.HIGH.ordinal
        )
        val mediumPriorityTask = TaskEntity(
            taskId = "medium1",
            title = "Средняя важность",
            priority = TaskPriority.MEDIUM.ordinal
        )

        whenever(taskDao.getTasksByPriority(TaskPriority.HIGH.ordinal)).thenReturn(
            flowOf(listOf(highPriorityTask1, highPriorityTask2))
        )

        // Act
        val result = taskRepository.getTasksByPriority(TaskPriority.HIGH).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.priority == TaskPriority.HIGH })
    }



    @Test
    fun `getTasksByCategory возвращает задачи только из указанной категории`() = runTest {
        // Arrange
        val categoryId = "work123"
        val workTask1 = TaskEntity(
            taskId = "work1",
            title = "Рабочая задача 1",
            categoryId = categoryId
        )
        val workTask2 = TaskEntity(
            taskId = "work2",
            title = "Рабочая задача 2",
            categoryId = categoryId
        )
        val personalTask = TaskEntity(
            taskId = "personal1",
            title = "Личная задача",
            categoryId = "personal456"
        )

        whenever(taskDao.getTasksByCategory(categoryId)).thenReturn(
            flowOf(listOf(workTask1, workTask2))
        )

        // Act
        val result = taskRepository.getTasksByCategory(categoryId).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryId == categoryId })
    }

    @Test
    fun `getTasksByEisenhowerQuadrant возвращает задачи только из указанного квадранта`() = runTest {
        // Arrange
        val quadrant = 1 // Важно и срочно
        val q1Task1 = TaskEntity(
            taskId = "q1task1",
            title = "Важная срочная задача 1",
            eisenhowerQuadrant = quadrant
        )
        val q1Task2 = TaskEntity(
            taskId = "q1task2",
            title = "Важная срочная задача 2",
            eisenhowerQuadrant = quadrant
        )
        val q2Task = TaskEntity(
            taskId = "q2task",
            title = "Важная несрочная задача",
            eisenhowerQuadrant = 2
        )

        whenever(taskDao.getTasksByEisenhowerQuadrant(quadrant)).thenReturn(
            flowOf(listOf(q1Task1, q1Task2))
        )

        // Act
        val result = taskRepository.getTasksByEisenhowerQuadrant(quadrant).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.eisenhowerQuadrant == quadrant })
    }

}