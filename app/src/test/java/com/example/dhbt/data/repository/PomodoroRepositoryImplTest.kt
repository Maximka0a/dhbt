package com.example.dhbt.data.repository

import androidx.datastore.core.DataStore
import com.example.dhbt.data.local.dao.PomodoroSessionDao
import com.example.dhbt.data.local.entity.PomodoroSessionEntity
import com.example.dhbt.data.mapper.PomodoroSessionMapper
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class PomodoroRepositoryImplTest {

    @Mock
    private lateinit var pomodoroSessionDao: PomodoroSessionDao

    @Mock
    private lateinit var pomodoroPreferencesStore: DataStore<PomodoroPreferences>

    private lateinit var pomodoroSessionMapper: PomodoroSessionMapper
    private lateinit var repository: PomodoroRepositoryImpl

    // Тестовые данные
    private val testSessionId = "test-session-id"
    private val testTaskId = "test-task-id"
    private val currentTime = System.currentTimeMillis()
    private val defaultPreferences = PomodoroPreferences(
        workDuration = 25,
        shortBreakDuration = 5,
        longBreakDuration = 15,
        pomodorosUntilLongBreak = 4,
        autoStartBreaks = true,
        autoStartPomodoros = false
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        pomodoroSessionMapper = PomodoroSessionMapper()

        repository = PomodoroRepositoryImpl(
            pomodoroSessionDao = pomodoroSessionDao,
            pomodoroSessionMapper = pomodoroSessionMapper,
            pomodoroPreferencesStore = pomodoroPreferencesStore
        )

        // Настраиваем DataStore для возврата тестовых настроек
        whenever(pomodoroPreferencesStore.data).thenReturn(flowOf(defaultPreferences))
    }

    @Test
    fun `startSession создает новую сессию Pomodoro`() = runTest {
        // Arrange - инициализация UUID для предсказуемых результатов
        val sessionId = UUID.nameUUIDFromBytes("predictable".toByteArray()).toString()

        // Act
        val result = repository.startSession(testTaskId, PomodoroSessionType.WORK)

        // Assert
        verify(pomodoroSessionDao).insertSession(any())
        assertNotNull(result)

        // Проверка что правильные данные были переданы в DAO
        argumentCaptor<PomodoroSessionEntity>().apply {
            verify(pomodoroSessionDao).insertSession(capture())
            val capturedSession = firstValue

            assertEquals(testTaskId, capturedSession.taskId)
            assertEquals(PomodoroSessionType.WORK.ordinal, capturedSession.type)
            assertEquals(defaultPreferences.workDuration, capturedSession.duration)
            assertEquals(false, capturedSession.isCompleted)
            assertEquals(null, capturedSession.endTime)
        }
    }

    @Test
    fun `startSession с разными типами имеет разную продолжительность`() = runTest {
        // Arrange
        val workType = PomodoroSessionType.WORK
        val shortBreakType = PomodoroSessionType.SHORT_BREAK
        val longBreakType = PomodoroSessionType.LONG_BREAK

        // Act
        repository.startSession(testTaskId, workType)
        repository.startSession(testTaskId, shortBreakType)
        repository.startSession(testTaskId, longBreakType)

        // Assert
        argumentCaptor<PomodoroSessionEntity>().apply {
            verify(pomodoroSessionDao, times(3)).insertSession(capture())

            assertEquals(defaultPreferences.workDuration, allValues[0].duration)
            assertEquals(defaultPreferences.shortBreakDuration, allValues[1].duration)
            assertEquals(defaultPreferences.longBreakDuration, allValues[2].duration)
        }
    }

    @Test
    fun `completeSession отмечает сессию как завершенную`() = runTest {
        // Act
        repository.completeSession(testSessionId)

        // Assert
        verify(pomodoroSessionDao).completeSession(eq(testSessionId), any())
    }

    @Test
    fun `cancelSession удаляет незавершенную сессию`() = runTest {
        // Arrange
        val session = createTestSessionEntity(testSessionId, isCompleted = false)
        whenever(pomodoroSessionDao.getAllSessions()).thenReturn(flowOf(listOf(session)))

        // Act
        repository.cancelSession(testSessionId)

        // Assert
        verify(pomodoroSessionDao).deleteSession(session)
    }

    @Test
    fun `updateSessionNotes обновляет заметки для сессии`() = runTest {
        // Arrange
        val session = createTestSessionEntity(testSessionId)
        whenever(pomodoroSessionDao.getAllSessions()).thenReturn(flowOf(listOf(session)))
        val notes = "Test session notes"

        // Act
        repository.updateSessionNotes(testSessionId, notes)

        // Assert
        verify(pomodoroSessionDao).updateSession(argThat {
            this.sessionId == testSessionId && this.notes == notes
        })
    }

    @Test
    fun `getTotalFocusTime возвращает суммарное время сессий`() = runTest {
        // Arrange
        val startDate = currentTime - 86400000 // 24 часа назад
        val endDate = currentTime
        whenever(pomodoroSessionDao.getTotalFocusTimeInRange(startDate, endDate)).thenReturn(120)

        // Act
        val result = repository.getTotalFocusTime(startDate, endDate)

        // Assert
        assertEquals(120, result)
    }

    @Test
    fun `getTotalFocusTimeForTask возвращает время для конкретной задачи`() = runTest {
        // Arrange
        whenever(pomodoroSessionDao.getTotalFocusTimeForTask(testTaskId)).thenReturn(45)

        // Act
        val result = repository.getTotalFocusTimeForTask(testTaskId)

        // Assert
        assertEquals(45, result)
    }

    @Test
    fun `getSessionsForTask возвращает сессии для задачи`() = runTest {
        // Arrange
        val sessions = listOf(
            createTestSessionEntity("session1", taskId = testTaskId),
            createTestSessionEntity("session2", taskId = testTaskId)
        )
        whenever(pomodoroSessionDao.getSessionsForTask(testTaskId)).thenReturn(flowOf(sessions))

        // Act
        val result = repository.getSessionsForTask(testTaskId).first()

        // Assert
        assertEquals(2, result.size)
        assertEquals(testTaskId, result[0].taskId)
    }

    @Test
    fun `getSessionsForTimeRange возвращает сессии в диапазоне времени`() = runTest {
        // Arrange
        val startTime = currentTime - 3600000 // 1 час назад
        val endTime = currentTime
        val sessions = listOf(
            createTestSessionEntity("session1", startTime = startTime + 10000),
            createTestSessionEntity("session2", startTime = startTime + 20000)
        )
        whenever(pomodoroSessionDao.getSessionsInTimeRange(startTime, endTime)).thenReturn(flowOf(sessions))

        // Act
        val result = repository.getSessionsForTimeRange(startTime, endTime).first()

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `updatePomodoroPreferences обновляет настройки`() = runTest {
        // Arrange
        val newPreferences = PomodoroPreferences(
            workDuration = 30,
            shortBreakDuration = 7,
            longBreakDuration = 20,
            pomodorosUntilLongBreak = 3,
            autoStartBreaks = false,
            autoStartPomodoros = true
        )

        // Act
        repository.updatePomodoroPreferences(newPreferences)

        // Assert
        verify(pomodoroPreferencesStore).updateData(any())
    }

    // Вспомогательные методы

    private fun createTestSessionEntity(
        id: String = testSessionId,
        taskId: String = testTaskId,
        startTime: Long = currentTime,
        endTime: Long? = null,
        duration: Int = 25,
        type: Int = PomodoroSessionType.WORK.ordinal,
        isCompleted: Boolean = false,
        notes: String? = null
    ): PomodoroSessionEntity {
        return PomodoroSessionEntity(
            sessionId = id,
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