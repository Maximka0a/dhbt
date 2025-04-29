package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.data.local.dao.HabitFrequencyDao
import com.example.dhbt.data.local.dao.HabitTrackingDao
import com.example.dhbt.data.local.entity.HabitEntity
import com.example.dhbt.data.local.entity.HabitFrequencyEntity
import com.example.dhbt.data.local.entity.HabitTrackingEntity
import com.example.dhbt.data.mapper.HabitFrequencyMapper
import com.example.dhbt.data.mapper.HabitMapper
import com.example.dhbt.data.mapper.HabitTrackingMapper
import com.example.dhbt.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class HabitRepositoryImplTest {

    @Mock
    private lateinit var habitDao: HabitDao

    @Mock
    private lateinit var habitFrequencyDao: HabitFrequencyDao

    @Mock
    private lateinit var habitTrackingDao: HabitTrackingDao

    private lateinit var habitMapper: HabitMapper
    private lateinit var habitFrequencyMapper: HabitFrequencyMapper
    private lateinit var habitTrackingMapper: HabitTrackingMapper
    private lateinit var repository: HabitRepositoryImpl

    // Константы для тестов
    private val testHabitId = "test-habit-id"
    private val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val yesterday = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        habitMapper = HabitMapper()
        habitFrequencyMapper = HabitFrequencyMapper()
        habitTrackingMapper = HabitTrackingMapper()

        repository = HabitRepositoryImpl(
            habitDao = habitDao,
            habitFrequencyDao = habitFrequencyDao,
            habitTrackingDao = habitTrackingDao,
            habitMapper = habitMapper,
            habitFrequencyMapper = habitFrequencyMapper,
            habitTrackingMapper = habitTrackingMapper
        )
    }

    // CRUD операции

    @Test
    fun `addHabit успешно добавляет привычку`() = runTest {
        // Arrange
        val habit = createTestHabit(testHabitId)

        // Act
        val resultId = repository.addHabit(habit)

        // Assert
        verify(habitDao).insertHabit(any())
        assertEquals(testHabitId, resultId)
    }

    @Test
    fun `getHabitById возвращает привычку при наличии`() = runTest {
        // Arrange
        val habitEntity = createTestHabitEntity(testHabitId)
        whenever(habitDao.getHabitById(testHabitId)).thenReturn(habitEntity)

        // Act
        val result = repository.getHabitById(testHabitId)

        // Assert
        assertNotNull(result)
        assertEquals(testHabitId, result.id)
        assertEquals("Тестовая привычка", result.title)
    }

    @Test
    fun `getHabitById возвращает null если привычка не найдена`() = runTest {
        // Arrange
        whenever(habitDao.getHabitById(testHabitId)).thenReturn(null)

        // Act
        val result = repository.getHabitById(testHabitId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `updateHabit обновляет существующую привычку`() = runTest {
        // Arrange
        val habit = createTestHabit(testHabitId).copy(title = "Обновленная привычка")

        // Act
        repository.updateHabit(habit)

        // Assert
        verify(habitDao).updateHabit(any())
    }

    @Test
    fun `deleteHabit удаляет привычку`() = runTest {
        // Act
        repository.deleteHabit(testHabitId)

        // Assert
        verify(habitDao).deleteHabitById(testHabitId)
    }

    // Методы фильтрации

    @Test
    fun `getActiveHabits возвращает только активные привычки`() = runTest {
        // Arrange
        val activeEntity1 = createTestHabitEntity("active1", status = HabitStatus.ACTIVE.value)
        val activeEntity2 = createTestHabitEntity("active2", status = HabitStatus.ACTIVE.value)
        val pausedEntity = createTestHabitEntity("paused1", status = HabitStatus.PAUSED.value)

        whenever(habitDao.getHabitsByStatus(HabitStatus.ACTIVE.value)).thenReturn(
            flowOf(listOf(activeEntity1, activeEntity2))
        )

        // Act
        val result = repository.getActiveHabits().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == HabitStatus.ACTIVE })
    }

    @Test
    fun `getHabitsByCategory фильтрует привычки по категории`() = runTest {
        // Arrange
        val categoryId = "category-1"
        val inCategory1 = createTestHabitEntity("hab1", categoryId = categoryId)
        val inCategory2 = createTestHabitEntity("hab2", categoryId = categoryId)
        val otherCategory = createTestHabitEntity("hab3", categoryId = "other-category")

        whenever(habitDao.getHabitsByCategory(categoryId)).thenReturn(
            flowOf(listOf(inCategory1, inCategory2))
        )

        // Act
        val result = repository.getHabitsByCategory(categoryId).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryId == categoryId })
    }

    // Тесты для работы с частотой

    @Test
    fun `getHabitWithFrequency возвращает привычку и её частоту`() = runTest {
        // Arrange
        val habitEntity = createTestHabitEntity(testHabitId)
        val frequencyEntity = createTestHabitFrequencyEntity(testHabitId)

        whenever(habitDao.getHabitById(testHabitId)).thenReturn(habitEntity)
        whenever(habitFrequencyDao.getFrequencyForHabit(testHabitId)).thenReturn(frequencyEntity)

        // Act
        val (habit, frequency) = repository.getHabitWithFrequency(testHabitId)

        // Assert
        assertNotNull(habit)
        assertNotNull(frequency)
        assertEquals(testHabitId, habit.id)
        assertEquals(testHabitId, frequency.habitId)
    }

    @Test
    fun `setHabitFrequency удаляет старую частоту и устанавливает новую`() = runTest {
        // Arrange
        val habitId = testHabitId
        val frequency = createTestHabitFrequency(habitId)

        // Симулируем, что привычка существует
        whenever(habitDao.getHabitById(habitId)).thenReturn(createTestHabitEntity(habitId))

        // Act
        repository.setHabitFrequency(habitId, frequency)

        // Assert
        verify(habitFrequencyDao).deleteFrequencyForHabit(habitId)
        verify(habitFrequencyDao).insertHabitFrequency(any())
    }

    // Тесты для отслеживания прогресса

    @Test
    fun `incrementHabitProgress для бинарной привычки отмечает её как выполненную`() = runTest {
        // Arrange
        val habitId = testHabitId
        val date = today

        val habit = createTestHabit(habitId).copy(type = HabitType.BINARY)
        whenever(habitDao.getHabitById(habitId)).thenReturn(createTestHabitEntity(habitId, habitType = HabitType.BINARY.ordinal))

        // Нет существующего отслеживания
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, date)).thenReturn(null)

        // Act
        repository.incrementHabitProgress(habitId, date)

        // Assert
        // Должна быть создана новая запись отслеживания с isCompleted = true
        verify(habitTrackingDao).insertHabitTracking(argThat {
            this.habitId == habitId && this.isCompleted && this.date == date
        })
    }

    @Test
    fun `incrementHabitProgress для количественной привычки увеличивает значение`() = runTest {
        // Arrange
        val habitId = testHabitId
        val date = today
        val currentValue = 2f
        val targetValue = 5f

        // Создаем привычку с типом QUANTITY и целевым значением 5
        val habitEntity = createTestHabitEntity(
            habitId,
            habitType = HabitType.QUANTITY.ordinal,
            targetValue = targetValue
        )
        whenever(habitDao.getHabitById(habitId)).thenReturn(habitEntity)

        // Существующее отслеживание с текущим значением 2
        val trackingEntity = createTestHabitTrackingEntity(
            habitId = habitId,
            date = date,
            isCompleted = false,
            value = currentValue
        )
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, date)).thenReturn(trackingEntity)

        // Act
        repository.incrementHabitProgress(habitId, date)

        // Assert
        // Проверяем, что значение увеличено на 1 и не отмечено как завершенное (3 < 5)
        verify(habitTrackingDao).updateHabitTracking(argThat {
            this.habitId == habitId && this.value == 3f && !this.isCompleted
        })
    }

    @Test
    fun `decrementHabitProgress для количественной привычки уменьшает значение`() = runTest {
        // Arrange
        val habitId = testHabitId
        val date = today
        val currentValue = 3f
        val targetValue = 5f

        // Создаем привычку с типом QUANTITY и целевым значением 5
        val habitEntity = createTestHabitEntity(
            habitId,
            habitType = HabitType.QUANTITY.ordinal,
            targetValue = targetValue
        )
        whenever(habitDao.getHabitById(habitId)).thenReturn(habitEntity)

        // Существующее отслеживание с текущим значением 3
        val trackingEntity = createTestHabitTrackingEntity(
            habitId = habitId,
            date = date,
            isCompleted = false,
            value = currentValue
        )
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, date)).thenReturn(trackingEntity)

        // Act
        repository.decrementHabitProgress(habitId, date)

        // Assert
        // Проверяем, что значение уменьшено на 1
        verify(habitTrackingDao).updateHabitTracking(argThat {
            this.habitId == habitId && this.value == 2f && !this.isCompleted
        })
    }

    // Тесты для стриков и статистики

    @Test
    fun `getHabitStreak возвращает текущую серию`() = runTest {
        // Arrange
        val currentStreak = 5
        val habitEntity = createTestHabitEntity(testHabitId, currentStreak = currentStreak)
        whenever(habitDao.getHabitById(testHabitId)).thenReturn(habitEntity)

        // Act
        val result = repository.getHabitStreak(testHabitId)

        // Assert
        assertEquals(currentStreak, result)
    }

    @Test
    fun `updateStreakAfterCompletion увеличивает стрик если привычка выполнялась вчера`() = runTest {
        // Arrange
        val habitId = testHabitId
        val currentStreak = 3

        // Привычка с текущим стриком 3
        val habitEntity = createTestHabitEntity(habitId, currentStreak = currentStreak)
        whenever(habitDao.getHabitById(habitId)).thenReturn(habitEntity)

        // Создаем отслеживание для вчерашнего и сегодняшнего дня (оба выполнены)
        val yesterdayTracking = createTestHabitTrackingEntity(habitId, yesterday, isCompleted = true)
        val todayTracking = createTestHabitTrackingEntity(habitId, today, isCompleted = true)

        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, yesterday)).thenReturn(yesterdayTracking)
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, today)).thenReturn(todayTracking)

        // Для тестирования приватного метода, мы вызовем его через публичный метод
        // который его использует
        val tracking = HabitTracking(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            date = today,
            isCompleted = true,
            value = 1f,
            duration = null
        )

        // Act
        repository.updateHabitTracking(tracking)

        // Assert
        // Проверяем, что стрик увеличен на 1 (было 3, стало 4)
        verify(habitDao).updateHabitStreak(habitId, 4)
    }

    @Test
    fun `updateStreakAfterCompletion сбрасывает стрик если был пропущен день`() = runTest {
        // Arrange
        val habitId = testHabitId
        val currentStreak = 5

        // Привычка с текущим стриком 5
        val habitEntity = createTestHabitEntity(habitId, currentStreak = currentStreak)
        whenever(habitDao.getHabitById(habitId)).thenReturn(habitEntity)

        // Вчерашнее отслеживание НЕ выполнено, сегодняшнее выполнено
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, yesterday)).thenReturn(null) // Не было отслеживания (пропуск)
        val todayTracking = createTestHabitTrackingEntity(habitId, today, isCompleted = true)
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, today)).thenReturn(todayTracking)

        val tracking = HabitTracking(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            date = today,
            isCompleted = true,
            value = 1f,
            duration = null
        )

        // Act
        repository.updateHabitTracking(tracking)

        // Assert
        // Стрик должен быть сброшен и начат заново (равен 1)
        verify(habitDao).updateHabitStreak(habitId, 1)
    }

    @Test
    fun `updateStreakAfterCompletion обновляет лучший стрик если текущий превышает предыдущий`() = runTest {
        // Arrange
        val habitId = testHabitId
        val currentStreak = 9
        val bestStreak = 8

        // Привычка с текущим стриком 9 и лучшим 8
        val habitEntity = createTestHabitEntity(habitId, currentStreak = currentStreak, bestStreak = bestStreak)
        whenever(habitDao.getHabitById(habitId)).thenReturn(habitEntity)

        // Вчера и сегодня выполнены
        val yesterdayTracking = createTestHabitTrackingEntity(habitId, yesterday, isCompleted = true)
        val todayTracking = createTestHabitTrackingEntity(habitId, today, isCompleted = true)

        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, yesterday)).thenReturn(yesterdayTracking)
        whenever(habitTrackingDao.getHabitTrackingForDate(habitId, today)).thenReturn(todayTracking)

        val tracking = HabitTracking(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            date = today,
            isCompleted = true,
            value = 1f,
            duration = null
        )

        // Act
        repository.updateHabitTracking(tracking)

        // Assert
        // Текущий стрик должен стать 10 и превзойти лучший (8)
        verify(habitDao).updateHabitStreak(habitId, 10)
        verify(habitDao).updateBestStreak(habitId, 10)
    }

