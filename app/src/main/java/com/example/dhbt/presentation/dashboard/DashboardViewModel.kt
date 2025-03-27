package com.example.dhbt.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TaskRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // State flow для состояния экрана
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Временные константы для фильтрации
    private val today: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val tomorrow: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    init {
        loadData()
        logCurrentDay()
    }

    // Логирование текущего дня для отладки
    private fun logCurrentDay() {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val formattedDate = getTodayFormatted()

        Log.d(TAG, "===== Сегодняшний день =====")
        Log.d(TAG, "Дата: $formattedDate")
        Log.d(TAG, "День недели: $dayOfWeek (значение: ${today.dayOfWeek.value})")
        Log.d(TAG, "Timeststamp начала дня: $today")
        Log.d(TAG, "Timeststamp конца дня: $tomorrow")
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isRefreshing = true) }

            try {
                // Получаем данные о прогрессе привычек на сегодня
                val todayProgresses = habitRepository.getAllHabitsProgressForToday()

                // Комбинируем потоки данных из репозиториев
                combine(
                    userPreferencesRepository.getUserData(),
                    taskRepository.getAllTasks(),
                    habitRepository.getAllHabits()
                ) { userData, tasks, habits ->
                    // Логирование и фильтрация как было раньше

                    // Фильтруем задачи и привычки на сегодня
                    val todayTasks = filterTasksForToday(tasks)
                    val todayHabits = filterHabitsForToday(habits)

                    // Логирование привычек с АКТУАЛЬНЫМ прогрессом с сегодня
                    Log.d(TAG, "===== Привычки на сегодня с актуальным прогрессом (${todayHabits.size}) =====")
                    todayHabits.forEach { habit ->
                        val actualProgress = todayProgresses[habit.id] ?: 0f
                        Log.d(TAG, "Привычка: ${habit.title}, " +
                                "ID: ${habit.id}, " +
                                "Тип: ${habit.type.value}, " +
                                "Статус: ${habit.status.value}, " +
                                "Текущий прогресс в БД: ${habit.currentStreak}/${habit.targetValue ?: 1}, " +
                                "Сегодняшний прогресс: $actualProgress/${habit.targetValue ?: 1}")
                    }

                    // Привязываем данные о сегодняшних прогрессах к привычкам для UI
// Привязываем данные о сегодняшних прогрессах к привычкам для UI
                    val todayHabitsWithProgress = todayHabits.map { habit ->
                        // Создаем копию привычки с актуальным прогрессом на сегодня
                        val todayProgress = todayProgresses[habit.id] ?: 0f

                        // Округляем или обрезаем до целого числа в зависимости от типа привычки
                        val progressAsInt = when (habit.type.value) {
                            0 -> if (todayProgress >= 1f) 1 else 0 // BINARY: 0 или 1
                            1 -> todayProgress.toInt() // QUANTITY: количество целых единиц
                            2 -> todayProgress.toInt() // TIME: количество целых минут
                            else -> 0
                        }

                        // Логируем для отладки преобразование типов
                        Log.d(TAG, "Привычка ${habit.title}: преобразование прогресса $todayProgress (Float) -> $progressAsInt (Int)")

                        habit.copy(currentStreak = progressAsInt)
                    }
                    // Считаем статистику с актуальным прогрессом
                    val completedTasks = todayTasks.count { it.status == TaskStatus.COMPLETED }
                    val totalTasks = todayTasks.size

                    val completedHabits = countCompletedHabitsWithTodayProgress(todayHabitsWithProgress, todayProgresses)
                    val totalHabits = todayHabitsWithProgress.size

                    // Обновляем состояние с актуальными данными
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            userData = userData,
                            todayTasks = todayTasks,
                            todayHabits = todayHabitsWithProgress,
                            completedTasks = completedTasks,
                            totalTasks = totalTasks,
                            completedHabits = completedHabits,
                            totalHabits = totalHabits,
                            error = null
                        )
                    }
                }.catch { error ->
                    // Обработка ошибок как было
                }.collect()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading today's habit progress", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Ошибка при загрузке данных о прогрессе привычек"
                    )
                }
            }
        }
    }

    // Подсчет выполненных привычек на сегодня с актуальным прогрессом
    private fun countCompletedHabitsWithTodayProgress(
        habits: List<Habit>,
        todayProgresses: Map<String, Float>
    ): Int {
        return habits.count { habit ->
            val todayProgress = todayProgresses[habit.id] ?: 0f

            when (habit.type.value) {
                0 -> todayProgress >= 1f  // BINARY: выполнено, если прогресс >= 1
                1, 2 -> { // QUANTITY или TIME
                    val target = habit.targetValue ?: 1f
                    todayProgress >= target
                }
                else -> false
            }
        }
    }

    // Обновление обработчика инкремента прогресса
    fun onHabitProgressIncrement(habitId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Увеличение прогресса привычки $habitId")
            try {
                val today = Calendar.getInstance().timeInMillis
                habitRepository.incrementHabitProgress(habitId, today)

                // После инкремента получаем актуальный прогресс на сегодня
                val todayProgress = habitRepository.getHabitProgressForToday(habitId)
                val habit = habitRepository.getHabitById(habitId)

                Log.d(TAG, "Привычка после инкремента: $habitId, " +
                        "текущий прогресс в БД: ${habit?.currentStreak}/${habit?.targetValue}, " +
                        "прогресс на сегодня: $todayProgress/${habit?.targetValue}")

                // Обновляем UI после изменения прогресса
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении прогресса привычки", e)
                _state.update {
                    it.copy(error = "Ошибка при обновлении прогресса привычки: ${e.message}")
                }
            }
        }
    }
    // Обработчики событий UI
