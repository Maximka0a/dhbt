package com.example.dhbt.data.repository

import androidx.room.Transaction
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
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
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
    // Базовые операции с привычками
    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { habitMapper.mapFromEntity(it) }
        }
    }

    override fun getActiveHabits(): Flow<List<Habit>> {
        return habitDao.getHabitsByStatus(HabitStatus.ACTIVE.value).map { entities ->
            entities.map { habitMapper.mapFromEntity(it) }
        }
    }

    override fun getHabitsByCategory(categoryId: String): Flow<List<Habit>> {
        return habitDao.getHabitsByCategory(categoryId).map { entities ->
            entities.map { habitMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getHabitById(habitId: String): Habit? {
        val entity = habitDao.getHabitById(habitId) ?: return null
        return habitMapper.mapFromEntity(entity)
    }

    override suspend fun addHabit(habit: Habit): String {
        try {
            val entity = habitMapper.mapToEntity(habit)
            habitDao.insertHabit(entity)
            Timber.d("Привычка добавлена с ID: ${habit.id}")
            return habit.id
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении привычки")
            throw e
        }
    }

    override suspend fun updateHabit(habit: Habit) {
        try {
            val entity = habitMapper.mapToEntity(habit)
            habitDao.updateHabit(entity)
            Timber.d("Привычка обновлена с ID: ${habit.id}")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении привычки")
            throw e
        }
    }
    // Добавьте метод для транзакционного добавления привычки с частотой
    suspend fun addHabitWithFrequency(habit: Habit, frequency: HabitFrequency): String {
        // Открываем транзакцию
        try {
            // Сохраняем привычку
            val habitId = habit.id
            habitDao.insertHabit(habitMapper.mapToEntity(habit))

            // Сохраняем частоту
            val frequencyEntity = habitFrequencyMapper.mapToEntity(frequency.copy(habitId = habitId))
            habitFrequencyDao.insertHabitFrequency(frequencyEntity)

            Timber.d("Транзакция успешно завершена. Сохранены привычка и частота: $habitId")
            return habitId
        } catch (e: Exception) {
            Timber.e(e, "Ошибка транзакции при сохранении привычки с частотой")
            throw e
        }
    }

    override suspend fun deleteHabit(habitId: String) {
        try {
            habitDao.deleteHabitById(habitId)
            Timber.d("Привычка удалена с ID: $habitId")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении привычки")
            throw e
        }
    }

    // Новые методы для работы с привычкой и частотой вместе
    override suspend fun getHabitWithFrequency(habitId: String): Pair<Habit, HabitFrequency?> {
        val habit = getHabitById(habitId)
            ?: throw NoSuchElementException("Привычка с ID $habitId не найдена")
        val frequency = getHabitFrequency(habitId)
        return Pair(habit, frequency)
    }

    override fun getAllHabitsWithFrequency(): Flow<List<Pair<Habit, HabitFrequency?>>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { entity ->
                val habit = habitMapper.mapFromEntity(entity)
                val frequency = habitFrequencyDao.getFrequencyForHabit(entity.habitId)?.let {
                    habitFrequencyMapper.mapFromEntity(it)
                }
                Pair(habit, frequency)
            }
        }
    }

    override fun getActiveHabitsWithFrequency(): Flow<List<Pair<Habit, HabitFrequency?>>> {
        return habitDao.getHabitsByStatus(HabitStatus.ACTIVE.value).map { entities ->
            entities.map { entity ->
                val habit = habitMapper.mapFromEntity(entity)
                val frequency = habitFrequencyDao.getFrequencyForHabit(entity.habitId)?.let {
                    habitFrequencyMapper.mapFromEntity(it)
                }
                Pair(habit, frequency)
            }
        }
    }

    // Методы для работы с частотой
    override suspend fun getHabitFrequency(habitId: String): HabitFrequency? {
        val entity = habitFrequencyDao.getFrequencyForHabit(habitId) ?: return null
        return habitFrequencyMapper.mapFromEntity(entity)
    }

    override suspend fun setHabitFrequency(habitId: String, frequency: HabitFrequency) {
        try {
            // Проверяем, что привычка существует
            val habitExists = habitDao.getHabitById(habitId) != null
            if (!habitExists) {
                Timber.e("Ошибка: привычка с ID $habitId не найдена")
                throw IllegalArgumentException("Привычка с ID $habitId не существует")
            }

            // Удаляем существующую частоту, если есть
            habitFrequencyDao.deleteFrequencyForHabit(habitId)

            // Сохраняем новую частоту
            val entity = habitFrequencyMapper.mapToEntity(frequency.copy(habitId = habitId))
            habitFrequencyDao.insertHabitFrequency(entity)
            Timber.d("Частота привычки установлена для ID: $habitId")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при установке частоты привычки")
            throw e
        }
    }

    override suspend fun updateHabitFrequency(frequency: HabitFrequency) {
        try {
            val entity = habitFrequencyMapper.mapToEntity(frequency)
            habitFrequencyDao.updateHabitFrequency(entity)
            Timber.d("Частота привычки обновлена для ID: ${frequency.habitId}")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении частоты привычки")
            throw e
        }
    }

    override suspend fun deleteHabitFrequency(habitId: String) {
        try {
            habitFrequencyDao.deleteFrequencyForHabit(habitId)
            Timber.d("Частота привычки удалена для ID: $habitId")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении частоты привычки")
            throw e
        }
    }

    override suspend fun getHabitProgressForToday(habitId: String): Float {
        // Получаем сегодняшнюю дату как начало дня
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val tracking = habitTrackingDao.getHabitTrackingForDate(
            habitId = habitId,
            date = today
        )

        if (tracking == null) {
            Timber.d("Нет записи трекинга для привычки $habitId на сегодня")
            return 0f
        }

        // Получаем привычку для определения типа
        val habit = getHabitById(habitId)
        if (habit == null) {
            Timber.e("Привычка $habitId не найдена")
            return 0f
        }

        // Возвращаем прогресс в зависимости от типа привычки
        val progress = when (habit.type) {
            HabitType.BINARY -> {
                // Для бинарного типа: 1f если выполнено, иначе 0f
                if (tracking.isCompleted) 1f else 0f
            }
            HabitType.QUANTITY -> {
                // Для количественного типа: используем значение
                tracking.value ?: 0f
            }
            HabitType.TIME -> {
                // Для типа с временем: используем длительность
                tracking.duration?.toFloat() ?: 0f
            }
        }

        Timber.d("Получен прогресс для привычки $habitId: $progress (тип: ${habit.type})")
        return progress
    }

    override suspend fun getAllHabitsProgressForToday(): Map<String, Float> {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Получаем все записи трекинга за сегодня
        val trackings = habitTrackingDao.getAllHabitTrackingsForDate(today)

        // Создаем пустую карту для результатов
        val progressMap = mutableMapOf<String, Float>()

        // Обрабатываем каждую запись трекинга согласно типу привычки
        for (tracking in trackings) {
            val habit = getHabitById(tracking.habitId) ?: continue

            // Определяем прогресс в зависимости от типа привычки
            val progress = when (habit.type) {
                HabitType.BINARY -> {
                    // Для бинарной привычки: 1f если выполнена, иначе 0f
                    if (tracking.isCompleted) 1f else 0f
                }
                HabitType.QUANTITY -> {
                    // Для количественной привычки: используем значение
                    tracking.value ?: 0f
                }
                HabitType.TIME -> {
                    // Для временной привычки: конвертируем duration в float
                    tracking.duration?.toFloat() ?: 0f
                }
            }

            // Сохраняем в карту результатов
            progressMap[tracking.habitId] = progress

            Timber.d("Привычка ${habit.title} (тип: ${habit.type}): прогресс $progress")
        }

        return progressMap
    }

    override suspend fun incrementHabitProgress(habitId: String, dateMillis: Long) {
        try {
            val habit = getHabitById(habitId) ?: return

            // Конвертируем dateMillis в начало этого дня (00:00:00)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dateMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            // Ищем запись за этот день
            val existingTracking = getHabitTrackingForDate(habitId, startOfDay)

            // Логирование для отладки
            Timber.d("Инкремент привычки ${habit.title} (${habit.id}), тип: ${habit.type}")
            Timber.d("Дата: $startOfDay, существующая запись: ${existingTracking != null}")

            if (existingTracking != null) {
                when (habit.type) {
                    HabitType.BINARY -> {
                        // Для бинарного типа просто отмечаем как выполненное
                        val updatedTracking = existingTracking.copy(isCompleted = true, value = 1f)
                        updateHabitTracking(updatedTracking)
                        Timber.d("Обновлена бинарная привычка ${habit.title}, отмечена как выполненная")
                    }
                    HabitType.QUANTITY -> {
                        // Для количественных привычек увеличиваем значение
                        val currentValue = existingTracking.value ?: 0f
                        val newValue = currentValue + 1
                        val targetValue = habit.targetValue ?: 1f
                        val updatedTracking = existingTracking.copy(
                            value = newValue,
                            isCompleted = newValue >= targetValue
                        )
                        updateHabitTracking(updatedTracking)
                        Timber.d("Обновлена количественная привычка ${habit.title}, прогресс: $newValue/$targetValue")
                    }
                    HabitType.TIME -> {
                        // Для временных привычек увеличиваем duration
                        val currentDuration = existingTracking.duration ?: 0
                        val newDuration = currentDuration + 1
                        val targetDuration = habit.targetValue?.toInt() ?: 1
                        val updatedTracking = existingTracking.copy(
                            duration = newDuration,
                            isCompleted = newDuration >= targetDuration
                        )
                        updateHabitTracking(updatedTracking)
                        Timber.d("Обновлена временная привычка ${habit.title}, длительность: $newDuration/$targetDuration мин")
                    }
                }
            } else {
                // Создаем новую запись с начальными значениями
                val newTracking = when (habit.type) {
                    HabitType.BINARY -> {
                        HabitTracking(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = startOfDay,  // Используем начало дня, а не текущее время
                            isCompleted = true,
                            value = 1f,
                            duration = null
                        )
                    }
                    HabitType.QUANTITY -> {
                        HabitTracking(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = startOfDay,  // Используем начало дня, а не текущее время
                            isCompleted = 1f >= (habit.targetValue ?: 1f),
                            value = 1f,
                            duration = null
                        )
                    }
                    HabitType.TIME -> {
                        HabitTracking(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = startOfDay,  // Используем начало дня, а не текущее время
                            isCompleted = 1 >= (habit.targetValue?.toInt() ?: 1),
                            value = null,
                            duration = 1
                        )
                    }
                }
                trackHabit(newTracking)
                Timber.d("Создана новая запись для привычки ${habit.title}, тип: ${habit.type}, на дату: $startOfDay")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении прогресса привычки $habitId")
            throw e
        }
    }

    override suspend fun decrementHabitProgress(habitId: String, dateMillis: Long) {
        // Получаем привычку
        val habit = getHabitById(habitId) ?: return

        // Проверяем, есть ли запись за выбранный день
        val existingTracking = getHabitTrackingForDate(habitId, dateMillis)

        if (existingTracking != null) {
            when (habit.type) {
                HabitType.QUANTITY -> {
                    val currentValue = existingTracking.value ?: 0f
                    if (currentValue > 0) {
                        val newValue = currentValue - 1
                        val targetValue = habit.targetValue ?: 1f
                        val isCompleted = newValue >= targetValue

                        val updatedTracking = existingTracking.copy(
                            value = newValue,
                            isCompleted = isCompleted
                        )
                        updateHabitTracking(updatedTracking)
                    }
                }
                HabitType.TIME -> {
                    val currentDuration = existingTracking.duration ?: 0
                    if (currentDuration > 0) {
                        val newDuration = currentDuration - 1
                        val targetDuration = habit.targetValue?.toInt() ?: 1
                        val isCompleted = newDuration >= targetDuration

                        val updatedTracking = existingTracking.copy(
                            duration = newDuration,
                            isCompleted = isCompleted
                        )
                        updateHabitTracking(updatedTracking)
                    }
                }
                else -> {} // Для бинарных привычек уменьшение не имеет смысла
            }
        }
        // Если записи нет, то нечего уменьшать
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


    override fun getHabitTrackings(habitId: String): Flow<List<HabitTracking>> {
        return habitTrackingDao.getTrackingsForHabit(habitId).map { entities ->
            entities.map { habitTrackingMapper.mapFromEntity(it) }
        }
    }

    override fun getHabitTrackingsForRange(
        habitId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<HabitTracking>> {
        return habitTrackingDao.getTrackingsForHabitInRange(habitId, startDate, endDate)
            .map { entities ->
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

        val completedCount =
            habitTrackingDao.countCompletedTrackingsInRange(habitId, startDate, endDate)
        return completedCount.toFloat() / days
    }

    override suspend fun calculateHabitProgress(habitId: String, date: Long): Float {
        val habit = getHabitById(habitId) ?: return 0f
        val tracking = getHabitTrackingForDate(habitId, date)

        return when (habit.type) {
            HabitType.BINARY -> {
                // Для бинарных привычек - либо 0, либо 1
                if (tracking?.isCompleted == true) 1f else 0f
            }
            HabitType.QUANTITY -> {
                // Для количественных привычек - показываем прогресс на основе значения,
                // даже если привычка не отмечена как выполненная
                val targetValue = habit.targetValue ?: 1f
                val currentValue = tracking?.value ?: 0f
                (currentValue / targetValue).coerceIn(0f, 1f)
            }
            HabitType.TIME -> {
                // Для привычек с временем - показываем прогресс на основе длительности,
                // даже если привычка не отмечена как выполненная
                val targetDuration = habit.targetValue ?: 1f
                val currentDuration = tracking?.duration?.toFloat() ?: 0f
                (currentDuration / targetDuration).coerceIn(0f, 1f)
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