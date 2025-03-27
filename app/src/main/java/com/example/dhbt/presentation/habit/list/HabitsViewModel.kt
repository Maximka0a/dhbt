package com.example.dhbt.presentation.habit.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.*
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Состояние UI
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState = _uiState.asStateFlow()

    // Состояние фильтров
    private val _filterState = MutableStateFlow(HabitsFilterState())
    val filterState = _filterState.asStateFlow()

    // Список всех привычек
    private val _habits = MutableStateFlow<List<HabitWithProgress>>(emptyList())

    // Категории
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    // Выбранная дата
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    // Начало текущей недели (needed for weekly navigation)
    private val _weekStartDate = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val weekStartDate = _weekStartDate.asStateFlow()

    // Общий прогресс привычек на выбранную дату
    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress = _overallProgress.asStateFlow()

    // Отфильтрованные привычки
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredHabits: StateFlow<List<HabitWithProgress>> = combine(
        _habits,
        _filterState,
        _selectedDate
    ) { habits, filterState, selectedDate ->
        habits.filter { habitWithProgress ->
            // Фильтрация по статусу
            val statusMatch = when (filterState.statusFilter) {
                HabitStatusFilter.ALL -> true
                HabitStatusFilter.ACTIVE -> habitWithProgress.habit.status == HabitStatus.ACTIVE
                HabitStatusFilter.ARCHIVED -> habitWithProgress.habit.status == HabitStatus.ARCHIVED
            }

            // Фильтрация по поисковому запросу
            val searchMatch = if (filterState.searchQuery.isNotBlank()) {
                habitWithProgress.habit.title.contains(filterState.searchQuery, ignoreCase = true) ||
                        habitWithProgress.habit.description?.contains(filterState.searchQuery, ignoreCase = true) == true
            } else true

            // Фильтрация по категории
            val categoryMatch = filterState.selectedCategoryId?.let { categoryId ->
                habitWithProgress.habit.categoryId == categoryId
            } ?: true

            statusMatch && searchMatch && categoryMatch
        }.let { filteredHabits ->
            // Сортировка
            when (filterState.sortOrder) {
                SortOrder.NAME_ASC -> filteredHabits.sortedBy { it.habit.title }
                SortOrder.NAME_DESC -> filteredHabits.sortedByDescending { it.habit.title }
                SortOrder.STREAK_ASC -> filteredHabits.sortedBy { it.habit.currentStreak }
                SortOrder.STREAK_DESC -> filteredHabits.sortedByDescending { it.habit.currentStreak }
                SortOrder.PROGRESS_ASC -> filteredHabits.sortedBy { it.currentProgress }
                SortOrder.PROGRESS_DESC -> filteredHabits.sortedByDescending { it.currentProgress }
                SortOrder.CREATION_DATE_ASC -> filteredHabits.sortedBy { it.habit.creationDate }
                SortOrder.CREATION_DATE_DESC -> filteredHabits.sortedByDescending { it.habit.creationDate }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadHabits()
        loadCategories()
    }

    /**
     * Загружает привычки и их прогресс
     */
    private fun loadHabits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            habitRepository.getAllHabits().collect { habits ->
                val habitsWithProgress = habits.map { habit ->
                    val dateMillis = _selectedDate.value
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    val progress = habitRepository.calculateHabitProgress(habit.id, dateMillis)
                    val tracking = habitRepository.getHabitTrackingForDate(habit.id, dateMillis)

                    // Загружаем завершенные даты для этой привычки
                    // за последние 7 дней (или другой период по вашему выбору)
                    val today = LocalDate.now()
                    val dateRange = (-2..4).map { today.plusDays(it.toLong()) }

                    val completedDates = dateRange.filter { date ->
                        val dateMills = date.atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        val trackingForDate = habitRepository.getHabitTrackingForDate(habit.id, dateMills)
                        trackingForDate?.isCompleted == true
                    }.toSet()

                    HabitWithProgress(
                        habit = habit,
                        currentProgress = progress,
                        todayTracking = tracking,
                        isCompletedToday = tracking?.isCompleted ?: false,
                        completedDates = completedDates
                    )
                }

                _habits.value = habitsWithProgress
                calculateOverallProgress(habitsWithProgress)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Загружает категории для фильтров
     */
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _categories.value = cats
            }
        }
    }

    /**
     * Вычисляет общий прогресс привычек
     */
    private fun calculateOverallProgress(habits: List<HabitWithProgress>) {
        if (habits.isEmpty()) {
            _overallProgress.value = 0f
            return
        }

        val activeHabits = habits.filter { it.habit.status == HabitStatus.ACTIVE }
        if (activeHabits.isEmpty()) {
            _overallProgress.value = 0f
            return
        }

        val totalProgress = activeHabits.sumOf { it.currentProgress.toDouble() }
        _overallProgress.value = (totalProgress / activeHabits.size).toFloat()
    }

    /**
     * Обрабатывает намерения пользователя
     */
    fun handleIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.SetViewMode -> setViewMode(intent.viewMode)
            is HabitsIntent.SetStatusFilter -> setStatusFilter(intent.statusFilter)
            is HabitsIntent.SetSortOrder -> setSortOrder(intent.sortOrder)
            is HabitsIntent.SetSelectedDate -> setSelectedDate(intent.date)
            is HabitsIntent.SetSearchQuery -> setSearchQuery(intent.query)
            is HabitsIntent.SelectCategory -> selectCategory(intent.categoryId)
            is HabitsIntent.ClearFilters -> clearFilters()
            is HabitsIntent.ToggleHabitCompletion -> toggleHabitCompletion(intent.habitId)
            is HabitsIntent.IncrementHabitProgress -> incrementHabitProgress(intent.habitId)
            is HabitsIntent.DecrementHabitProgress -> decrementHabitProgress(intent.habitId)
            is HabitsIntent.ArchiveHabit -> archiveHabit(intent.habitId)
            // Add new navigation intents
            is HabitsIntent.NavigateToPreviousWeek -> navigateToPreviousWeek()
            is HabitsIntent.NavigateToNextWeek -> navigateToNextWeek()
            is HabitsIntent.JumpToCurrentWeek -> jumpToCurrentWeek()
        }
    }

    private fun setViewMode(viewMode: HabitViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    private fun setStatusFilter(statusFilter: HabitStatusFilter) {
        _filterState.update { it.copy(statusFilter = statusFilter) }
    }

    private fun setSortOrder(sortOrder: SortOrder) {
        _filterState.update { it.copy(sortOrder = sortOrder) }
    }

    private fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date

        // Check if the selected date is outside the current displayed week
        val currentWeekStart = _weekStartDate.value
        val currentWeekEnd = currentWeekStart.plusDays(6)

        if (date.isBefore(currentWeekStart) || date.isAfter(currentWeekEnd)) {
            // Adjust the week to include the selected date
            _weekStartDate.value = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

        refreshHabitsForSelectedDate()
    }

    private fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    private fun selectCategory(categoryId: String?) {
        _filterState.update { it.copy(selectedCategoryId = categoryId) }
    }

    private fun clearFilters() {
        _filterState.value = HabitsFilterState()
    }

    /**
     * Перемещается на предыдущую неделю
     */
    private fun navigateToPreviousWeek() {
        _weekStartDate.value = _weekStartDate.value.minusWeeks(1)
        _selectedDate.value = _weekStartDate.value
        refreshHabitsForSelectedDate()
    }

    /**
     * Перемещается на следующую неделю
     */
    private fun navigateToNextWeek() {
        _weekStartDate.value = _weekStartDate.value.plusWeeks(1)
        _selectedDate.value = _weekStartDate.value
        refreshHabitsForSelectedDate()
    }

    /**
     * Переходит к текущей неделе
     */
    private fun jumpToCurrentWeek() {
        val today = LocalDate.now()
        _weekStartDate.value = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        _selectedDate.value = today
        refreshHabitsForSelectedDate()
    }

    /**
     * Отмечает привычку как выполненную или не выполненную
     */
    private fun toggleHabitCompletion(habitId: String) {
        viewModelScope.launch {
            val dateMillis = _selectedDate.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val tracking = habitRepository.getHabitTrackingForDate(habitId, dateMillis)
            val habit = habitRepository.getHabitById(habitId) ?: return@launch

            if (tracking != null) {
                // Если запись уже существует, просто инвертируем статус выполнения
                val updatedTracking = tracking.copy(
                    isCompleted = !tracking.isCompleted
                )
                habitRepository.updateHabitTracking(updatedTracking)
            } else {
                // Создаем новую запись
                val newTracking = HabitTracking(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = dateMillis,
                    isCompleted = true,
                    value = if (habit.type == HabitType.QUANTITY) habit.targetValue else null,
                    duration = if (habit.type == HabitType.TIME) habit.targetValue?.toInt() else null
                )
                habitRepository.trackHabit(newTracking)
            }

            // Обновляем данные
            refreshHabitsForSelectedDate()
        }
    }

    /**
     * Увеличивает прогресс привычки
     */
    private fun incrementHabitProgress(habitId: String) {
        viewModelScope.launch {
            val dateMillis = _selectedDate.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            habitRepository.incrementHabitProgress(habitId, dateMillis)
            refreshHabitsForSelectedDate()
        }
    }

    private fun decrementHabitProgress(habitId: String) {
        viewModelScope.launch {
            val dateMillis = _selectedDate.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            habitRepository.decrementHabitProgress(habitId, dateMillis)
            refreshHabitsForSelectedDate()
        }
    }

    /**
     * Архивирует привычку
     */
    private fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.changeHabitStatus(habitId, HabitStatus.ARCHIVED)
            // Данные обновятся автоматически через flow
        }
    }

    /**
     * Обновляет данные привычек для выбранной даты
     */
    private fun refreshHabitsForSelectedDate() {
        viewModelScope.launch {
            val dateMillis = _selectedDate.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Обновляем прогресс для каждой привычки
            val updatedHabits = _habits.value.map { habitWithProgress ->
                val progress = habitRepository.calculateHabitProgress(
                    habitWithProgress.habit.id,
                    dateMillis
                )
                val tracking = habitRepository.getHabitTrackingForDate(
                    habitWithProgress.habit.id,
                    dateMillis
                )

                habitWithProgress.copy(
                    currentProgress = progress,
                    todayTracking = tracking,
                    isCompletedToday = tracking?.isCompleted ?: false
                )
            }

            _habits.value = updatedHabits
            calculateOverallProgress(updatedHabits)
        }
    }
}
// Классы состояний UI и намерений
data class HabitsUiState(
    val isLoading: Boolean = false,
    val viewMode: HabitViewMode = HabitViewMode.LIST
)

