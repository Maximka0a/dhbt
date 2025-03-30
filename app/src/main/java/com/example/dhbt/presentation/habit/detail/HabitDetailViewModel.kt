package com.example.dhbt.presentation.habit.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.*
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.presentation.components.SnackbarType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val initialDate: String? = savedStateHandle["date"]

    // Selected date (default to today if not provided)
    private val _selectedDate = MutableStateFlow(
        initialDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
    )
    val selectedDate = _selectedDate.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState = _uiState.asStateFlow()

    // Habit data
    private val _habit = MutableStateFlow<Habit?>(null)
    val habit = _habit.asStateFlow()

    // Category
    private val _category = MutableStateFlow<Category?>(null)
    val category = _category.asStateFlow()

    // Tags
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    // Tracking history
    private val _trackingHistory = MutableStateFlow<List<HabitTracking>>(emptyList())
    val trackingHistory = _trackingHistory.asStateFlow()

    // Calendar data
    private val _calendarData = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val calendarData = _calendarData.asStateFlow()

    // Chart data
    private val _weeklyCompletion = MutableStateFlow<List<Float>>(List(7) { 0f })
    val weeklyCompletion = _weeklyCompletion.asStateFlow()

    private val _monthlyCompletion = MutableStateFlow<List<Float>>(List(30) { 0f })
    val monthlyCompletion = _monthlyCompletion.asStateFlow()

    // Current day progress data
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress = _currentProgress.asStateFlow()

    private val _currentValue = MutableStateFlow(0f)
    val currentValue = _currentValue.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    // One-time events
    private val _events = MutableSharedFlow<HabitDetailEvent>()
    val events = _events.asSharedFlow()

    // Selected chart period
    private val _selectedChartPeriod = MutableStateFlow(ChartPeriod.WEEK)
    val selectedChartPeriod = _selectedChartPeriod.asStateFlow()

    // Delete dialog visibility
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    // Date picker dialog visibility
    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker = _showDatePicker.asStateFlow()

    init {
        loadHabit()
        loadTrackingData()
        updateProgressForDate(_selectedDate.value)
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
            is HabitDetailAction.ShowDatePicker -> _showDatePicker.value = action.show
            is HabitDetailAction.SelectDate -> selectDate(action.date)
        }
    }

    /**
     * Method to reload data after habit editing
     */
    fun reloadHabitData() {
        loadHabit()
        loadTrackingData()
        updateProgressForDate(_selectedDate.value)
    }

    /**
     * Method to select a specific date
     */
    private fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        updateProgressForDate(date)
        _showDatePicker.value = false
    }

    /**
     * Loads habit data
     */
    private fun loadHabit() {
        viewModelScope.launch {
            try {
                val habit = habitRepository.getHabitById(habitId.toString())
                _habit.value = habit

                // Load related category if exists
                habit?.categoryId?.let { categoryId ->
                    _category.value = categoryRepository.getCategoryById(categoryId)
                }

                // Load related tags
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

    /**
     * Loads tracking data for the last 30 days
     */
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
                }
        }
    }

    /**
     * Updates progress for the selected date
     */
    private fun updateProgressForDate(date: LocalDate) {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            try {
                val tracking = habitRepository.getHabitTrackingForDate(habitId.toString(), startOfDay)
                val habit = _habit.value ?: return@launch

                if (tracking == null) {
                    _currentProgress.value = 0f
                    _currentValue.value = 0f
                    _isCompleted.value = false
                    return@launch
                }

                _isCompleted.value = tracking.isCompleted

                when (habit.type) {
                    HabitType.BINARY -> {
                        _currentProgress.value = if (tracking.isCompleted) 1f else 0f
                        _currentValue.value = if (tracking.isCompleted) 1f else 0f
                    }
                    HabitType.QUANTITY -> {
                        val value = tracking.value ?: 0f
                        val target = habit.targetValue ?: 1f
                        _currentValue.value = value
                        _currentProgress.value = value / target
                    }
                    HabitType.TIME -> {
                        val duration = tracking.duration?.toFloat() ?: 0f
                        val target = habit.targetValue ?: 1f
                        _currentValue.value = duration
                        _currentProgress.value = duration / target
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Не удалось загрузить данные за выбранную дату")
                }
                _events.emit(HabitDetailEvent.Error("Не удалось загрузить данные за выбранную дату"))
            }
        }
    }

    /**
     * Calculates calendar heatmap data
     */
    private fun calculateCalendarData() {
        val trackings = _trackingHistory.value
        if (trackings.isEmpty()) return

        val dateMap = mutableMapOf<LocalDate, Float>()

        trackings.forEach { tracking ->
            val date = LocalDate.ofEpochDay(tracking.date / 86400000) // Convert milliseconds to days
            val progress = calculateCompletionRateForTracking(tracking)
            dateMap[date] = progress
        }

        _calendarData.value = dateMap
    }

    /**
     * Calculates chart data (weekly and monthly)
     */
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

    /**
     * Finds tracking data for a specific date
     */
    private fun findTrackingForDate(date: LocalDate): HabitTracking? {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return _trackingHistory.value.find { tracking ->
            tracking.date in startOfDay..endOfDay
        }
    }

    /**
     * Calculates completion rate for a tracking entry
     */
    private fun calculateCompletionRateForTracking(tracking: HabitTracking?): Float {
        if (tracking == null) return 0f
        if (!tracking.isCompleted) return 0f

        val habit = _habit.value ?: return 0f

        return when (habit.type) {
            HabitType.BINARY -> if (tracking.isCompleted) 1f else 0f
            HabitType.QUANTITY -> {
                val value = tracking.value ?: 0f
                val target = habit.targetValue ?: 1f
                value / target
            }
            HabitType.TIME -> {
                val duration = tracking.duration?.toFloat() ?: 0f
                val target = habit.targetValue ?: 1f
                duration / target
            }
        }
    }

    /**
     * Increments progress for the selected date
     */
    private fun incrementProgress() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            habitRepository.incrementHabitProgress(habitId.toString(), startOfDay)
            updateProgressForDate(date)
            loadTrackingData() // Reload to update charts

            _events.emit(HabitDetailEvent.ProgressUpdated(
                message = "Прогресс увеличен",
                type = SnackbarType.SUCCESS
            ))
        }
    }

    /**
     * Decrements progress for the selected date
     */
    private fun decrementProgress() {
        val habit = _habit.value ?: return
        val date = _selectedDate.value
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val tracking = habitRepository.getHabitTrackingForDate(habitId.toString(), startOfDay)

            if (tracking == null) return@launch

            when (habit.type) {
                HabitType.BINARY -> {
                    val updatedTracking = tracking.copy(isCompleted = false)
                    habitRepository.updateHabitTracking(updatedTracking)
                }
                HabitType.QUANTITY -> {
                    val currentValue = tracking.value ?: 0f
                    val newValue = (currentValue - 1).coerceAtLeast(0f)
                    val targetValue = habit.targetValue ?: 1f
                    val isCompleted = newValue >= targetValue

                    val updatedTracking = tracking.copy(
                        value = newValue,
                        isCompleted = isCompleted
                    )
                    habitRepository.updateHabitTracking(updatedTracking)
                }
                HabitType.TIME -> {
                    val currentDuration = tracking.duration ?: 0
                    val newDuration = (currentDuration - 1).coerceAtLeast(0)
                    val targetDuration = habit.targetValue?.toInt() ?: 1
                    val isCompleted = newDuration >= targetDuration

                    val updatedTracking = tracking.copy(
                        duration = newDuration,
                        isCompleted = isCompleted
                    )
                    habitRepository.updateHabitTracking(updatedTracking)
                }
            }

            updateProgressForDate(date)
            loadTrackingData()

            _events.emit(HabitDetailEvent.ProgressUpdated(
                message = "Прогресс уменьшен",
                type = SnackbarType.INFO
            ))
        }
    }

    /**
     * Toggles completion status for the selected date
     */
    private fun toggleCompletion() {
        val date = _selectedDate.value
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val tracking = habitRepository.getHabitTrackingForDate(habitId.toString(), startOfDay)

            if (tracking != null) {
                val updatedTracking = tracking.copy(isCompleted = !tracking.isCompleted)
                habitRepository.updateHabitTracking(updatedTracking)
            } else {
                val newTracking = HabitTracking(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId.toString(),
                    date = startOfDay,
                    isCompleted = true
                )
                habitRepository.trackHabit(newTracking)
            }

            updateProgressForDate(date)
            loadTrackingData()

            _events.emit(HabitDetailEvent.ProgressUpdated(
                message = "Статус выполнения обновлен",
                type = SnackbarType.SUCCESS
            ))
        }
    }

    /**
     * Archives or unarchives the habit
     */
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

            _events.emit(HabitDetailEvent.HabitStatusChanged(
                message = message,
                type = SnackbarType.INFO
            ))
        }
    }

    /**
     * Prepares share text for the habit
     */
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

    /**
     * Deletes the habit
     */
    private fun deleteHabit() {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(habitId.toString())
                _events.emit(HabitDetailEvent.HabitDeleted)
            } catch (e: Exception) {
                _events.emit(HabitDetailEvent.Error(
                    message = "Не удалось удалить привычку",
                    type = SnackbarType.ERROR
                ))
            }
        }
    }

    /**
     * Sets the chart period (week/month)
     */
    private fun setChartPeriod(period: ChartPeriod) {
        _selectedChartPeriod.value = period
    }

    /**
     * Returns formatted frequency text
     */
    fun getFrequencyText(): String {
        val habit = _habit.value ?: return "Ежедневно"

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
                "По дням: ${dayNames.joinToString(", ")}"
            }
            FrequencyType.TIMES_PER_WEEK -> {
                val times = frequency.timesPerPeriod ?: 1
                "$times раз в неделю"
            }
            FrequencyType.TIMES_PER_MONTH -> {
                val times = frequency.timesPerPeriod ?: 1
                "$times раз в месяц"
            }
            else -> "Ежедневно"
        }
    }

    /**
     * Returns formatted target value text
     */
    fun getTargetValueText(): String {
        val habit = _habit.value ?: return "Выполнить"

        return when (habit.type) {
            HabitType.BINARY -> "Отметить"
            HabitType.QUANTITY -> {
                val target = habit.targetValue ?: 1f
                val unit = habit.unitOfMeasurement ?: "раз"
                "$target $unit"
            }
            HabitType.TIME -> {
                val target = habit.targetValue?.toInt() ?: 1
                "$target мин"
            }
        }
    }

    /**
     * Returns formatted current progress text
     */
    fun getCurrentProgressText(): String {
        val habit = _habit.value ?: return "0%"
        val value = _currentValue.value

        return when (habit.type) {
            HabitType.BINARY -> if (_isCompleted.value) "Выполнено" else "Не выполнено"
            HabitType.QUANTITY -> {
                val target = habit.targetValue ?: 1f
                val unit = habit.unitOfMeasurement ?: "раз"
                "$value/$target $unit"
            }
            HabitType.TIME -> {
                val target = habit.targetValue?.toInt() ?: 1
                "$value/$target мин"
            }
        }
    }

    /**
     * Returns formatted selected date text
     */
    fun getSelectedDateText(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
        return _selectedDate.value.format(formatter)
    }

    /**
     * Checks if selected date is today
     */
    fun isSelectedDateToday(): Boolean {
        return _selectedDate.value == LocalDate.now()
    }
}