// Метод для переключения статуса задачи
    fun onTaskCheckedChange(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            // Устанавливаем указанный статус (isCompleted уже содержит целевой статус)
            val newStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE
            Log.d(TAG, "Изменение статуса задачи $taskId на $newStatus")
            taskRepository.updateTaskStatus(taskId, newStatus)
        }
    }

    // Новый метод для простого переключения текущего статуса без указания целевого значения
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                // Получаем текущую задачу
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    // Определяем текущий статус и переключаем на противоположный
                    val currentStatus = task.status
                    val newStatus = if (currentStatus == TaskStatus.COMPLETED)
                        TaskStatus.ACTIVE
                    else
                        TaskStatus.COMPLETED

                    Log.d(TAG, "Переключение статуса задачи $taskId с ${currentStatus} на ${newStatus}")
                    taskRepository.updateTaskStatus(taskId, newStatus)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при переключении статуса задачи", e)
                _state.update {
                    it.copy(error = "Ошибка при обновлении статуса задачи: ${e.message}")
                }
            }
        }
    }

    fun onDeleteTask(taskId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Удаление задачи $taskId")
            taskRepository.deleteTask(taskId)
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        loadData()
    }

    // Фильтрация задач на сегодня (включая выполненные)
    private fun filterTasksForToday(tasks: List<Task>): List<Task> {
        return tasks.filter { task ->
            // Фильтруем задачи с дедлайном на сегодня (включая выполненные)
            val dueDate = task.dueDate ?: return@filter false

            // Задача на сегодня или просрочена
            val isDueToday = dueDate in today until tomorrow
            val isOverdue = dueDate < today && task.status == TaskStatus.ACTIVE

            isDueToday || isOverdue
        }.sortedWith(compareBy(
            // Сначала активные, потом выполненные
            { it.status != TaskStatus.ACTIVE },
            // Потом по времени
            { it.dueDate }
        ))
    }

    // Фильтрация привычек на сегодня
    // Фильтрация привычек на сегодня
    private suspend fun filterHabitsForToday(habits: List<Habit>): List<Habit> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1 (понедельник) - 7 (воскресенье)

        Log.d(TAG, "Фильтрация привычек по дню недели: $dayOfWeek")

        return habits.filter { habit ->
            // Фильтр только активных привычек (status.value == 0 означает активную привычку)
            if (habit.status.value != 0) {
                Log.d(TAG, "Привычка ${habit.title} пропускается: не активна (статус ${habit.status.value})")
                return@filter false
            }

            // Проверка по расписанию - получаем частоту из репозитория
            val frequency = habitRepository.getHabitFrequency(habit.id) ?: return@filter true

            val shouldBeShown = when (frequency.type.value) {
                0 -> true // Ежедневная привычка
                1 -> { // Привычка по конкретным дням недели
                    val daysOfWeek = frequency.daysOfWeek
                    val result = daysOfWeek?.contains(dayOfWeek) ?: false
                    Log.d(TAG, "Привычка ${habit.title} по дням недели $daysOfWeek, день $dayOfWeek: $result")
                    result
                }
                2, 3 -> { // X раз в неделю/месяц - показываем каждый день для простоты
                    true
                }
                else -> {
                    Log.d(TAG, "Привычка ${habit.title} с неизвестным типом частоты ${frequency.type.value}")
                    false
                }
            }

            Log.d(TAG, "Привычка ${habit.title} должна отображаться: $shouldBeShown")
            shouldBeShown
        }
    }

    // Форматирование текущей даты для отображения
    fun getTodayFormatted(): String {
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
        return LocalDate.now().format(dateFormatter)
    }
}

// Состояние UI для экрана Dashboard
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val todayTasks: List<Task> = emptyList(),
    val todayHabits: List<Habit> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val userData: UserData? = null
)