data class HabitsFilterState(
    val statusFilter: HabitStatusFilter = HabitStatusFilter.ACTIVE,
    val sortOrder: SortOrder = SortOrder.CREATION_DATE_DESC,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null
)

// Модель привычки с её прогрессом
data class HabitWithProgress(
    val habit: Habit,
    val currentProgress: Float = 0f,
    val todayTracking: HabitTracking? = null,
    val isCompletedToday: Boolean = false,
    val completedDates: Set<LocalDate> = emptySet() // Добавляем это свойство
)

// Настройки отображения
enum class HabitViewMode {
    LIST, GRID, CATEGORIES
}




// Фильтры по статусу
enum class HabitStatusFilter {
    ALL, ACTIVE, ARCHIVED
}

// Сортировка
enum class SortOrder {
    NAME_ASC, NAME_DESC,
    STREAK_ASC, STREAK_DESC,
    PROGRESS_ASC, PROGRESS_DESC,
    CREATION_DATE_ASC, CREATION_DATE_DESC
}

// Намерения пользователя
sealed class HabitsIntent {
    data class SetViewMode(val viewMode: HabitViewMode) : HabitsIntent()
    data class SetStatusFilter(val statusFilter: HabitStatusFilter) : HabitsIntent()
    data class SetSortOrder(val sortOrder: SortOrder) : HabitsIntent()
    data class SetSelectedDate(val date: LocalDate) : HabitsIntent()
    data class SetSearchQuery(val query: String) : HabitsIntent()
    data class SelectCategory(val categoryId: String?) : HabitsIntent()
    object ClearFilters : HabitsIntent()
    data class ToggleHabitCompletion(val habitId: String) : HabitsIntent()
    data class IncrementHabitProgress(val habitId: String) : HabitsIntent()
    data class DecrementHabitProgress(val habitId: String) : HabitsIntent()
    data class ArchiveHabit(val habitId: String) : HabitsIntent()
    // Add new navigation intents
    object NavigateToPreviousWeek : HabitsIntent()
    object NavigateToNextWeek : HabitsIntent()
    object JumpToCurrentWeek : HabitsIntent()
}