// Вспомогательные методы для создания тестовых объектов

    private fun createTestHabit(
        id: String = testHabitId,
        title: String = "Тестовая привычка",
        type: HabitType = HabitType.BINARY,
        status: HabitStatus = HabitStatus.ACTIVE
    ): Habit {
        return Habit(
            id = id,
            title = title,
            description = "Описание тестовой привычки",
            iconEmoji = "💪",
            color = "#FF5722",
            creationDate = System.currentTimeMillis(),
            type = type,
            targetValue = 1f,
            unitOfMeasurement = "раз",
            targetStreak = 7,
            currentStreak = 0,
            bestStreak = 0,
            status = status,
            pausedDate = null,
            categoryId = null
        )
    }

    private fun createTestHabitEntity(
        id: String = testHabitId,
        title: String = "Тестовая привычка",
        habitType: Int = HabitType.BINARY.ordinal,
        status: Int = HabitStatus.ACTIVE.value,
        currentStreak: Int = 0,
        bestStreak: Int = 0,
        categoryId: String? = null,
        targetValue: Float = 1f
    ): HabitEntity {
        return HabitEntity(
            habitId = id,
            title = title,
            description = "Описание тестовой привычки",
            iconEmoji = "💪",
            color = "#FF5722",
            creationDate = System.currentTimeMillis(),
            habitType = habitType,
            targetValue = targetValue,
            unitOfMeasurement = "раз",
            targetStreak = 7,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            status = status,
            pausedDate = null,
            categoryId = categoryId
        )
    }

    private fun createTestHabitFrequency(habitId: String = testHabitId): HabitFrequency {
        return HabitFrequency(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            type = FrequencyType.DAILY,
            daysOfWeek = listOf(1, 3, 5), // Пн, Ср, Пт
            timesPerPeriod = 3,
            periodType = PeriodType.WEEK
        )
    }

    private fun createTestHabitFrequencyEntity(habitId: String = testHabitId): HabitFrequencyEntity {
        return HabitFrequencyEntity(
            frequencyId = UUID.randomUUID().toString(),
            habitId = habitId,
            frequencyType = FrequencyType.DAILY.value,
            daysOfWeek = "1,3,5",
            timesPerPeriod = 3,
            periodType = PeriodType.WEEK.value
        )
    }

    private fun createTestHabitTrackingEntity(
        habitId: String = testHabitId,
        date: Long = System.currentTimeMillis(),
        isCompleted: Boolean = false,
        value: Float? = 0f,
        duration: Int? = null
    ): HabitTrackingEntity {
        return HabitTrackingEntity(
            trackingId = UUID.randomUUID().toString(),
            habitId = habitId,
            date = date,
            isCompleted = isCompleted,
            value = value,
            duration = duration,
            notes = null // Добавлено поле notes
        )
    }
}