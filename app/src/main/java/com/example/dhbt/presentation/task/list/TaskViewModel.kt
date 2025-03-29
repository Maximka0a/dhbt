package com.example.dhbt.presentation.task.list

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // State declarations remain the same
    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _datesWithTasks = MutableStateFlow<Set<LocalDate>>(emptySet())
    val datesWithTasks: StateFlow<Set<LocalDate>> = _datesWithTasks.asStateFlow()

    init {
        loadCategories()
        loadTags()
        observeTasks()
    }

    // Loading methods remain the same
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByType(CategoryType.TASK)
                .collect { taskCategories ->
                    _categories.value = taskCategories
                }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags()
                .collect { allTags ->
                    _tags.value = allTags
                }
        }
    }

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

    // Modified to apply multiple filters together
// Modified tag filtering logic
    private fun applyFilters(filters: TaskFilterState): Flow<List<Task>> {
        return taskRepository.getAllTasks().map { allTasks ->
            var filteredTasks = allTasks

            // Apply date filter if selected
            if (filters.selectedDate != null) {
                val startOfDay = filters.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = filters.selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
                filteredTasks = filteredTasks.filter { task ->
                    val dueDate = task.dueDate ?: 0L
                    dueDate in startOfDay..endOfDay
                }
            }

            // Apply category filter if selected
            if (filters.selectedCategoryId != null) {
                filteredTasks = filteredTasks.filter { task ->
                    task.categoryId == filters.selectedCategoryId
                }
            }

            // Apply priority filter if selected
            if (filters.selectedPriority != null) {
                filteredTasks = filteredTasks.filter { task ->
                    task.priority == filters.selectedPriority
                }
            }

            // Apply status filter if selected
            if (filters.selectedStatus != null) {
                filteredTasks = filteredTasks.filter { task ->
                    task.status == filters.selectedStatus
                }
            }

            // Apply tag filters if selected - FIXED
            if (filters.selectedTagIds.isNotEmpty()) {
                filteredTasks = filteredTasks.filter { task ->
                    // Extract tag IDs from the tag objects and check if any match the selected IDs
                    task.tags.any { tag -> filters.selectedTagIds.contains(tag.id) }
                }
            }

            // Apply search query if not empty
            if (filters.searchQuery.isNotEmpty()) {
                filteredTasks = filteredTasks.filter { task ->
                    task.title.contains(filters.searchQuery, ignoreCase = true) ||
                            (task.description?.contains(filters.searchQuery, ignoreCase = true) ?: false)
                }
            }

            filteredTasks
        }
    }

    // Sorting remains the same
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

    private fun updateDatesWithTasks(tasks: List<Task>) {
        val dates = tasks
            .mapNotNull { task -> task.dueDate }
            .map { millis ->
                LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
            }
            .toSet()

        _datesWithTasks.value = dates
    }

    // Modified filter methods to not reset other filters

    fun onDateSelected(date: LocalDate?) {
        _filterState.update { it.copy(selectedDate = date) }
    }

    fun onCategorySelected(categoryId: String?) {
        _filterState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onStatusSelected(status: TaskStatus?) {
        _filterState.update { it.copy(selectedStatus = status) }
    }

    fun onPrioritySelected(priority: TaskPriority?) {
        _filterState.update { it.copy(selectedPriority = priority) }
    }

    fun onTagSelected(tagId: String) {
        _filterState.update {
            val tagIds = if (it.selectedTagIds.contains(tagId)) {
                it.selectedTagIds - tagId
            } else {
                it.selectedTagIds + tagId
            }
            it.copy(selectedTagIds = tagIds)
        }
    }

    // Other methods remain the same
    fun onSortOptionSelected(sortOption: SortOption) {
        _filterState.update { it.copy(sortOption = sortOption) }
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

    // Task actions remain the same
    fun onTaskStatusChanged(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE
            taskRepository.updateTaskStatus(taskId, status)
        }
    }
    // Add this new method for direct status toggling
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            try {
                // Get the current task
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    // Determine current status and toggle to the opposite
                    val newStatus = if (task.status == TaskStatus.COMPLETED)
                        TaskStatus.ACTIVE
                    else
                        TaskStatus.COMPLETED

                    taskRepository.updateTaskStatus(taskId, newStatus)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Error updating task status: ${e.message}")
                }
            }
        }
    }

    // Добавьте это поле в TasksViewModel
    private var lastDeletedTask: Task? = null

    // Метод для восстановления удаленной задачи
    fun restoreDeletedTask() {
        viewModelScope.launch {
            lastDeletedTask?.let { task ->
                try {
                    // Используем существующий метод addTask
                    taskRepository.addTask(task)
                    // Очищаем сохраненную задачу после восстановления
                    lastDeletedTask = null
                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = "Ошибка при восстановлении задачи: ${e.message}")
                    }
                }
            }
        }
    }

    // Модифицированный метод onDeleteTask
    fun onDeleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                // Сохраняем задачу перед удалением
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    lastDeletedTask = task
                    // Теперь удаляем задачу
                    taskRepository.deleteTask(taskId)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Ошибка при удалении задачи: ${e.message}")
                }
            }
        }
    }

    fun onArchiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.ARCHIVED)
        }
    }

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