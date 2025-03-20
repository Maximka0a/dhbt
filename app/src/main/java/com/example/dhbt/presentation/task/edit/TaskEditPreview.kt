package com.example.dhbt.presentation.task.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.presentation.theme.DHbtTheme
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.util.UUID

/**
 * Мок-версия ViewModel для использования в превью
 */
class MockTaskEditViewModel : ViewModel() {

    private val _state = mutableStateOf(
        TaskEditState(
            id = "task1",
            title = "Завершить отчет по проекту",
            description = "Необходимо подготовить финальную версию отчета и отправить руководству до конца недели",
            categoryId = "category1",
            color = "#4285F4",
            dueDate = LocalDate.now().plusDays(3),
            dueTime = LocalTime.of(14, 30),
            priority = TaskPriority.HIGH,
            subtasks = listOf(
                Subtask(
                    id = "sub1",
                    taskId = "task1",
                    title = "Собрать данные",
                    isCompleted = true
                ),
                Subtask(
                    id = "sub2",
                    taskId = "task1",
                    title = "Подготовить презентацию",
                    isCompleted = false
                ),
                Subtask(
                    id = "sub3",
                    taskId = "task1",
                    title = "Отправить отчет руководству",
                    isCompleted = false
                )
            ),
            tags = listOf("tag1", "tag3"),
            eisenhowerQuadrant = 1,
            estimatedPomodoroSessions = 3,
            recurrence = TaskRecurrence(
                id = UUID.randomUUID().toString(),
                taskId = "task1",
                type = RecurrenceType.WEEKLY,
                daysOfWeek = listOf(1, 3, 5),
                startDate = System.currentTimeMillis()
            )
        )
    )
    val state: State<TaskEditState> = _state

    private val _categories = mutableStateOf<List<Category>>(
        listOf(
            Category(
                id = "category1",
                name = "Работа",
                color = "#4285F4",
                iconEmoji = "💼",
                type =  CategoryType.TASK
            ),
            Category(
                id = "category2",
                name = "Личное",
                color = "#34A853",
                iconEmoji = "🏠",
                type =  CategoryType.TASK
            ),
            Category(
                id = "category3",
                name = "Здоровье",
                color = "#EA4335",
                iconEmoji = "🏃",
                type =  CategoryType.TASK
            )
        )
    )
    val categories: State<List<Category>> = _categories

    private val _allTags = mutableStateOf<List<Tag>>(
        listOf(
            Tag(id = "tag1", name = "Срочно", color = "#F44336"),
            Tag(id = "tag2", name = "Проект X", color = "#2196F3"),
            Tag(id = "tag3", name = "Клиент", color = "#4CAF50"),
            Tag(id = "tag4", name = "Документация", color = "#FF9800")
        )
    )
    val allTags: State<List<Tag>> = _allTags

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    private val _showTimePicker = mutableStateOf(false)
    val showTimePicker: State<Boolean> = _showTimePicker

    private val _showCategoryDialog = mutableStateOf(false)
    val showCategoryDialog: State<Boolean> = _showCategoryDialog

    private val _showTagDialog = mutableStateOf(false)
    val showTagDialog: State<Boolean> = _showTagDialog

    private val _showRecurrenceDialog = mutableStateOf(false)
    val showRecurrenceDialog: State<Boolean> = _showRecurrenceDialog

    private val _validationErrors = mutableStateOf<Map<TaskEditField, String>>(emptyMap())
    val validationErrors: State<Map<TaskEditField, String>> = _validationErrors

    // Заглушки для функций, которые просто переключают состояния диалогов
    fun onToggleDatePicker() {
        _showDatePicker.value = !_showDatePicker.value
    }

    fun onToggleTimePicker() {
        _showTimePicker.value = !_showTimePicker.value
    }

    fun onToggleCategoryDialog() {
        _showCategoryDialog.value = !_showCategoryDialog.value
    }

    fun onToggleTagDialog() {
        _showTagDialog.value = !_showTagDialog.value
    }

    fun onToggleRecurrenceDialog() {
        _showRecurrenceDialog.value = !_showRecurrenceDialog.value
    }

    // Заглушки для функций, которые не делают ничего в превью
    fun onTitleChanged(title: String) {}

    fun onDescriptionChanged(description: String) {}

    fun onDueDateSelected(date: LocalDate?) {}

    fun onDueTimeSelected(time: LocalTime?) {}

    fun onCategorySelected(categoryId: String?) {}

    fun onPriorityChanged(priority: TaskPriority) {}

    fun onToggleTag(tagId: String) {}

    fun onAddSubtask(title: String) {}

    fun onUpdateSubtask(id: String, title: String) {}

    fun onDeleteSubtask(id: String) {}

    fun onToggleSubtaskCompletion(id: String, isCompleted: Boolean) {}

    fun onColorSelected(color: String?) {}

    fun onEisenhowerQuadrantChanged(quadrant: Int?) {}

    fun onEstimatedPomodoroSessionsChanged(sessions: Int?) {}

    fun onRecurrenceChanged(recurrence: TaskRecurrence?) {}

    fun onCreateCategory(name: String, color: String?, emoji: String?) {}

    fun onCreateTag(name: String, color: String?) {}

    fun saveTask(onSuccess: () -> Unit) {}
}

@Composable
fun PreviewTaskEditScreen() {
    val viewModel = MockTaskEditViewModel()

    DHbtTheme {
        TaskEditScreen(
            taskId = "1233",
            onNavigateBack = {}
        )
    }
}