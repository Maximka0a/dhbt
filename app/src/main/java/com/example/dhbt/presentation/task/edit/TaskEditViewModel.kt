package com.example.dhbt.presentation.task.edit

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Получаем ID задачи из аргументов навигации, если есть
    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _state = mutableStateOf(TaskEditState())
    val state: State<TaskEditState> = _state

    private val _categories = mutableStateOf<List<Category>>(emptyList())
    val categories: State<List<Category>> = _categories

    private val _allTags = mutableStateOf<List<Tag>>(emptyList())
    val allTags: State<List<Tag>> = _allTags

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _showCategoryDialog = mutableStateOf(false)
    val showCategoryDialog: State<Boolean> = _showCategoryDialog

    private val _showTagDialog = mutableStateOf(false)
    val showTagDialog: State<Boolean> = _showTagDialog

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    private val _showTimePicker = mutableStateOf(false)
    val showTimePicker: State<Boolean> = _showTimePicker

    private val _showRecurrenceDialog = mutableStateOf(false)
    val showRecurrenceDialog: State<Boolean> = _showRecurrenceDialog

    private val _validationErrors = mutableStateOf<Map<TaskEditField, String>>(emptyMap())
    val validationErrors: State<Map<TaskEditField, String>> = _validationErrors

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Загружаем категории и теги
            loadCategories()
            loadTags()

            // Если это редактирование существующей задачи
            if (taskId.isNotEmpty()) {
                loadTask(taskId)
            }

            _isLoading.value = false
        }
    }

    private suspend fun loadTask(id: String) {
        val task = taskRepository.getTaskById(id)
        task?.let {
            // Заполняем состояние данными существующей задачи
            _state.value = TaskEditState(
                id = it.id,
                title = it.title,
                description = it.description.orEmpty(),
                categoryId = it.categoryId,
                color = it.color,
                dueDate = it.dueDate?.let { date ->
                    LocalDate.ofEpochDay(date / (24 * 60 * 60 * 1000))
                },
                dueTime = it.dueTime?.let { time ->
                    val parts = time.split(":").map { part -> part.toInt() }
                    if (parts.size >= 2) {
                        LocalTime.of(parts[0], parts[1])
                    } else {
                        null
                    }
                },
                duration = it.duration,
                priority = it.priority,
                subtasks = it.subtasks.toMutableList(),
                tags = it.tags.map { tag -> tag.id }.toMutableList(),
                eisenhowerQuadrant = it.eisenhowerQuadrant,
                estimatedPomodoroSessions = it.estimatedPomodoroSessions,
                recurrence = it.recurrence
            )
        }
    }

    private suspend fun loadCategories() {
        categoryRepository.getCategoriesByType(CategoryType.TASK).collect {
            _categories.value = it
        }
    }

    private suspend fun loadTags() {
        tagRepository.getAllTags().collect {
            _allTags.value = it
        }
    }

    // Функции обработки событий пользовательского интерфейса

    fun onTitleChanged(title: String) {
        _state.value = state.value.copy(title = title)
        validateField(TaskEditField.TITLE)
    }

    fun onDescriptionChanged(description: String) {
        _state.value = state.value.copy(description = description)
    }

    fun onCategorySelected(categoryId: String?) {
        _state.value = state.value.copy(categoryId = categoryId)
    }

    fun onColorSelected(color: String?) {
        _state.value = state.value.copy(color = color)
    }

    fun onDueDateSelected(date: LocalDate?) {
        _state.value = state.value.copy(dueDate = date)
    }

    fun onDueTimeSelected(time: LocalTime?) {
        _state.value = state.value.copy(dueTime = time)
    }

    fun onDurationChanged(duration: Int?) {
        _state.value = state.value.copy(duration = duration)
    }

    fun onPriorityChanged(priority: TaskPriority) {
        _state.value = state.value.copy(priority = priority)
    }

    fun onEisenhowerQuadrantChanged(quadrant: Int?) {
        _state.value = state.value.copy(eisenhowerQuadrant = quadrant)
    }

    fun onEstimatedPomodoroSessionsChanged(sessions: Int?) {
        _state.value = state.value.copy(estimatedPomodoroSessions = sessions)
    }

    // Управление подзадачами
    fun onAddSubtask(title: String) {
        if (title.isBlank()) return
        val newSubtask = Subtask(
            id = UUID.randomUUID().toString(),
            taskId = state.value.id,
            title = title,
            isCompleted = false,
            order = state.value.subtasks.size
        )
        _state.value = state.value.copy(
            subtasks = state.value.subtasks.toMutableList().apply { add(newSubtask) }
        )
    }

    fun onUpdateSubtask(subtaskId: String, title: String) {
        val subtasks = state.value.subtasks.toMutableList()
        val index = subtasks.indexOfFirst { it.id == subtaskId }
        if (index != -1) {
            subtasks[index] = subtasks[index].copy(title = title)
            _state.value = state.value.copy(subtasks = subtasks)
        }
    }

    fun onDeleteSubtask(subtaskId: String) {
        val subtasks = state.value.subtasks.toMutableList()
        subtasks.removeIf { it.id == subtaskId }
        _state.value = state.value.copy(subtasks = subtasks)
    }

    fun onToggleSubtaskCompletion(subtaskId: String, isCompleted: Boolean) {
        val subtasks = state.value.subtasks.toMutableList()
        val index = subtasks.indexOfFirst { it.id == subtaskId }
        if (index != -1) {
            subtasks[index] = subtasks[index].copy(isCompleted = isCompleted)
            _state.value = state.value.copy(subtasks = subtasks)
        }
    }

    // Управление тегами
    fun onToggleTag(tagId: String) {
        val tags = state.value.tags.toMutableList()
        if (tags.contains(tagId)) {
            tags.remove(tagId)
        } else {
            tags.add(tagId)
        }
        _state.value = state.value.copy(tags = tags)
    }

    // Создание новой категории
    fun onCreateCategory(name: String, color: String?, iconEmoji: String?) {
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color,
                iconEmoji = iconEmoji,
                type = CategoryType.TASK
            )
            val categoryId = categoryRepository.addCategory(newCategory)
            _state.value = state.value.copy(categoryId = categoryId)
            loadCategories() // Перезагружаем список категорий
        }
    }

    // Создание нового тега
    fun onCreateTag(name: String, color: String?) {
        viewModelScope.launch {
            val newTag = Tag(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color
            )
            val tagId = tagRepository.addTag(newTag)
            val tags = state.value.tags.toMutableList().apply { add(tagId) }
            _state.value = state.value.copy(tags = tags)
            loadTags() // Перезагружаем список тегов
        }
    }

    // Настройка повторения задачи
    fun onRecurrenceChanged(recurrence: TaskRecurrence?) {
        _state.value = state.value.copy(recurrence = recurrence)
    }

    // Управление диалогами
    fun onToggleCategoryDialog() {
        _showCategoryDialog.value = !_showCategoryDialog.value
    }

    fun onToggleTagDialog() {
        _showTagDialog.value = !_showTagDialog.value
    }

    fun onToggleDatePicker() {
        _showDatePicker.value = !_showDatePicker.value
    }

    fun onToggleTimePicker() {
        _showTimePicker.value = !_showTimePicker.value
    }

    fun onToggleRecurrenceDialog() {
        _showRecurrenceDialog.value = !_showRecurrenceDialog.value
    }

    // Сохранение задачи
    fun saveTask(onSuccess: () -> Unit) {
        if (!validateAllFields()) return

        viewModelScope.launch {
            val task = createTaskFromState()

            try {
                if (state.value.id.isEmpty()) {
                    // Создание новой задачи
                    taskRepository.addTask(task)
                } else {
                    // Обновление существующей задачи
                    taskRepository.updateTask(task)
                }

                onSuccess()
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    private fun createTaskFromState(): Task {
        val currentState = state.value

        // Конвертация LocalDate в миллисекунды для хранения
        val dueDate = currentState.dueDate?.let {
            it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        // Конвертация LocalTime в строку формата "HH:MM"
        val dueTime = currentState.dueTime?.let {
            String.format("%02d:%02d", it.hour, it.minute)
        }

        // Сопоставляем ID тегов с объектами Tag
        val tagObjects = currentState.tags.mapNotNull { tagId ->
            allTags.value.find { it.id == tagId }
        }

        return Task(
            id = if (currentState.id.isEmpty()) UUID.randomUUID().toString() else currentState.id,
            title = currentState.title,
            description = if (currentState.description.isBlank()) null else currentState.description,
            categoryId = currentState.categoryId,
            color = currentState.color,
            creationDate = if (currentState.id.isEmpty()) System.currentTimeMillis() else currentState.creationDate,
            dueDate = dueDate,
            dueTime = dueTime,
            duration = currentState.duration,
            priority = currentState.priority,
            status = TaskStatus.ACTIVE,
            completionDate = null,
            eisenhowerQuadrant = currentState.eisenhowerQuadrant,
            estimatedPomodoroSessions = currentState.estimatedPomodoroSessions,
            subtasks = currentState.subtasks,
            tags = tagObjects,
            recurrence = currentState.recurrence?.let {
                if (currentState.id.isEmpty() || it.taskId.isEmpty()) {
                    it.copy(taskId = currentState.id, id = UUID.randomUUID().toString())
                } else {
                    it
                }
            }
        )
    }

    // Валидация полей
    private fun validateAllFields(): Boolean {
        val errors = mutableMapOf<TaskEditField, String>()

        if (state.value.title.isBlank()) {
            errors[TaskEditField.TITLE] = "Название не может быть пустым"
        }

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    private fun validateField(field: TaskEditField) {
        val errors = _validationErrors.value.toMutableMap()

        when (field) {
            TaskEditField.TITLE -> {
                if (state.value.title.isBlank()) {
                    errors[TaskEditField.TITLE] = "Название не может быть пустым"
                } else {
                    errors.remove(TaskEditField.TITLE)
                }
            }
            else -> {}
        }

        _validationErrors.value = errors
    }
}

// Перечисление полей формы для валидации
enum class TaskEditField {
    TITLE,
    DESCRIPTION,
    DUE_DATE,
    DUE_TIME,
    DURATION,
    PRIORITY,
    CATEGORY,
    TAGS,
    EISENHOWER_QUADRANT,
    POMODORO_SESSIONS,
    RECURRENCE
}

// Состояние редактирования задачи
data class TaskEditState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val color: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val duration: Int? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<String> = emptyList(),
    val eisenhowerQuadrant: Int? = null,
    val estimatedPomodoroSessions: Int? = null,
    val recurrence: TaskRecurrence? = null
)

// Модель для создания повторяющейся задачи
data class RecurrenceModel(
    val type: Int = 0,
    val daysOfWeek: List<Int> = emptyList(),
    val monthDay: Int? = null,
    val interval: Int = 1,
    val endDate: LocalDate? = null
)