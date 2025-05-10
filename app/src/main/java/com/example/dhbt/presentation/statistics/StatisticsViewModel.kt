package com.example.dhbt.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.*
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.StatisticsRepository
import com.example.dhbt.domain.repository.TaskRepository
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.dhbt.domain.model.StatisticPeriod
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
/**
 * ViewModel для экрана статистики с оптимизированным управлением данными
 * и эффективной загрузкой метрик для различных периодов времени.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val pomodoroRepository: PomodoroRepository
) : ViewModel() {

    // Состояние пользовательского интерфейса и выбранные параметры
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    // Используем StatisticPeriod из domain слоя, а не дублируем его в presentation
    private val _selectedPeriod = MutableStateFlow(StatisticPeriod.WEEK)
    private val _selectedTab = MutableStateFlow(StatisticsTab.TASKS)
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    // Объединенные потоки данных для эффективности
    val viewState = combine(
        _uiState,
        _selectedPeriod,
        _selectedTab,
        _selectedDate
    ) { state, period, tab, date ->
        StatisticsViewState(
            isLoading = state.isLoading,
            error = state.error,
            selectedPeriod = period,
            selectedTab = tab,
            selectedDate = date
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsViewState()
    )

    // Потоки метрик с отложенной загрузкой и кешированием
    private val _metricsData = MutableStateFlow<StatisticsMetricsData?>(null)

    // Производные потоки данных для UI
    val productivityMetrics = _metricsData.map { it?.productivityMetrics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val taskMetrics = _metricsData.map { it?.taskMetrics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val habitMetrics = _metricsData.map { it?.habitMetrics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pomodoroMetrics = _metricsData.map { it?.pomodoroMetrics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Данные для графиков, обновляются при изменении вкладки или метрик
    val chartData = combine(
        _selectedTab,
        _metricsData
    ) { tab, data ->
        generateChartData(tab, data)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsChartData()
    )

    // Сводные метрики
    val summaryStats = _metricsData
        .map { generateSummaryStatistics(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Списки доступных периодов для выбора
    private val _periodsData = MutableStateFlow(StatisticsPeriodsData())
    val periodsData = _periodsData.asStateFlow()

    // Отслеживание текущих загружаемых данных для избежания дублирования запросов
    private var currentLoadJob: Job? = null
    private var lastLoadedPeriod: StatisticPeriod? = null
    private var lastLoadedRange: Pair<Long, Long>? = null

    init {
        initializeAvailablePeriods()

        // Наблюдаем за изменениями периода и вкладки
        viewModelScope.launch {
            _selectedPeriod
                .collect { period ->
                    loadStatisticsForPeriod(period)
                }
        }
    }

    /**
     * Обрабатывает пользовательские действия
     */
    fun onAction(action: StatisticsAction) {
        when (action) {
            is StatisticsAction.SetPeriod -> {
                _selectedPeriod.value = action.period
                _periodsData.update { it.copy(daysInSelectedPeriod = calculateDaysInPeriod(action.period)) }
            }
            is StatisticsAction.SetTab -> _selectedTab.value = action.tab
            is StatisticsAction.SetDate -> _selectedDate.value = action.date
            is StatisticsAction.RefreshData -> refreshData()
            is StatisticsAction.ExportStatistics -> exportStatisticsData()
        }
    }

    /**
     * Инициализирует списки доступных периодов для выбора
     */
    private fun initializeAvailablePeriods() {
        val now = LocalDate.now()

        // Доступные месяцы - последние 12 месяцев
        val months = (0L..11L).map { now.minusMonths(it) }
            .map { YearMonth.of(it.year, it.month) }
            .reversed()

        // Доступные годы - последние 3 года
        val years = (0L..2L).map { now.year - it.toInt() }.reversed()

        _periodsData.update {
            StatisticsPeriodsData(
                availableMonths = months,
                availableYears = years,
                daysInSelectedPeriod = calculateDaysInPeriod(_selectedPeriod.value)
            )
        }
    }

    /**
     * Вычисляет количество дней в выбранном периоде
     */
    private fun calculateDaysInPeriod(period: StatisticPeriod): Int {
        return when(period) {
            StatisticPeriod.DAY -> 1
            StatisticPeriod.WEEK -> 7
            StatisticPeriod.MONTH -> YearMonth.now().lengthOfMonth()
            StatisticPeriod.YEAR -> if (YearMonth.now().isLeapYear) 366 else 365
        }
    }

    /**
     * Принудительно обновляет данные
     */
    private fun refreshData() {
        lastLoadedPeriod = null
        lastLoadedRange = null
        loadStatisticsForPeriod(_selectedPeriod.value)
    }

    /**
     * Загружает статистические данные для выбранного периода
     */
    private fun loadStatisticsForPeriod(period: StatisticPeriod) {
        // Отменяем предыдущую загрузку, если она еще выполняется
        currentLoadJob?.cancel()

        // Рассчитываем временной диапазон
        val dateRange = calculateDateRange(period)

        // Проверяем, не загружены ли уже данные для этого периода
        if (period == lastLoadedPeriod && dateRange == lastLoadedRange) {
            Timber.d("Данные для периода $period уже загружены")
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        currentLoadJob = viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                // Создаём контейнер для всех метрик
                val metrics = StatisticsMetricsData()

                // Запускаем параллельную загрузку различных метрик
                val (startDate, endDate) = dateRange
                val jobs = listOf(
                    async { loadProductivityMetrics(startDate, endDate, metrics) },
                    async { loadTaskMetrics(startDate, endDate, metrics) },
                    async { loadHabitMetrics(startDate, endDate, metrics) },
                    async { loadPomodoroMetrics(startDate, endDate, metrics) }
                )

                // Ждём завершения всех операций загрузки
                jobs.awaitAll()

                // Сохраняем результат
                _metricsData.value = metrics

                // Запоминаем загруженный период
                lastLoadedPeriod = period
                lastLoadedRange = dateRange

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка при загрузке статистики")
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Неизвестная ошибка") }
            }
        }
    }

    /**
     * Вычисляет временной диапазон на основе выбранного периода
     */
    private fun calculateDateRange(period: StatisticPeriod): Pair<Long, Long> {
        val today = LocalDate.now()
        val zonedEndDate = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())

        val startLocalDate = when (period) {
            StatisticPeriod.DAY -> today
            StatisticPeriod.WEEK -> today.minusDays(6)
            StatisticPeriod.MONTH -> today.minusDays(today.dayOfMonth.toLong() - 1)
            StatisticPeriod.YEAR -> today.withDayOfYear(1)
        }

        val zonedStartDate = startLocalDate.atStartOfDay(ZoneId.systemDefault())

        return Pair(
            zonedStartDate.toInstant().toEpochMilli(),
            zonedEndDate.toInstant().toEpochMilli() - 1
        )
    }

    /**
     * Загружает метрики продуктивности
     */
    private suspend fun loadProductivityMetrics(startDate: Long, endDate: Long, metrics: StatisticsMetricsData) {
        try {
            // Правильно используем тип StatisticPeriod из domain слоя
            val statisticSummaries = statisticsRepository.getStatisticsForRange(
                startDate, endDate, _selectedPeriod.value
            ).first()

            // Метрики выполнения задач - обрабатываем пустые списки и NaN
            val taskCompletionRate = statisticSummaries
                .mapNotNull { it.taskCompletionPercentage }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.times(100)
                ?: 0.0

            // Метрики выполнения привычек
            val habitCompletionRate = statisticSummaries
                .mapNotNull { it.habitCompletionPercentage }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.times(100)
                ?: 0.0

            // Общее время фокусировки
            val totalPomodoroMinutes = statisticSummaries
                .mapNotNull { it.totalPomodoroMinutes }
                .sum()

            // Продуктивная серия
            val productiveStreak = statisticSummaries
                .mapNotNull { it.productiveStreak }
                .maxOrNull() ?: 0

            metrics.productivityMetrics = ProductivityMetrics(
                taskCompletionRate = taskCompletionRate,
                habitCompletionRate = habitCompletionRate,
                focusTimeMinutes = totalPomodoroMinutes,
                productiveStreak = productiveStreak,
                daysAnalyzed = statisticSummaries.size
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки метрик продуктивности")
            throw e
        }
    }

    /**
     * Загружает метрики задач
     */
    private suspend fun loadTaskMetrics(startDate: Long, endDate: Long, metrics: StatisticsMetricsData) {
        try {
            // Получаем все задачи
            val allTasks = taskRepository.getAllTasks().first()

            // 1. Задачи, попадающие в период по дате завершения
            val completedTasksInPeriod = allTasks.filter { task ->
                task.status == TaskStatus.COMPLETED &&
                        task.completionDate != null &&
                        task.completionDate in startDate..endDate
            }

            // 2. Задачи, попадающие в период по дате выполнения
            val activeTasksInPeriod = allTasks.filter { task ->
                task.status != TaskStatus.COMPLETED &&
                        task.dueDate != null &&
                        task.dueDate in startDate..endDate
            }

            // 3. Объединяем выборки для метрик
            val tasksInPeriod = completedTasksInPeriod + activeTasksInPeriod

            // Если совсем нет задач в периоде, показываем хотя бы общую статистику о завершенных
            // (можно убрать, если нужны только задачи строго из периода)
            val tasksToAnalyze = if (tasksInPeriod.isEmpty()) {
                allTasks.filter { it.status == TaskStatus.COMPLETED }.take(10)
            } else {
                tasksInPeriod
            }

            val completedTasks = tasksToAnalyze.count { it.status == TaskStatus.COMPLETED }
            val totalTasks = tasksToAnalyze.size

            // Остальной код остается без изменений
            val categoryDistribution = tasksToAnalyze
                .groupingBy { it.categoryId }
                .eachCount()

            val priorityDistribution = tasksToAnalyze
                .groupingBy { it.priority }
                .eachCount()
                .withDefault { 0 }

            val dayOfWeekDistribution = tasksToAnalyze
                .asSequence()
                .filter { it.dueDate != null || it.completionDate != null }
                .map {
                    val millis = it.completionDate ?: it.dueDate ?: 0L
                    LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000)).dayOfWeek.value
                }
                .groupingBy { it }
                .eachCount()

            val averageCompletionTime = tasksToAnalyze
                .asSequence()
                .filter { it.status == TaskStatus.COMPLETED && it.completionDate != null && it.creationDate > 0 }
                .map { it.completionDate!! - it.creationDate }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            metrics.taskMetrics = TaskMetrics(
                completedTasks = completedTasks,
                totalTasks = totalTasks,
                categoryDistribution = categoryDistribution,
                priorityDistribution = priorityDistribution,
                dayOfWeekDistribution = dayOfWeekDistribution,
                averageCompletionTimeMinutes = (averageCompletionTime / (1000 * 60)).toInt()
            )

        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки метрик задач")
            throw e
        }
    }

    /**
     * Загружает метрики привычек
     */
    private suspend fun loadHabitMetrics(startDate: Long, endDate: Long, metrics: StatisticsMetricsData) {
        try {
            // Используем структурированный подход и кеширование
            val habitData = mutableListOf<HabitData>()
            var totalTracking = 0
            var completedTracking = 0
            var longestStreak = 0
            var currentStreak = 0

            // Получаем все привычки одним запросом
            val habits = habitRepository.getAllHabits().first()

            // Для каждой привычки получаем отслеживания в указанном диапазоне
            habits.forEach { habit ->
                val tracking = habitRepository.getHabitTrackingsForRange(habit.id, startDate, endDate).first()

                // Обновляем агрегированные метрики
                totalTracking += tracking.size
                completedTracking += tracking.count { it.isCompleted }
                if (habit.bestStreak > longestStreak) longestStreak = habit.bestStreak
                if (habit.currentStreak > currentStreak) currentStreak = habit.currentStreak

                // Сохраняем данные для этой привычки
                habitData.add(
                    HabitData(
                        habit = habit,
                        tracking = tracking,
                        completionRate = if (tracking.isEmpty()) 0.0
                        else tracking.count { it.isCompleted }.toDouble() / tracking.size
                    )
                )
            }

            // Получаем распределения по типам и категориям
            val typeDistribution = habits
                .groupingBy { it.type }
                .eachCount()
                .withDefault { 0 }

            val categoryDistribution = habits
                .groupingBy { it.categoryId }
                .eachCount()

            metrics.habitMetrics = HabitMetrics(
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
            Timber.e(e, "Ошибка загрузки метрик привычек")
            throw e
        }
    }

    /**
     * Загружает метрики помодоро
     */
    private suspend fun loadPomodoroMetrics(startDate: Long, endDate: Long, metrics: StatisticsMetricsData) {
        try {
            // Получаем сессии помодоро за выбранный период
            val pomodoroSessions = pomodoroRepository.getSessionsForTimeRange(startDate, endDate).first()

            // Вычисляем общее время фокусировки (только завершенные рабочие сессии)
            val totalFocusTime = pomodoroSessions
                .filter { it.type == PomodoroSessionType.WORK && it.isCompleted }
                .sumOf { it.duration }

            // Количество завершенных и незавершенных сессий
            val completedSessions = pomodoroSessions.count { it.isCompleted }
            val incompleteSessions = pomodoroSessions.size - completedSessions

            // Оптимизированное распределение по дням
            val dailyDistribution = pomodoroSessions
                .asSequence()
                .filter { it.type == PomodoroSessionType.WORK }
                .groupBy {
                    LocalDate.ofEpochDay(it.startTime / (24 * 60 * 60 * 1000))
                }
                .mapValues { (_, sessions) ->
                    sessions.sumOf { it.duration }
                }

            // Распределение по задачам (фильтруем null-задачи)
            val taskDistribution = pomodoroSessions
                .asSequence()
                .filter { it.type == PomodoroSessionType.WORK && it.taskId != null }
                .groupBy { it.taskId }
                .mapValues { (_, sessions) ->
                    sessions.sumOf { it.duration }
                }

            // Расчет среднего дневного времени фокуса
            val averageDailyMinutes = if (dailyDistribution.isNotEmpty())
                totalFocusTime / dailyDistribution.size
            else 0

            metrics.pomodoroMetrics = PomodoroMetrics(
                totalFocusTimeMinutes = totalFocusTime,
                completedSessions = completedSessions,
                incompleteSessions = incompleteSessions,
                dailyDistribution = dailyDistribution,
                taskDistribution = taskDistribution,
                averageDailyMinutes = averageDailyMinutes
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки метрик Pomodoro")
            throw e
        }
    }

    /**
     * Генерирует данные для графиков на основе выбранной вкладки
     */
    private fun generateChartData(tab: StatisticsTab, metricsData: StatisticsMetricsData?): StatisticsChartData {
        if (metricsData == null) return StatisticsChartData()

        return when (tab) {
            StatisticsTab.TASKS -> generateTaskChartData(metricsData)
            StatisticsTab.HABITS -> generateHabitChartData(metricsData)
            StatisticsTab.POMODORO -> generatePomodoroChartData(metricsData)
        }
    }

    /**
     * Генерирует данные для графиков производительности
     */
    private fun generateProductivityChartData(metricsData: StatisticsMetricsData): StatisticsChartData {
        val productivityMetrics = metricsData.productivityMetrics ?: return StatisticsChartData()
        val taskMetrics = metricsData.taskMetrics ?: return StatisticsChartData()

        // Линейный график для отслеживания задач
        val period = _selectedPeriod.value
        val now = LocalDate.now()

        val timelineData = when (period) {
            StatisticPeriod.DAY -> {
                listOf(Entry(0f, productivityMetrics.taskCompletionRate.toFloat()))
            }
            StatisticPeriod.WEEK -> {
                (0..6).map { day ->
                    val date = now.minusDays((6 - day).toLong())
                    val tasks = taskMetrics.dayOfWeekDistribution[date.dayOfWeek.value] ?: 0
                    Entry(day.toFloat(), tasks.toFloat())
                }
            }
            StatisticPeriod.MONTH -> {
                val daysInMonth = calculateDaysInPeriod(StatisticPeriod.MONTH)
                (0 until daysInMonth).map { day ->
                    val date = now.withDayOfMonth(1).plusDays(day.toLong())
                    val tasks = taskMetrics.dayOfWeekDistribution[date.dayOfWeek.value] ?: 0
                    Entry(day.toFloat(), tasks.toFloat())
                }
            }
            StatisticPeriod.YEAR -> {
                (0..11).map { month ->
                    Entry(month.toFloat(), ((month % 6) + 1).toFloat() * 5f) // Заглушка для демонстрации
                }
            }
        }

        // Круговая диаграмма общей производительности
        val productivityPie = listOf(
            PieEntry(productivityMetrics.taskCompletionRate.toFloat(), "Задачи"),
            PieEntry(productivityMetrics.habitCompletionRate.toFloat(), "Привычки"),
            PieEntry(productivityMetrics.focusTimeMinutes.toFloat() / 60f, "Фокус (ч)")
        )

        return StatisticsChartData(
            timelineEntries = timelineData,
            pieCharts = mapOf("productivity" to productivityPie),
            xAxisLabels = generateXAxisLabels(period)
        )
    }

    /**
     * Генерирует данные для графиков задач
     */
    private fun generateTaskChartData(metricsData: StatisticsMetricsData): StatisticsChartData {
        val taskMetrics = metricsData.taskMetrics ?: return StatisticsChartData()

        // Круговая диаграмма приоритетов задач
        val priorityPie = listOf(
            PieEntry(taskMetrics.priorityDistribution.getValue(TaskPriority.LOW).toFloat(), "Низкий"),
            PieEntry(taskMetrics.priorityDistribution.getValue(TaskPriority.MEDIUM).toFloat(), "Средний"),
            PieEntry(taskMetrics.priorityDistribution.getValue(TaskPriority.HIGH).toFloat(), "Высокий")
        )

        // Круговая диаграмма статусов задач
        val statusPie = listOf(
            PieEntry(taskMetrics.completedTasks.toFloat(), "Выполнено"),
            PieEntry((taskMetrics.totalTasks - taskMetrics.completedTasks).toFloat(), "Активно")
        )

        // Линейный график по дням недели
        val dayOfWeekData = (1..7).map { day ->
            Entry((day - 1).toFloat(), taskMetrics.dayOfWeekDistribution[day]?.toFloat() ?: 0f)
        }

        return StatisticsChartData(
            timelineEntries = dayOfWeekData,
            pieCharts = mapOf(
                "priority" to priorityPie,
                "status" to statusPie
            ),
            xAxisLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        )
    }

    /**
     * Генерирует данные для графиков привычек
     */
    private fun generateHabitChartData(metricsData: StatisticsMetricsData): StatisticsChartData {
        val habitMetrics = metricsData.habitMetrics ?: return StatisticsChartData()

        // Круговая диаграмма типов привычек
        val typePie = listOf(
            PieEntry(habitMetrics.typeDistribution.getValue(HabitType.BINARY).toFloat(), "Бинарные"),
            PieEntry(habitMetrics.typeDistribution.getValue(HabitType.QUANTITY).toFloat(), "Количественные"),
            PieEntry(habitMetrics.typeDistribution.getValue(HabitType.TIME).toFloat(), "Временные")
        )

        // Круговая диаграмма статусов привычек
        val statusPie = listOf(
            PieEntry(habitMetrics.activeHabits.toFloat(), "Активные"),
            PieEntry((habitMetrics.totalHabits - habitMetrics.activeHabits).toFloat(), "Неактивные")
        )

        // Линейный график выполнения топ-5 привычек
        val habitEntries = habitMetrics.habitData
            .sortedByDescending { it.completionRate }
            .take(5)
            .mapIndexed { index, data ->
                Entry(index.toFloat(), (data.completionRate * 100).toFloat())
            }

        // Создаем метки для оси X из названий привычек
        val habitLabels = habitMetrics.habitData
            .sortedByDescending { it.completionRate }
            .take(5)
            .map { it.habit.title.take(10) + if (it.habit.title.length > 10) "..." else "" }

        return StatisticsChartData(
            timelineEntries = habitEntries,
            pieCharts = mapOf(
                "type" to typePie,
                "status" to statusPie
            ),
            xAxisLabels = habitLabels
        )
    }

    /**
     * Генерирует данные для графиков помодоро
     */
    private fun generatePomodoroChartData(metricsData: StatisticsMetricsData): StatisticsChartData {
        val pomodoroMetrics = metricsData.pomodoroMetrics ?: return StatisticsChartData()

        // Круговая диаграмма сессий
        val sessionsPie = listOf(
            PieEntry(pomodoroMetrics.completedSessions.toFloat(), "Завершенные"),
            PieEntry(pomodoroMetrics.incompleteSessions.toFloat(), "Незавершенные")
        )

        // Линейный график времени фокуса по дням
        val daysInPeriod = _periodsData.value.daysInSelectedPeriod
        val now = LocalDate.now()

        val focusTimeData = when (_selectedPeriod.value) {
            StatisticPeriod.DAY -> {
                listOf(Entry(0f, pomodoroMetrics.totalFocusTimeMinutes.toFloat()))
            }
            StatisticPeriod.WEEK -> {
                (0..6).map { day ->
                    val date = now.minusDays((6 - day).toLong())
                    val focusTime = pomodoroMetrics.dailyDistribution[date]?.toFloat() ?: 0f
                    Entry(day.toFloat(), focusTime)
                }
            }
            StatisticPeriod.MONTH -> {
                val days = calculateDaysInPeriod(StatisticPeriod.MONTH)
                (0 until days).map { day ->
                    val date = now.withDayOfMonth(1).plusDays(day.toLong())
                    val focusTime = pomodoroMetrics.dailyDistribution[date]?.toFloat() ?: 0f
                    Entry(day.toFloat(), focusTime)
                }
            }
            StatisticPeriod.YEAR -> {
                (0..11).map { month ->
                    val yearMonth = YearMonth.now().withMonth(month + 1)
                    val focusTime = pomodoroMetrics.dailyDistribution
                        .filterKeys { it.month.value == month + 1 && it.year == now.year }
                        .values
                        .sum()
                        .toFloat()
                    Entry(month.toFloat(), focusTime)
                }
            }
        }

        return StatisticsChartData(
            timelineEntries = focusTimeData,
            pieCharts = mapOf("sessions" to sessionsPie),
            xAxisLabels = generateXAxisLabels(_selectedPeriod.value)
        )
    }

    /**
     * Генерирует метки для оси X в зависимости от периода
     */
    private fun generateXAxisLabels(period: StatisticPeriod): List<String> {
        val now = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
        val monthFormatter = DateTimeFormatter.ofPattern("MMM")

        return when (period) {
            StatisticPeriod.DAY -> listOf(now.format(dateFormatter))
            StatisticPeriod.WEEK -> (0..6).map {
                now.minusDays((6 - it).toLong()).format(dateFormatter)
            }
            StatisticPeriod.MONTH -> {
                val days = calculateDaysInPeriod(StatisticPeriod.MONTH)
                (0 until days).map {
                    if (it % 5 == 0) now.withDayOfMonth(1).plusDays(it.toLong()).format(dateFormatter)
                    else ""
                }
            }
            StatisticPeriod.YEAR -> (1..12).map {
                now.withMonth(it).format(monthFormatter)
            }
        }
    }

    /**
     * Генерирует сводные статистические данные
     */
    private fun generateSummaryStatistics(metricsData: StatisticsMetricsData?): Map<String, String> {
        if (metricsData == null) return emptyMap()

        val productivity = metricsData.productivityMetrics
        val tasks = metricsData.taskMetrics
        val habits = metricsData.habitMetrics
        val pomodoro = metricsData.pomodoroMetrics

        if (productivity == null || tasks == null || habits == null || pomodoro == null) {
            return emptyMap()
        }

        val summary = mutableMapOf<String, String>()

        // Форматирование процентов и времени
        val percentFormatter = { value: Double -> if (value.isNaN()) "0.0%" else "%.1f%%".format(value) }
        val hourFormatter = { value: Double -> "%.1f ч".format(value) }

        // Общие метрики
        summary["Выполнено задач"] = "${tasks.completedTasks} из ${tasks.totalTasks} (${percentFormatter(productivity.taskCompletionRate)})"
        summary["Выполнено привычек"] = percentFormatter(productivity.habitCompletionRate)
        summary["Время фокуса"] = hourFormatter(pomodoro.totalFocusTimeMinutes / 60.0)
        summary["Сессий Pomodoro"] = "${pomodoro.completedSessions} завершено"
        summary["Текущая серия"] = "${habits.currentStreak} дней"
        summary["Лучшая серия"] = "${habits.longestStreak} дней"

        // Продуктивность
        summary["Среднее время на задачу"] = "${tasks.averageCompletionTimeMinutes / 60} часов"
        summary["Ежедневный фокус"] = "${pomodoro.averageDailyMinutes} минут"

        return summary
    }

    /**
     * Экспортирует статистические данные (заглушка)
     */
    private fun exportStatisticsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1000) // Имитация экспорта
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "Статистика экспортирована"
                )
            }
            delay(3000) // Время показа сообщения
            _uiState.update { it.copy(message = null) }
        }
    }

    /**
     * Контейнер для всех метрик
     */
    private data class StatisticsMetricsData(
        var productivityMetrics: ProductivityMetrics? = null,
        var taskMetrics: TaskMetrics? = null,
        var habitMetrics: HabitMetrics? = null,
        var pomodoroMetrics: PomodoroMetrics? = null
    )
}

