package com.example.dhbt.presentation.habit.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.*
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]
    val isCreationMode = habitId == null

    // Состояние UI
    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState = _uiState.asStateFlow()

    // Данные привычки
    private val _habit = MutableStateFlow<Habit?>(null)
    val habit = _habit.asStateFlow()

    // Категория
    private val _category = MutableStateFlow<Category?>(null)
    val category = _category.asStateFlow()

    // Теги
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    // История отслеживания
    private val _trackingHistory = MutableStateFlow<List<HabitTracking>>(emptyList())
    val trackingHistory = _trackingHistory.asStateFlow()

    // Данные для календаря активности
    private val _calendarData = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val calendarData = _calendarData.asStateFlow()

    // Данные для графиков
    private val _weeklyCompletion = MutableStateFlow<List<Float>>(List(7) { 0f })
    val weeklyCompletion = _weeklyCompletion.asStateFlow()

    private val _monthlyCompletion = MutableStateFlow<List<Float>>(List(30) { 0f })
    val monthlyCompletion = _monthlyCompletion.asStateFlow()

    // Данные о прогрессе сегодняшнего дня
    private val _todayProgress = MutableStateFlow(0f)
    val todayProgress = _todayProgress.asStateFlow()

    private val _todayValue = MutableStateFlow(0f)
    val todayValue = _todayValue.asStateFlow()

    private val _todayIsCompleted = MutableStateFlow(false)
    val todayIsCompleted = _todayIsCompleted.asStateFlow()

    // Одноразовые события
    private val _events = MutableSharedFlow<HabitDetailEvent>()
    val events = _events.asSharedFlow()

    // Текущая выбранная секция для графика
    private val _selectedChartPeriod = MutableStateFlow(ChartPeriod.WEEK)
    val selectedChartPeriod = _selectedChartPeriod.asStateFlow()

    // Для отображения диалога удаления
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    // Для отображения меню
    private val _showMenu = MutableStateFlow(false)
    val showMenu = _showMenu.asStateFlow()

    init {
        loadHabit()
        loadTrackingData()
        calculateCalendarData()
        calculateChartData()
        updateTodayProgress()
    }

    fun onAction(action: HabitDetailAction) {
        when (action) {
            is HabitDetailAction.IncrementProgress -> incrementProgress()
            is HabitDetailAction.DecrementProgress -> decrementProgress()
            is HabitDetailAction.ToggleCompletion -> toggleCompletion()
            is HabitDetailAction.ArchiveHabit -> archiveHabit()
            is HabitDetailAction.ShareHabit -> shareHabit()
            is HabitDetailAction.DeleteHabit -> deleteHabit()
            is HabitDetailAction.SetChartPeriod -> setChartPeriod(action.period)
            is HabitDetailAction.ShowDeleteDialog -> _showDeleteDialog.value = action.show
            is HabitDetailAction.ShowMenu -> _showMenu.value = action.show
        }
    }

    private fun loadHabit() {
        viewModelScope.launch {
            try {
                val habit = habitRepository.getHabitById(habitId.toString())
                _habit.value = habit

                // Загружаем связанную категорию, если она есть
                habit?.categoryId?.let { categoryId ->
                    _category.value = categoryRepository.getCategoryById(categoryId)
                }

                // Загружаем связанные теги (предполагая, что API такой же для привычек)
                _tags.value = tagRepository.getTagsForTask(habitId.toString()).first()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Не удалось загрузить привычку: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun loadTrackingData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val thirtyDaysAgo = now.minusDays(30)

            val startDate = thirtyDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endDate = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

            habitRepository.getHabitTrackingsForRange(habitId.toString(), startDate, endDate)
                .collect { trackings ->
                    _trackingHistory.value = trackings
                    calculateCalendarData()
                    calculateChartData()
                    updateTodayProgress()
                }
        }
    }

    private fun calculateCalendarData() {
        val trackings = _trackingHistory.value
        if (trackings.isEmpty()) return

        val dateMap = mutableMapOf<LocalDate, Float>()

        trackings.forEach { tracking ->
            val date = LocalDate.ofEpochDay(tracking.date / 86400000) // Convert milliseconds to days
            val progress = if (tracking.isCompleted) 1f else 0f
            dateMap[date] = progress
        }

        _calendarData.value = dateMap
    }

    private fun calculateChartData() {
        val trackings = _trackingHistory.value
        if (trackings.isEmpty()) return

        val now = LocalDate.now()
        val weeklyData = MutableList(7) { 0f }
        val monthlyData = MutableList(30) { 0f }

        // Weekly data (last 7 days)
        for (i in 0 until 7) {
            val day = now.minusDays(i.toLong())
            val tracking = findTrackingForDate(day)
            weeklyData[6 - i] = calculateCompletionRateForTracking(tracking)
        }

        // Monthly data (last 30 days)
        for (i in 0 until 30) {
            val day = now.minusDays(i.toLong())
            val tracking = findTrackingForDate(day)
            monthlyData[29 - i] = calculateCompletionRateForTracking(tracking)
        }

        _weeklyCompletion.value = weeklyData
        _monthlyCompletion.value = monthlyData
    }

    private fun findTrackingForDate(date: LocalDate): HabitTracking? {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return _trackingHistory.value.find { tracking ->
            tracking.date in startOfDay..endOfDay
        }
    }

    private fun calculateCompletionRateForTracking(tracking: HabitTracking?): Float {
        if (tracking == null) return 0f
        if (!tracking.isCompleted) return 0f

        val habit = _habit.value ?: return 0f

        return when (habit.type) {
            HabitType.BINARY -> if (tracking.isCompleted) 1f else 0f
            HabitType.QUANTITY -> {
                val value = tracking.value ?: 0f
                val target = habit.targetValue ?: 1f
                // Remove coerceIn to allow values > 1.0
                value / target
            }
            HabitType.TIME -> {
                val duration = tracking.duration?.toFloat() ?: 0f
                val target = habit.targetValue ?: 1f
                // Remove coerceIn to allow values > 1.0
                duration / target
            }
        }
    }

    private fun updateTodayProgress() {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val todayTracking = habitRepository.getHabitTrackingForDate(habitId.toString(), today)
            val habit = _habit.value ?: return@launch

            if (todayTracking == null) {
                _todayProgress.value = 0f
                _todayValue.value = 0f
                _todayIsCompleted.value = false
                return@launch
            }

            _todayIsCompleted.value = todayTracking.isCompleted

            when (habit.type) {
                HabitType.BINARY -> {
                    _todayProgress.value = if (todayTracking.isCompleted) 1f else 0f
                    _todayValue.value = if (todayTracking.isCompleted) 1f else 0f
                }
                HabitType.QUANTITY -> {
                    val value = todayTracking.value ?: 0f
                    val target = habit.targetValue ?: 1f
                    _todayValue.value = value
                    // Remove the coerceIn to allow progress > 1.0
                    _todayProgress.value = value / target
                }
                HabitType.TIME -> {
                    val duration = todayTracking.duration?.toFloat() ?: 0f
                    val target = habit.targetValue ?: 1f
                    _todayValue.value = duration
                    // Remove the coerceIn to allow progress > 1.0
                    _todayProgress.value = duration / target
                }
            }
        }
    }

    private fun incrementProgress() {
        viewModelScope.launch {
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            habitRepository.incrementHabitProgress(habitId.toString(), today)
            updateTodayProgress()
            loadTrackingData()
            _events.emit(HabitDetailEvent.ProgressUpdated("Прогресс увеличен"))
        }
    }

    private fun decrementProgress() {
        val habit = _habit.value ?: return
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val todayTracking = habitRepository.getHabitTrackingForDate(habitId.toString(), today)

            if (todayTracking == null) return@launch

            when (habit.type) {
                HabitType.BINARY -> {
                    // Для бинарного типа просто отмечаем как не выполненное
                    val updatedTracking = todayTracking.copy(isCompleted = false)
                    habitRepository.updateHabitTracking(updatedTracking)
                }
                HabitType.QUANTITY -> {
                    // Для количественного типа уменьшаем значение на 1
                    val currentValue = todayTracking.value ?: 0f
                    val newValue = (currentValue - 1).coerceAtLeast(0f)
                    val targetValue = habit.targetValue ?: 1f
                    val isCompleted = newValue >= targetValue

                    val updatedTracking = todayTracking.copy(
                        value = newValue,
                        isCompleted = isCompleted
                    )
                    habitRepository.updateHabitTracking(updatedTracking)
                }
                HabitType.TIME -> {
                    // Для времени уменьшаем на 1 минуту (или другую логику)
                    val currentDuration = todayTracking.duration ?: 0
                    val newDuration = (currentDuration - 1).coerceAtLeast(0)
                    val targetDuration = habit.targetValue?.toInt() ?: 1
                    val isCompleted = newDuration >= targetDuration

                    val updatedTracking = todayTracking.copy(
                        duration = newDuration,
                        isCompleted = isCompleted
                    )
                    habitRepository.updateHabitTracking(updatedTracking)
                }
            }

            updateTodayProgress()
            loadTrackingData()
            _events.emit(HabitDetailEvent.ProgressUpdated("Прогресс уменьшен"))
        }
    }

    private fun toggleCompletion() {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val todayTracking = habitRepository.getHabitTrackingForDate(habitId.toString(), today)

            if (todayTracking != null) {
                // Переключаем существующую запись
                val updatedTracking = todayTracking.copy(isCompleted = !todayTracking.isCompleted)
                habitRepository.updateHabitTracking(updatedTracking)
            } else {
                // Создаем новую запись
                val newTracking = HabitTracking(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId.toString(),
                    date = today,
                    isCompleted = true
                )
                habitRepository.trackHabit(newTracking)
            }

            updateTodayProgress()
            loadTrackingData()
            _events.emit(HabitDetailEvent.ProgressUpdated("Статус выполнения обновлен"))
        }
    }

    private fun archiveHabit() {
        viewModelScope.launch {
            val habit = _habit.value ?: return@launch
            val currentStatus = habit.status

            val newStatus = if (currentStatus == HabitStatus.ARCHIVED) {
                HabitStatus.ACTIVE
            } else {
                HabitStatus.ARCHIVED
            }

            habitRepository.changeHabitStatus(habitId.toString(), newStatus)

            _habit.value = _habit.value?.copy(status = newStatus)

            val message = if (newStatus == HabitStatus.ARCHIVED) {
                "Привычка архивирована"
            } else {
                "Привычка восстановлена из архива"
            }

            _events.emit(HabitDetailEvent.HabitStatusChanged(message))
        }
    }

    private fun shareHabit() {
        val habit = _habit.value ?: return

        val shareText = buildString {
            append("Моя привычка: ${habit.title}")
            habit.description?.let { append("\n$it") }
            append("\nТекущая серия: ${habit.currentStreak} дней")
            append("\nРекордная серия: ${habit.bestStreak} дней")
        }

        viewModelScope.launch {
            _events.emit(HabitDetailEvent.ShareHabit(shareText))
        }
    }

    private fun deleteHabit() {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(habitId.toString())
                _events.emit(HabitDetailEvent.HabitDeleted)
            } catch (e: Exception) {
                _events.emit(HabitDetailEvent.Error("Не удалось удалить привычку: ${e.localizedMessage}"))
            }
        }
    }

    private fun setChartPeriod(period: ChartPeriod) {
        _selectedChartPeriod.value = period
    }

    // Получение форматированной строки для отображения частоты
    fun getFrequencyText(): String {
        val habit = _habit.value ?: return "Ежедневно"

        // Получаем частоту из репозитория, т.к. она больше не хранится в Habit
        val frequency = runBlocking { habitRepository.getHabitFrequency(habit.id) } ?: return "Ежедневно"

        return when (frequency.type) {
            FrequencyType.DAILY -> "Ежедневно"
            FrequencyType.SPECIFIC_DAYS -> {
                val days = frequency.daysOfWeek ?: return "По дням недели"
                val dayNames = days.map { dayNumber ->
                    try {
                        val day = DayOfWeek.of(dayNumber)
                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    } catch (e: Exception) {
                        "$dayNumber"
                    }
                }
                "По дням: ${days.joinToString<Int>(", ")}"
            }
            FrequencyType.TIMES_PER_WEEK -> {
                val times = frequency.timesPerPeriod ?: 1
                "$times раз в неделю"
            }
            FrequencyType.TIMES_PER_MONTH -> {
                val times = frequency.timesPerPeriod ?: 1
                "$times раз в месяц"
            }
            else -> "Ежедневно" // добавляем ветку else для исчерпывающего when
        }
    }

    // Получение информации о целевом значении
    fun getTargetValueText(): String {
        val habit = _habit.value ?: return "Выполнить"

        return when (habit.type) {
            HabitType.BINARY -> "Отметить выполнение"
            HabitType.QUANTITY -> {
                val target = habit.targetValue ?: 1f
                val unit = habit.unitOfMeasurement ?: "раз"
                "$target $unit"
            }
            HabitType.TIME -> {
                val target = habit.targetValue?.toInt() ?: 1
                "$target минут"
            }
        }
    }

    // Получение текущего прогресса в текстовом виде
    fun getCurrentProgressText(): String {
        val habit = _habit.value ?: return "0%"
        val todayValue = _todayValue.value

        return when (habit.type) {
            HabitType.BINARY -> if (_todayIsCompleted.value) "Выполнено" else "Не выполнено"
            HabitType.QUANTITY -> {
                val target = habit.targetValue ?: 1f
                val unit = habit.unitOfMeasurement ?: "раз"
                // Here we show the actual value, not capped at the target
                "$todayValue / $target $unit"
            }
            HabitType.TIME -> {
                val target = habit.targetValue?.toInt() ?: 1
                // Here we show the actual value, not capped at the target
                "$todayValue / $target минут"
            }
        }
    }
}

// UI состояние для экрана деталей привычки
data class HabitDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

// События для одноразовых действий
sealed class HabitDetailEvent {
    data class ProgressUpdated(val message: String) : HabitDetailEvent()
    data class HabitStatusChanged(val message: String) : HabitDetailEvent()
    data class ShareHabit(val text: String) : HabitDetailEvent()
    data class Error(val message: String) : HabitDetailEvent()
    object HabitDeleted : HabitDetailEvent()
}

// Действия, которые пользователь может выполнять
sealed class HabitDetailAction {
    object IncrementProgress : HabitDetailAction()
    object DecrementProgress : HabitDetailAction()
    object ToggleCompletion : HabitDetailAction()
    object ArchiveHabit : HabitDetailAction()
    object ShareHabit : HabitDetailAction()
    object DeleteHabit : HabitDetailAction()
    data class SetChartPeriod(val period: ChartPeriod) : HabitDetailAction()
    data class ShowDeleteDialog(val show: Boolean) : HabitDetailAction()
    data class ShowMenu(val show: Boolean) : HabitDetailAction()
}

// Периоды для графиков
enum class ChartPeriod {
    WEEK, MONTH
}