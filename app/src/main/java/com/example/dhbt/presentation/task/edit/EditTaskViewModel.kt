package com.example.dhbt.presentation.task.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.*
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Не извлекаем taskId из savedStateHandle изначально
    private var _taskId: String? = null

    private val _uiState = MutableStateFlow(EditTaskUiState())
    val uiState = _uiState.asStateFlow()

    private val _categoryState = MutableStateFlow<List<Category>>(emptyList())
    val categoryState = _categoryState.asStateFlow()

    private val _tagsState = MutableStateFlow<List<Tag>>(emptyList())
    val tagsState = _tagsState.asStateFlow()

    private val _subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val subtasks = _subtasks.asStateFlow()

    private val _events = MutableSharedFlow<EditTaskEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            loadCategories()
            loadTags()
        }
    }

    fun setTaskId(taskId: String) {
        // Убираем проверку на совпадение с предыдущим значением taskId
        if (taskId.isNotEmpty()) {
            _taskId = taskId
            viewModelScope.launch {
                loadTaskData(taskId)
            }
        }
    }

    private suspend fun loadCategories() {
        categoryRepository.getCategoriesByType(CategoryType.TASK)
            .collect { categories ->
                _categoryState.value = categories
            }
    }

    private suspend fun loadTags() {
        tagRepository.getAllTags()
            .collect { tags ->
                _tagsState.value = tags
            }
    }

    private suspend fun loadTaskData(taskId: String) {
        // Добавляем индикатор загрузки
        _uiState.update { it.copy(isLoading = true) }

        try {
            val task = taskRepository.getTaskById(taskId)
            task?.let { existingTask ->
                _uiState.update { currentState ->
                    currentState.copy(
                        title = existingTask.title,
                        description = existingTask.description ?: "",
                        color = existingTask.color,
                        categoryId = existingTask.categoryId,
                        dueDate = existingTask.dueDate?.let {
                            LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                        },
                        dueTime = existingTask.dueTime?.let {
                            val parts = it.split(":")
                            if (parts.size == 2) {
                                LocalTime.of(parts[0].toInt(), parts[1].toInt())
                            } else null
                        },
                        duration = existingTask.duration,
                        priority = existingTask.priority,
                        eisenhowerQuadrant = existingTask.eisenhowerQuadrant,
                        estimatedPomodoros = existingTask.estimatedPomodoroSessions,
                        isEditing = true,
                        isLoading = false
                    )
                }

                // Load subtasks
                taskRepository.getSubtasksForTask(taskId).collect { loadedSubtasks ->
                    _subtasks.value = loadedSubtasks
                }

                // Load tags
                taskRepository.getTagsForTask(taskId).collect { loadedTags ->
                    _uiState.update { it.copy(selectedTagIds = loadedTags.map { tag -> tag.id }.toSet()) }
                }

                // Load recurrence
                task.recurrence?.let { loadedRecurrence ->
                    _uiState.update { it.copy(
                        recurrenceType = loadedRecurrence.type,
                        daysOfWeek = loadedRecurrence.daysOfWeek?.toSet() ?: emptySet(),
                        monthDay = loadedRecurrence.monthDay,
                        customInterval = loadedRecurrence.customInterval
                    ) }
                }
            } ?: run {
                // Задача не найдена, сбрасываем статус загрузки
                _uiState.update { it.copy(isLoading = false) }
            }
        } catch (e: Exception) {
            // Обработка ошибок загрузки
            _uiState.update { it.copy(isLoading = false) }
            viewModelScope.launch {
                _events.emit(EditTaskEvent.ShowError("Ошибка загрузки задачи: ${e.message}"))
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onColorSelected(color: String) {
        _uiState.update { it.copy(color = color) }
    }

    fun onDueDateChanged(date: LocalDate?) {
        _uiState.update { it.copy(dueDate = date) }
    }

    fun onDueTimeChanged(time: LocalTime?) {
        _uiState.update { it.copy(dueTime = time) }
    }

    fun onDurationChanged(duration: Int?) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun onPriorityChanged(priority: TaskPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onEisenhowerQuadrantChanged(quadrant: Int?) {
        _uiState.update { it.copy(eisenhowerQuadrant = quadrant) }
    }

    fun onEstimatedPomodorosChanged(count: Int?) {
        _uiState.update { it.copy(estimatedPomodoros = count) }
    }

    fun onTagToggled(tagId: String) {
        _uiState.update { currentState ->
            val currentTags = currentState.selectedTagIds.toMutableSet()
            if (currentTags.contains(tagId)) {
                currentTags.remove(tagId)
            } else {
                currentTags.add(tagId)
            }
            currentState.copy(selectedTagIds = currentTags)
        }
    }

    fun onRecurrenceTypeChanged(type: RecurrenceType?) {
        _uiState.update { it.copy(recurrenceType = type) }
    }

    fun onDayOfWeekToggled(dayOfWeek: Int) {
        _uiState.update { currentState ->
            val currentDays = currentState.daysOfWeek.toMutableSet()
            if (currentDays.contains(dayOfWeek)) {
                currentDays.remove(dayOfWeek)
            } else {
                currentDays.add(dayOfWeek)
            }
            currentState.copy(daysOfWeek = currentDays)
        }
    }

    fun onMonthDayChanged(day: Int?) {
        _uiState.update { it.copy(monthDay = day) }
    }

    fun onCustomIntervalChanged(interval: Int?) {
        _uiState.update { it.copy(customInterval = interval) }
    }

    fun addSubtask(title: String) {
        if (title.isNotBlank()) {
            val newSubtask = Subtask(
                id = UUID.randomUUID().toString(),
                taskId = _taskId ?: "",
                title = title,
                order = _subtasks.value.size
            )
            _subtasks.update { currentList -> currentList + newSubtask }

            // Clear the subtask input field by notifying the UI
            viewModelScope.launch {
                _events.emit(EditTaskEvent.ClearSubtaskInput)
            }
        }
    }

    fun removeSubtask(subtaskId: String) {
        _subtasks.update { currentList -> currentList.filterNot { it.id == subtaskId } }
    }

    fun toggleSubtaskCompletion(subtaskId: String) {
        _subtasks.update { currentList ->
            currentList.map {
                if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
            }
        }
    }

    fun onAddNewCategory(name: String, color: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color,
                type = CategoryType.TASK
            )
            val categoryId = categoryRepository.addCategory(newCategory)
            onCategorySelected(categoryId)
            loadCategories()
        }
    }

    fun onAddNewTag(name: String, color: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val newTag = Tag(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color
            )
            val tagId = tagRepository.addTag(newTag)
            onTagToggled(tagId)
            loadTags()
        }
    }

    fun saveTask() {
        val currentState = _uiState.value

        if (currentState.title.isBlank()) {
            viewModelScope.launch {
                _events.emit(EditTaskEvent.ShowError("Необходимо указать название задачи"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val dueDate = currentState.dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                val dueTime = currentState.dueTime?.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }

                // Create TaskRecurrence if needed
                val recurrence = if (currentState.recurrenceType != null) {
                    TaskRecurrence(
                        id = UUID.randomUUID().toString(),
                        taskId = _taskId ?: UUID.randomUUID().toString(),
                        type = currentState.recurrenceType,
                        daysOfWeek = currentState.daysOfWeek.toList(),
                        monthDay = currentState.monthDay,
                        customInterval = currentState.customInterval,
                        startDate = System.currentTimeMillis()
                    )
                } else null

                val task = Task(
                    id = _taskId ?: UUID.randomUUID().toString(),
                    title = currentState.title,
                    description = currentState.description.takeIf { it.isNotBlank() },
                    categoryId = currentState.categoryId,
                    color = currentState.color,
                    creationDate = System.currentTimeMillis(),
                    dueDate = dueDate,
                    dueTime = dueTime,
                    duration = currentState.duration,
                    priority = currentState.priority,
                    status = TaskStatus.ACTIVE,
                    eisenhowerQuadrant = currentState.eisenhowerQuadrant,
                    estimatedPomodoroSessions = currentState.estimatedPomodoros,
                    subtasks = _subtasks.value,
                    tags = currentState.selectedTagIds.mapNotNull { tagId ->
                        _tagsState.value.find { it.id == tagId }
                    },
                    recurrence = recurrence
                )

                // Create or update the task
                if (_taskId == null) {
                    taskRepository.addTask(task)
                } else {
                    taskRepository.updateTask(task)
                }

                _events.emit(EditTaskEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(EditTaskEvent.ShowError("Ошибка сохранения задачи: ${e.message}"))
            }
        }
    }

    fun onCancel() {
        viewModelScope.launch {
            _events.emit(EditTaskEvent.NavigateBack)
        }
    }
}

data class EditTaskUiState(
    val title: String = "",
    val description: String = "",
    val color: String? = null,
    val categoryId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val duration: Int? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val eisenhowerQuadrant: Int? = null,
    val estimatedPomodoros: Int? = null,
    val selectedTagIds: Set<String> = emptySet(),
    val recurrenceType: RecurrenceType? = null,
    val daysOfWeek: Set<Int> = emptySet(),
    val monthDay: Int? = null,
    val customInterval: Int? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
)

sealed class EditTaskEvent {
    object NavigateBack : EditTaskEvent()
    object ClearSubtaskInput : EditTaskEvent()
    data class ShowError(val message: String) : EditTaskEvent()
}