/**
 * Состояние представления для объединенных данных UI
 */
data class StatisticsViewState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val selectedPeriod: StatisticPeriod = StatisticPeriod.WEEK,
    val selectedTab: StatisticsTab = StatisticsTab.TASKS,
    val selectedDate: LocalDate = LocalDate.now()
)

/**
 * Данные для отображения периодов
 */
data class StatisticsPeriodsData(
    val availableMonths: List<YearMonth> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val daysInSelectedPeriod: Int = 7
)

/**
 * Данные для графиков
 */
data class StatisticsChartData(
    val timelineEntries: List<Entry> = emptyList(),
    val pieCharts: Map<String, List<PieEntry>> = emptyMap(),
    val xAxisLabels: List<String> = emptyList()
)

/**
 * UI состояние экрана
 */
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null
)

/**
 * Вкладки в экране статистики
 */
enum class StatisticsTab(val title: String) {
    TASKS("Задачи"),
    HABITS("Привычки"),
    POMODORO("Pomodoro")
}

/**
 * Actions для взаимодействия с ViewModel
 */
sealed class StatisticsAction {
    data class SetPeriod(val period: StatisticPeriod) : StatisticsAction()
    data class SetTab(val tab: StatisticsTab) : StatisticsAction()
    data class SetDate(val date: LocalDate) : StatisticsAction()
    object RefreshData : StatisticsAction()
    object ExportStatistics : StatisticsAction()
}