// UI state
data class HabitDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

// Events
sealed class HabitDetailEvent {
    data class ProgressUpdated(
        val message: String,
        val type: SnackbarType = SnackbarType.SUCCESS
    ) : HabitDetailEvent()

    data class HabitStatusChanged(
        val message: String,
        val type: SnackbarType = SnackbarType.INFO
    ) : HabitDetailEvent()

    data class ShareHabit(val text: String) : HabitDetailEvent()

    data class Error(
        val message: String,
        val type: SnackbarType = SnackbarType.ERROR
    ) : HabitDetailEvent()

    object HabitDeleted : HabitDetailEvent()
}

// Actions
sealed class HabitDetailAction {
    object IncrementProgress : HabitDetailAction()
    object DecrementProgress : HabitDetailAction()
    object ToggleCompletion : HabitDetailAction()
    object ArchiveHabit : HabitDetailAction()
    object ShareHabit : HabitDetailAction()
    object DeleteHabit : HabitDetailAction()
    data class SetChartPeriod(val period: ChartPeriod) : HabitDetailAction()
    data class ShowDeleteDialog(val show: Boolean) : HabitDetailAction()
    data class ShowDatePicker(val show: Boolean) : HabitDetailAction()
    data class SelectDate(val date: LocalDate) : HabitDetailAction()
}

// Chart periods
enum class ChartPeriod {
    WEEK, MONTH
}