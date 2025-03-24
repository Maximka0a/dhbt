package com.example.dhbt.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitTracking
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.domain.model.PomodoroSessionType
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.StatisticPeriod
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.StatisticsRepository
import com.example.dhbt.domain.repository.TaskRepository
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val pomodoroRepository: PomodoroRepository
) : ViewModel() {

    // UI состояние
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    // Выбранный период анализа
    private val _selectedPeriod = MutableStateFlow(StatisticPeriod.WEEK)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    // Выбранная вкладка (задачи, привычки, помодоро)
    private val _selectedTab = MutableStateFlow(StatisticsTab.PRODUCTIVITY)
    val selectedTab = _selectedTab.asStateFlow()

    // Метрики производительности
    private val _productivityMetrics = MutableStateFlow<ProductivityMetrics?>(null)
    val productivityMetrics = _productivityMetrics.asStateFlow()

    // Метрики задач
    private val _taskMetrics = MutableStateFlow<TaskMetrics?>(null)
    val taskMetrics = _taskMetrics.asStateFlow()

    // Метрики привычек
    private val _habitMetrics = MutableStateFlow<HabitMetrics?>(null)
    val habitMetrics = _habitMetrics.asStateFlow()

    // Метрики Pomodoro
    private val _pomodoroMetrics = MutableStateFlow<PomodoroMetrics?>(null)
    val pomodoroMetrics = _pomodoroMetrics.asStateFlow()

    // Данные для временного графика (задачи или привычки по дням)
    private val _timelineChartData = MutableStateFlow<List<Entry>>(emptyList())
    val timelineChartData = _timelineChartData.asStateFlow()

    // Данные для круговых диаграмм
    private val _pieChartData = MutableStateFlow<Map<String, List<PieEntry>>>(emptyMap())
    val pieChartData = _pieChartData.asStateFlow()

    // Общая статистическая информация за текущий период
    private val _summaryStats = MutableStateFlow<Map<String, String>>(emptyMap())
    val summaryStats = _summaryStats.asStateFlow()

    // Выбранная дата для детальной статистики (по умолчанию сегодня)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    // Список доступных месяцев для выбора
    private val _availableMonths = MutableStateFlow<List<YearMonth>>(emptyList())
    val availableMonths = _availableMonths.asStateFlow()

    // Список доступных лет для выбора
    private val _availableYears = MutableStateFlow<List<Int>>(emptyList())
    val availableYears = _availableYears.asStateFlow()

    // Количество дней в текущем периоде
    private val _daysInSelectedPeriod = MutableStateFlow(7)
    val daysInSelectedPeriod = _daysInSelectedPeriod.asStateFlow()

    init {
        // Инициализация доступных периодов
        initializeAvailablePeriods()

        // Загрузка начальных данных
        loadStatisticsForPeriod(StatisticPeriod.WEEK)

        // Наблюдение за изменениями периода
        viewModelScope.launch {
            _selectedPeriod.collect { period ->
                loadStatisticsForPeriod(period)
            }
        }

        // Наблюдение за изменениями вкладки
        viewModelScope.launch {
            _selectedTab.collect { tab ->
                updateChartsForTab(tab)
            }
        }
    }

    private fun initializeAvailablePeriods() {
        val now = LocalDate.now()

        // Доступные месяцы - последние 12 месяцев
        val months = (0L..11L).map { now.minusMonths(it) }
            .map { YearMonth.of(it.year, it.month) }
            .reversed()
        _availableMonths.value = months

        // Доступные годы - последние 3 года
        val years = (0L..2L).map { now.year - it.toInt() }.reversed()
        _availableYears.value = years
    }

    fun onAction(action: StatisticsAction) {
        when (action) {
            is StatisticsAction.SetPeriod -> {
                _selectedPeriod.value = action.period
                adjustDaysInPeriod(action.period)
            }
            is StatisticsAction.SetTab -> _selectedTab.value = action.tab
            is StatisticsAction.SetDate -> _selectedDate.value = action.date
            is StatisticsAction.RefreshData -> loadStatisticsForPeriod(_selectedPeriod.value)
            is StatisticsAction.ExportStatistics -> exportStatisticsData()
        }
    }

    private fun adjustDaysInPeriod(period: StatisticPeriod) {
        _daysInSelectedPeriod.value = when(period) {
            StatisticPeriod.DAY -> 1
            StatisticPeriod.WEEK -> 7
            StatisticPeriod.MONTH -> 30
            StatisticPeriod.YEAR -> 365
        }
    }

    private fun loadStatisticsForPeriod(period: StatisticPeriod) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Определяем временной диапазон на основе периода
                val (startDate, endDate) = calculateDateRange(period)

                // Загрузка всех необходимых данных
                loadProductivityMetrics(startDate, endDate)
                loadTaskMetrics(startDate, endDate)
                loadHabitMetrics(startDate, endDate)
                loadPomodoroMetrics(startDate, endDate)

                // Обновляем графики для текущей вкладки
                updateChartsForTab(_selectedTab.value)

                // Генерируем сводную статистику
                generateSummaryStatistics()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun calculateDateRange(period: StatisticPeriod): Pair<Long, Long> {
        val today = LocalDate.now()
        val startDate = when (period) {
            StatisticPeriod.DAY -> today.atStartOfDay()
            StatisticPeriod.WEEK -> today.minusDays(6).atStartOfDay()
            StatisticPeriod.MONTH -> today.minusDays(29).atStartOfDay()
            StatisticPeriod.YEAR -> today.minusYears(1).plusDays(1).atStartOfDay()
        }
        val endDate = today.plusDays(1).atStartOfDay().minusNanos(1)

        return Pair(
            startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    private suspend fun loadProductivityMetrics(startDate: Long, endDate: Long) {
        try {
            // Получаем статистику из репозитория
            val statisticSummaries = statisticsRepository.getStatisticsForRange(
                startDate, endDate, _selectedPeriod.value
            ).first()

            // Метрики выполнения задач - Handle empty lists and NaN
            val taskCompletionValues = statisticSummaries.mapNotNull { it.taskCompletionPercentage }
            val taskCompletion = if (taskCompletionValues.isEmpty()) 0.0
            else taskCompletionValues.average() * 100

            // Метрики выполнения привычек - Handle empty lists and NaN
            val habitCompletionValues = statisticSummaries.mapNotNull { it.habitCompletionPercentage }
            val habitCompletion = if (habitCompletionValues.isEmpty()) 0.0
            else habitCompletionValues.average() * 100

            // Общее время сфокусированной работы
            val totalPomodoroMinutes = statisticSummaries
                .mapNotNull { it.totalPomodoroMinutes }
                .sum()

            // Серия продуктивных дней
            val productiveStreak = statisticSummaries
                .mapNotNull { it.productiveStreak }
                .maxOrNull() ?: 0

            _productivityMetrics.value = ProductivityMetrics(
                taskCompletionRate = taskCompletion,
                habitCompletionRate = habitCompletion,
                focusTimeMinutes = totalPomodoroMinutes,
                productiveStreak = productiveStreak,
                daysAnalyzed = statisticSummaries.size
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка загрузки метрик: ${e.message}") }
        }
    }

    private suspend fun loadTaskMetrics(startDate: Long, endDate: Long) {
        try {
            // Получаем задачи в указанном диапазоне
            val tasks = taskRepository.getAllTasks()
                .first()
                .filter { task ->
                    task.dueDate != null && task.dueDate in startDate..endDate
                }

            // Количество выполненных и общее количество задач
            val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
            val totalTasks = tasks.size

            // Распределение по категориям
            val categoryDistribution = tasks
                .groupBy { it.categoryId }
                .mapValues { it.value.size }

            // Распределение по приоритетам
            val priorityDistribution = tasks
                .groupBy { it.priority }
                .mapValues { it.value.size }

            // Распределение по дням недели
            val dayOfWeekDistribution = tasks
                .filter { it.dueDate != null }
                .groupBy { LocalDate.ofEpochDay(it.dueDate!! / (24 * 60 * 60 * 1000)).dayOfWeek.value }
                .mapValues { it.value.size }

            // Среднее время выполнения задач
            val averageCompletionTime = tasks
                .filter { it.status == TaskStatus.COMPLETED && it.completionDate != null && it.creationDate > 0 }
                .map { it.completionDate!! - it.creationDate }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            _taskMetrics.value = TaskMetrics(
                completedTasks = completedTasks,
                totalTasks = totalTasks,
                categoryDistribution = categoryDistribution,
                priorityDistribution = priorityDistribution,
                dayOfWeekDistribution = dayOfWeekDistribution,
                averageCompletionTimeMinutes = (averageCompletionTime / (1000 * 60)).toInt()
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка загрузки метрик задач: ${e.message}") }
        }
    }

    private suspend fun loadHabitMetrics(startDate: Long, endDate: Long) {
        try {
            // Получаем привычки
            val habits = habitRepository.getAllHabits().first()

            // Получаем историю отслеживания привычек в указанном диапазоне
            var totalTracking = 0
            var completedTracking = 0
            var longestStreak = 0
            var currentStreak = 0

            val habitData = habits.map { habit ->
                val tracking = habitRepository.getHabitTrackingsForRange(habit.id, startDate, endDate).first()
                totalTracking += tracking.size
                completedTracking += tracking.count { it.isCompleted }
                if (habit.bestStreak > longestStreak) longestStreak = habit.bestStreak
                if (habit.currentStreak > currentStreak) currentStreak = habit.currentStreak

                HabitData(
                    habit = habit,
                    tracking = tracking,
                    completionRate = if (tracking.isEmpty()) 0.0 else tracking.count { it.isCompleted }.toDouble() / tracking.size
                )
            }

            // Распределение по типам привычек
            val typeDistribution = habits
                .groupBy { it.type }
                .mapValues { it.value.size }

            // Распределение по категориям
            val categoryDistribution = habits
                .groupBy { it.categoryId }
                .mapValues { it.value.size }

            _habitMetrics.value = HabitMetrics(
                activeHabits = habits.count { it.status == HabitStatus.ACTIVE },
                totalHabits = habits.size,
                completionRate = if (totalTracking > 0) completedTracking.toDouble() / totalTracking else 0.0,
                longestStreak = longestStreak,
                currentStreak = currentStreak,
                typeDistribution = typeDistribution,
                categoryDistribution = categoryDistribution,
                habitData = habitData
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка загрузки метрик привычек: ${e.message}") }
        }
    }

    private suspend fun loadPomodoroMetrics(startDate: Long, endDate: Long) {
        try {
            // Получаем сессии помодоро в указанном диапазоне
            val pomodoroSessions = pomodoroRepository.getSessionsForTimeRange(startDate, endDate).first()

            // Общее время фокуса
            val totalFocusTime = pomodoroSessions
                .filter { it.type == PomodoroSessionType.WORK && it.isCompleted }
                .sumOf { it.duration }

            // Количество завершенных сессий
            val completedSessions = pomodoroSessions
                .count { it.isCompleted }

            // Количество незавершенных сессий
            val incompleteSessions = pomodoroSessions.size - completedSessions

            // Распределение по дням
            val dailyDistribution = pomodoroSessions
                .filter { it.type == PomodoroSessionType.WORK }
                .groupBy { LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000)) }
                .mapValues { entry ->
                    entry.value.sumOf { it.duration }
                }

            // Распределение по задачам
            val taskDistribution = pomodoroSessions
                .filter { it.type == PomodoroSessionType.WORK && it.taskId != null }
                .groupBy { it.taskId }
                .mapValues { entry ->
                    entry.value.sumOf { it.duration }
                }

            _pomodoroMetrics.value = PomodoroMetrics(
                totalFocusTimeMinutes = totalFocusTime,
                completedSessions = completedSessions,
                incompleteSessions = incompleteSessions,
                dailyDistribution = dailyDistribution,
                taskDistribution = taskDistribution,
                averageDailyMinutes = if (dailyDistribution.isEmpty()) 0 else totalFocusTime / dailyDistribution.size
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Ошибка загрузки метрик Pomodoro: ${e.message}") }
        }
    }

    private fun updateChartsForTab(tab: StatisticsTab) {
        when (tab) {
            StatisticsTab.PRODUCTIVITY -> updateProductivityCharts()
            StatisticsTab.TASKS -> updateTaskCharts()
            StatisticsTab.HABITS -> updateHabitCharts()
            StatisticsTab.POMODORO -> updatePomodoroCharts()
        }
    }

    private fun updateProductivityCharts() {
        val productivity = _productivityMetrics.value ?: return
        val tasks = _taskMetrics.value ?: return
        val habits = _habitMetrics.value ?: return
        val pomodoro = _pomodoroMetrics.value ?: return

        // Линейный график производительности по дням
        val period = _selectedPeriod.value
        val now = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")

        val timelineData = when (period) {
            StatisticPeriod.DAY -> {
                listOf(Entry(0f, productivity.taskCompletionRate.toFloat()))
            }
            StatisticPeriod.WEEK -> {
                (0..6).map { day ->
                    val date = now.minusDays((6 - day).toLong())
                    val taskRate = tasks.dayOfWeekDistribution[date.dayOfWeek.value]?.toFloat() ?: 0f
                    Entry(day.toFloat(), taskRate)
                }
            }
            StatisticPeriod.MONTH -> {
                (0..29).map { day ->
                    val date = now.minusDays((29 - day).toLong())
                    val taskRate = tasks.dayOfWeekDistribution[date.dayOfWeek.value]?.toFloat() ?: 0f
                    Entry(day.toFloat(), taskRate)
                }
            }
            StatisticPeriod.YEAR -> {
                (0..11).map { month ->
                    val date = YearMonth.now().minusMonths(11 - month.toLong())
                    Entry(month.toFloat(), (month + 1).toFloat() * 2) // Заглушка для демонстрации
                }
            }
        }
        _timelineChartData.value = timelineData

        // Круговые диаграммы для общих метрик
        val productivityPie = listOf(
            PieEntry(productivity.taskCompletionRate.toFloat(), "Задачи"),
            PieEntry(productivity.habitCompletionRate.toFloat(), "Привычки"),
            PieEntry(productivity.focusTimeMinutes.toFloat() / 60f, "Фокус (ч)")
        )

        val pieData = mapOf(
            "productivity" to productivityPie
        )
        _pieChartData.value = pieData
    }

    private fun updateTaskCharts() {
        val tasks = _taskMetrics.value ?: return

        // Данные для круговой диаграммы приоритетов
        val priorityPie = listOf(
            PieEntry(tasks.priorityDistribution[TaskPriority.LOW]?.toFloat() ?: 0f, "Низкий"),
            PieEntry(tasks.priorityDistribution[TaskPriority.MEDIUM]?.toFloat() ?: 0f, "Средний"),
            PieEntry(tasks.priorityDistribution[TaskPriority.HIGH]?.toFloat() ?: 0f, "Высокий")
        )


        // Данные для круговой диаграммы статусов
        val statusPie = listOf(
            PieEntry(tasks.completedTasks.toFloat(), "Выполнено"),
            PieEntry((tasks.totalTasks - tasks.completedTasks).toFloat(), "Не выполнено")
        )

        // Данные для линейного графика выполнения задач по дням недели
        val dayOfWeekData = (1..7).map { day ->
            Entry((day - 1).toFloat(), tasks.dayOfWeekDistribution[day]?.toFloat() ?: 0f)
        }

        _timelineChartData.value = dayOfWeekData
        _pieChartData.value = mapOf(
            "priority" to priorityPie,
            "status" to statusPie
        )
    }

    private fun updateHabitCharts() {
        val habits = _habitMetrics.value ?: return

        // Данные для круговой диаграммы типов привычек
        val typePie = listOf(
            PieEntry(habits.typeDistribution[HabitType.BINARY]?.toFloat() ?: 0f, "Бинарные"),
            PieEntry(habits.typeDistribution[HabitType.QUANTITY]?.toFloat() ?: 0f, "Количественные"),
            PieEntry(habits.typeDistribution[HabitType.TIME]?.toFloat() ?: 0f, "Временные")
        )

        // Данные для круговой диаграммы статусов привычек
        val statusPie = listOf(
            PieEntry(habits.activeHabits.toFloat(), "Активные"),
            PieEntry((habits.totalHabits - habits.activeHabits).toFloat(), "Неактивные")
        )

        // Данные для линейного графика выполнения привычек по топ 5 привычкам
        val habitData = habits.habitData.sortedByDescending { it.completionRate }.take(5)
        val habitEntries = habitData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.completionRate.toFloat() * 100)
        }

        _timelineChartData.value = habitEntries
        _pieChartData.value = mapOf(
            "type" to typePie,
            "status" to statusPie
        )
    }

    private fun updatePomodoroCharts() {
        val pomodoro = _pomodoroMetrics.value ?: return

        // Данные для круговой диаграммы завершенных сессий
        val sessionsPie = listOf(
            PieEntry(pomodoro.completedSessions.toFloat(), "Завершенные"),
            PieEntry(pomodoro.incompleteSessions.toFloat(), "Незавершенные")
        )

        // Данные для линейного графика времени фокуса по дням
        val now = LocalDate.now()
        val daysInPeriod = _daysInSelectedPeriod.value

        val focusTimeData = (0 until daysInPeriod).map { day ->
            val date = now.minusDays((daysInPeriod - 1 - day).toLong())
            val focusTime = pomodoro.dailyDistribution[date]?.toFloat() ?: 0f
            Entry(day.toFloat(), focusTime)
        }

        _timelineChartData.value = focusTimeData
        _pieChartData.value = mapOf(
            "sessions" to sessionsPie
        )
    }

    private fun generateSummaryStatistics() {
        val productivity = _productivityMetrics.value ?: return
        val tasks = _taskMetrics.value ?: return
        val habits = _habitMetrics.value ?: return
        val pomodoro = _pomodoroMetrics.value ?: return

        val summary = mutableMapOf<String, String>()

        // Форматирование для процентов и времени
        val percentFormatter = { value: Double ->
            if (value.isNaN()) "0.0%" else "%.1f%%".format(value)
        }
        val hourFormatter = { value: Double -> "%.1f ч".format(value) }

        // Общие метрики
        val taskCompletionPercent = if (productivity.taskCompletionRate.isNaN())
            0.0 else productivity.taskCompletionRate
        summary["Выполнено задач"] = "${tasks.completedTasks} из ${tasks.totalTasks} (${percentFormatter(taskCompletionPercent)})"

        val habitCompletionPercent = if (productivity.habitCompletionRate.isNaN())
            0.0 else productivity.habitCompletionRate
        summary["Выполнено привычек"] = percentFormatter(habitCompletionPercent)

        summary["Время фокуса"] = hourFormatter(pomodoro.totalFocusTimeMinutes / 60.0)
        summary["Сессий Pomodoro"] = "${pomodoro.completedSessions} завершено"
        summary["Текущая серия"] = "${habits.currentStreak} дней"
        summary["Лучшая серия"] = "${habits.longestStreak} дней"

        // Продуктивность
        summary["Среднее время на задачу"] = "${tasks.averageCompletionTimeMinutes / 60} часов"
        summary["Ежедневный фокус"] = "${pomodoro.averageDailyMinutes} минут"

        _summaryStats.value = summary
    }

    private fun exportStatisticsData() {
        // Этот метод будет реализован для экспорта статистики в CSV или PDF
        // Здесь будет логика подготовки данных для экспорта
    }
}

enum class StatisticPeriod(val value: Int, val displayName: String) {
    DAY(0, "День"),
    WEEK(1, "Неделя"),
    MONTH(2, "Месяц"),
    YEAR(3, "Год")
}

// Вкладки в экране статистики
enum class StatisticsTab(val title: String) {
    PRODUCTIVITY("Общее"),
    TASKS("Задачи"),
    HABITS("Привычки"),
    POMODORO("Pomodoro")
}


// Actions для взаимодействия с ViewModel
sealed class StatisticsAction {
    data class SetPeriod(val period: StatisticPeriod) : StatisticsAction()
    data class SetTab(val tab: StatisticsTab) : StatisticsAction()
    data class SetDate(val date: LocalDate) : StatisticsAction()
    object RefreshData : StatisticsAction()
    object ExportStatistics : StatisticsAction()
}

// UI состояние экрана
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

// Модель метрик производительности
data class ProductivityMetrics(
    val taskCompletionRate: Double = 0.0,
    val habitCompletionRate: Double = 0.0,
    val focusTimeMinutes: Int = 0,
    val productiveStreak: Int = 0,
    val daysAnalyzed: Int = 0
)


// Модель метрик задач
data class TaskMetrics(
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val categoryDistribution: Map<String?, Int> = emptyMap(),
    val priorityDistribution: Map<TaskPriority, Int> = emptyMap(),
    val dayOfWeekDistribution: Map<Int, Int> = emptyMap(),
    val averageCompletionTimeMinutes: Int = 0
)

// Модель метрик привычек
data class HabitMetrics(
    val activeHabits: Int = 0,
    val totalHabits: Int = 0,
    val completionRate: Double = 0.0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val typeDistribution: Map<HabitType, Int> = emptyMap(),
    val categoryDistribution: Map<String?, Int> = emptyMap(),
    val habitData: List<HabitData> = emptyList()
)

// Дополнительная модель для данных конкретной привычки
data class HabitData(
    val habit: Habit,
    val tracking: List<HabitTracking>,
    val completionRate: Double
)

// Модель метрик Pomodoro
data class PomodoroMetrics(
    val totalFocusTimeMinutes: Int = 0,
    val completedSessions: Int = 0,
    val incompleteSessions: Int = 0,
    val dailyDistribution: Map<LocalDate, Int> = emptyMap(),
    val taskDistribution: Map<String?, Int> = emptyMap(),
    val averageDailyMinutes: Int = 0
)