/**
 * Модель метрик производительности
 */
data class ProductivityMetrics(
    val taskCompletionRate: Double = 0.0,
    val habitCompletionRate: Double = 0.0,
    val focusTimeMinutes: Int = 0,
    val productiveStreak: Int = 0,
    val daysAnalyzed: Int = 0
)

/**
 * Модель метрик задач
 */
data class TaskMetrics(
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val categoryDistribution: Map<String?, Int> = emptyMap(),
    val priorityDistribution: Map<TaskPriority, Int> = emptyMap(),
    val dayOfWeekDistribution: Map<Int, Int> = emptyMap(),
    val averageCompletionTimeMinutes: Int = 0
)

/**
 * Модель метрик привычек
 */
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

/**
 * Дополнительная модель для данных конкретной привычки
 */
data class HabitData(
    val habit: Habit,
    val tracking: List<HabitTracking>,
    val completionRate: Double
)

/**
 * Модель метрик Pomodoro
 */
data class PomodoroMetrics(
    val totalFocusTimeMinutes: Int = 0,
    val completedSessions: Int = 0,
    val incompleteSessions: Int = 0,
    val dailyDistribution: Map<LocalDate, Int> = emptyMap(),
    val taskDistribution: Map<String?, Int> = emptyMap(),
    val averageDailyMinutes: Int = 0
)