package com.example.dhbt.presentation.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val pomodoroRepository: PomodoroRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    // Настройки Pomodoro загружаем первыми
    private val _preferences = MutableStateFlow(PomodoroPreferences())
    val preferences = _preferences.asStateFlow()

    // Затем инициализируем состояние с начальными значениями
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState = _uiState.asStateFlow()

    // Текущая сессия
    private var currentSessionId: String? = null
    private var timerJob: Job? = null

    // Список последних задач для быстрого доступа
    private val _recentTasks = MutableStateFlow<List<Task>>(emptyList())
    val recentTasks = _recentTasks.asStateFlow()

    // Статистика
    private val _todayStats = MutableStateFlow(PomodoroStats())
    val todayStats = _todayStats.asStateFlow()

    // Статистика по выбранной задаче
    private val _taskStats = MutableStateFlow(TaskPomodoroStats())
    val taskStats = _taskStats.asStateFlow()

    // Добавляем состояние загрузки для задач
    private val _tasksLoading = MutableStateFlow(false)
    val tasksLoading = _tasksLoading.asStateFlow()

    // Состояние загрузки статистики
    private val _statsLoading = MutableStateFlow(false)
    val statsLoading = _statsLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Загружаем настройки и инициализируем правильное время для таймера
            pomodoroRepository.getPomodoroPreferences().collect { prefs ->
                _preferences.value = prefs

                // Обновляем время для таймера на основе настроек
                updateTimerDurationFromPreferences(prefs)
            }
        }

        // Загружаем недавние задачи отдельно
        loadRecentTasks()

        // Загружаем статистику за сегодня сразу
        loadTodayStats()
    }

    // Метод для загрузки задачи по ID (вызывается при открытии экрана с параметром taskId)
    fun loadTaskById(taskId: String) {
        if (taskId.isEmpty()) return

        viewModelScope.launch {
            _tasksLoading.value = true
            try {
                // Пытаемся получить задачу по ID
                val task = taskRepository.getTaskById(taskId)

                // Если задача существует, выбираем её и загружаем статистику
                task?.let {
                    // Устанавливаем тип таймера в Pomodoro, если он ещё не установлен
                    if (_uiState.value.timerType != TimerType.POMODORO) {
                        onTimerTypeSelected(TimerType.POMODORO)
                    }

                    // Выбираем задачу (это запустит обновление статистики)
                    onTaskSelected(it)
                }
            } catch (e: Exception) {
                // Обработка ошибок получения задачи
            } finally {
                _tasksLoading.value = false
            }
        }
    }

    // Метод для обновления времени для таймера на основе настроек
    private fun updateTimerDurationFromPreferences(prefs: PomodoroPreferences) {
        _uiState.update { currentState ->
            if (currentState.timerType == TimerType.POMODORO && currentState.timerState != TimerState.RUNNING) {
                val newTime = when (currentState.pomodoroSessionType) {
                    PomodoroSessionType.WORK -> TimeUnit.MINUTES.toMillis(prefs.workDuration.toLong())
                    PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(prefs.shortBreakDuration.toLong())
                    PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(prefs.longBreakDuration.toLong())
                }
                currentState.copy(totalTime = newTime, remainingTime = newTime)
            } else {
                currentState
            }
        }
    }

    fun onTimerTypeSelected(timerType: TimerType) {
        if (_uiState.value.timerState == TimerState.RUNNING) {
            // Если таймер запущен, останавливаем его перед сменой типа
            stopTimer()
        }

        // При переключении на Pomodoro сбрасываем время
        val totalTime = when (timerType) {
            TimerType.POMODORO -> {
                when (_uiState.value.pomodoroSessionType) {
                    PomodoroSessionType.WORK -> TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                    PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
                    PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
                }
            }
            TimerType.STOPWATCH -> 0L
        }

        _uiState.update {
            it.copy(
                timerType = timerType,
                timerState = TimerState.IDLE,
                remainingTime = if (timerType == TimerType.POMODORO) totalTime else 0L,
                totalTime = totalTime,
                elapsedTime = 0L
            )
        }

        // Если есть выбранная задача, обновляем таймер для неё
        if (timerType == TimerType.POMODORO && _uiState.value.selectedTask != null) {
            setupTimerForTask(_uiState.value.selectedTask!!, _uiState.value.completedPomodoros)
        }
    }

    fun onPomodoroSessionTypeSelected(sessionType: PomodoroSessionType) {
        if (_uiState.value.timerState == TimerState.RUNNING || _uiState.value.timerState == TimerState.PAUSED) {
            // Если таймер запущен или на паузе, останавливаем его перед сменой типа
            stopTimer()
        }

        val totalTime = when (sessionType) {
            PomodoroSessionType.WORK -> {
                // Если выбрана задача и это рабочая сессия, используем время из задачи
                if (_uiState.value.selectedTask != null) {
                    calculateSessionDurationForTask(_uiState.value.selectedTask!!)
                } else {
                    TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                }
            }
            PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
            PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
        }

        _uiState.update {
            it.copy(
                pomodoroSessionType = sessionType,
                timerState = TimerState.IDLE,
                remainingTime = totalTime,
                totalTime = totalTime
            )
        }
    }

    // Обновленный метод выбора задачи
    fun onTaskSelected(task: Task?) {
        viewModelScope.launch {
            try {
                _tasksLoading.value = true

                if (task == null) {
                    // Очищаем предыдущую статистику по задаче
                    _taskStats.value = TaskPomodoroStats()

                    // Сбрасываем состояние, связанное с задачей
                    _uiState.update { it.copy(
                        selectedTask = null,
                        totalTaskSessions = 0,
                        completedPomodoros = 0
                    )}

                    // Возвращаем время таймера из настроек
                    val defaultTime = when (_uiState.value.pomodoroSessionType) {
                        PomodoroSessionType.WORK -> TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                        PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
                        PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
                    }

                    _uiState.update { it.copy(
                        remainingTime = defaultTime,
                        totalTime = defaultTime
                    )}

                    _tasksLoading.value = false
                    return@launch
                }

                // Задача выбрана - обновляем состояние
                val estimatedSessions = task.estimatedPomodoroSessions ?: 0
                _uiState.update { it.copy(
                    selectedTask = task,
                    totalTaskSessions = estimatedSessions
                )}

                // Загружаем все данные по задаче
                loadTaskStats(task.id)

                // Переключаемся на режим Pomodoro, если это еще не сделано
                if (_uiState.value.timerType != TimerType.POMODORO) {
                    onTimerTypeSelected(TimerType.POMODORO)
                }

                // Настраиваем таймер для выбранной задачи
                if (_uiState.value.timerState != TimerState.RUNNING &&
                    _uiState.value.timerState != TimerState.PAUSED) {
                    setupTimerForTask(task, 0)
                }

                _tasksLoading.value = false
            } catch (e: Exception) {
                _tasksLoading.value = false
                // Обработка ошибок
            }
        }
    }

    // Новый метод для настройки таймера под задачу
    private fun setupTimerForTask(task: Task, completedSessions: Int) {
        if (_uiState.value.timerState == TimerState.RUNNING ||
            _uiState.value.timerState == TimerState.PAUSED ||
            _uiState.value.pomodoroSessionType != PomodoroSessionType.WORK) {
            return
        }

        // Вычисляем продолжительность на основе параметров задачи
        val sessionDuration = calculateSessionDurationForTask(task)

        // Обновляем таймер
        _uiState.update { it.copy(
            remainingTime = sessionDuration,
            totalTime = sessionDuration,
            completedPomodoros = completedSessions
        )}
    }

    // Помощник для вычисления продолжительности сессии на основе задачи
    private fun calculateSessionDurationForTask(task: Task): Long {
        val estimatedSessions = task.estimatedPomodoroSessions ?: 0
        val estimatedMinutes = task.duration ?: 0

        return when {
            // Если указано и время, и количество сессий, вычисляем время на одну сессию
            estimatedMinutes > 0 && estimatedSessions > 0 -> {
                val minutesPerSession = (estimatedMinutes / estimatedSessions).coerceAtLeast(1)
                TimeUnit.MINUTES.toMillis(minutesPerSession.toLong())
            }
            // Если указано только время, используем его целиком
            estimatedMinutes > 0 -> {
                TimeUnit.MINUTES.toMillis(estimatedMinutes.toLong())
            }
            // Иначе используем значение по умолчанию
            else -> {
                TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
            }
        }
    }

    // Загрузка статистики по задаче
    private fun loadTaskStats(taskId: String) {
        viewModelScope.launch {
            try {
                // Загружаем статистику по времени
                val focusTimeForTask = pomodoroRepository.getTotalFocusTimeForTask(taskId)

                // Получаем информацию о задаче
                val task = taskRepository.getTaskById(taskId) ?: return@launch
                val estimatedSessions = task.estimatedPomodoroSessions ?: 0
                val estimatedMinutes = task.duration ?: 0

                // Загружаем завершенные сессии
                pomodoroRepository.getSessionsForTask(taskId).collect { sessions ->
                    val completedSessions = sessions.count { it.isCompleted && it.type == PomodoroSessionType.WORK }

                    // Обновляем статистику по задаче
                    _taskStats.value = TaskPomodoroStats(
                        focusTimeMinutes = focusTimeForTask,
                        estimatedTotalMinutes = estimatedMinutes,
                        completedSessions = completedSessions,
                        estimatedTotalSessions = estimatedSessions
                    )

                    // Обновляем счетчик выполненных сессий
                    _uiState.update { it.copy(completedPomodoros = completedSessions) }
                }
            } catch (e: Exception) {
                // Обработка ошибок
                _taskStats.value = TaskPomodoroStats()
            }
        }
    }

    fun startTimer() {
        if (_uiState.value.timerState == TimerState.RUNNING) return

        when (_uiState.value.timerType) {
            TimerType.POMODORO -> startPomodoroTimer()
            TimerType.STOPWATCH -> startStopwatch()
        }
    }

    private fun startPomodoroTimer() {
        // Создаем новую сессию Pomodoro
        viewModelScope.launch {
            // Убеждаемся, что таймер имеет правильное начальное значение
            val currentSessionType = _uiState.value.pomodoroSessionType

            // Если время не установлено, инициализируем его
            if (_uiState.value.remainingTime <= 0) {
                val totalTime = when (currentSessionType) {
                    PomodoroSessionType.WORK -> {
                        if (_uiState.value.selectedTask != null) {
                            calculateSessionDurationForTask(_uiState.value.selectedTask!!)
                        } else {
                            TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                        }
                    }
                    PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
                    PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
                }
                _uiState.update { it.copy(remainingTime = totalTime, totalTime = totalTime) }
            }

            // Создаем сессию только если у нас есть время для работы
            if (_uiState.value.remainingTime > 0) {
                val sessionId = pomodoroRepository.startSession(
                    _uiState.value.selectedTask?.id,
                    currentSessionType
                )
                currentSessionId = sessionId

                _uiState.update { it.copy(timerState = TimerState.RUNNING) }

                timerJob = viewModelScope.launch {
                    val startTime = System.currentTimeMillis()
                    val targetTime = startTime + _uiState.value.remainingTime

                    while (System.currentTimeMillis() < targetTime) {
                        val remaining = targetTime - System.currentTimeMillis()
                        _uiState.update { it.copy(remainingTime = remaining.coerceAtLeast(0)) }
                        delay(100) // Обновляем каждые 100 мс для плавности
                    }

                    // Таймер завершен
                    _uiState.update { it.copy(
                        timerState = TimerState.FINISHED,
                        remainingTime = 0L
                    )}

                    // Отмечаем сессию как завершенную
                    currentSessionId?.let { sessionId ->
                        pomodoroRepository.completeSession(sessionId)
                        currentSessionId = null

                        // Обновляем статистику
                        loadTodayStats()

                        // Если выбрана задача, обновляем ее статистику
                        _uiState.value.selectedTask?.let { task ->
                            // Обновляем статистику задачи
                            loadTaskStats(task.id)
                        }

                        // Если автостарт включен, запускаем следующую сессию
                        if (_uiState.value.pomodoroSessionType == PomodoroSessionType.WORK) {
                            // После работы обновляем счетчик завершенных помидоров
                            val newCompletedPomodoros = _uiState.value.completedPomodoros + 1

                            // После работы идет перерыв
                            val nextSessionType = if (newCompletedPomodoros % _preferences.value.pomodorosUntilLongBreak == 0) {
                                PomodoroSessionType.LONG_BREAK
                            } else {
                                PomodoroSessionType.SHORT_BREAK
                            }

                            // Устанавливаем новое время для следующего типа сессии
                            val nextSessionTime = when (nextSessionType) {
                                PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
                                PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
                                else -> TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                            }

                            _uiState.update { it.copy(
                                pomodoroSessionType = nextSessionType,
                                completedPomodoros = newCompletedPomodoros,
                                remainingTime = nextSessionTime,
                                totalTime = nextSessionTime,
                                timerState = TimerState.IDLE // Сброс состояния таймера
                            )}

                            if (_preferences.value.autoStartBreaks) {
                                // Автоматически запускаем перерыв (если включено)
                                delay(800) // Небольшая задержка для обновления UI
                                startPomodoroTimer()
                            }
                        } else {
                            // После перерыва идет работа
                            val workTime = if (_uiState.value.selectedTask != null) {
                                calculateSessionDurationForTask(_uiState.value.selectedTask!!)
                            } else {
                                TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                            }

                            _uiState.update { it.copy(
                                pomodoroSessionType = PomodoroSessionType.WORK,
                                remainingTime = workTime,
                                totalTime = workTime,
                                timerState = TimerState.IDLE // Сброс состояния таймера
                            )}

                            if (_preferences.value.autoStartPomodoros) {
                                // Автоматически запускаем работу (если включено)
                                delay(800) // Небольшая задержка для обновления UI
                                startPomodoroTimer()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startStopwatch() {
        _uiState.update { it.copy(
            timerState = TimerState.RUNNING,
            startTime = System.currentTimeMillis()
        )}

        timerJob = viewModelScope.launch {
            while (_uiState.value.timerState == TimerState.RUNNING) {
                val elapsed = System.currentTimeMillis() - _uiState.value.startTime
                _uiState.update { it.copy(elapsedTime = elapsed) }
                delay(100) // Обновляем каждые 100 мс для плавности
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerState = TimerState.PAUSED) }
    }

    fun resumeTimer() {
        when (_uiState.value.timerType) {
            TimerType.POMODORO -> {
                _uiState.update { it.copy(timerState = TimerState.RUNNING) }

                timerJob = viewModelScope.launch {
                    val startTime = System.currentTimeMillis()
                    val targetTime = startTime + _uiState.value.remainingTime

                    while (System.currentTimeMillis() < targetTime) {
                        val remaining = targetTime - System.currentTimeMillis()
                        _uiState.update { it.copy(remainingTime = remaining.coerceAtLeast(0)) }
                        delay(100)
                    }

                    // Таймер завершен
                    _uiState.update { it.copy(
                        timerState = TimerState.FINISHED,
                        remainingTime = 0L
                    )}

                    // Отмечаем сессию как завершенную
                    currentSessionId?.let { sessionId ->
                        pomodoroRepository.completeSession(sessionId)
                        currentSessionId = null

                        // Обновляем статистику
                        loadTodayStats()

                        // Если выбрана задача, обновляем ее статистику
                        _uiState.value.selectedTask?.let { task ->
                            loadTaskStats(task.id)
                        }
                    }
                }
            }
            TimerType.STOPWATCH -> {
                val pausedElapsedTime = _uiState.value.elapsedTime
                val newStartTime = System.currentTimeMillis() - pausedElapsedTime

                _uiState.update { it.copy(
                    timerState = TimerState.RUNNING,
                    startTime = newStartTime
                )}

                timerJob = viewModelScope.launch {
                    while (_uiState.value.timerState == TimerState.RUNNING) {
                        val elapsed = System.currentTimeMillis() - _uiState.value.startTime
                        _uiState.update { it.copy(elapsedTime = elapsed) }
                        delay(100)
                    }
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()

        when (_uiState.value.timerType) {
            TimerType.POMODORO -> {
                // Отменяем текущую сессию
                currentSessionId?.let { sessionId ->
                    viewModelScope.launch {
                        pomodoroRepository.cancelSession(sessionId)
                        currentSessionId = null
                    }
                }

                // Сбрасываем таймер
                val totalTime = when (_uiState.value.pomodoroSessionType) {
                    PomodoroSessionType.WORK -> {
                        if (_uiState.value.selectedTask != null) {
                            calculateSessionDurationForTask(_uiState.value.selectedTask!!)
                        } else {
                            TimeUnit.MINUTES.toMillis(_preferences.value.workDuration.toLong())
                        }
                    }
                    PomodoroSessionType.SHORT_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.shortBreakDuration.toLong())
                    PomodoroSessionType.LONG_BREAK -> TimeUnit.MINUTES.toMillis(_preferences.value.longBreakDuration.toLong())
                }

                _uiState.update { it.copy(
                    timerState = TimerState.IDLE,
                    remainingTime = totalTime,
                    totalTime = totalTime
                )}
            }
            TimerType.STOPWATCH -> {
                _uiState.update { it.copy(
                    timerState = TimerState.IDLE,
                    elapsedTime = 0L
                )}
            }
        }
    }

    fun updatePreferences(newPreferences: PomodoroPreferences) {
        viewModelScope.launch {
            pomodoroRepository.updatePomodoroPreferences(newPreferences)

            // После обновления настроек, обновляем время таймера, если он не запущен
            if (_uiState.value.timerState != TimerState.RUNNING) {
                updateTimerDurationFromPreferences(newPreferences)
            }
        }
    }

    fun resetCompletedPomodoros() {
        _uiState.update { it.copy(completedPomodoros = 0) }
    }

    fun handleBackPressed(): Boolean {
        // Если таймер запущен, приостанавливаем его и разрешаем навигацию
        if (_uiState.value.timerState == TimerState.RUNNING) {
            pauseTimer()
            return true // Разрешаем навигацию назад
        }
        return true // Всегда разрешаем навигацию назад
    }

    private fun loadRecentTasks() {
        viewModelScope.launch {
            _tasksLoading.value = true
            try {
                taskRepository.getTasksByStatus(com.example.dhbt.domain.model.TaskStatus.ACTIVE)
                    .collect { tasks ->
                        _recentTasks.value = tasks
                        _tasksLoading.value = false
                    }
            } catch (e: Exception) {
                _tasksLoading.value = false
                _recentTasks.value = emptyList()
            }
        }
    }

    fun loadTodayStats() {
        viewModelScope.launch {
            _statsLoading.value = true

            try {
                val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
                val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

                val totalFocusTime = pomodoroRepository.getTotalFocusTime(startOfDay, endOfDay)

                // Получаем все сессии за сегодня
                pomodoroRepository.getSessionsForTimeRange(startOfDay, endOfDay).collect { sessions ->
                    val completedSessions = sessions.count { it.isCompleted && it.type == PomodoroSessionType.WORK }

                    _todayStats.value = PomodoroStats(
                        completedSessions = completedSessions,
                        totalFocusMinutes = totalFocusTime
                    )
                    _statsLoading.value = false
                }
            } catch (e: Exception) {
                _statsLoading.value = false
                // В случае ошибки сохраняем пустую статистику
                _todayStats.value = PomodoroStats(0, 0)
            }
        }
    }
}

// Класс для хранения статистики по задаче
data class TaskPomodoroStats(
    val focusTimeMinutes: Int = 0,                // Затраченное время в минутах
    val estimatedTotalMinutes: Int = 0,           // Ожидаемое время в минутах
    val completedSessions: Int = 0,               // Завершенные сессии
    val estimatedTotalSessions: Int = 0           // Ожидаемое количество сессий
) {
    val hasEstimatedTime: Boolean get() = estimatedTotalMinutes > 0
    val hasEstimatedSessions: Boolean get() = estimatedTotalSessions > 0

    val timeProgress: Float
        get() = if (estimatedTotalMinutes > 0) {
            (focusTimeMinutes.toFloat() / estimatedTotalMinutes).coerceIn(0f, 1f)
        } else 0f

    val sessionsProgress: Float
        get() = if (estimatedTotalSessions > 0) {
            (completedSessions.toFloat() / estimatedTotalSessions).coerceIn(0f, 1f)
        } else 0f
}

// Добавляем новое поле в PomodoroUiState для хранения общего количества сессий из задачи
data class PomodoroUiState(
    val timerType: TimerType = TimerType.POMODORO,
    val timerState: TimerState = TimerState.IDLE,
    val pomodoroSessionType: PomodoroSessionType = PomodoroSessionType.WORK,
    val totalTime: Long = TimeUnit.MINUTES.toMillis(25), // 25 минут по умолчанию
    val remainingTime: Long = totalTime,
    val elapsedTime: Long = 0L,
    val startTime: Long = 0L,
    val completedPomodoros: Int = 0,
    val selectedTask: Task? = null,
    val totalTaskSessions: Int = 0 // Новое поле для общего количества сессий задачи
)

data class PomodoroStats(
    val completedSessions: Int = 0,
    val totalFocusMinutes: Int = 0
)

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

enum class TimerType {
    POMODORO, STOPWATCH
}