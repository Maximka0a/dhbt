package com.example.dhbt.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TaskRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import com.example.dhbt.presentation.util.ErrorManager
import com.example.dhbt.presentation.util.logDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject
import kotlin.collections.set

private const val TAG = "DashboardViewModel"
private const val UPDATE_THROTTLE_MS = 300L // Задержка для группировки обновлений

/**
 * ViewModel для экрана дашборда с оптимизированным управлением обновлениями UI
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Состояние и события
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    // Кеш данных для предотвращения повторных загрузок
    private var habitProgressCache = mutableMapOf<String, Float>()
    private var pendingUpdates = mutableSetOf<String>()
    private var updateDebounceJob: Job? = null

    // Временные константы для фильтрации
    private val todayMillis: Long by lazy {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val tomorrowMillis: Long by lazy {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    init {
        loadInitialData()
    }

    /**
     * Загружает начальные данные с минимальным воздействием на UI
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                logDebug(TAG, "Загрузка начальных данных")

                // Параллельная загрузка критически важных данных
                val progressDeferred = async { habitRepository.getAllHabitsProgressForToday() }

                // Выполняем остальные загрузки
                loadDashboardData(progressDeferred.await())
                logCurrentDay()
            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.DATA_LOAD_ERROR)
            }
        }
    }

    /**
     * Логирует информацию о текущем дне (для отладки)
     */
    private fun logCurrentDay() {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val formattedDate = getTodayFormatted()

        logDebug(TAG, "===== Текущий день =====")
        logDebug(TAG, "Дата: $formattedDate")
        logDebug(TAG, "День недели: $dayOfWeek (значение: ${today.dayOfWeek.value})")
    }

    /**
     * Основная загрузка данных для дашборда
     */
    private fun loadDashboardData(habitsProgress: Map<String, Float> = emptyMap()) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, isRefreshing = true) }

                // Кешируем данные о прогрессе привычек
                habitProgressCache = habitsProgress.toMutableMap()

                // Используем Flow.combine для одновременного получения всех данных
                combine(
                    userPreferencesRepository.getUserData(),
                    taskRepository.getAllTasks(),
                    habitRepository.getAllHabits()
                ) { userData, tasks, habits ->
                    val todayTasks = filterTasksForToday(tasks)
                    val todayHabits = filterHabitsForToday(habits)

                    val todayHabitsWithProgress = applyProgressToHabits(todayHabits)

                    // Подсчет статистики
                    val completedTasks = todayTasks.count { it.status == TaskStatus.COMPLETED }
                    val completedHabits = countCompletedHabits(todayHabitsWithProgress)
                    val habitsWithProgress = countHabitsWithAnyProgress(todayHabits)

                    // Обновляем состояние
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            userData = userData,
                            todayTasks = todayTasks,
                            todayHabits = todayHabitsWithProgress,
                            completedTasks = completedTasks,
                            totalTasks = todayTasks.size,
                            completedHabits = completedHabits,
                            totalHabits = todayHabitsWithProgress.size,
                            habitsWithProgress = habitsWithProgress,
                            error = null
                        )
                    }
                }.catch { error ->
                    handleError(error, ErrorManager.ErrorType.DATA_LOAD_ERROR)
                }.collect()
            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.DATA_LOAD_ERROR)
            }
        }
    }

    /**
     * Применяет сохраненный прогресс к привычкам
     */
    private fun applyProgressToHabits(habits: List<Habit>): List<Habit> {
        return habits.map { habit ->
            val progress = habitProgressCache[habit.id] ?: 0f

            // Конвертируем прогресс в целое число в зависимости от типа
            val progressAsInt = when (habit.type.value) {
                0 -> if (progress >= 1f) 1 else 0 // BINARY
                1, 2 -> progress.toInt()         // QUANTITY, TIME
                else -> 0
            }

            // Возвращаем копию привычки с актуальным прогрессом
            habit.copy(currentStreak = progressAsInt)
        }
    }

    /**
     * Увеличивает прогресс привычки с дебаунсингом обновлений UI
     */
    fun onHabitProgressIncrement(habitId: String) {
        viewModelScope.launch {
            try {
                // Получаем исходный прогресс
                val initialProgress = habitRepository.getHabitProgressForToday(habitId)
                logDebug(TAG, "Инкремент прогресса привычки $habitId (текущий: $initialProgress)")

                // Увеличиваем прогресс
                habitRepository.incrementHabitProgress(habitId, Calendar.getInstance().timeInMillis)

                // Небольшая задержка для завершения транзакции БД
                delay(50)

                // Получаем обновленный прогресс
                val updatedProgress = habitRepository.getHabitProgressForToday(habitId)
                habitProgressCache[habitId] = updatedProgress

                // Добавляем ID в ожидающие обновления и запускаем дебаунсинг
                synchronized(pendingUpdates) {
                    pendingUpdates.add(habitId)
                    scheduleUpdateUI()
                }

            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.HABIT_PROGRESS_ERROR)
            }
        }
    }

    /**
     * Планирует обновление UI с дебаунсингом
     */
    private fun scheduleUpdateUI() {
        // Отменяем предыдущую запланированную работу, если она есть
        updateDebounceJob?.cancel()

        // Создаем новую работу для обновления UI через заданное время
        updateDebounceJob = viewModelScope.launch {
            delay(UPDATE_THROTTLE_MS)

            val habitsToUpdate: Set<String>
            synchronized(pendingUpdates) {
                habitsToUpdate = pendingUpdates.toSet()
                pendingUpdates.clear()
            }

            if (habitsToUpdate.isNotEmpty()) {
                updateHabitsInState(habitsToUpdate)
            }
        }
    }

    /**
     * Обновляет привычки в состоянии без полной перезагрузки данных
     */
    private fun updateHabitsInState(habitIds: Set<String>) {
        _state.update { currentState ->
            val updatedHabits = currentState.todayHabits.map { habit ->
                if (habitIds.contains(habit.id)) {
                    // Получаем актуальный прогресс из кеша
                    val progress = habitProgressCache[habit.id] ?: 0f

                    // Конвертируем прогресс в целое число
                    val progressAsInt = when (habit.type.value) {
                        0 -> if (progress >= 1f) 1 else 0 // BINARY
                        1, 2 -> progress.toInt()         // QUANTITY, TIME
                        else -> 0
                    }

                    // Создаем копию привычки с новым прогрессом
                    habit.copy(currentStreak = progressAsInt)
                } else {
                    habit
                }
            }

            // Пересчитываем статистику
            val completedHabits = countCompletedHabits(updatedHabits)
            val habitsWithProgress = countHabitsWithAnyProgress(updatedHabits)

            // Возвращаем обновленное состояние
            currentState.copy(
                todayHabits = updatedHabits,
                completedHabits = completedHabits,
                habitsWithProgress = habitsWithProgress
            )
        }
    }

    /**
     * Обрабатывает изменение состояния задачи
     */
    fun onTaskCheckedChange(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE
            try {
                taskRepository.updateTaskStatus(taskId, newStatus)

                // Обновляем только эту задачу в UI
                updateTaskInState(taskId, newStatus)

            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.TASK_UPDATE_ERROR)
            }
        }
    }

    /**
     * Переключает статус задачи
     */
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId) ?: return@launch

                val newStatus = if (task.status == TaskStatus.COMPLETED)
                    TaskStatus.ACTIVE
                else
                    TaskStatus.COMPLETED

                taskRepository.updateTaskStatus(taskId, newStatus)

                // Обновляем только эту задачу в UI
                updateTaskInState(taskId, newStatus)

            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.TASK_UPDATE_ERROR)
            }
        }
    }

    /**
     * Обновляет задачу в состоянии без полной перезагрузки
     */
    private fun updateTaskInState(taskId: String, newStatus: TaskStatus) {
        _state.update { currentState ->
            val updatedTasks = currentState.todayTasks.map { task ->
                if (task.id == taskId) task.copy(status = newStatus) else task
            }

            val completedTasks = updatedTasks.count { it.status == TaskStatus.COMPLETED }

            currentState.copy(
                todayTasks = updatedTasks,
                completedTasks = completedTasks
            )
        }
    }

    /**
     * Удаляет задачу
     */
    fun onDeleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)

                // Удаляем задачу из состояния
                _state.update { currentState ->
                    val updatedTasks = currentState.todayTasks.filter { it.id != taskId }
                    val completedTasks = updatedTasks.count { it.status == TaskStatus.COMPLETED }

                    currentState.copy(
                        todayTasks = updatedTasks,
                        completedTasks = completedTasks,
                        totalTasks = updatedTasks.size
                    )
                }
            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.TASK_DELETE_ERROR)
            }
        }
    }

    /**
     * Обработка ошибок с локализацией
     */
    private fun handleError(e: Throwable, errorType: ErrorManager.ErrorType) {
        logDebug(TAG, "Ошибка: ${e.message}", e)

        val uiError = ErrorManager.fromException(e, errorType)

        // Обновляем состояние - только необходимые поля
        _state.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = uiError
            )
        }

        // Уведомляем UI о необходимости показать ошибку
        viewModelScope.launch {
            _uiEvent.send(UiEvent.ShowError(uiError))
        }
    }

    /**
     * Скрывает сообщение об ошибке
     */
    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Обновляет данные с умной стратегией
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                // Получаем свежие данные о прогрессе привычек
                val freshProgressData = habitRepository.getAllHabitsProgressForToday()

                // Обновляем только если есть изменения в прогрессе
                if (shouldRefreshHabitData(freshProgressData)) {
                    habitProgressCache = freshProgressData.toMutableMap()
                    loadDashboardData(freshProgressData)
                } else {
                    // Если прогресс не изменился, просто завершаем индикатор обновления
                    _state.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                handleError(e, ErrorManager.ErrorType.DATA_LOAD_ERROR)
            }
        }
    }

    /**
     * Проверяет, нужно ли обновлять данные привычек
     */
    private fun shouldRefreshHabitData(freshData: Map<String, Float>): Boolean {
        // Проверяем различия между кешем и новыми данными
        if (freshData.size != habitProgressCache.size) return true

        return freshData.any { (habitId, progress) ->
            val cachedProgress = habitProgressCache[habitId] ?: -1f
            progress != cachedProgress
        }
    }

    /**
     * Фильтрует задачи на сегодняшний день
     */
    private fun filterTasksForToday(tasks: List<Task>): List<Task> {
        return tasks.filter { task ->
            val dueDate = task.dueDate ?: return@filter false

            val isDueToday = dueDate in todayMillis until tomorrowMillis
            val isOverdue = dueDate < todayMillis && task.status == TaskStatus.ACTIVE

            isDueToday || isOverdue
        }.sortedWith(compareBy(
            { it.status != TaskStatus.ACTIVE },
            { it.dueDate }
        ))
    }

    /**
     * Фильтрует привычки на сегодняшний день
     */
    private suspend fun filterHabitsForToday(habits: List<Habit>): List<Habit> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value

        return habits.filter { habit ->
            // Проверяем активность привычки
            if (habit.status.value != 0) return@filter false

            // Получаем данные о расписании привычки
            val frequency = habitRepository.getHabitFrequency(habit.id) ?: return@filter true

            when (frequency.type.value) {
                0 -> true // Ежедневно
                1 -> { // По дням недели
                    val daysOfWeek = frequency.daysOfWeek
                    daysOfWeek?.contains(dayOfWeek) ?: false
                }
                2, 3 -> true // Еженедельно/ежемесячно - показываем каждый день
                else -> false
            }
        }
    }

    /**
     * Подсчет привычек с любым прогрессом
     */
    private fun countHabitsWithAnyProgress(habits: List<Habit>): Int {
        return habits.count { habit ->
            val progress = habitProgressCache[habit.id] ?: 0f
            progress > 0f
        }
    }

    /**
     * Подсчет полностью выполненных привычек
     */
    private fun countCompletedHabits(habits: List<Habit>): Int {
        return habits.count { habit ->
            val progress = habitProgressCache[habit.id] ?: 0f

            when (habit.type.value) {
                0 -> progress >= 1f // BINARY
                1, 2 -> { // QUANTITY или TIME
                    val target = habit.targetValue ?: 1f
                    progress >= target
                }
                else -> false
            }
        }
    }

    /**
     * Форматирует текущую дату для отображения
     */
    fun getTodayFormatted(): String {
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
        return LocalDate.now().format(dateFormatter)
    }

    /**
     * События UI для одноразовых действий
     */
    sealed class UiEvent {
        data class ShowError(val error: ErrorManager.UiError) : UiEvent()
        data class ShowMessage(val message: String) : UiEvent()
    }
}

/**
 * Состояние дашборда
 */
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ErrorManager.UiError? = null,
    val todayTasks: List<Task> = emptyList(),
    val todayHabits: List<Habit> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val userData: UserData? = null,
    val habitsWithProgress: Int = 0
)