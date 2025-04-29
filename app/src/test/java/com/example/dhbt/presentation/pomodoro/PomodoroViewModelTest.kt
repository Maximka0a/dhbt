package com.example.dhbt.presentation.pomodoro

import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class PomodoroViewModelTest {

    @Mock
    private lateinit var pomodoroRepository: PomodoroRepository

    @Mock
    private lateinit var taskRepository: TaskRepository

    private lateinit var viewModel: PomodoroViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val defaultPrefs = PomodoroPreferences(
        workDuration = 25,
        shortBreakDuration = 5,
        longBreakDuration = 15,
        pomodorosUntilLongBreak = 4,
        autoStartBreaks = false,
        autoStartPomodoros = false
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Настраиваем моки
        whenever(pomodoroRepository.getPomodoroPreferences()).thenReturn(flowOf(defaultPrefs))
        whenever(taskRepository.getTasksByStatus(TaskStatus.ACTIVE)).thenReturn(flowOf(emptyList()))

        // Сессии для тестирования загрузки статистики
        val todaySessions = listOf<PomodoroSession>()
        whenever(pomodoroRepository.getSessionsForTimeRange(any(), any())).thenReturn(flowOf(todaySessions))

        viewModel = PomodoroViewModel(pomodoroRepository, taskRepository)

        // Выполняем инициализацию
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `начальные значения соответствуют настройкам Pomodoro`() = runTest {
        val uiState = viewModel.uiState.first()
        val prefs = viewModel.preferences.first()

        assertEquals(TimerType.POMODORO, uiState.timerType)
        assertEquals(TimerState.IDLE, uiState.timerState)
        assertEquals(PomodoroSessionType.WORK, uiState.pomodoroSessionType)

        // Проверяем, что начальное время таймера соответствует настройкам
        assertEquals(TimeUnit.MINUTES.toMillis(prefs.workDuration.toLong()), uiState.totalTime)
        assertEquals(TimeUnit.MINUTES.toMillis(prefs.workDuration.toLong()), uiState.remainingTime)
    }

    @Test
    fun `выбор типа таймера обновляет состояние UI`() = runTest {
        // Act
        viewModel.onTimerTypeSelected(TimerType.STOPWATCH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.first()
        assertEquals(TimerType.STOPWATCH, uiState.timerType)
        assertEquals(0L, uiState.totalTime)
        assertEquals(0L, uiState.remainingTime)
    }

    @Test
    fun `выбор типа сессии Pomodoro обновляет длительность таймера`() = runTest {
        // Act
        viewModel.onPomodoroSessionTypeSelected(PomodoroSessionType.SHORT_BREAK)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.first()
        val prefs = viewModel.preferences.first()

        assertEquals(PomodoroSessionType.SHORT_BREAK, uiState.pomodoroSessionType)
        assertEquals(TimeUnit.MINUTES.toMillis(prefs.shortBreakDuration.toLong()), uiState.totalTime)
        assertEquals(TimeUnit.MINUTES.toMillis(prefs.shortBreakDuration.toLong()), uiState.remainingTime)
    }

    @Test
    fun `выбор задачи обновляет состояние UI`() = runTest {
        // Arrange
        val task = createTestTask("task1", estimatedPomodoroSessions = 4)
        whenever(taskRepository.getTaskById("task1")).thenReturn(task)
        whenever(pomodoroRepository.getTotalFocusTimeForTask("task1")).thenReturn(0)
        whenever(pomodoroRepository.getSessionsForTask("task1")).thenReturn(flowOf(emptyList()))

        // Act
        viewModel.onTaskSelected(task)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.first()

        assertEquals(task, uiState.selectedTask)
        assertEquals(4, uiState.totalTaskSessions)
        assertEquals(TimerType.POMODORO, uiState.timerType)
    }

    @Test
    fun `updatePreferences обновляет настройки Pomodoro`() = runTest {
        // Arrange
        val newPrefs = PomodoroPreferences(
            workDuration = 30,
            shortBreakDuration = 10,
            longBreakDuration = 20,
            pomodorosUntilLongBreak = 3,
            autoStartBreaks = true,
            autoStartPomodoros = true
        )

        // Act
        viewModel.updatePreferences(newPrefs)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(pomodoroRepository).updatePomodoroPreferences(newPrefs)
    }

    @Test
    fun `resetCompletedPomodoros сбрасывает счетчик Pomodoro`() = runTest {
        // Arrange - устанавливаем счетчик
        val task = createTestTask("task1")
        whenever(taskRepository.getTaskById("task1")).thenReturn(task)
        whenever(pomodoroRepository.getTotalFocusTimeForTask("task1")).thenReturn(0)

        val completedSessions = listOf(
            createTestPomodoroSession("session1", taskId = "task1", isCompleted = true),
            createTestPomodoroSession("session2", taskId = "task1", isCompleted = true)
        )

        val mutableFlow = MutableStateFlow(completedSessions)
        whenever(pomodoroRepository.getSessionsForTask("task1")).thenReturn(mutableFlow)

        viewModel.onTaskSelected(task)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.resetCompletedPomodoros()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.first()
        assertEquals(0, uiState.completedPomodoros)
    }

    @Test
    fun `loadTaskById загружает задачу и статистику`() = runTest {
        // Arrange
        val task = createTestTask("task1", estimatedPomodoroSessions = 3)
        whenever(taskRepository.getTaskById("task1")).thenReturn(task)
        whenever(pomodoroRepository.getTotalFocusTimeForTask("task1")).thenReturn(45)

        val completedSessions = listOf(
            createTestPomodoroSession("session1", taskId = "task1", isCompleted = true),
            createTestPomodoroSession("session2", taskId = "task1", isCompleted = true)
        )
        whenever(pomodoroRepository.getSessionsForTask("task1")).thenReturn(flowOf(completedSessions))

        // Act
        viewModel.loadTaskById("task1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.first()
        val taskStats = viewModel.taskStats.first()

        assertNotNull(uiState.selectedTask)
        assertEquals("task1", uiState.selectedTask?.id)
        assertEquals(3, uiState.totalTaskSessions)
        assertEquals(2, taskStats.completedSessions)
        assertEquals(45, taskStats.focusTimeMinutes)
    }

    @Test
    fun `loadTodayStats загружает статистику за день`() = runTest {
        // Arrange
        val completedSessions = listOf(
            createTestPomodoroSession("session1", isCompleted = true),
            createTestPomodoroSession("session2", isCompleted = true),
            createTestPomodoroSession("session3", isCompleted = false)
        )

        whenever(pomodoroRepository.getSessionsForTimeRange(any(), any())).thenReturn(flowOf(completedSessions))
        whenever(pomodoroRepository.getTotalFocusTime(any(), any())).thenReturn(75)

        // Act
        viewModel.loadTodayStats()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val todayStats = viewModel.todayStats.first()
        assertEquals(2, todayStats.completedSessions)
        assertEquals(75, todayStats.totalFocusMinutes)
    }

    // Вспомогательные методы

    private fun createTestTask(
        id: String = "test-task-id",
        title: String = "Test Task",
        duration: Int? = 50,
        estimatedPomodoroSessions: Int? = 2
    ): Task {
        return Task(
            id = id,
            title = title,
            description = null,
            categoryId = null,
            color = null,
            creationDate = System.currentTimeMillis(),
            dueDate = null,
            dueTime = null,
            duration = duration,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = null,
            estimatedPomodoroSessions = estimatedPomodoroSessions,
            subtasks = emptyList(),
            tags = emptyList(),
            recurrence = null
        )
    }

    private fun createTestPomodoroSession(
        id: String = "test-session-id",
        taskId: String? = null,
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null,
        duration: Int = 25,
        type: PomodoroSessionType = PomodoroSessionType.WORK,
        isCompleted: Boolean = false,
        notes: String? = null
    ): PomodoroSession {
        return PomodoroSession(
            id = id,
            taskId = taskId,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            type = type,
            isCompleted = isCompleted,
            notes = notes
        )
    }
}