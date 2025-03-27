package com.example.dhbt.presentation.task.list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // Состояние фильтрации
    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    // Состояние UI экрана
    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    // Категории для фильтрации
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // Теги для фильтрации
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    // Даты с задачами для отображения в календаре
    private val _datesWithTasks = MutableStateFlow<Set<LocalDate>>(emptySet())
    val datesWithTasks: StateFlow<Set<LocalDate>> = _datesWithTasks.asStateFlow()

    init {
        loadCategories()
        loadTags()
        observeTasks()
    }

    // Загрузка категорий
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByType(CategoryType.TASK)
                .collect { taskCategories ->
                    _categories.value = taskCategories
                }
        }
    }

    // Загрузка тегов
    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags()
                .collect { allTags ->
                    _tags.value = allTags
                }
        }
    }

    // Наблюдение за задачами с применением фильтров
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTasks() {
        filterState
            .flatMapLatest { filters ->
                applyFilters(filters)
            }
            .onEach { tasks ->
                updateDatesWithTasks(tasks)
                _state.update {
                    it.copy(
                        isLoading = false,
                        tasks = applySorting(tasks, filterState.value.sortOption),
                        error = null
                    )
                }
            }
            .catch { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Ошибка при загрузке задач"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // Применение фильтров к потоку задач
    private fun applyFilters(filters: TaskFilterState): Flow<List<Task>> {
        return when {
            filters.selectedDate != null -> {
                // Фильтр по дате
                val startOfDay = filters.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = filters.selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
                taskRepository.getAllTasks().map { tasks ->
                    tasks.filter { task ->
                        val dueDate = task.dueDate ?: 0L
                        dueDate in startOfDay..endOfDay
                    }
                }
            }
            filters.selectedCategoryId != null -> {
                // Фильтр по категории
                taskRepository.getTasksByCategory(filters.selectedCategoryId)
            }
            filters.selectedPriority != null -> {
                // Фильтр по приоритету
                taskRepository.getTasksByPriority(filters.selectedPriority)
            }
            filters.selectedStatus != null -> {
                // Фильтр по статусу
                taskRepository.getTasksByStatus(filters.selectedStatus)
            }
            filters.selectedTagIds.isNotEmpty() -> {
                // Фильтр по тегам
                taskRepository.getTasksWithTags(filters.selectedTagIds)
            }
            filters.searchQuery.isNotEmpty() -> {
                // Поиск по запросу
                taskRepository.getAllTasks().map { tasks ->
                    tasks.filter { task ->
                        task.title.contains(filters.searchQuery, ignoreCase = true) ||
                                (task.description?.contains(filters.searchQuery, ignoreCase = true) ?: false)
                    }
                }
            }
            filters.showEisenhowerMatrix -> {
                // Режим матрицы Эйзенхауэра - возвращаем все задачи, сортировку сделаем отдельно
                taskRepository.getAllTasks()
            }
            else -> {
                // Без фильтров - все задачи
                taskRepository.getAllTasks()
            }
        }
    }

    // Сортировка задач
    private fun applySorting(tasks: List<Task>, sortOption: SortOption): List<Task> {
        return when (sortOption) {
            SortOption.DATE_ASC -> tasks.sortedBy { it.dueDate }
            SortOption.DATE_DESC -> tasks.sortedByDescending { it.dueDate }
            SortOption.PRIORITY_HIGH -> tasks.sortedByDescending { it.priority.value }
            SortOption.PRIORITY_LOW -> tasks.sortedBy { it.priority.value }
            SortOption.ALPHABETICAL -> tasks.sortedBy { it.title.lowercase() }
            SortOption.CREATION_DATE -> tasks.sortedBy { it.creationDate }
        }
    }

    // Обновление состояния календаря - находим даты с задачами
// In your TasksViewModel.kt
    private fun updateDatesWithTasks(tasks: List<Task>) {
        val dates = tasks
            .mapNotNull { task -> task.dueDate }
            .map { millis ->
                LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
            }
            .toSet()

        _datesWithTasks.value = dates
    }

    // Обработчики событий UI

    fun onDateSelected(date: LocalDate?) {
        _filterState.update { it.copy(selectedDate = date) }
    }

    fun onCategorySelected(categoryId: String?) {
        _filterState.update {
            it.copy(
                selectedCategoryId = categoryId,
                // Сбрасываем другие фильтры при выборе категории
                selectedStatus = null,
                selectedPriority = null,
                selectedTagIds = emptyList(),
                selectedDate = null
            )
        }
    }

    fun onStatusSelected(status: TaskStatus?) {
        _filterState.update {
            it.copy(
                selectedStatus = status,
                // Сбрасываем другие фильтры при выборе статуса
                selectedCategoryId = null,
                selectedPriority = null,
                selectedTagIds = emptyList(),
                selectedDate = null
            )
        }
    }

    fun onPrioritySelected(priority: TaskPriority?) {
        _filterState.update {
            it.copy(
                selectedPriority = priority,
                // Сбрасываем другие фильтры при выборе приоритета
                selectedCategoryId = null,
                selectedStatus = null,
                selectedTagIds = emptyList(),
                selectedDate = null
            )
        }
    }

    fun onTagSelected(tagId: String) {
        _filterState.update {
            val tagIds = if (it.selectedTagIds.contains(tagId)) {
                it.selectedTagIds - tagId
            } else {
                it.selectedTagIds + tagId
            }

            it.copy(
                selectedTagIds = tagIds,
                // Сбрасываем другие фильтры при выборе тегов
                selectedCategoryId = null,
                selectedStatus = null,
                selectedPriority = null,
                selectedDate = null
            )
        }
    }

    fun onSortOptionSelected(sortOption: SortOption) {
        _filterState.update { it.copy(sortOption = sortOption) }
        // Пересортируем задачи с новой опцией сортировки
        _state.update { currentState ->
            currentState.copy(
                tasks = applySorting(currentState.tasks, sortOption)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onToggleEisenhowerMatrix(show: Boolean) {
        _filterState.update { it.copy(showEisenhowerMatrix = show) }
    }

    fun resetFilters() {
        _filterState.value = TaskFilterState()
    }

    // Действия с задачами

    fun onTaskStatusChanged(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE
            taskRepository.updateTaskStatus(taskId, status)
        }
    }

    fun onDeleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun onArchiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.ARCHIVED)
        }
    }

    // Форматирование для UI
    fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM")
        return date.format(formatter)
    }
}

// Состояние экрана задач
data class TasksState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val isCalendarExpanded: Boolean = false
)


data class TaskFilterState(
    val searchQuery: String = "",
    val selectedDate: LocalDate? = null,
    val selectedCategoryId: String? = null,
    val selectedTagIds: List<String> = emptyList(),
    val selectedStatus: TaskStatus? = null,
    val selectedPriority: TaskPriority? = null,
    val sortOption: SortOption = SortOption.DATE_ASC,
    val showEisenhowerMatrix: Boolean = false
)

enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    PRIORITY_HIGH,
    PRIORITY_LOW,
    ALPHABETICAL,
    CREATION_DATE
}