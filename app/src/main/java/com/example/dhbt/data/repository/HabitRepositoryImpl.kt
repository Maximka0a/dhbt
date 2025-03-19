package com.example.dhbt.data.repository

import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.data.local.dao.HabitFrequencyDao
import com.example.dhbt.data.local.dao.HabitTrackingDao
import com.example.dhbt.data.mapper.HabitFrequencyMapper
import com.example.dhbt.data.mapper.HabitMapper
import com.example.dhbt.data.mapper.HabitTrackingMapper
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitFrequency
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitTracking
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import java.util.UUID
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitFrequencyDao: HabitFrequencyDao,
    private val habitTrackingDao: HabitTrackingDao,
    private val habitMapper: HabitMapper,
    private val habitFrequencyMapper: HabitFrequencyMapper,
    private val habitTrackingMapper: HabitTrackingMapper
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { entity ->
                val habit = habitMapper.mapFromEntity(entity)
                // Загрузка частоты привычки
                val frequencyEntity = habitFrequencyDao.getFrequencyForHabit(entity.habitId)
                val frequency = frequencyEntity?.let { habitFrequencyMapper.mapFromEntity(it) }
                habit.copy(frequency = frequency)
            }
        }
    }

    override fun getActiveHabits(): Flow<List<Habit>> {
        return habitDao.getHabitsByStatus(HabitStatus.ACTIVE.value).map { entities ->
            entities.map { entity ->
                val habit = habitMapper.mapFromEntity(entity)
                // Загрузка частоты привычки
                val frequencyEntity = habitFrequencyDao.getFrequencyForHabit(entity.habitId)
                val frequency = frequencyEntity?.let { habitFrequencyMapper.mapFromEntity(it) }
                habit.copy(frequency = frequency)
            }
        }
    }
    // Добавляем реализацию метода incrementHabitProgress
    override suspend fun incrementHabitProgress(habitId: String) {
        // Получаем привычку
        val habit = getHabitById(habitId) ?: return

        // Получаем текущую дату
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Проверяем, есть ли уже запись за сегодняшний день
        val existingTracking = getHabitTrackingForDate(habitId, today)

        if (existingTracking != null) {
            // Обновляем существующую запись
            when (habit.type) {
                HabitType.BINARY -> {
                    // Для бинарного типа просто отмечаем как выполненное
                    val updatedTracking = existingTracking.copy(
                        isCompleted = true
                    )
                    updateHabitTracking(updatedTracking)
                }
                HabitType.QUANTITY -> {
                    // Для количественного типа увеличиваем значение на 1
                    val currentValue = existingTracking.value ?: 0f
                    val newValue = currentValue + 1
                    val targetValue = habit.targetValue ?: 1f
                    val isCompleted = newValue >= targetValue

                    val updatedTracking = existingTracking.copy(
                        value = newValue,
                        isCompleted = isCompleted
                    )
                    updateHabitTracking(updatedTracking)
                }
                HabitType.TIME -> {
                    // Для времени увеличиваем на 1 минуту (или другую логику)
                    val currentDuration = existingTracking.duration ?: 0
                    val newDuration = currentDuration + 1 // Предположим, +1 минута
                    val targetDuration = habit.targetValue?.toInt() ?: 1
                    val isCompleted = newDuration >= targetDuration

                    val updatedTracking = existingTracking.copy(
                        duration = newDuration,
                        isCompleted = isCompleted
                    )
                    updateHabitTracking(updatedTracking)
                }
            }
        } else {
            // Создаем новую запись
            val isCompleted = when (habit.type) {
                HabitType.BINARY -> true
                HabitType.QUANTITY -> 1f >= (habit.targetValue ?: 1f)
                HabitType.TIME -> 1 >= (habit.targetValue?.toInt() ?: 1)
            }

            val value = if (habit.type == HabitType.QUANTITY) 1f else null
            val duration = if (habit.type == HabitType.TIME) 1 else null

            val tracking = HabitTracking(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = today,
                isCompleted = isCompleted,
                value = value,
                duration = duration
            )

            trackHabit(tracking)
        }
    }
    override fun getHabitsByCategory(categoryId: String): Flow<List<Habit>> {
        return habitDao.getHabitsByCategory(categoryId).map { entities ->
            entities.map { entity ->
                val habit = habitMapper.mapFromEntity(entity)
                // Загрузка частоты привычки
                val frequencyEntity = habitFrequencyDao.getFrequencyForHabit(entity.habitId)
                val frequency = frequencyEntity?.let { habitFrequencyMapper.mapFromEntity(it) }
                habit.copy(frequency = frequency)
            }
        }
    }

    override suspend fun getHabitById(habitId: String): Habit? {
        val entity = habitDao.getHabitById(habitId) ?: return null
        val habit = habitMapper.mapFromEntity(entity)

        // Загрузка частоты привычки
        val frequencyEntity = habitFrequencyDao.getFrequencyForHabit(habitId)
        val frequency = frequencyEntity?.let { habitFrequencyMapper.mapFromEntity(it) }

        return habit.copy(frequency = frequency)
    }

    override suspend fun addHabit(habit: Habit): String {
        val habitEntity = habitMapper.mapToEntity(habit)
        val habitId = habitDao.insertHabit(habitEntity).toString()

        // Сохранение частоты привычки
        habit.frequency?.let { frequency ->
            val frequencyEntity = habitFrequencyMapper.mapToEntity(
                frequency.copy(habitId = habitId)
            )
            habitFrequencyDao.insertHabitFrequency(frequencyEntity)
        }

        return habitId
    }

    override suspend fun updateHabit(habit: Habit) {
        val habitEntity = habitMapper.mapToEntity(habit)
        habitDao.updateHabit(habitEntity)

        // Обновление частоты привычки
        habitFrequencyDao.deleteFrequencyForHabit(habit.id)
        habit.frequency?.let { frequency ->
            val frequencyEntity = habitFrequencyMapper.mapToEntity(frequency)
            habitFrequencyDao.insertHabitFrequency(frequencyEntity)
        }
    }

    override suspend fun deleteHabit(habitId: String) {
        habitDao.deleteHabitById(habitId)
        // Каскадное удаление частоты и отслеживаний происходит автоматически
    }

    override suspend fun changeHabitStatus(habitId: String, status: HabitStatus) {
        habitDao.updateHabitStatus(habitId, status.value)

        // Если привычка поставлена на паузу, сохраняем дату
        if (status == HabitStatus.PAUSED) {
            val habit = habitDao.getHabitById(habitId)
            habit?.let {
                habitDao.updateHabit(it.copy(pausedDate = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun getHabitFrequency(habitId: String): HabitFrequency? {
        val entity = habitFrequencyDao.getFrequencyForHabit(habitId) ?: return null
        return habitFrequencyMapper.mapFromEntity(entity)
    }

    override suspend fun setHabitFrequency(habitId: String, frequency: HabitFrequency) {
        val frequencyEntity = habitFrequencyMapper.mapToEntity(
            frequency.copy(habitId = habitId)
        )
        habitFrequencyDao.insertHabitFrequency(frequencyEntity)
    }

    override fun getHabitTrackings(habitId: String): Flow<List<HabitTracking>> {
        return habitTrackingDao.getTrackingsForHabit(habitId).map { entities ->
            entities.map { habitTrackingMapper.mapFromEntity(it) }
        }
    }

    override fun getHabitTrackingsForRange(habitId: String, startDate: Long, endDate: Long): Flow<List<HabitTracking>> {
        return habitTrackingDao.getTrackingsForHabitInRange(habitId, startDate, endDate).map { entities ->
            entities.map { habitTrackingMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getHabitTrackingForDate(habitId: String, date: Long): HabitTracking? {
        val entity = habitTrackingDao.getHabitTrackingForDate(habitId, date) ?: return null
        return habitTrackingMapper.mapFromEntity(entity)
    }

    override suspend fun trackHabit(tracking: HabitTracking) {
        val entity = habitTrackingMapper.mapToEntity(tracking)
        habitTrackingDao.insertHabitTracking(entity)

        // Обновление streak если привычка выполнена
        if (tracking.isCompleted) {
            updateStreakAfterCompletion(tracking.habitId)
        }
    }

    override suspend fun updateHabitTracking(tracking: HabitTracking) {
        val entity = habitTrackingMapper.mapToEntity(tracking)
        habitTrackingDao.updateHabitTracking(entity)

        // Обновление streak после изменения статуса отслеживания
        updateStreakAfterCompletion(tracking.habitId)
    }

    override suspend fun getHabitStreak(habitId: String): Int {
        val habit = habitDao.getHabitById(habitId)
        return habit?.currentStreak ?: 0
    }

    override suspend fun getBestStreak(habitId: String): Int {
        val habit = habitDao.getHabitById(habitId)
        return habit?.bestStreak ?: 0
    }

    override suspend fun getCompletionRate(habitId: String, days: Int): Float {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endDate = today.plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val completedCount = habitTrackingDao.countCompletedTrackingsInRange(habitId, startDate, endDate)
        return completedCount.toFloat() / days
    }

    override suspend fun calculateHabitProgress(habitId: String, date: Long): Float {
        val habit = getHabitById(habitId) ?: return 0f
        val tracking = getHabitTrackingForDate(habitId, date)

        if (tracking == null || !tracking.isCompleted) return 0f

        return when (habit.type) {
            HabitType.BINARY -> 1f
            HabitType.QUANTITY -> {
                val targetValue = habit.targetValue ?: 1f
                val value = tracking.value ?: 0f
                (value / targetValue).coerceIn(0f, 1f)
            }
            HabitType.TIME -> {
                val targetDuration = habit.targetValue?.toInt() ?: 0
                val duration = tracking.duration ?: 0
                if (targetDuration <= 0) 0f else (duration.toFloat() / targetDuration).coerceIn(0f, 1f)
            }
        }
    }

    // Вспомогательные методы
    private suspend fun updateStreakAfterCompletion(habitId: String) {
        val habit = habitDao.getHabitById(habitId) ?: return

        // Получаем дату вчерашнего дня
        val yesterday = LocalDate.now().minusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Проверяем, была ли привычка выполнена вчера
        val yesterdayTracking = habitTrackingDao.getHabitTrackingForDate(habitId, yesterday)
        val wasCompletedYesterday = yesterdayTracking?.isCompleted ?: false

        // Получаем сегодняшнее отслеживание
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayTracking = habitTrackingDao.getHabitTrackingForDate(habitId, today)
        val isCompletedToday = todayTracking?.isCompleted ?: false

        // Обновляем стрик
        var newStreak = habit.currentStreak
        if (isCompletedToday) {
            if (wasCompletedYesterday || newStreak == 0) {
                // Увеличиваем стрик если выполнялась вчера или это первый день
                newStreak += 1
            } else {
                // Сбрасываем стрик, если был пропуск
                newStreak = 1
            }
        } else if (!wasCompletedYesterday && newStreak > 0) {
            // Сбрасываем стрик, если была пропущена вчера и сегодня
            newStreak = 0
        }

        // Обновляем текущий стрик
        habitDao.updateHabitStreak(habitId, newStreak)

        // Обновляем лучший стрик если текущий больше
        if (newStreak > habit.bestStreak) {
            habitDao.updateBestStreak(habitId, newStreak)
        }
    }
}